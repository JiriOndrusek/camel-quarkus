package tika.runtime.graal;

import java.awt.*;
import java.util.Locale;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.poi.poifs.crypt.temp.EncryptedTempData;
import org.apache.poi.ss.format.CellFormat;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlError;

public class Subst {
}

@TargetClass(CellFormat.class)
final class CellFormatSubs {

    @Substitute //to avoid Random at build time
    private CellFormatSubs(Locale locale, String format) {
        throw new RuntimeException("Not supported");
    }
}

@TargetClass(EncryptedTempData.class)
final class EncryptedTempDataSubst {

    @Substitute //to avoid awt.Colour at build time
    public EncryptedTempDataSubst() {
        throw new RuntimeException("Not supported");
    }
}

@TargetClass(XmlError.class)
final class XmlErrorSubst {

    @Substitute
    protected XmlErrorSubst(String message, String code, int severity, XmlCursor cursor) {
        throw new RuntimeException("Not supported");
    }
}
