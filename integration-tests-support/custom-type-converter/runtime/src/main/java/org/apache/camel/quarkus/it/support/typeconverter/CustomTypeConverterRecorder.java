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
package org.apache.camel.quarkus.it.support.typeconverter;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import org.apache.camel.spi.Registry;
import org.apache.camel.spi.TypeConverterLoader;

@Recorder
public class CustomTypeConverterRecorder {
    public RuntimeValue<TypeConverterLoader> createTypeConverterLoader() {
        return new RuntimeValue<>(new CustomTypeConverterLoader());
    }

    public void bindMyStringConverter(RuntimeValue<Registry> registry) {
        registry.getValue().bind("myStringConverter", new MyStringConverterLoader());
    }

    //    public void bindCustomConverter(RuntimeValue<TypeConverterRegistry> registry, String className) {
    //        try {
    //            new AnnotationTypeConverterLoader(null).loadConverterMethods(registry.getValue(), Class.forName(className));
    //        } catch (ClassNotFoundException e) {
    //            e.printStackTrace();
    //        }
    //        ////        try {
    //        //            return new RuntimeValue<>( registry ->
    //        //                    registry.addTypeConverter(fromType, toType, new SimpleTypeConverter(true, (type, exchange, value) -> {
    //        //                        Object converter = method.getDeclaringClass().getDeclaredConstructor().newInstance();
    //        //                        return method.invoke(converter, fromType, value);
    //        //                    })));
    //        //
    //        ////            registry.getValue().bind("myStringConverter" + className,
    //        ////                    Class.forName(className).getDeclaredConstructor().newInstance());
    //        ////        } catch (Exception e) {
    //        ////            e.printStackTrace();
    //        ////        }
    //        ////        return null
    //    }

}
