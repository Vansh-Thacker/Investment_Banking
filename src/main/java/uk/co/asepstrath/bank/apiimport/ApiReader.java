package uk.co.asepstrath.bank.apiimport;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiReader {

    private static final HttpClient client = HttpClient.newHttpClient();

    public static String get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> res =
                client.send(req, HttpResponse.BodyHandlers.ofString());

        return res.body();
    }
}