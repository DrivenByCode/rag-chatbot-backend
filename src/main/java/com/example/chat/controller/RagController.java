package com.example.chat.controller;

import com.example.chat.dto.ChatMessage;
import com.example.chat.dto.ChatResponse;
import com.example.chat.service.RagService;
import com.example.chat.service.SessionMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;
    private final SessionMemoryService memoryService;

    @PostMapping("/ask")
    public ResponseEntity<ChatResponse> ask(@RequestParam String sessionId, @RequestBody String question) {
        ChatResponse response = ragService.ask(sessionId, question);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getHistory(@PathVariable String sessionId) {
        List<ChatMessage> history = memoryService.getRecentMessages(sessionId, 100);
        return ResponseEntity.ok(history);
    }
    
    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<Void> clearHistory(@PathVariable String sessionId) {
        memoryService.clearSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
