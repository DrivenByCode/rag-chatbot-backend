package com.example.chat.controller;

import com.example.chat.dto.ChatMessage;
import com.example.chat.service.SessionMemoryService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequiredArgsConstructor
public class BackupController {

    private final SessionMemoryService memoryService;

    @GetMapping("/chat/backup")
    public void downloadBackup(@RequestParam String sessionId, HttpServletResponse response) throws IOException {
        List<ChatMessage> messages = memoryService.getAllMessages(sessionId);
        List<String> messageTexts = messages.stream()
                .map(m -> m.role() + ": " + m.content())
                .toList();

        String content = String.join("\n\n", messageTexts);
        String summary = memoryService.getSummary(sessionId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        // 대화 로그 저장
        zos.putNextEntry(new ZipEntry("chat-log.txt"));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        // 요약 저장
        zos.putNextEntry(new ZipEntry("summary.txt"));
        zos.write((summary != null ? summary : "요약 없음").getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();

        // 스트림 종료
        zos.close();

        // 응답 설정
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=chat-backup.zip");
        response.getOutputStream().write(baos.toByteArray());
    }
}
