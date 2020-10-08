package org.apache.camel.quarkus.component.leveldb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.support.DefaultExchangeHolder;
import org.fusesource.hawtbuf.codec.ObjectCodec;

@TargetClass(value = ObjectCodec.class)
final class ObjectCodecSubstitute {

    @Substitute
    public void encode(Object object, DataOutput dataOut) throws IOException {
        System.out.println("******** serializing ***********");
        Jsonb jsonb = JsonbBuilder.create();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            jsonb.toJson(object, output);
            byte[] data = output.toByteArray();
            System.out.println("**************************** data length: " + data.length);
            System.out.println(new String(data));
            dataOut.writeInt(data.length);
            dataOut.write(data);
        }
    }

    @Substitute
    public Object decode(DataInput dataIn) throws IOException {
        //        todo remove System.out.println("******** deserializing ***********");
        int size = dataIn.readInt();
        byte[] data = new byte[size];
        dataIn.readFully(data);
        Jsonb jsonb = JsonbBuilder.create();
        try (InputStream is = new ByteArrayInputStream(data)) {
            return jsonb.fromJson(is, DefaultExchangeHolder.class);
        }
    }

}
