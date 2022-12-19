/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.quarkus.test;

import org.apache.camel.util.CollectionHelper;
import org.apache.camel.util.ObjectHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

public class SerializationTest {

    @Test
    public void map() {
        Map map =  new LinkedHashMap() {
            {
                put("item1", 1);
                put("item2", "value2");
            }};

        test(map, "m{s{item1};i{1};s{item2};s{value2}}");
    }

    @Test
    public void mapCustomDelimiter() {
        Map map =  new LinkedHashMap() {
            {
                put("item1", 1);
                put("item2", "value2");
            }};

        test(map, "m{b}s{b}item1{e}{d}i{b}1{e}{d}s{b}item2{e}{d}s{b}value2{e}{e}", "{d}", "{b}", "{e}");
    }

    @Test
    public void list() {
        LinkedList list = new LinkedList<>();
        list.add("item1");
        list.add(1);
        list.add(2l);

        test(list, "l{s{item1};i{1};o{2}}");
    }

    @Test
    public void mapWithMap() {
        Map map =  new LinkedHashMap() {
            {
                put("item1", 1);
                put("item2", "value2");
                put("item3", CollectionHelper.mapOf("item4", 12l));
            }};

        test(map, "m{s{item1};i{1};s{item2};s{value2};s{item3};m{s{item4};o{12}}}");
    }


    private void test(Object list, String expected, String delimiter, String begin, String end) {
        String serialized = SerializationUtil.serialize(list, delimiter, begin, end);
        Assertions.assertEquals(expected, serialized);

        Object deserialized = SerializationUtil.deserialize(serialized, delimiter, begin, end);

        Assertions.assertTrue(ObjectHelper.equal(deserialized, list));
    }

    private void test(Object list, String expected) {
        String serialized = SerializationUtil.serialize(list);
        Assertions.assertEquals(expected, serialized);

        Object deserialized = SerializationUtil.deserialize(serialized);

        Assertions.assertTrue(ObjectHelper.equal(deserialized, list));
    }
}
