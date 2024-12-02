package no.hal.httpfile;

import java.io.IOException;
import java.util.function.Consumer;

import no.hal.httpfile.HttpFile.StringTemplate.Part;

public class StringTemplateValueProvider {
    
    private StringValueProvider stringValueProvider;
    private InputStreamProvider inputStreamProvider;

    public StringTemplateValueProvider() {
    }

    public StringTemplateValueProvider(StringValueProvider stringValueProvider, InputStreamProvider inputStreamProvider) {
        this.stringValueProvider = stringValueProvider;
        this.inputStreamProvider = inputStreamProvider;
    }

    public void setStringValueProvider(StringValueProvider stringValueProvider) {
        this.stringValueProvider = stringValueProvider;
    }

    public void setInputStreamProvider(InputStreamProvider inputStreamProvider) {
        this.inputStreamProvider = inputStreamProvider;
    }

    public void forEach(HttpFile.StringTemplate stringTemplate, Consumer<String> consumer) {
        for (var part : stringTemplate.parts()) {
            var s = switch (part) {
                case Part.Constant constant -> constant.value();
                case Part.VariableRef variableRef -> stringValueProvider.getStringValue(variableRef.variable());
                case Part.ResourceRef resourceRef -> {
                    try (var inputStream = inputStreamProvider.getInputStream(resourceRef.resource())) {
                        if (inputStream == null) {
                            yield "Resource '" + resourceRef.resource() + "' not found";
                        } else {
                            yield new String(inputStream.readAllBytes());
                        }
                    } catch (IOException e) {
                        yield e.getMessage();
                    }
                }
                // shouldn't need this one, it's already exhaustive
                default -> null;
            };
            consumer.accept(s);
        }
    }

    public void toStringBuffer(HttpFile.StringTemplate stringTemplate, StringBuffer buffer) {
        forEach(stringTemplate, s -> {
            if (s != null) buffer.append(s);
        });
    }

    public String toString(HttpFile.StringTemplate stringTemplate) {
        StringBuffer buffer = new StringBuffer();
        toStringBuffer(stringTemplate, buffer);
        return buffer.toString();
    }
}
