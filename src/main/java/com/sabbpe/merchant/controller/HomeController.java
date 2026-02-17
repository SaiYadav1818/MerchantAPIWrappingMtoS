package com.sabbpe.merchant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Home controller for the application.
 * Provides links to test payment gateway redirects.
 */
@Controller
@RequestMapping("/")
public class HomeController {

    /**
     * Display home page with navigation and test links.
     *
     * @return home view
     */
    @GetMapping
    public String home() {
        return "home";
    }

    /**
     * Display payment gateway test form.
     *
     * @return payment gateway test form
     */
    @GetMapping("test")
    public String testPaymentGateway() {
        return "payment-gateway-test";
    }
}
