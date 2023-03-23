package com.ibm.icu.text;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.tokenstore.EHCacheTokenStoreFactory;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;

@TargetClass(EHCacheTokenStoreFactory.class)
public final class EhcacheTokenStoreFactorySubs {

    @Substitute
    public TokenStore newTokenStore(String key, Message message) throws TokenStoreException {
        throw new RuntimeException("Not supported!");
    }
}
