package com.anubhav.Backend.github;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GitHubConfig {

    @Value("${github.app.id}")
    private Long appId;

    @Value("${github.app.private-key-path}")
    private String privateKeyPath;

    public Long getAppId() {
        return appId;
    }

    public String getPrivateKeyPath() {
        return privateKeyPath;
    }
}