package org.apache.camel.quarkus.component.shiro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.component.shiro.security.ShiroSecurityToken;
import org.apache.camel.component.shiro.security.ShiroSecurityTokenSerializer;

@TargetClass(value = ShiroSecurityTokenSerializer.class)
final class ShireSecurityTokenSerializerSubstitute {

    @Alias
    private ShiroSecurityToken securityToken;

    @Alias
    private byte[] serializedToken;

    @Substitute
    public byte[] getBytes() throws Exception {
        System.out.println("******** serializing ***********");
        Jsonb jsonb = JsonbBuilder.create();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            jsonb.toJson(securityToken, output);
            return output.toByteArray();
        }
    }

    @Substitute
    public ShiroSecurityToken getToken() throws Exception {
        System.out.println("******** deserializing ***********");
        Jsonb jsonb = JsonbBuilder.create();
        try (InputStream is = new ByteArrayInputStream(serializedToken)) {
            return jsonb.fromJson(is, ShiroSecurityToken.class);
        }
    }
}
