package io.github.abhjtm.booking.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.abhjtm.booking.dto.request.*;

@RestController
public class HelloController {

    @GetMapping("/")
    public String hello() {
        return "Welcome to your Abhijeet Mohanty's facility booking homepage!";
    }

    @GetMapping("/api/greeting")
    public String greeting() {
        return "Hello, test greeting from Abhijeet Mohanty's facility booking application!";
    }
}
