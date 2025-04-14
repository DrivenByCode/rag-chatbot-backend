package com.example.chat.util.store;

import com.example.chat.config.OllamaProperties;
import com.example.chat.config.QdrantProperties;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.Condition;
import io.qdrant.client.grpc.Points.FieldCondition;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.Match;
import io.qdrant.client.grpc.QdrantOuterClass.HealthCheckReply;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QdrantAdminUtil {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);
    private static final String FAQ_TYPE = "FAQ";
    private final QdrantClient qdrantClient;
    private final QdrantProperties qdrantProperties;
    private final OllamaProperties ollamaProperties;

    @PreDestroy
    public void cleanup() {
        log.info("QdrantAdminUtil 종료 중...");
        if (qdrantClient != null) {
            qdrantClient.close();
        }
        log.info("QdrantAdminUtil 종료 됨");
    }

    /**
     * FAQ 타입의 포인트 삭제 (metadata.type == FAQ_TYPE)
     */
    public boolean deleteFaqPoints() {
        try {
            log.info("qdrant points 삭제 중");
            // 필터 생성: metadata.type == FAQ_TYPE
            Filter filter = Filter.newBuilder()
                    .addMust(
                            Condition.newBuilder()
                                    .setField(
                                            FieldCondition.newBuilder()
                                                    .setKey("type")
                                                    .setMatch(Match.newBuilder().setKeyword(FAQ_TYPE).build())
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();
            // deleteAsync(String collectionName, Filter filter, Duration timeout) 메서드 사용
            qdrantClient.deleteAsync(qdrantProperties.getCollectionName(), filter, DEFAULT_TIMEOUT).get();
            log.info("qdrant points 삭제 성공");
            return true;
        } catch (Exception e) {
            log.error("qdrant points 삭제 실패", e);
            return false;
        }
    }

    /**
     * 컬렉션 존재 여부 확인
     */
    public boolean checkCollectionExists() {
        try {
            log.info("Checking if collection '{}' exists", qdrantProperties.getCollectionName());
            boolean exists = qdrantClient.collectionExistsAsync(qdrantProperties.getCollectionName(), DEFAULT_TIMEOUT)
                    .get();
            log.info("Collection '{}' exists: {}", qdrantProperties.getCollectionName(), exists);
            return exists;
        } catch (Exception e) {
            log.error("컬렉션 존재 확인 실패", e);
            return false;
        }
    }

    /**
     * 컬렉션 생성
     */
    public boolean createCollection() {
        try {
            log.info("Creating collection '{}'", qdrantProperties.getCollectionName());
            VectorParams vectorParams = VectorParams.newBuilder()
                    .setSize(ollamaProperties.getEmbedding().getDimensions())
                    .setDistance(Distance.Cosine)
                    .build();
            // createCollectionAsync(String, VectorParams, Duration) 사용
            qdrantClient.createCollectionAsync(qdrantProperties.getCollectionName(), vectorParams, DEFAULT_TIMEOUT)
                    .get();
            log.info("Collection '{}' created", qdrantProperties.getCollectionName());
            return true;
        } catch (Exception e) {
            log.error("컬렉션 생성 에러", e);
            return false;
        }
    }

    /**
     * Qdrant 서버 연결 확인
     */
    public boolean checkQdrantConnection() {
        try {
            log.info("Qdrant 서버 연결 확인 시도...");

            // healthCheckAsync 사용 예시
            HealthCheckReply reply = qdrantClient
                    .healthCheckAsync(DEFAULT_TIMEOUT)
                    .get();

            log.info("Qdrant 서버 HealthCheck: title={}", reply.getTitle());
            return true;
        } catch (Exception e) {
            log.error("Qdrant 서버 건강 상태 확인 중 오류 발생", e);
            return false;
        }
    }

    /**
     * 컬렉션 초기화 (존재하면 삭제 후 재생성)
     */
    public boolean resetCollection() {
        try {
            if (checkCollectionExists()) {
                log.info("초기화를 위해 collection 삭제 중 '{}'", qdrantProperties.getCollectionName());
                qdrantClient.deleteCollectionAsync(qdrantProperties.getCollectionName(), DEFAULT_TIMEOUT).get();
                log.info("Collection '{}' 삭제 완료", qdrantProperties.getCollectionName());
            }
            return createCollection();
        } catch (Exception e) {
            log.error("qdrant collecion 초기화 실패", e);
            return false;
        }
    }


    /**
     * 컬렉션 내 포인트 개수 조회
     */
    public long countPoints() {
        try {
            log.info("Counting points in collection '{}'", qdrantProperties.getCollectionName());
            long count = qdrantClient.countAsync(qdrantProperties.getCollectionName(), null, true, DEFAULT_TIMEOUT)
                    .get();
            log.info("Collection '{}' has {} points", qdrantProperties.getCollectionName(), count);
            return count;
        } catch (Exception e) {
            log.error("qdrant points 갯수 세기 실패", e);
            return -1;
        }
    }

}