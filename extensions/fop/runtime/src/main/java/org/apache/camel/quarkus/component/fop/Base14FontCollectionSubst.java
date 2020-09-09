package org.apache.camel.quarkus.component.fop;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.fop.fonts.FontInfo;
import org.apache.fop.fonts.base14.Base14FontCollection;

@TargetClass(value = Base14FontCollection.class)
public final class Base14FontCollectionSubst {

    //todo jondruse just to verify some facts
    @Substitute
    public int setup(int start, FontInfo fontInfo) {
        System.out.println("================== ignoring base fonts ========================");
        return 0;
    }
}
