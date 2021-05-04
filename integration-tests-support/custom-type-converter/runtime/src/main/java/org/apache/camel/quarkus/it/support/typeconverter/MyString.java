package org.apache.camel.quarkus.it.support.typeconverter;

import java.util.Objects;

public class MyString {

    String value;

    String formattedValue;

    public static MyString valueOf(String value) {
        return new MyString(value, "<tag>" + value + "</tag>");
    }

    private MyString(String value, String formattedValue) {
        this.value = value;
        this.formattedValue = formattedValue;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getFormattedValue() {
        return formattedValue;
    }

    public void setFormattedValue(String formattedValue) {
        this.formattedValue = formattedValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MyString that = (MyString) o;
        return Objects.equals(value, that.value) &&
                Objects.equals(formattedValue, that.formattedValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, formattedValue);
    }
}
