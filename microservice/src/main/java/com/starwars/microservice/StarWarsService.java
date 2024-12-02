package com.starwars.microservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
class StarWarsService {

    @Value("${starwars.cache.directory}")
    private String cacheDirectory;

    @Value("${starwars.cache.enable}")
    private boolean cacheEnabled;

    @Value("${starwars.api.base-url}")
    private String apiBaseUrl;

    @Value("${starwars.offline.mode}")
    private boolean offlineMode;

    @PostConstruct
    public void initializeCacheDirectory() {
        Path path = Paths.get(cacheDirectory);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create cache directory", e);
            }
        }
    }

    public List<Map<String, Object>> fetchAllResults(String type, String name) throws IOException {
        validateType(type);
        String cacheFilePath = cacheDirectory + "/" + type + ".json";

        if (offlineMode || (cacheEnabled && Files.exists(Paths.get(cacheFilePath)))) {
            return fetchFromCache(cacheFilePath, name);
        }

        List<Map<String, Object>> allResults = new ArrayList<>();
        String nextPage = apiBaseUrl + type + "/";

        while (nextPage != null) {
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.getForEntity(nextPage, Map.class);

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) break;

            List<Map<String, Object>> results = (List<Map<String, Object>>) responseBody.get("results");

            for (Map<String, Object> item : results) {
                if (item.get("name").toString().equalsIgnoreCase(name)) {
                    allResults.add(item);
                }
            }
            nextPage = (String) responseBody.get("next");
        }

        if (cacheEnabled) {
            saveToCache(cacheFilePath, allResults);
        }

        return allResults;
    }

    private List<Map<String, Object>> fetchFromCache(String cacheFilePath, String name) throws IOException {
        String content = Files.readString(Paths.get(cacheFilePath));
        List<Map<String, Object>> cachedResults = new ObjectMapper().readValue(content, List.class);
        List<Map<String, Object>> filteredResults = new ArrayList<>();

        for (Map<String, Object> item : cachedResults) {
            if (item.get("name").toString().equalsIgnoreCase(name)) {
                filteredResults.add(item);
            }
        }
        return filteredResults;
    }

    private void saveToCache(String cacheFilePath, List<Map<String, Object>> data) throws IOException {
        String content = new ObjectMapper().writeValueAsString(data);
        Files.writeString(Paths.get(cacheFilePath), content);
    }

    private void validateType(String type) {
        List<String> validTypes = Arrays.asList("people", "planets", "starships", "vehicles", "species", "films");
        if (!validTypes.contains(type.toLowerCase())) {
            throw new IllegalArgumentException("Invalid type provided. Must be one of: " + String.join(", ", validTypes));
        }
    }
}
