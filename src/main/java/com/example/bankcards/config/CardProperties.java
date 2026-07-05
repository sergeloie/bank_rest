package com.example.bankcards.config;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties("card")
@Getter
@Setter
public class CardProperties {
    @Pattern(regexp = "^\\d{6}$")
    private String bin;
    private int retryLimit;
}