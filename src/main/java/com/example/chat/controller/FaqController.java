package com.example.chat.controller;

import com.example.chat.service.FaqLoaderService;
import com.example.chat.util.store.QdrantAdminUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/faq")
@RequiredArgsConstructor
@Slf4j
public class FaqController {

    private final FaqLoaderService faqLoaderService;
    private final QdrantAdminUtil qdrantAdminUtil;

    /**
     * FAQ 데이터를 다시 로드합니다 (기존 FAQ 제거 후 새로 적재)
     * 오류 처리 및 반환값 개선
     */
    @PostMapping("/reload")
    public ResponseEntity<String> reloadFaqs() {
        log.info("FAQ 데이터 재로드 요청 받음");

        // 1. 서버 연결 상태 확인
        if (!qdrantAdminUtil.checkQdrantConnection()) {
            return ResponseEntity.status(503)
                    .body("Qdrant 서버에 연결할 수 없습니다. 서버 상태를 확인하세요.");
        }

        try {
            // 2. 기존 FAQ 데이터 삭제
            boolean deleted = qdrantAdminUtil.deleteFaqPoints();
            if (!deleted) {
                return ResponseEntity.status(500)
                        .body("기존 FAQ 데이터 삭제에 실패했습니다. 다시 시도해주세요.");
            }

            // 3. FAQ 데이터 새로 적재
            boolean loaded = faqLoaderService.loadAndIndexFaqs();
            if (!loaded) {
                return ResponseEntity.status(500)
                        .body("FAQ 데이터 적재에 실패했습니다. 파일 형식과 내용을 확인하세요.");
            }

            // 4. 적재된 문서 수 확인
            long docCount = faqLoaderService.countFaqDocuments();

            return ResponseEntity.ok(
                    String.format("FAQ 데이터가 성공적으로 다시 로드되었습니다. 적재된 문서 수: %d", docCount)
            );
        } catch (Exception e) {
            log.error("FAQ 데이터 재로드 중 예기치 않은 오류 발생", e);
            return ResponseEntity.status(500)
                    .body("FAQ 데이터 재로드 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * FAQ 데이터를 업데이트합니다 (새 항목만 추가)
     * 오류 처리 및 반환값 개선
     */
    @PostMapping("/update")
    public ResponseEntity<String> updateFaqs() {
        log.info("FAQ 데이터 업데이트 요청 받음");

        // 1. 서버 연결 상태 확인
        if (!qdrantAdminUtil.checkQdrantConnection()) {
            return ResponseEntity.status(503)
                    .body("Qdrant 서버에 연결할 수 없습니다. 서버 상태를 확인하세요.");
        }

        try {
            // 2. FAQ 데이터 업데이트
            boolean updated = faqLoaderService.updateFaqs();
            if (!updated) {
                return ResponseEntity.status(500)
                        .body("FAQ 데이터 업데이트에 실패했습니다. 다시 시도해주세요.");
            }

            // 3. 적재된 문서 수 확인
            long docCount = faqLoaderService.countFaqDocuments();

            return ResponseEntity.ok(
                    String.format("FAQ 데이터가 성공적으로 업데이트되었습니다. 현재 문서 수: %d", docCount)
            );
        } catch (Exception e) {
            log.error("FAQ 데이터 업데이트 중 예기치 않은 오류 발생", e);
            return ResponseEntity.status(500)
                    .body("FAQ 데이터 업데이트 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 모든 벡터 데이터를 초기화하고 FAQ를 다시 로드합니다.
     * 오류 처리 및 반환값 개선
     */
    @PostMapping("/reset-and-reload")
    public ResponseEntity<String> resetAndReload() {
        log.info("Qdrant 컬렉션 초기화 및 FAQ 데이터 재로드 요청 받음");

        // 1. 서버 연결 상태 확인
        if (!qdrantAdminUtil.checkQdrantConnection()) {
            return ResponseEntity.status(503)
                    .body("Qdrant 서버에 연결할 수 없습니다. 서버 상태를 확인하세요.");
        }

        try {
            // 2. 컬렉션 초기화
            boolean reset = qdrantAdminUtil.resetCollection();
            if (!reset) {
                return ResponseEntity.status(500)
                        .body("Qdrant 컬렉션 초기화에 실패했습니다. 다시 시도해주세요.");
            }

            // 3. FAQ 데이터 새로 적재
            boolean loaded = faqLoaderService.loadAndIndexFaqs();
            if (!loaded) {
                return ResponseEntity.status(500)
                        .body("FAQ 데이터 적재에 실패했습니다. 파일 형식과 내용을 확인하세요.");
            }

            // 4. 적재된 문서 수 확인
            long docCount = faqLoaderService.countFaqDocuments();

            return ResponseEntity.ok(
                    String.format("Qdrant 컬렉션이 초기화되고 FAQ 데이터가 다시 로드되었습니다. 적재된 문서 수: %d", docCount)
            );
        } catch (Exception e) {
            log.error("컬렉션 초기화 및 재로드 중 예기치 않은 오류 발생", e);
            return ResponseEntity.status(500)
                    .body("컬렉션 초기화 및 재로드 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 현재 FAQ 상태를 확인합니다.
     */
    @GetMapping("/status")
    public ResponseEntity<String> checkFaqStatus() {
        try {
            boolean connected = qdrantAdminUtil.checkQdrantConnection();
            if (!connected) {
                return ResponseEntity.status(503)
                        .body("Qdrant 서버에 연결할 수 없습니다.");
            }

            long docCount = faqLoaderService.countFaqDocuments();

            return ResponseEntity.ok(
                    String.format("FAQ 시스템 상태: 정상, 저장된 FAQ 문서 수: %d", docCount)
            );
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("FAQ 상태 확인 중 오류 발생: " + e.getMessage());
        }
    }
}