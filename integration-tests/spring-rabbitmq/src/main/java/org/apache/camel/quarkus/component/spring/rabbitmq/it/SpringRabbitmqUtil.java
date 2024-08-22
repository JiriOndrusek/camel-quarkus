package org.apache.camel.quarkus.component.spring.rabbitmq.it;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpringRabbitmqUtil {

    public static String headersToString(Map<String, Object> headers) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(";");
        }
        return sb.toString();
    }

    public static Map<String, Object> stringToHeaders(String headers) {
        Map<String, Object> headersMap = new HashMap<>();
        if (headers != null) {
            for (String header : headers.split(";")) {
                String[] keyValue = header.split(":");
                headersMap.put(keyValue[0], keyValue[1]);
            }
        }
        return headersMap;
    }

    public static String listToString(List<String> list) {
        StringBuilder sb = new StringBuilder();
        if (list != null) {
            for (String item : list) {
                sb.append(item).append(";");
            }
        }
        return sb.toString();
    }

    public static List<Object> stringToList(String string) {
        List<Object> list = new ArrayList<>();
        if (string != null) {
            for (String item : string.split(";")) {
                list.add(item);
            }
        }
        return list;
    }
}