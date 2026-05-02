package com.example.homeprotect.controller;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.homeprotect.controller.docs.AddressControllerDocs;
import com.example.homeprotect.service.AddressService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/address")
@Validated
public class AddressController implements AddressControllerDocs {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @GetMapping("/search")
    public Mono<ResponseEntity<Map<String, Object>>> searchAddress(@RequestParam @NotBlank String query) {

            return addressService.searchAddress(query)
            .map(results -> ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of("results", results)
            )));
    }
}
