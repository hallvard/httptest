package no.hal.httpfile;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import no.hal.httpfile.HttpFile.Variable;

public interface StringValueProvider {

    public String getStringValue(String name);

    public record Variables(Iterable<Variable> variables, StringTemplateValueProvider templateValueProvider) implements StringValueProvider {
        @Override
        public String getStringValue(String name) {
            for (var variable : variables) {
                if (name.equals(variable.name())) {
                    return templateValueProvider.toString(variable.value());
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

    public record Properties(java.util.Properties properties) implements StringValueProvider {

        public static Properties of(Path path) {
            var props = new java.util.Properties();
            try (var inputStream = new FileInputStream(path.toFile())) {
                props.load(inputStream);
                return new Properties(props);
            } catch (Exception e) {
                throw new IllegalArgumentException("Exception when loading properties from " + path + "; " + e, e);
            }
        }

        @Override
        public String getStringValue(String name) {
            return properties.getProperty(name);
        }
    }

    public record Providers(Iterable<StringValueProvider> providers) implements StringValueProvider {
        public Providers(StringValueProvider... providers) {
            this(List.of(providers));
        }
        @Override
        public String getStringValue(String name) {
            for (var provider : providers) {
                var value = provider.getStringValue(name);
                if (value != null) {
                    return value.toString();
                }
            }
            return null;
        }
    }
}
