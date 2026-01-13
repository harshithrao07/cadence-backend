package com.project.cadence.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/app/v1")
public class AppController {

    @GetMapping("/ping")
    public ResponseEntity<String> pong() {
        return ResponseEntity.ok("pong");
    }

}
