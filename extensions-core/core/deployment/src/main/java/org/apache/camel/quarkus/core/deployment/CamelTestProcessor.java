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
package org.apache.camel.quarkus.core.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelTestProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CamelTestProcessor.class);
    static String TEST_ANNOTATION_CLASS_NAME = "org.apache.camel.quarkus.test.CamelQuarkusTest";

    @BuildStep
    void produceTestAnnotationBuildItem(BuildProducer<TestAnnotationBuildItem> testAnnotationBuildItems) {
        testAnnotationBuildItems.produce(new TestAnnotationBuildItem(TEST_ANNOTATION_CLASS_NAME));
    }

    //    @BuildStep
    //    void annotationTransformerBuildItem(BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformerBuildItems) {
    //        annotationsTransformerBuildItems.produce(createAnnotationTransformer(null));
    //    }
    //
    //    private AnnotationsTransformerBuildItem createAnnotationTransformer(DotName className) {
    //        return new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {
    //            public boolean appliesTo(org.jboss.jandex.AnnotationTarget.Kind kind) {
    //                return kind == AnnotationTarget.Kind.CLASS;
    //            }
    //
    //            public void transform(TransformationContext context) {
    //
    //                if (context.getAnnotations().contains(
    //                        AnnotationInstance.create(
    //                                DotName.createSimple(CamelQuarkusTest.class.getName()),
    //                                context.getTarget(),
    //                                new AnnotationValue[] {}))) {
    //                    context.transform().add(AnnotationInstance.create(
    //                                    DotName.createSimple(TestProfile.class.getName()),
    //                                    context.getTarget(),
    //                                    new AnnotationValue[] { AnnotationValue.createClassValue("value",
    //                                            Type.create(context.getTarget().asClass().name(), Type.Kind.CLASS)) }))
    //                            .done();
    //                }
    //            }
    //        });
    //    }
}
