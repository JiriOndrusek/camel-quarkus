package org.apache.camel.quarkus.component.leveldb;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.iq80.leveldb.impl.MMapLogWriter;

/**
 * TODO work-around because of https://github.com/oracle/graal/issues/2761
 */
@TargetClass(value = MMapLogWriter.class)
final class MMapLogWriterSubstitute {

    @Substitute
    private void unmap() {
        //do nothing
    }
}
