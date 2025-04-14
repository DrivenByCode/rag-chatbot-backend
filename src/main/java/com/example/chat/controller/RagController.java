package com.example.chat.controller;

import com.example.chat.dto.ChatResponse;
import com.example.chat.service.RagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@RequestParam String sessionId, @RequestBody String question) {
        ChatResponse response = ragService.ask(sessionId, question);
        return ResponseEntity.ok(response);
    }
}
