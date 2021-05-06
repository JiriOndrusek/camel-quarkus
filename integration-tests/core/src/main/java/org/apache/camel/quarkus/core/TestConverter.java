package org.apache.camel.quarkus.core;

import org.apache.camel.Converter;

@Converter
public class TestConverter {

    @Converter
    public static MyString2 toMyString2(String s) {
        return MyString2.valueOf(s);
    }
}
