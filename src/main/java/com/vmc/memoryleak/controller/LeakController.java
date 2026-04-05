package com.vmc.memoryleak.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class LeakController {

    // Static root: Unintentional Object Retention using a static list
    // This simulates a common production issue where a custom cache never removes elements
    private static final List<String> customCache = new ArrayList<>();

    @GetMapping("/leak")
    public String triggerLeak() {
        // Simulating caching some data on every request
        // Because the list is static and never cleared, this will cause a memory leak
        for (int i = 0; i < 10000; i++) {
            // Generating strings and caching them
            customCache.add(UUID.randomUUID().toString() + " - " + System.currentTimeMillis());
        }
        return "Leak triggered! Added 10,000 items to the custom cache. Cache size: " + customCache.size();
    }

    @GetMapping("/clear")
    public String clearCache() {
        int previousSize = customCache.size();
        customCache.clear();
        return "Cache cleared. Removed " + previousSize + " items.";
    }
}
