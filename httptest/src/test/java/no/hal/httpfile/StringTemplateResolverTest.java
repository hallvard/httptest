package no.hal.httpfile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.hal.httpfile.HttpFile.StringTemplate.Part;

public class StringTemplateResolverTest {
    
    private HttpFileParser parser;
    private InputStreamProvider inputStreamProvider;

    @BeforeEach
    public void setupParser() {
        this.parser = new HttpFileParser();
        this.inputStreamProvider = new InputStreamProvider.Default();
    }

    @Test
    public void testVariableRequestLine() {
        var model = parser.parse("""
            @section=sport
            GET http://vg.no/{{section}}
            """);
        assertEquals(1, model.requests().size());

        var request = model.requests().get(0);
        var stringTemplateResolver = new StringTemplateResolver();
        stringTemplateResolver.setInputStreamProvider(inputStreamProvider);
        var stringValueProvider = new StringValueProvider.Variables(request.requestVariables(), stringTemplateResolver);
        stringTemplateResolver.setStringValueProvider(stringValueProvider);
        stringTemplateResolver.resolve(model);
    
        assertEquals(
            new HttpFile.Model(
                new HttpFile.Request(
                    List.of(new HttpFile.Variable("section", "sport")), List.of(),
                    HttpFile.HttpMethod.GET, new HttpFile.StringTemplate(
                        new Part.Constant("http://vg.no/"),
                        new Part.Constant("sport")
                    ), null,
                    List.of(), null)
            ),
            model
        );
    }
}
