package com.example.bankcards.config;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@Validated
@ConfigurationProperties("card")
@Getter
@Setter
public class CardProperties {
    @Pattern(regexp = "^[0-9]{6}$")
    private String bin;
    private int retryLimit;
}