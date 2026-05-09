package com.piedrazul.backend.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.rate-limiting")
public class RateLimitingProperties {

    private List<Policy> publicPolicies = new ArrayList<>();
    private List<Policy> pacientePolicies = new ArrayList<>();

    public List<Policy> getPublicPolicies() {
        return publicPolicies;
    }

    public void setPublicPolicies(List<Policy> publicPolicies) {
        this.publicPolicies = publicPolicies;
    }

    public List<Policy> getPacientePolicies() {
        return pacientePolicies;
    }

    public void setPacientePolicies(List<Policy> pacientePolicies) {
        this.pacientePolicies = pacientePolicies;
    }

    public static class Policy {
        private String id;
        private String method;
        private String path;
        private int capacity;
        private Duration window;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }
    }
}

