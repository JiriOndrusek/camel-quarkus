package org.apache.camel.quarkus.core;

import java.util.Objects;

public class MyString2 {

    String value;

    String formattedValue;

    public static MyString2 valueOf(String value) {
        return new MyString2(value, "<myString2>" + value + "</myString2>");
    }

    private MyString2(String value, String formattedValue) {
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
        MyString2 myString2 = (MyString2) o;
        return Objects.equals(value, myString2.value) &&
                Objects.equals(formattedValue, myString2.formattedValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, formattedValue);
    }
}
