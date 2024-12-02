package no.hal.httpfile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.hal.httpfile.HttpFile.StringTemplate.Part;

public class HttpFileParserTest {
    
    private HttpFileParser parser;

    @BeforeEach
    public void setupParser() {
        this.parser = new HttpFileParser();
    }

    @Test
    public void testRequestLine() {
        assertEquals(
            new HttpFile.Model(
                new HttpFile.Request(List.of(), HttpFile.HttpMethod.GET, "http://vg.no/", List.of(), null)
            ),
            parser.parse("""
            GET http://vg.no/
            """)
        );
    }

    @Test
    public void testRequestLines() {
        assertEquals(
            new HttpFile.Model(
                new HttpFile.Request(List.of(), HttpFile.HttpMethod.GET, "http://vg.no/", List.of(), null),
                new HttpFile.Request(List.of(), HttpFile.HttpMethod.GET, "http://yr.no/", List.of(), null)
            ),
            parser.parse("""
            GET http://vg.no/

            ###
            GET http://yr.no/
            """)
        );
    }

    @Test
    public void testVariableRequestLine() {
        assertEquals(
            new HttpFile.Model(
                new HttpFile.Request(
                    List.of(new HttpFile.Variable("section", "sport")), List.of(),
                    HttpFile.HttpMethod.GET, new HttpFile.StringTemplate(
                        new Part.Constant("http://vg.no/"),
                        new Part.VariableRef("section"),
                        new Part.Constant("/"),
                        new Part.FunctionCall("guid")
                    ), null,
                    List.of(), null)
            ),
            parser.parse("""
            @section=sport
            GET http://vg.no/{{section}}/{{$guid}}
            """)
        );
    }

    @Test
    public void testRequestLineHeadersBody() {
        assertEquals(
            new HttpFile.Model(
                new HttpFile.Request(
                    List.of(), List.of(),
                    HttpFile.HttpMethod.POST, new HttpFile.StringTemplate(new Part.Constant("http://vg.no/")), null,
                    List.of(new HttpFile.Header("Content-Type", "text/plain")),
                    new HttpFile.Body(null, new HttpFile.StringTemplate(
                        new Part.Constant("Here's some content")
                    ))
                )
            ),
            parser.parse("""
            POST http://vg.no/
            Content-Type: text/plain

            Here's some content
            """)
        );
    }

    @Test
    public void testResourceBody() {
        assertEquals(
            new HttpFile.Model(
                new HttpFile.Request(
                    List.of(), List.of(),
                    HttpFile.HttpMethod.POST, new HttpFile.StringTemplate(new Part.Constant("http://vg.no/")), null,
                    List.of(new HttpFile.Header("Content-Type", "text/plain")),
                    new HttpFile.Body(null, new HttpFile.StringTemplate(
                        new Part.ResourceRef("content.txt")
                    ))
                )
            ),
            parser.parse("""
            POST http://vg.no/
            Content-Type: text/plain

            < content.txt

            """)
        );
    }
}
