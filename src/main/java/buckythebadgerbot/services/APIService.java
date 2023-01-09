package buckythebadgerbot.services;

import java.net.http.HttpClient;

public abstract class APIService {
    protected final String apiKey;
    protected final HttpClient httpClient;

    public APIService(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newHttpClient();
    }
}
