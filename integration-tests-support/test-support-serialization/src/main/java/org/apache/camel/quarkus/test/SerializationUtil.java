package org.apache.camel.quarkus.test;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class SerializationUtil {

    public static String DEFAULT_DELIMITER = ";";
    public static String DEFAULT_BEGIN = "{";
    public static String DEFAULT_END = "}";

    private enum Type {
        map('m'),
        list('l'),
        integer('i'),
        longInt('o'),
        string('s'),
        mapEntry('e');

        char serialized;

        Type(char serialized) {
            this.serialized = serialized;
        }

        public char getSerialized() {
            return serialized;
        }
    }

    private static class Node {
        final String value;
        final Type type;
        final List<Node> children = new LinkedList();

        public Node(String value, Type type) {
            this.value = value;
            this.type = type;
        }

        public List<Node> getChildren() {
            return children;
        }
    }

    public static String serialize(Object object) {
        return serialize(object, DEFAULT_DELIMITER, DEFAULT_BEGIN, DEFAULT_END);
    }

    public static Object deserialize(String serialized) {
        return deserialize(serialized, DEFAULT_DELIMITER, DEFAULT_BEGIN, DEFAULT_END);
    }

    public static Object deserialize(String string, String delimiter, String begin, String end) {
        Node tree = parseTree(string, delimiter, begin, end);
        Object result = instantiateNode(tree);
        return result;
    }

    public static String serialize(Object object, String delimiter, String begin, String end) {

        StringBuilder sb = new StringBuilder();
        Type t = detectType(object);

        switch (t) {
        case integer:
        case longInt:
        case string:
            sb.append(t.getSerialized()).append(begin).append(object).append(end);
            break;
        case map:
            sb.append(t.getSerialized());
            sb.append(begin);
            sb.append(
                    ((Map<Object, Object>) object).entrySet().stream()
                            .map(e -> serialize(e, delimiter, begin, end))
                            .collect(Collectors.joining(delimiter)));
            sb.append(end);
            break;
        case mapEntry:
            Map.Entry<Object, Object> e = (Map.Entry<Object, Object>) object;
            sb.append(serialize(e.getKey(), delimiter, begin, end)).append(delimiter)
                    .append(serialize(e.getValue(), delimiter, begin, end));
            break;
        case list:
            sb.append(t.getSerialized());
            sb.append(begin);
            sb.append(
                    ((List) object).stream()
                            .map(o -> serialize(o, delimiter, begin, end))
                            .collect(Collectors.joining(delimiter)));
            sb.append(end);
            break;
        default:
            throw new IllegalStateException("Unknown object type");
        }
        return sb.toString();
    }

    private static Node parseTree(String string, String delimiter, String begin, String end) {
        Type type = deserializeType(string.charAt(0));
        String value = string.substring(1 + begin.length(), string.length() - end.length());
        switch (type) {
        case integer:
        case longInt:
        case string:
            return new Node(value, type);
        case map:
            Node mapRoot = new Node(null, Type.map);

            //parse keys and values
            while (value.length() > 0) {
                int keyEnd = findEndIndex(value, begin, end);
                String keySerialized = value.substring(0, keyEnd + 1);
                //key
                mapRoot.getChildren().add(parseTree(keySerialized, delimiter, begin, end));
                //remove key from value
                value = value.substring(keyEnd + 1 + delimiter.length());
                //value
                int valueEnd = findEndIndex(value, begin, end);
                mapRoot.getChildren().add(parseTree(value.substring(0, valueEnd + 1), delimiter, begin, end));

                //remove value
                value = value.substring(valueEnd + 1);
                //if there is a delimiter, another key/value continues
                if (value.length() == 0) {
                    return mapRoot;
                }
                //remove delimiter
                value = value.substring(delimiter.length());
            }
            throw new IllegalStateException("Map is not properly finished.");
        case list:
            Node listRoot = new Node(null, Type.list);

            //parse keys and values
            while (value.length() > 0) {
                int itemEnd = findEndIndex(value, begin, end);
                String itemSerialized = value.substring(0, itemEnd + 1);
                listRoot.getChildren().add(parseTree(itemSerialized, delimiter, begin, end));
                value = value.substring(itemEnd + 1);
                //if value is not empty, another item continues
                if (value.length() == 0) {
                    return listRoot;
                }
                //remove delimiter
                value = value.substring(delimiter.length());
            }
            throw new IllegalStateException("Map is not properly finished.");
        default:
            throw new IllegalStateException("Unknown object type");
        }
    }

    private static Object instantiateNode(Node node) {
        switch (node.type) {
        case integer:
            return Integer.parseInt(node.value);
        case longInt:
            return Long.parseLong(node.value);
        case string:
            return node.value;
        case map:
            LinkedHashMap<Object, Object> map = new LinkedHashMap<>();
            for (Iterator<Node> iter = node.getChildren().iterator(); iter.hasNext();) {
                Node key = iter.next();
                if (!iter.hasNext()) {
                    throw new IllegalStateException("Key/value pair is not complete.");
                }
                Node value = iter.next();
                map.put(instantiateNode(key), instantiateNode(value));
            }
            return map;
        case list:
            LinkedList<Object> list = new LinkedList<>();
            for (Node chiLd : node.getChildren()) {
                list.add(instantiateNode(chiLd));
            }
            return list;
        default:
            throw new IllegalStateException("Unsupported type.");
        }
    }

    //  ------------------------------- helper methods --------------------------------------------

    private static int findEndIndex(String string, String begin, String end) {
        //find end index of the serialized object
        int i = 0;
        //we are currently after start of the object {, so the number of starting object is 1
        int counter = 0;
        boolean foundStart = false;
        while (i < string.length()) {
            if (string.substring(i).startsWith(begin)) {
                foundStart = true;
                counter++;
            } else if (string.substring(i).startsWith(end)) {
                counter--;
            }
            if (foundStart && counter == 0) {
                i += end.length() - 1;
                break;
            }

            i++;
        }

        if (counter > 0) {
            throw new IllegalStateException("Serialized object is not valid, More object started then ended.");
        }
        return i;
    }

    private static Type detectType(Object o) {

        if (o instanceof Integer) {
            return Type.integer;
        } else if (o instanceof Long) {
            return Type.longInt;
        } else if (o instanceof List) {
            return Type.list;
        } else if (o instanceof Map.Entry) {
            return Type.mapEntry;
        } else if (o instanceof Map) {
            return Type.map;
        } else {
            return Type.string;
        }

    }

    private static Type deserializeType(char c) {

        if (Type.integer.serialized == c) {
            return Type.integer;
        } else if (Type.integer.serialized == c) {
            return Type.integer;
        } else if (Type.longInt.serialized == c) {
            return Type.longInt;
        } else if (Type.map.serialized == c) {
            return Type.map;
        } else if (Type.list.serialized == c) {
            return Type.list;
        } else if (Type.integer.serialized == c) {
            return Type.integer;
        } else if (Type.string.serialized == c) {
            return Type.string;
        } else {
            throw new IllegalStateException("Unknown serialized type " + String.valueOf(c));
        }
    }

}
