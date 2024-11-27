package no.hal.httptest;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
                    HttpFile.HttpMethod.GET, new HttpFile.StringTemplate("http://vg.no/", "section"), null,
                    List.of(), null)
            ),
            parser.parse("""
            @section=sport
            GET http://vg.no/{{section}}
            """)
        );
    }
}
