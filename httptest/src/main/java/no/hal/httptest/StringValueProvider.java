package no.hal.httptest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import no.hal.httptest.HttpFile.Variable;

public interface StringValueProvider extends Function<String, String> {

    String getStringValue(String name);

    default String apply(String name) {
        return getStringValue(name);
    }

    public record Variables(Iterable<Variable> variables) implements StringValueProvider {
        @Override
        public String getStringValue(String name) {
            for (var variable : variables) {
                if (name.equals(variable.name())) {
                    return variable.value().toString(this);
                }
            }
            return null;
        }
    }

    public record MapEntries(Map<String, ? extends Object> map) implements StringValueProvider {
        private static Object getValue1(String name, Map<String, ? extends Object> map) {
            for (var entry : map.entrySet()) {
                if (name.equals(entry.getKey())) {
                    var value = entry.getValue();
                    return value;
                }
            }
            return null;
        }
        public static Object getValue(String name, Map<String, ? extends Object> entries) {
            int pos = 0;
            while (pos < name.length()) {
                int dotPos = name.indexOf('.', pos);
                if (dotPos < 0) {
                    dotPos = name.length();
                }
                var key = name.substring(pos, dotPos);
                var value = getValue1(key, entries);
                if (value instanceof Map) {
                    entries = (Map) value;
                    pos = dotPos + 1;
                } else {
                    return value;
                }
            }
            return null;
        }

        public Object getValue(String name) {
            return getValue(name, map);
        }

        @Override
        public String getStringValue(String name) {
            var value = getValue(name, map);
            return (value != null ? String.valueOf(value) : null);
        }
    }

    public record Functions(Iterable<Function<String, String>> functions) implements StringValueProvider {
        public Functions(Function<String, String>... functions) {
            this(List.of(functions));
        }
        @Override
        public String getStringValue(String name) {
            for (var function : functions) {
                var value = function.apply(name);
                if (value != null) {
                    return value.toString();
                }
            }
            return null;
        }
    }
}
