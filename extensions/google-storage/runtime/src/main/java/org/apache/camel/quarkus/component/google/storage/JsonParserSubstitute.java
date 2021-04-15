package org.apache.camel.quarkus.component.google.storage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import com.google.api.client.json.CustomizeJsonParser;
import com.oracle.svm.core.annotate.Substitute;

//@TargetClass(value = JsonParser.class)
public final class JsonParserSubstitute {

    //    @Alias
    //    public Object parse(Type dataType, boolean close, CustomizeJsonParser customizeParser) {
    //        return null;
    //    }
    //    @Alias
    //    private Object parseValue(
    //            Field fieldContext,
    //            Type valueType,
    //            ArrayList<Type> context,
    //            Object destination,
    //            CustomizeJsonParser customizeParser,
    //            boolean handlePolymorphic)
    //            throws IOException {
    //        return null;
    //    }
    //
    //    @Alias
    //    private JsonToken startParsing() throws IOException {
    //        return null;
    //    }
    //
    //    @Substitute
    //    public Object parse(Type dataType, boolean close) throws IOException {
    //        System.out.println("1111111111111111111 - type: " + dataType);
    //        //        Object o = parse(dataType, close, null);
    //
    //        if (!Void.class.equals(dataType)) {
    //            startParsing();
    //        }
    //        Object o = parseValue(null, dataType, new ArrayList<Type>(), null, null, true);
    //
    //        System.out.println("1111111111111111111 - result: " + o);
    //
    //        return o;
    //    }

    @Substitute
    private void parse(
            ArrayList<Type> context, Object destination, CustomizeJsonParser customizeParser)
            throws IOException {

        System.out.println("1111111111111111111 - destination: " + destination);

    }

}
