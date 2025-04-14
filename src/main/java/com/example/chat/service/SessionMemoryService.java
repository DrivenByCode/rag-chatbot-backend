package com.example.chat.service;

import com.example.chat.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionMemoryService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Duration ttl = Duration.ofDays(7);
    private final ObjectMapper objectMapper;

    public void saveMessage(String sessionId, ChatMessage message) {
        String key = getKey(sessionId);
        redisTemplate.opsForList().rightPush(key, message);
        redisTemplate.expire(key, ttl);
    }

    public List<ChatMessage> getRecentMessages(String sessionId, int count) {
        String key = getKey(sessionId);
        List<Object> raw = redisTemplate.opsForList().range(key, -count, -1);
        List<ChatMessage> result = new ArrayList<>();
        if (raw != null) {
            for (Object o : raw) {
                ChatMessage msg = objectMapper.convertValue(o, ChatMessage.class);
                result.add(msg);
            }
        }
        return result;
    }

    public List<ChatMessage> getAllMessages(String sessionId) {
        String key = getKey(sessionId);
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        List<ChatMessage> result = new ArrayList<>();
        if (raw != null) {
            for (Object o : raw) {
                ChatMessage msg = objectMapper.convertValue(o, ChatMessage.class);
                result.add(msg);
            }
        }
        return result;
    }

    public int countMessages(String sessionId) {
        String key = getKey(sessionId);
        Long size = redisTemplate.opsForList().size(key);
        return size != null ? size.intValue() : 0;
    }

    public void saveSummary(String sessionId, String summary) {
        String key = getSummaryKey(sessionId);
        redisTemplate.opsForValue().set(key, summary, ttl);
    }

    public String getSummary(String sessionId) {
        String key = getSummaryKey(sessionId);
        return Optional.ofNullable(redisTemplate.opsForValue().get(key))
                .map(Object::toString)
                .orElse(null);
    }

    private String getKey(String sessionId) {
        return "session:" + sessionId + ":messages";
    }

    private String getSummaryKey(String sessionId) {
        return "session:" + sessionId + ":summary";
    }
}
