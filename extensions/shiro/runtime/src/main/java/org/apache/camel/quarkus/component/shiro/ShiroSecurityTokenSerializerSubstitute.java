package org.apache.camel.quarkus.component.shiro;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.camel.component.shiro.security.ShiroSecurityToken;
import org.apache.camel.component.shiro.security.ShiroSecurityTokenSerializer;
import org.apache.camel.component.shiro.security.ShiroSecurityTokenSerializerImpl;

@TargetClass(value = ShiroSecurityTokenSerializerImpl.class)
final class ShiroSecurityTokenSerializerSubstitute implements ShiroSecurityTokenSerializer {

    @Substitute
    public byte[] serialize(ShiroSecurityToken token) throws IOException {
        Jsonb jsonb = JsonbBuilder.create();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            jsonb.toJson(token, output);
            return output.toByteArray();
        }
    }

    @Substitute
    public ShiroSecurityToken deserialize(byte[] data) throws IOException, ClassNotFoundException {
        Jsonb jsonb = JsonbBuilder.create();
        try (InputStream is = new ByteArrayInputStream(data)) {
            return jsonb.fromJson(is, ShiroSecurityToken.class);
        }
    }
}
