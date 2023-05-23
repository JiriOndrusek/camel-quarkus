package org.apache.camel.quarkus.support.swagger.runtime;

import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

@Interceptor
@SwaggerParserInterceptorBinding
public class SwaggerParserInterceptor {

    @AroundInvoke
    public Object intercept(InvocationContext context) throws Exception {

        Object[] params = context.getParameters();
        String url = (String) params[0];

        System.out.println("================ url before: " + url);

        if (url.startsWith("resource:")) {
            url = url.replaceFirst("resource:", "");
            params[0] = url;
            context.setParameters(params);
        }

        System.out.println("=============== url after:" + url);

        return context.proceed();
    }
}
