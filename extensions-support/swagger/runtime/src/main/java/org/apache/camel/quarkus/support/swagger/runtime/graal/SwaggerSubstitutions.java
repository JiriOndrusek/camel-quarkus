package org.apache.camel.quarkus.support.swagger.runtime.graal;

import java.util.Calendar;
import java.util.List;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.extensions.SwaggerParserExtension;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

final class SwaggerSubstitutions {
}

@TargetClass(OpenAPIParser.class)
final class OpenAPIParserSubstitutions {

    @Substitute
    public SwaggerParseResult readLocation(String url, List<AuthorizationValue> auth, ParseOptions options) {
        if (url.startsWith("resource:")) {
            url = url.replaceFirst("resource:", "");
        }

        SwaggerParseResult output = null;

        for (SwaggerParserExtension extension : OpenAPIV3Parser.getExtensions()) {
            output = extension.readLocation(url, auth, options);
            if (output != null && output.getOpenAPI() != null) {
                return output;
            }
        }

        return output;
    }
}

@TargetClass(Calendar.Builder.class)
final class CalendarBuilderSubstitution {
    @Substitute
    public Calendar build() {
        throw new UnsupportedOperationException("Calendar::build is not supported");
    }
}
