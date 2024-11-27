package no.hal.httptest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public interface HttpFile {

    /**
     * All contents in a http file.
     */
    public record Model(List<Request> requests) {
        public Model(Request... requests) {
            this(List.of(requests));
        }
    }

    public interface StringValue {
        public String toString(StringValueProvider valueProvider);
    }

    public record StringConstant(String constant) implements StringValue {
        public String toString(StringValueProvider valueProvider) {
            return constant;
        }
    }

    /**
     * A string value split into alternating static and variable part.
     * Starts with string contents.
     */
    public record StringTemplate(List<String> parts) implements StringValue {
        public StringTemplate(String... part) {
            this(List.of(part));
        }

        public static StringTemplate of(String s) {
            List<String> parts = new ArrayList<>();
            int pos = 0;
            while (pos < s.length()) {
                int varStart = s.indexOf("{{", pos), varEnd = s.indexOf("}}", varStart + 1); // works even if varStart = -1
                if (varStart < 0) {
                    // add final static part
                    parts.add(s.substring(pos));
                    break;
                } else {
                    if (varEnd < 0) {
                        throw new IllegalArgumentException("{{ without }}");
                    }
                    // add intermediate static part
                    parts.add(s.substring(pos, varStart));
                    // add variable part
                    parts.add(s.substring(varStart + 2, varEnd));
                }
                pos = varEnd + 2;
            }
            return new StringTemplate(parts);
        }

        public String toString(StringValueProvider valueProvider) {
            StringBuilder buffer = new StringBuilder();
            for (int i = 0; i < parts.size(); i++) {
                var s = parts.get(i);
                if (i % 2 == 1) {
                    s = valueProvider.getStringValue(s);
                }
                if (s != null) {
                    buffer.append(s);
                }
            }
            return buffer.toString();
        }
    }

    public interface Named<T> {
        public String name();
        public T value();
    }

    public static <T> Optional<T> getValue(String name, List<? extends Named<T>> nameds) {
        if (nameds != null) {
            for (var named : nameds) {
                if (name.equals(named.name())) {
                    return Optional.ofNullable(named.value());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * A property declaration.
     */
    public record Property(String name, String value) implements Named<String> {
    }

    /**
     * A variable declaration.
     */
    public record Variable(String name, StringTemplate value) implements Named<StringTemplate>, StringValue {
        public Variable(String name, String value) {
            this(name, StringTemplate.of(value));
        }
        public String toString(StringValueProvider valueProvider) {
            return value.toString(valueProvider);
        }
    }

    /**
     * The HTTP verbs.
     */
    public enum HttpMethod {
        GET, HEAD, POST, PUT, PATCH, DELETE;

        public static boolean is(String s) {
            try {
                return HttpMethod.valueOf(s) != null;
            } catch (IllegalArgumentException iae) {
                return false;
            }
        }
    }

    public record Request(
        List<Variable> requestVariables,
        List<Property> requestProperties,
        HttpMethod method,
        StringTemplate target,
        String version,
        List<Header> headers,
        Body body
    ) {
        public Request(String name, List<Variable> requestVariables, HttpMethod method, String target, List<Header> headers, Body body) {
            this(requestVariables, List.of(new Property("name", name)), method, StringTemplate.of(target), null, headers, body);
        }
        public Request(List<Variable> requestVariables, HttpMethod method, String target, List<Header> headers, Body body) {
            this(requestVariables, List.of(), method, StringTemplate.of(target), null, headers, body);
        }

        public Optional<StringTemplate> getRequestVariableValue(String name) {
            return HttpFile.getValue(name, requestVariables);
        }
        public Optional<String> getRequestPropertyValue(String name) {
            return HttpFile.getValue(name, requestProperties);
        }
    }

    public record Header(String name, StringTemplate value) {
        public Header(String name, String value) {
            this(name, StringTemplate.of(value));
        }
    }

    public record Body(String contentType, StringTemplate content) {
        public Body(String contentType, String value) {
            this(contentType, StringTemplate.of(value));
        }
    }

    //

    public static Model of(Iterator<String> lines) {
        return new HttpFileParser().parse(lines);
    }
    public static Model of(String input) {
        return new HttpFileParser().parse(input);
    }
}
