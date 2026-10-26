
package com.anubhav.Backend.controller;

import com.anubhav.Backend.dto.AiRequest;
import com.anubhav.Backend.service.OllamaChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class OllamaController {

    private final OllamaChatService ollamaChatService;

    public OllamaController(OllamaChatService ollamaChatService) {
        this.ollamaChatService = ollamaChatService;
    }

    @PostMapping("/ask")
    public String ask(@RequestBody AiRequest request) {
        return ollamaChatService.ask(request.getQuestion());
    }

}
