package org.apache.camel.quarkus.component.leveldb;

import java.util.concurrent.Callable;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.iq80.leveldb.table.MMapTable;

/**
 * Workaround for https://github.com/oracle/graal/issues/2761
 */
@TargetClass(value = MMapTable.class)
final class MMapTableSubstitute {

    @Substitute
    public Callable<?> closer() {
        //return nothing
        return null;
    }

}
