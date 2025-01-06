package com.project.cadence.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class RestClient {
    private final RestTemplate rest;
    private final HttpHeaders headers;
    @Setter
    @Getter
    private HttpStatus status;

    public RestClient() {
        this.rest = new RestTemplate();
        this.headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "*/*");
    }

    public String get(String url) {
        HttpEntity<String> requestEntity = new HttpEntity<String>("", headers);
        ResponseEntity<String> responseEntity = rest.exchange(url, HttpMethod.GET, requestEntity, String.class);
        this.setStatus((HttpStatus) responseEntity.getStatusCode());
        return responseEntity.getBody();
    }

    public String post(String url, String json) {
        HttpEntity<String> requestEntity = new HttpEntity<String>(json, headers);
        ResponseEntity<String> responseEntity = rest.exchange(url, HttpMethod.POST, requestEntity, String.class);
        this.setStatus((HttpStatus) responseEntity.getStatusCode());
        return responseEntity.getBody();
    }

    public String put(String url, String json) {
        HttpEntity<String> requestEntity = new HttpEntity<>(json, headers);
        ResponseEntity<String> responseEntity = rest.exchange(url, HttpMethod.PUT, requestEntity, String.class);
        this.setStatus((HttpStatus) responseEntity.getStatusCode());
        return responseEntity.getBody();
    }

    public HttpStatus delete(String url) {
        HttpEntity<String> requestEntity = new HttpEntity<>("", headers);
        ResponseEntity<String> responseEntity = rest.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
        return (HttpStatus) responseEntity.getStatusCode();
    }

}
