package com.ibm.icu.text;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.security.trust.AbstractSTSClient;

@TargetClass(AbstractSTSClient.class)
final class AbstractSTSClientSubst {

    @Substitute
    public void configureViaEPR(EndpointReferenceType ref, boolean useEPRWSAAddrAsMEXLocation) {
        throw new RuntimeException("Not supported!");
    }
}
