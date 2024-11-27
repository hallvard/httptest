package no.hal.httptest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpTestClient implements AutoCloseable {
    
    private HttpClient httpClient;

    public HttpTestClient() {
        var builder = HttpClient.newBuilder();
        httpClient = builder.build();
    }

    @Override
    public void close() throws Exception {
        if (httpClient != null && !httpClient.isTerminated()) {
            try {
                httpClient.close();
            } finally {
                httpClient = null;
            }
        }
    }

    public Map<String, Object> performRequests(HttpFile.Model requests) {
        Map<String, Object> results = new HashMap<>();
        for (var request : requests.requests()) {
            var stringValueProvider = new StringValueProvider.Functions(
                new StringValueProvider.Variables(request.requestVariables()),
                new StringValueProvider.MapEntries(results)
            );
            try {
                var result = performRequest(request, stringValueProvider);
                var requestName = request.getRequestPropertyValue("name");
                if (requestName.isPresent()) {
                    results.put(requestName.get(), result);
                }
            } catch (Exception ex) {
                System.err.println("Aborting, due to exception when performing %s %s".formatted(request.method(), request.target()));
                break;
            }
        }
        return results;
    }

    public Map<String, Object> performRequest(HttpFile.Request request) {
        return performRequest(request, new StringValueProvider.Variables(request.requestVariables()));
    }

    private Map<String, Object> performRequest(HttpFile.Request request, StringValueProvider valueProvider) {
        var builder = HttpRequest.newBuilder(URI.create(request.target().toString(valueProvider)));
        for (var header : request.headers()) {
            builder.header(header.name(), header.value().toString(valueProvider));
        }
        builder.method(request.method().name(), BodyPublishers.ofString(request.body().content().toString(valueProvider)));
        var httpRequest = builder.build();
        
        var requestMap = Map.of(
            "uri", httpRequest.uri(),
            "headers", httpRequest.headers().map()
        );
        try {
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, BodyHandlers.ofString());
            var responseMap = Map.of(
                "status", httpResponse.statusCode(),
                "headers", httpResponse.headers(),
                "body", httpResponse.body()
            );
            return Map.of("request", requestMap, "response", responseMap);
        } catch (IOException | InterruptedException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String sample = """
        @host=www.vg.no
        GET https://{{host}}/
        Accept: text/html
        
        """;

    public static void main(String[] args) {
        HttpFileParser parser = new HttpFileParser();
        var requests = parser.parse(List.of(sample.split("\n")).iterator());
        try (var testClient = new HttpTestClient()) {
            for (var request : requests.requests()) {
                System.out.println(request.requestVariables());
                testClient.performRequest(request, new StringValueProvider.Variables(request.requestVariables()));
            }
        } catch (Exception ioe) {
        }
    }
}