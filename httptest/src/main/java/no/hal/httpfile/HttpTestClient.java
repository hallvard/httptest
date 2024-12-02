package no.hal.httpfile;

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
    
    private InputStreamProvider inputStreamProvider;
    private HttpClient httpClient;

    public HttpTestClient() {
        this.inputStreamProvider = new InputStreamProvider.Default();
        var builder = HttpClient.newBuilder();
        this.httpClient = builder.build();
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
        var stringTemplateValueProvider = new StringTemplateValueProvider();
        stringTemplateValueProvider.setInputStreamProvider(inputStreamProvider);
        for (var request : requests.requests()) {
            StringValueProvider stringValueProvider = new StringValueProvider.Providers(
                new StringValueProvider.Variables(request.requestVariables(), stringTemplateValueProvider),
                new StringValueProvider.MapEntries(results)
            );
            stringTemplateValueProvider.setStringValueProvider(stringValueProvider);
            try {
                var result = performRequest(request, stringTemplateValueProvider);
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
        var stringTemplateValueProvider = new StringTemplateValueProvider();
        stringTemplateValueProvider.setInputStreamProvider(inputStreamProvider);
        var stringValueProvider = new StringValueProvider.Variables(request.requestVariables(), stringTemplateValueProvider);
        stringTemplateValueProvider.setStringValueProvider(stringValueProvider);
        return performRequest(request, stringTemplateValueProvider);
    }

    private Map<String, Object> performRequest(HttpFile.Request request, StringTemplateValueProvider templateResolver) {
        var builder = HttpRequest.newBuilder(URI.create(templateResolver.toString(request.target())));
        for (var header : request.headers()) {
            builder.header(header.name(), templateResolver.toString(header.value()));
        }
        builder.method(request.method().name(), BodyPublishers.ofString(templateResolver.toString(request.body().content())));
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
                testClient.performRequest(request);
            }
        } catch (Exception ioe) {
        }
    }

    /*
    {{$guid}}
    {{$randomInt min max}}
    {{$timestamp [offset option]}}
    {{$datetime rfc1123|iso8601 [offset option]}}
    {{$localDatetime rfc1123|iso8601 [offset option]}}
    {{$processEnv [%]envVarName}}
    {{$dotenv [%]variableName}}
    {{$aadToken [new] [public|cn|de|us|ppe] [<domain|tenantId>] [aud:<domain|tenantId>]}}
     */
}