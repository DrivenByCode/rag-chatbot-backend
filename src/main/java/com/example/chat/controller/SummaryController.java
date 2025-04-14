package com.example.chat.controller;

import com.example.chat.service.SummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping("/summary")
    public ResponseEntity<String> getSummary(@RequestParam String sessionId) {
        log.info("세션 {} : 대화 요약 요청", sessionId);

        String summary = summaryService.getSummary(sessionId);
        return ResponseEntity.ok(summary);
    }
}
