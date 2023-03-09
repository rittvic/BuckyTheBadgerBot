package buckythebadgerbot.services.impl;

import buckythebadgerbot.commands.impl.uwmadison.SearchCommand;
import buckythebadgerbot.services.APIService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TimeZone;

public class DiningMenuService extends APIService {
    private static final String BASE_URL = "https://wisc-housingdining.api.nutrislice.com/menu/api/weeks/school/";

    private static final Logger logger = LoggerFactory.getLogger(DiningMenuService.class);

    public DiningMenuService() {
        super(null);
    }

    /**
     * Fetch a menu containing the food items of every food station at the passed in dining market
     * @param diningMarket the dining market to fetch the menu from
     * @param menuType the type of menu (Breakfast, Lunch, or Dinner)
     * @return HashMap containing every food station and its sub-categories matching with all food items in the sub-category
     */
    public HashMap<String,String> getDiningMenu(String diningMarket, String menuType){
        //Get the current date in CST
        String date = "\"" + LocalDate.now(TimeZone.getTimeZone("US/Central").toZoneId()) + "\"";

        //Call the API
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE_URL + diningMarket + "/menu-type/" + menuType + "/"
                        + date.replaceAll("-", "/").replaceAll("\"", "") + "/"))
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

        try {
            ArrayNode days = jsonNode.withArray("days");
            JsonNode day = null;
            //Iterate through every day until it reaches the specified date
            for (JsonNode dayOfWeek : days) {
                if (dayOfWeek.get("date").toString().equals(date)) {
                    day = dayOfWeek;
                    break;
                }
            }

            if (day == null) {
                System.out.println("day is null");
            }

            //LinkedHashMap in the format of {stationID-0stationName-0foodCategory = String containing all the items in the station and its category
            LinkedHashMap<String, String> stations = new LinkedHashMap<>();

            //HashMap to retrieve the stationID-0stationName-0foodCategory key from above HashMap by getting the station ID alone
            HashMap<String, String> stationsKey = new HashMap<>();

            //Iterate through every station and store its ID and name into the HashMap
            Map<String, JsonNode> menuInfo = objectMapper.convertValue(day.get("menu_info"), new TypeReference<>() {});
            for (Map.Entry<String, JsonNode> entry : menuInfo.entrySet()) {
                String stationID = entry.getKey();
                String stationName = entry.getValue().get("section_options").get("display_name").asText();
                stationsKey.put(stationID, stationID + "-0" + stationName);
            }

            ArrayNode menuItems = day.withArray("menu_items");
            JsonNode food;
            //Iterate through every food item and obtain the following information:
            //Food Station ID, Food Name, Food Category, Food Calories,
            for (JsonNode menuItem : menuItems) {
                if (!menuItem.get("food").isNull()) {
                    food = menuItem.get("food");
                    String stationID = menuItem.get("menu_id").asText();
                    stations.putIfAbsent(stationsKey.get(stationID) + "-0" + "Entree", null);
                    stations.putIfAbsent(stationsKey.get(stationID) + "-0" + "Side", null);
                    String foodName = food.get("name").asText();
                    String foodCategory = food.get("food_category").asText().isBlank() ? "Unknown" : food.get("food_category").asText();
                    foodCategory = foodCategory.substring(0, 1).toUpperCase() + foodCategory.substring(1);
                    String key = stationsKey.get(stationID) + "-0" + foodCategory;
                    String calories = food.get("rounded_nutrition_info").get("calories").asText().replace(".0", "").replace("null", "N/A");

                    //{stationID-0stationName-0foodCategory = String containing all the items in the station and its category
                    if (stations.get(key) == null){
                        stations.put(key,"\n" + "\u25B6 **" +  foodName + "**"  + "\n" + calories + " Cal");
                    } else {
                        stations.put(key,stations.get(key) + "\n" + "\u25B6 **" +  foodName + "**"  + "\n" + calories + " Cal");
                    }
                }
            }
            return stations;
            //Catch any null pointer exceptions and return a null hashmap
        } catch (Exception e) {
            logger.error("Something went wrong with JSON parsing! {}",e.toString());
            return null;
        }
    }

}
