package com.ibm.icu.text;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxOutInterceptor;

@TargetClass(WSS4JStaxOutInterceptor.class)
final class wss4jinterceptor {

    @Substitute
    public void handleMessage(SoapMessage mc) throws Fault {
        throw new RuntimeException("Not supported!");
    }
}
