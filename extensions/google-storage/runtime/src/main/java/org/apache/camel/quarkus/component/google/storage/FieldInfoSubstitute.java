package org.apache.camel.quarkus.component.google.storage;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.api.client.util.FieldInfo;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;

//@TargetClass(value = FieldInfo.class)
public final class FieldInfoSubstitute {

    @Alias
    private Field field;

    @Alias
    private Method[] setters;

    @Substitute
    public Field getField() {
        System.out.println("------- Field: " + field.getName());
        return field;
    }

    @Substitute
    public void setValue(Object obj, Object value) {
        System.out.println("4444444444444 - obj: " + obj);
        System.out.println("4444444444444 - value: " + value);
        if (setters.length > 0) {
            for (Method method : setters) {
                if (value == null || method.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
                    try {
                        method.invoke(obj, value);
                        return;
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        // try to set field directly
                        System.out.println("4444444444444 - exception");
                        e.printStackTrace();
                    }
                }
            }
        }
        FieldInfo.setFieldValue(field, obj, value);
    }

}
