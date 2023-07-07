package buckythebadgerbot.services.impl;

import buckythebadgerbot.data.RegStudentOrg;
import buckythebadgerbot.services.APIService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RSOService extends APIService {
    private static final String BASE_URL = "https://win.wisc.edu/api/discovery/search/organizations?";

    private static final Logger logger = LoggerFactory.getLogger(RSOService.class);

    public RSOService() {
        super(null);
    }


    /**
     * Query registered student organizations through win.wisc.edu
     * @param query the query keyword
     * @return A list of every org in the query
     */
    public List<RegStudentOrg> getOrgs(String query) {

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE_URL + "top=10000&filter=&query=" + URLEncoder.encode(query,StandardCharsets.UTF_8) +"&skip=0"))
                .build();

        HttpResponse<String> response;
        ObjectMapper objectMapper;
        JsonNode jsonNode;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            objectMapper = new ObjectMapper();
            jsonNode = objectMapper.readTree(response.body());
        } catch (IOException | InterruptedException e) {
            logger.error("Something went wrong with the API request! {}",e.toString());
            logger.error("Request URL: {}",request.uri());
            return null;
        }
        ArrayNode results = jsonNode.withArray("value");
        ArrayList<RegStudentOrg> orgs = new ArrayList<>();
        try {
            RegStudentOrg regStudentOrg;
            for (JsonNode orgData : results) {
                regStudentOrg = objectMapper.readValue(orgData.toString(), RegStudentOrg.class);
                orgs.add(regStudentOrg);
            }
        } catch (JsonProcessingException e) {
            logger.error("Something went wrong with JSON parsing! {}",e.toString());
            return null;
        }
        return orgs;
    }
}
