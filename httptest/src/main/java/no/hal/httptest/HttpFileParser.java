package no.hal.httptest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import no.hal.httptest.HttpFile.Body;
import no.hal.httptest.HttpFile.Header;
import no.hal.httptest.HttpFile.HttpMethod;
import no.hal.httptest.HttpFile.Property;
import no.hal.httptest.HttpFile.Request;
import no.hal.httptest.HttpFile.Model;
import no.hal.httptest.HttpFile.Variable;
import no.hal.httptest.HttpFileParser.Token.HeaderLine;
import no.hal.httptest.HttpFileParser.Token.PropertyLine;
import no.hal.httptest.HttpFileParser.Token.RequestLine;
import no.hal.httptest.HttpFileParser.Token.VariableLine;

public class HttpFileParser {
    
    public sealed interface Token {

        static boolean matchesEnd(String line) {
            return line == null;
        }

        static boolean matchesBlank(String line) {
            return line.length() == 0;
        }

        static boolean matchesRequestSeparator(String line) {
            return line.startsWith("###");
        }

        static boolean matchesComment(String line) {
            return line.startsWith("#") || line.startsWith("//");
        }

        record VariableLine(String name, String value) implements Token {
            static boolean matches(String line) {
                return line.startsWith("@");
            }
            static VariableLine of(String line) {
                int pos = line.indexOf("=");
                return new VariableLine(line.substring(1, pos).trim(), line.substring(pos + 1).trim());
            }
        }

        record PropertyLine(String name, String value) implements Token {
            static boolean matches(String line) {
                if (! line.startsWith("#")) {
                    return false;
                }
                line = line.substring(1).trim();
                return line.startsWith("@");
            }
            static PropertyLine of(String line) {
                line = line.substring(1).trim();
                int pos = line.indexOf(" ");
                if (pos < 0) {
                    pos = line.indexOf("=");
                }
                return new PropertyLine(line.substring(1, pos).trim(), line.substring(pos + 1).trim());
            }
        }

        record ContinuationLine(String line) implements Token {
            static boolean matches(String line) {
                return line.startsWith(" ") || line.startsWith("\t");
            }
        }

        record RequestLine(HttpMethod verb, String target, String version) implements Token {
            static boolean matches(String line) {
                int pos1 = line.indexOf(" ");
                if (pos1 > 0) {
                    String first = line.substring(0, pos1);
                    if (HttpMethod.is(first)) {
                        return true;
                    }
                }
                // URI must start with scheme or /
                return line.startsWith("/") || line.indexOf(":") >= 3;
            }
            static RequestLine of(String line) {
                int pos1 = line.indexOf(" ");
                HttpMethod verb = HttpMethod.GET;
                String rest = line;
                String version = null;
                if (pos1 < 0) {
                    pos1 = rest.length();
                } else {
                    String first = line.substring(0, pos1);
                    rest = line.substring(pos1 + 1).trim();
                    if (HttpMethod.is(first)) {
                        verb = HttpMethod.valueOf(first);
                        pos1 = rest.indexOf(" ");
                    } else {
                        rest = line;
                    }
                    if (pos1 > 0) {
                        version = rest.substring(pos1 + 1).trim();
                    } else {
                        pos1 = rest.length();
                    }
                }
                return new RequestLine(verb, rest.substring(0, pos1), version);
            }
        }

        record HeaderLine(String name, String value) implements Token {
            static boolean matches(String line) {
                int pos = line.indexOf(":");
                return pos > 0 && line.indexOf(":", pos + 1) < 0;
            }
            static HeaderLine of(String line) {
                int pos = line.indexOf(":");
                return new HeaderLine(line.substring(0, pos).trim(), line.substring(pos + 1).trim());
            }
        }
    }

    class Builder {
        List<Variable> fileVariables;
        List<Request> requests = new ArrayList<>();
        List<Property> properties;
        List<Variable> variables;
        RequestLine requestLine;
        List<Header> headers;
    }

    record Next(String line, State state) {
    }

    sealed interface State {
        Next next(String line, Builder builder);

        record RequestSeparator() implements State {
            @Override
            public Next next(String line, Builder builder) {
                if (Token.matchesEnd(line)) {
                    return null;
                } else if (Token.matchesRequestSeparator(line)) {
                    return new Next(null, new RequestFeature());
                }
                throw new IllegalStateException("Expected RequestSeparator, was '" + line + "'");
            }
        }

        record RequestOrSeparator() implements State {
            @Override
            public Next next(String line, Builder builder) {
                var nextState = new RequestFeature();
                if (Token.matchesRequestSeparator(line)) {
                    return new Next(null, nextState);
                }
                return new Next(line, nextState);
            }
        }

        record RequestFeature(List<VariableLine> variables, List<PropertyLine> properties) implements State {
            public RequestFeature() {
                this(new ArrayList<>(), new ArrayList<>());
            }

            @Override
            public Next next(String line, Builder builder) {
                if (Token.PropertyLine.matches(line)) {
                    properties.add(Token.PropertyLine.of(line));
                    return new Next(null, this);
                } else if (Token.VariableLine.matches(line)) {
                    variables.add(Token.VariableLine.of(line));
                    return new Next(null, this);
                }
                builder.variables = variables.stream()
                    .map(varLine -> new HttpFile.Variable(varLine.name(), HttpFile.StringTemplate.of(varLine.value())))
                    .toList();
                builder.properties = properties.stream()
                    .map(propLine -> new HttpFile.Property(propLine.name(), propLine.value()))
                    .toList();
                return new Next(line, new RequestLine());
            }
        }

        record RequestLine() implements State {
            @Override
            public Next next(String line, Builder builder) {
                if (Token.RequestLine.matches(line)) {
                    builder.requestLine = Token.RequestLine.of(line);
                    return new Next(null, new HeaderLines());
                }
                throw new IllegalStateException("Expected RequestLine, was '" + line + "'");
            }
        }

        record HeaderLines(List<HeaderLine> headers, HeaderLine current) implements State {
            public HeaderLines() {
                this(new ArrayList<>(), null);
            }

            @Override
            public Next next(String line, Builder builder) {
                if (Token.matchesEnd(line) || Token.matchesBlank(line)) {
                    // fall through
                } else if (Token.ContinuationLine.matches(line)) {
                    if (current == null) {
                        throw new IllegalStateException("No current HeaderLine for ContinuationLine");
                    }
                    return new Next(null, new HeaderLines(headers, new HeaderLine(current.name(), current.value() + line.trim())));
                } else if (Token.HeaderLine.matches(line)) {
                    if (current != null) {
                        headers.add(current);
                    }
                    return new Next(null, new HeaderLines(headers, Token.HeaderLine.of(line)));
                }
                if (current != null) {
                    headers.add(current);
                }
                builder.headers = headers.stream()
                    .map(headerLine -> new Header(headerLine.name(), HttpFile.StringTemplate.of(headerLine.value())))
                    .toList();
                return new Next(line, new BodyLines(new StringBuilder()));
            }
        }

        record BodyLines(StringBuilder bodyLines) implements State {
            @Override
            public Next next(String line, Builder builder) {
                if (Token.matchesEnd(line) || Token.matchesBlank(line)) {
                    var body = (bodyLines == null || bodyLines.isEmpty() ? null :
                        new Body(null, HttpFile.StringTemplate.of(bodyLines.toString())));
                    Request request = new Request(
                        builder.variables,
                        builder.properties,
                        builder.requestLine.verb(),
                        HttpFile.StringTemplate.of(builder.requestLine.target()),
                        builder.requestLine.version(),
                        builder.headers,
                        body
                    );
                    builder.variables = null;
                    builder.properties = null;
                    builder.requestLine = null;
                    builder.headers = null;
                    builder.requests.add(request);
                    return new Next(null, new RequestSeparator());
                } else {
                    if (bodyLines.length() > 0) {
                        bodyLines.append("\n");
                    }
                    bodyLines.append(line);
                    return new Next(null, this);
                }
            }
        }
    }

    public Model parse(Iterator<String> lines) {
        Builder builder = new Builder();
        Next next = new Next(null, new State.RequestOrSeparator());
        while (true) {
            System.out.println(next);
            String line = next.line();
            if (line == null && lines.hasNext()) {
                line = lines.next();
            }
            next = next.state().next(line, builder);
            if (next == null) {
                break;
            }
        }
        return new Model(builder.requests);
    }
    public Model parse(String input) {
        return parse(List.of(input.split("\n")).iterator());
    }

    private static String sample = """
        @host=localhost:8080
        @json=application/json
        @myname=Hallvard
        # @name post
        POST http://{{host}}/
        Content-Type: {{json}};
            encoding=utf-8
        Accept: {{json}}

        {
            "name": "{{myname}}"
        }
        
        """;

    public static void main(String[] args) {
        HttpFileParser parser = new HttpFileParser();
        System.out.println(parser.parse(List.of(sample.split("\n")).iterator()));
    }
}