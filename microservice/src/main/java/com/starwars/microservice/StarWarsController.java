package com.starwars.microservice;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
@RestController
@RequestMapping("/api/starwars")
class StarWarsController {

    private final StarWarsService starWarsService;

    public StarWarsController(StarWarsService starWarsService) {
        this.starWarsService = starWarsService;
    }

    @GetMapping("/{type}")
    public ResponseEntity<?> getStarWarsData(@PathVariable String type, @RequestParam String name) {
        try {
            List<Map<String, Object>> results = starWarsService.fetchAllResults(type, name);
            return ResponseEntity.ok(Map.of("count", results.size(), "results", results));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error accessing cache or API"));
        }
    }
}