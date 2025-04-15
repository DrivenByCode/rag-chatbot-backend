package com.example.chat.service;

import com.example.chat.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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

    @Cacheable(value = "messages", key = "#sessionId + ':recent:' + #count", unless = "#result.isEmpty()")
    public List<ChatMessage> getRecentMessages(String sessionId, int count) {
        String key = getKey(sessionId);
        long size = Optional.ofNullable(redisTemplate.opsForList().size(key)).orElse(0L);

        // 요청한 개수가 전체 메시지 수보다 크면 전체 메시지를 가져옴
        int effectiveCount = (int) Math.min(count, size);
        if (effectiveCount <= 0) {
            return List.of();
        }

        List<Object> raw = redisTemplate.opsForList().range(key, -effectiveCount, -1);
        List<ChatMessage> result = new ArrayList<>();
        if (raw != null) {
            raw.forEach(o -> result.add(objectMapper.convertValue(o, ChatMessage.class)));
        }
        return result;
    }

    @Cacheable(value = "messages", key = "#sessionId + ':all'", unless = "#result.isEmpty()")
    public List<ChatMessage> getAllMessages(String sessionId) {
        String key = getKey(sessionId);
        List<Object> raw = redisTemplate.opsForList().range(key, 0, -1);
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }

        return raw.stream()
                .map(o -> objectMapper.convertValue(o, ChatMessage.class))
                .toList();
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

    /**
     * 세션을 완전히 삭제합니다.
     */
@CacheEvict(value = {"messages", "summary"}, allEntries = true)
    public void clearSession(String sessionId) {
        String messagesKey = getKey(sessionId);
        String summaryKey = getSummaryKey(sessionId);

        // 여러 키를 한 번에 삭제
        redisTemplate.delete(Arrays.asList(messagesKey, summaryKey));
    }
}
