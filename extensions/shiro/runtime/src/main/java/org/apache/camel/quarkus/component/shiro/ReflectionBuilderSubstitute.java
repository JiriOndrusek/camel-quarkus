package org.apache.camel.quarkus.component.shiro;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import org.apache.shiro.config.DefaultInterpolator;
import org.apache.shiro.config.Interpolator;
import org.apache.shiro.config.ReflectionBuilder;

@TargetClass(value = ReflectionBuilder.class)
final class ReflectionBuilderSubstitute {

    @Substitute
    private Interpolator createInterpolator() {
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++");
        System.out.println("++++++++++ substitute to defaultInterpolator +++++");
        System.out.println("++++++++++++++++++++++++++++++++++++++++++++");
        return new DefaultInterpolator();
    }
}
