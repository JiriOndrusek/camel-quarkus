package org.apache.camel.quarkus.component.ssh.runtime;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.sshd.common.util.security.bouncycastle.BouncyCastleSecurityProviderRegistrar;

@TargetClass(BouncyCastleSecurityProviderRegistrar.class)
final class SubstituteBouncyCastleSecurityProviderRegistrar {

    @Substitute
    public boolean isEnabled() {
        return false;
    }
}
