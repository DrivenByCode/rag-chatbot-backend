# RAG 챗봇 백엔드

본 프로젝트는 **RAG(Retrieval-Augmented Generation)** 방식을 통해 챗봇을 구현한 Spring Boot 애플리케이션입니다. 본 프로젝트에선 쇼핑몰 고객 센터 상담봇을 기준으로
작성되었습니다. 사용자가 원하는 대로 수정 가능합니다. Qdrant 벡터 데이터베이스를 사용해 유사도 검색을 수행하고, Ollama를 사용한 임베딩모델 bge-m3로 텍스트 임베딩을 처리합니다.
또한 Redis를 통해 세션 및 대화 이력을 관리합니다.

## 프로젝트 특징

1. **RAG (Retrieval-Augmented Generation)**
    - 임베딩 서버에서 질의 문장을 벡터화하고, Qdrant를 통해 가장 관련성이 높은 문서를 검색한 후 답변에 반영합니다.
    - 검색된 문서가 부족하거나 유의미하지 않은 경우, Spring AI를 통해 LLM(폴백) 기반 답변을 생성할 수 있습니다.

2. **Redis를 활용한 세션 관리**
    - Redis에 사용자별 대화 기록과 세션 정보 등을 보관해 빠른 접근이 가능합니다.
    - 최근 대화 내용을 요약하는 기능도 제공합니다.

3. **유연한 설정 가능**
    - Spring AI 설정(embedding, vectorstore, ollama base-url 등)은 모두 `application.yml`에서 수정할 수 있습니다.

4. **FAQ 관리**
    - FAQ 데이터의 업데이트, 리로딩, 상태 확인 등 다양한 관리 기능을 제공합니다.

## 환경 설정 및 실행 방법

### 1. Qdrant 설치 및 실행

```shell script
docker run -d -p 6333:6333 -p 6334:6334 qdrant/qdrant
```

### 2. Redis 설치 및 실행

```shell script
docker run -d -p 6379:6379 redis
```

### 3. 임베딩 서버 실행

```shell script
ollama pull bge-m3
```

### 4. RAG 챗봇 백엔드 실행 (Gradle)

```shell script
./gradlew bootRun
```

## API 문서

본 챗봇 백엔드는 다음 REST API를 제공합니다:

### 1. 질의응답 API

**POST /chat/ask**

- **설명**: 사용자의 질문을 받아 RAG 방식으로 처리한 후 답변을 생성합니다.
- **파라미터**: `sessionId` (쿼리 파라미터, 필수)
- **요청 본문**: 질문 내용 (텍스트)
- **응답**:

```json
{
  "reply": "답변 내용...",
  "finishReason": "stop"
}
```

- **응답 코드**: 200 OK

### 2. FAQ 관리 API

**POST /api/faq/update**

- **설명**: FAQ 데이터베이스를 업데이트합니다.
- **응답**: 업데이트 상태 메시지 (텍스트)
- **응답 코드**: 200 OK

**POST /api/faq/reset-and-reload**

- **설명**: FAQ 데이터를 초기화하고 다시 로드합니다.
- **응답**: 재설정 상태 메시지 (텍스트)
- **응답 코드**: 200 OK

**POST /api/faq/reload**

- **설명**: FAQ 데이터를 재로딩합니다.
- **응답**: 재로딩 상태 메시지 (텍스트)
- **응답 코드**: 200 OK

**GET /api/faq/status**

- **설명**: FAQ 서비스 상태를 확인합니다.
- **응답**: 상태 정보 (텍스트)
- **응답 코드**: 200 OK

### 3. 대화 관리 API

**GET /chat/summary**

- **설명**: 사용자의 최근 대화 내용을 요약하여 반환합니다.
- **파라미터**: `sessionId` (쿼리 파라미터, 필수)
- **응답**: 요약 텍스트
- **응답 코드**: 200 OK

**GET /chat/backup**

- **설명**: 특정 세션의 대화 이력을 백업 파일로 다운로드합니다.
- **파라미터**: `sessionId` (쿼리 파라미터, 필수)
- **응답**: 백업 파일 (다운로드)
- **응답 코드**: 200 OK

## 설정 파일: application.yml

이 프로젝트에서 공통 설정을 관리하는 파일로, 주요 항목은 다음과 같습니다:

```yaml
server:
  port: 8080

spring:
  application:
    name: rag-chatbot-backend

  data:
    redis:
      host: localhost
      port: 6379
      timeout: 60000

  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: gemma3:4b
      embedding:
        model: bge-m3
        dimensions: 1024  # 실제 임베딩 모델의 차원을 확인 후 기입
    vectorstore:
      qdrant:
        host: localhost  # Qdrant 서버 주소
        port: 6334
        use-ssl: false   # 로컬: false, 프로덕션: true
        collection-name: vector-store # qdrant에 입력될 collectionname 지정

  jackson:
    serialization:
      indent-output: true

chatbot:
  role: 쇼핑몰 고객 센터 상담봇   # 챗봇 역할 설정
  instruction: 300자 이내로 짧고 빠르게 대답. 이름은 쇼핑몰 챗봇.  # 응답 지시사항
  score-threshold: 0.6         # 유사도 점수 임계값 (권장: 0.6 ~ 0.65)
  just-llm: false              # LLM 순수 기능만 사용할 경우 true (현재 사용하지 않음)
  rag-only: false              # FAQ 질문 외 상호작용 불가 옵션, 비용 절감을 위해 사용 가능

  summary:
    recent-message-count: 5    # 대화 요약에 사용할 최근 메시지 수

logging:
  level:
    root: INFO
    com.example.chat: DEBUG
```

- **서버 포트**: 8080 (기본값)
- **Redis 연결 정보**: localhost:6379, timeout 60초
- **Spring AI 설정**:
    - Ollama 모델: gemma3:4b
    - 임베딩 차원: 1024
    - 임베딩 서버: http://localhost:8001/embed
    - Qdrant 벡터 스토어: localhost:6334
- **챗봇 설정**:
    - role: 역할설정 -> ex) 쇼핑몰 고객 센터 상담봇
    - instruction: 지시사항 -> ex) 300자 이내로 짧고 빠르게 대답
    - score-threshold : 유사도 점수 임계값 -> 0.6 ~ 0.65 권장
    - just-llm: true로 설정하면 그냥 llm 순수 기능만 사용가능. -> 사용할 일 없음.
    - rag-only: true로 설정하면 llm을 완전히 사용안하게 됨. -> 비용감소 효과가 있지만, 사용자가 챗봇과 FAQ 질문 외엔 소통 할 수 없음. (상호작용 불가)

## 빌드 스크립트: build.gradle

Java 17 환경에서 Gradle을 사용하며, 다음 구성 요소가 포함되어 있습니다:

```groovy
plugins {
    id 'org.springframework.boot' version '3.2.2'
    id 'io.spring.dependency-management' version '1.1.4'
    id 'java'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }
    maven { url 'https://repo.spring.io/snapshot' }
    maven {
        name = 'Central Portal Snapshots'
        url = 'https://central.sonatype.com/repository/maven-snapshots/'
    }
}

// gRPC 버전 고정을 위한 변수 선언
ext {
    set('grpcVersion', '1.57.2')
}

dependencies {
    implementation platform("org.springframework.ai:spring-ai-bom:1.0.0-M6")
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'org.springframework.boot:spring-boot-starter'

    // swagger 설정
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0'

    implementation 'org.springframework.ai:spring-ai-core'
    implementation 'org.springframework.ai:spring-ai-ollama'
    implementation("org.springframework.ai:spring-ai-qdrant-store-spring-boot-starter")
    implementation 'io.qdrant:client:1.8.0'
    implementation 'com.fasterxml.jackson.core:jackson-databind'

    // gRPC 버전 명시적 지정
    implementation "io.grpc:grpc-netty-shaded:${grpcVersion}"
    implementation "io.grpc:grpc-protobuf:${grpcVersion}"
    implementation "io.grpc:grpc-stub:${grpcVersion}"

    implementation 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // 테스트 의존성
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

주요 의존성:

- Spring Boot 3.2.2
- Spring AI 1.0.0-M6 (Milestone 릴리스)
- Spring Data Redis
- Spring AI Ollama, Qdrant Store 등
- Swagger UI를 통한 API 문서화
- Lombok을 통한 보일러플레이트 코드 감소
- gRPC 관련 라이브러리 (버전 고정: 1.57.2) -> qdrant와 gRPC로 통신

## 기술 스택

- **Java 17 / Spring Boot 3.2.2**
- **Spring AI**: LLM 연동 및 RAG 흐름 제어
- **Qdrant**: 벡터 데이터베이스(질문과 문서의 임베딩 유사도 처리)
- **Redis**: 세션 및 대화 관리
- **Ollama**: 로컬 LLM(gemma3) 서비스 및 Embedding 모델 사용 (bge-m3)

## 사용 예시

### 1. 질문 요청하기

```shell script
curl -X POST "http://localhost:8080/chat/ask?sessionId=user123" \
     -H "Content-Type: application/json" \
     -d "환불 방법을 알려주세요"
```

응답:

```json
{
  "reply": "환불을 원하시면 구매하신 상품의 '환불 신청' 버튼을 클릭하시거나, 고객센터(1234-5678)로 연락해주세요. 환불 처리는 상품 회수 후 영업일 기준 3~5일 내에 완료됩니다. 더 자세한 내용은 홈페이지 하단의 '환불 정책'을 참고해주세요.",
  "finishReason": "stop"
}
```

### 2. 대화 요약 받기

```shell script
curl -X GET "http://localhost:8080/chat/summary?sessionId=user123"
```

응답:

```
사용자는 홈페이지에서 환불 방법과 배송 현황 조회에 대해 문의했습니다. 봇은 환불 신청 방법과 배송 조회 방법에 대한 안내를 제공했습니다.
```