package com.sourav.interviewprep.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    String home() {
        return "index";
    }

    @GetMapping("/login")
    String login() {
        return "login";
    }

    @GetMapping("/register")
    String register() {
        return "register";
    }

    @GetMapping("/dashboard")
    String dashboard() {
        return "dashboard";
    }

    @GetMapping("/profile")
    String profile() {
        return "profile";
    }

    @GetMapping("/interviews")
    String interviews() {
        return "interviews";
    }

    @GetMapping("/interviews/{sessionId}")
    String interviewSession() {
        return "interview-session";
    }
}
