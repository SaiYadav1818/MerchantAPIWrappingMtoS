package com.sabbpe.merchant.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Controller to handle payment gateway redirect URLs.
 * Receives payment response parameters and displays them on HTML pages.
 */
@Slf4j
@Controller
@RequestMapping("/payment")
public class PaymentRedirectController {

    /**
     * Handle successful payment redirect from Easebuzz.
     * Displays all payment response parameters on success page.
     *
     * @param requestParams All request parameters from payment gateway
     * @param model Model to pass data to view
     * @return payment-success view
     */
    @PostMapping("/success")
    public String handlePaymentSuccess(@RequestParam Map<String, String> requestParams, Model model) {
        log.info("Payment Success Redirect - Parameters received: {}", requestParams.keySet());
        requestParams.forEach((key, value) -> log.debug("Param: {} = {}", key, value));

        // Add payment data to model for display
        model.addAttribute("paymentData", requestParams);
        model.addAttribute("title", "Payment Successful");

        return "payment-success";
    }

    /**
     * Handle failed payment redirect from Easebuzz.
     * Displays all payment response parameters on failure page.
     *
     * @param requestParams All request parameters from payment gateway
     * @param model Model to pass data to view
     * @return payment-failure view
     */
    @PostMapping("/failure")
    public String handlePaymentFailure(@RequestParam Map<String, String> requestParams, Model model) {
        log.warn("Payment Failure Redirect - Parameters received: {}", requestParams.keySet());
        requestParams.forEach((key, value) -> log.debug("Param: {} = {}", key, value));

        // Add payment data to model for display
        model.addAttribute("paymentData", requestParams);
        model.addAttribute("title", "Payment Failed");

        return "payment-failure";
    }
}
