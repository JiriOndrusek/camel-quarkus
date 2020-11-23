package org.apache.camel.quarkus.component.avro.rpc;

import java.util.IdentityHashMap;
import java.util.Map;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Inject;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import org.apache.avro.Schema;

//@TargetClass(value = GenericDatumReader.class)
public final class GenericDatumReaderSubstitute {

    @Inject
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private Map<Schema, Class> stringClassCache;

    @Alias
    protected Class findStringClass(Schema schema) {
        return null;
    }

    //    @Substitute
    //    private Class getStringClass(Schema s) {
    //
    //        System.out.println("++++++++++++++++++++++++++++ this class: " + getClass() + ", cache = " + stringClassCache);
    //        System.out.println("++++++++++++++++++++++++++++ ignoring cache");
    //        Class c = findStringClass(s);
    //        System.out.println("++++++++++++++++++++++++++++ found " + c + " for " + s);
    //        return c;
    //    }

    @Substitute
    private Class getStringClass(Schema s) {
        if (stringClassCache == null) {
            stringClassCache = new IdentityHashMap<>();
        }

        System.out.println("++++++++++++++++++++++++++++ this class: " + getClass());
        System.out.println("++++++++++++++++++++++++++++ schema: " + s + ", cache: " + stringClassCache);
        System.out.println("++++++++++++++++++++++++++++> cache size: " + stringClassCache.size());
        Class c = stringClassCache.get(s);

        if (c == null) {
            c = findStringClass(s);
            stringClassCache.put(s, c);
        }
        System.out.println("++++++++++++++++++++++++++++ class: " + c);
        return c;
    }
}
