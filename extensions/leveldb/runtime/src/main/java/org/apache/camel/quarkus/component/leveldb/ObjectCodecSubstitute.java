package org.apache.camel.quarkus.component.leveldb;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oracle.svm.core.annotate.Inject;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.support.DefaultExchangeHolder;
import org.fusesource.hawtbuf.codec.ObjectCodec;

/**
 * This os workaround for serialization of DefaultExchangeHolder.
 * Once serialization is implemented in graalVM (see https://github.com/oracle/graal/issues/460), this substitution could
 * be removed.
 */
@TargetClass(value = ObjectCodec.class)
final class ObjectCodecSubstitute {

    @Inject
    private ObjectMapper objectMapper;

    @Substitute
    public void encode(Object object, DataOutput dataOut) throws IOException {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
            objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        }
        objectMapper.writeValueAsBytes(dataOut);
    }

    @Substitute
    public Object decode(DataInput dataIn) throws IOException {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
            objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        }
        return objectMapper.readValue(dataIn, DefaultExchangeHolder.class);
    }

}
