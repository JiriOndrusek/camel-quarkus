/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.component.djl.it;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.basicmodelzoo.basic.Mlp;
import ai.djl.modality.cv.transform.ToTensor;
import ai.djl.modality.cv.translator.ImageClassificationTranslator;
import ai.djl.translate.Pipeline;
import ai.djl.translate.TranslateException;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jboss.logging.Logger;

@Path("/djl")
@ApplicationScoped
public class DjlResource {

    public static final String URL_WITH_EXTERNAL_MODEL = "djl:cv/image_classification?artifactId=ai.djl.mxnet:mlp:0.0.1";
    public static final String URL_WITH_LOCAL_MODEL = "djl:cv/image_classification?model=MyModel&translator=MyTranslator";

    private static final String MODEL_DIR = "target/classes/models/mnist";
    private static final String MODEL_NAME = "mlp";

    public enum ModelType {
        local, external
    };

    private static final Logger LOG = Logger.getLogger(DjlResource.class);

    @Inject
    ProducerTemplate producerTemplate;

    @Inject
    CamelContext context;

    @Path("/classificate/{modelType}")
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.TEXT_PLAIN)
    public String classificate(@PathParam("modelType") String modelType, byte[] message) throws Exception {
        //load localModel if necessary
        boolean local = ModelType.valueOf(modelType) == ModelType.local;
        if (local && context.getRegistry().lookupByName("MyModel") == null) {
            loadLocalModel();
        }
        final Map<String, Float> response = producerTemplate.requestBody(local ? URL_WITH_LOCAL_MODEL : URL_WITH_EXTERNAL_MODEL,
                message, Map.class);
        //find recognitionn with highest probability
        String max = Collections.max(response.entrySet(), Comparator.comparingDouble(Map.Entry::getValue)).getKey();

        return max;
    }

    private void loadLocalModel() throws IOException, MalformedModelException, TranslateException {
        // create deep learning model
        Model model = Model.newInstance();
        model.setBlock(new Mlp(28 * 28, 10, new int[] { 128, 64 }));
        model.load(Paths.get(MODEL_DIR), MODEL_NAME);
        // create translator for pre-processing and postprocessing
        ImageClassificationTranslator.Builder builder = ImageClassificationTranslator.builder();
        builder.setSynsetArtifactName("synset.txt");
        builder.setPipeline(new Pipeline(new ToTensor()));
        builder.optApplySoftmax(true);
        ImageClassificationTranslator translator = new ImageClassificationTranslator(builder);

        // Bind model beans
        context.getRegistry().bind("MyModel", model);
        context.getRegistry().bind("MyTranslator", translator);
    }

}
