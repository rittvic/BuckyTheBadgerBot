package buckythebadgerbot.services.impl;

import buckythebadgerbot.services.APIService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.util.*;

public class GymService extends APIService {
    private static final String BASE_URL = "https://goboardapi.azurewebsites.net/api/FacilityCount/GetCountsByAccount?AccountAPIKey=";

    private static final Logger logger = LoggerFactory.getLogger(GymService.class);

    //The standard timestamp format
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public GymService(String apiKey) {
        super(apiKey);
    }

    /**
     * Fetch live usage of every gym facility/location
     * @return An ArrayList of HashMaps for every main facility
     */
    public ArrayList<HashMap<String, String>> getGymUsages() {
        HttpRequest request = HttpRequest.newBuilder().GET().timeout(Duration.ofSeconds(5)).uri(URI.create(BASE_URL+this.apiKey)).build();
        HttpResponse<String> response;
        ObjectMapper objectMapper;
        List<JsonNode> gymEquipments;
        try {
            response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            objectMapper = new ObjectMapper();
            gymEquipments = objectMapper.readValue(response.body(), new TypeReference<>() {});
        } catch (IOException | InterruptedException e) {
            logger.error("Something went wrong with the API request! {}",e.toString());
            return null;
        }
        ArrayList<HashMap<String, String>> gymInformation = new ArrayList<>();
        try {
            //Sort the equipments based on "LastCount" value in descending order
            gymEquipments = gymEquipments.stream().sorted(Comparator.comparingInt((JsonNode obj) -> obj.get("LastCount").asInt()).reversed()).toList();

            //Set up HashMaps for the two facilities
            LinkedHashMap<String, String> nickFacility = new LinkedHashMap<>();
            LinkedHashMap<String, String> shellFacility = new LinkedHashMap<>();

            //Iterate through the list and store the respective information
            for (JsonNode equipment : gymEquipments) {
                String facilityName = equipment.get("FacilityName").asText();
                String locationName = equipment.get("LocationName").asText();
                String currentCount = equipment.get("LastCount").asText();
                String totalCapacity = equipment.get("TotalCapacity").asText();
                String lastUpdatedTime;

                //Convert the timestamp (which is set in US/Central) to unix timestamp (epoch)
                sdf.setTimeZone(TimeZone.getTimeZone(ZoneId.of("US/Central")));
                Date dt = sdf.parse(equipment.get("LastUpdatedDateAndTime").asText());
                long epoch = dt.getTime();
                lastUpdatedTime = String.valueOf((int) (epoch / 1000));

                //Add the locations to the respective hashmap
                if (facilityName.equals("Nicholas Recreation Center")) {
                    nickFacility.put(facilityName + "|" + locationName, "Usage: `" + currentCount + "/" + totalCapacity + "`" + "\n"
                            + "Last updated " + "<t:" + lastUpdatedTime + ":R>");
                } else if (facilityName.equals("Shell")) {
                    shellFacility.put(facilityName + "|" + locationName, "Usage: `" + currentCount + "/" + totalCapacity + "`" + "\n"
                            + "Last updated " + "<t:" + lastUpdatedTime + ":R>");
                }
            }

            gymInformation.add(nickFacility);
            gymInformation.add(shellFacility);
        } catch (Exception e) {
            logger.error("Something went wrong with JSON parsing! {}",e.toString());
            return null;
        }
        return gymInformation;
    }
}
