# 상세 기술 구현 계획서: 보안 채팅 서비스 (chat-be)

## 1. Goal Description
본 프로젝트는 Spring WebFlux와 Redis를 기반으로 하는 비동기 논블로킹 채팅 마이크로서비스(`chat-be`)를 구축하는 것을 목표로 합니다. 메시지는 종단간 암호화(E2EE)되어 서버를 통과하며, 1일(24시간) 후 Redis에서 자동 삭제됩니다. 단, 사용자가 신고할 경우 전자 서명 검증을 통해 위변조되지 않은 메시지에 한해 RDBMS에 영구 보관합니다. 부가적으로 WebRTC의 시그널링 역할을 수행하여 음성/화상 통화 연결을 지원합니다. 기존 `auth-be` 시스템과 JWT 토큰 기반으로 연동됩니다.

## 2. User Review Required
> [!IMPORTANT]
> **종단간 암호화(E2EE) 클라이언트 부담**
> 서버는 암호문만 중계하므로 메시지 복호화, 키 교환 관리, 서명 생성 등 핵심 암호학적 로직은 프론트엔드/앱 클라이언트가 직접 담당해야 합니다. 프론트엔드 측 작업량이 상당할 수 있습니다.

> [!WARNING]
> **미디어 서버 분리 필수**
> 본 계획에는 WebRTC 미디어 중계(TURN/SFU)가 포함되어 있지 않습니다. 다자간 화상 통화 등을 위해서는 추후 Coturn이나 mediasoup 같은 별도 서버를 인프라에 추가하셔야 합니다.

## 3. Open Questions
본격적인 개발 시작 전, 아래 항목들에 대한 결정이 필요합니다.
> [!CAUTION]
> 1. **데이터베이스 분리 여부**: 신고 내역 저장을 위한 RDBMS를 `auth-be`와 동일한 DB를 공유(Schema만 분리)하실 계획인가요? 아니면 `chat-be` 전용 독립 DB 인스턴스를 구축하실 계획인가요? 비동기 처리를 위해 R2DBC를 사용할 예정입니다.
> 2. **Redis 환경**: 현재 준비된 Redis가 Standalone 방식인지, 아니면 Cluster 방식으로 구성되어 있는지 확인 부탁드립니다. (설정 방식이 다릅니다)
> 3. **인증 토큰(JWT) 검증 방식**: 웹소켓 최초 연결 시 쿼리 파라미터나 첫 메시지 페이로드에 JWT 토큰을 실어 보내어 검증하는 방식을 사용할 예정입니다. 프론트엔드에서 이 방식이 가능한지 확인이 필요합니다.

---

## 4. Proposed Changes

### Project Setup & Dependencies
프로젝트 기본 설정 및 필요 라이브러리 추가
#### [MODIFY] `build.gradle` (또는 `pom.xml`)
- `spring-boot-starter-webflux`: 비동기 웹/웹소켓 통신
- `spring-boot-starter-data-redis-reactive`: 비동기 Redis 통신
- `spring-boot-starter-data-r2dbc`: 신고 내역 저장을 위한 비동기 RDBMS 통신
- `jjwt` 또는 `spring-security-oauth2-resource-server`: 기존 JWT 검증용

---

### Configuration
연결 및 보안 관련 설정 컴포넌트
#### [NEW] `config/RedisConfig.java`
- `ReactiveRedisTemplate` 설정
- 메시지 직렬화/역직렬화 및 Key 만료(1일 TTL) 정책 설정
#### [NEW] `config/WebSocketConfig.java`
- 웹소켓 엔드포인트 등록 (`/ws/chat`, `/ws/signaling`)
- WebFlux `WebSocketHandlerAdapter` 매핑 설정
#### [NEW] `config/SecurityConfig.java`
- `auth-be`에서 발급한 JWT 토큰의 유효성을 검증하는 필터
- 웹소켓 연결 시 인증 처리 로직

---

### Chat Domain (Redis 기반 메시징)
채팅 송수신 및 E2EE 처리를 담당하는 핵심 로직
#### [NEW] `domain/chat/dto/ChatMessageDto.java`
```java
public class ChatMessageDto {
    private String roomId;
    private String senderId;
    private String encryptedPayload; // E2EE 암호문
    private String digitalSignature; // 위변조 방지용 송신자 전자 서명
    private long timestamp;
}
```
#### [NEW] `domain/chat/repository/ReactiveChatRepository.java`
- Redis `ListOperations` 또는 `ZSetOperations`를 사용하여 방별로 메시지 저장
- 메시지 저장 시 `.expire(Duration.ofDays(1))` 호출을 통해 TTL 1일 보장
#### [NEW] `domain/chat/handler/ChatWebSocketHandler.java`
- 클라이언트 세션 관리 (방 접속, 퇴장)
- 수신된 메시지를 Redis에 저장하고 해당 방의 다른 세션으로 브로드캐스팅 (Pub/Sub 연동 고려)

---

### Key Exchange Domain (공개키 관리)
#### [NEW] `domain/key/controller/KeyController.java`
- 클라이언트가 자신의 '공개키'를 서버에 등록하고, 대화 상대방의 '공개키'를 조회하는 REST API (`/api/keys`)
- 저장소: Redis 사용

---

### Report Domain (신고 및 데이터 위변조 방지)
가장 중요한 보안 검증 로직 및 영구 저장소 연결
#### [NEW] `domain/report/entity/Report.java`
- 신고된 메시지, 복호화된 평문, 신고자, 피신고자 정보를 담는 R2DBC 엔티티 (RDBMS 저장용)
#### [NEW] `domain/report/service/ReportSignatureVerifier.java`
- 신고자가 제출한 암호문과 '송신자의 공개키'를 이용하여 `digitalSignature`를 검증하는 암호학적 검증 서비스
#### [NEW] `domain/report/controller/ReportController.java`
- `POST /api/reports` 엔드포인트
- 검증 서비스(`ReportSignatureVerifier`) 통과 시에만 RDBMS에 데이터 INSERT

---

### WebRTC Signaling Domain
음성/화상 통화를 위한 시그널링
#### [NEW] `domain/webrtc/handler/SignalingHandler.java`
- SDP Offer/Answer 및 ICE Candidate 교환을 위한 별도 웹소켓 핸들러
- Redis Pub/Sub을 이용해 다중 서버 환경에서도 시그널링이 가능하도록 설계

---

## 5. Verification Plan

### Automated Tests
- **단위 테스트 (Unit Tests):**
  - `ReportSignatureVerifierTest`: 임의의 키페어를 생성해 올바른 서명과 변조된 서명을 서버가 정확히 구별(성공/실패)해 내는지 검증.
- **통합 테스트 (Integration Tests):**
  - `Testcontainers`를 활용하여 일회성 Redis 인스턴스를 띄우고 웹소켓 메시지가 Redis에 정상 저장 및 조회되는지 검증 (`StepVerifier` 사용).
  - 메시지 저장 후 TTL 설정이 정확히 24시간으로 셋팅되는지 검증.

### Manual Verification
1. **웹소켓 통신 테스트**: `wscat` 또는 프론트엔드 목업을 통해 JWT를 넘겨 `/ws/chat`에 연결한 후, JSON 형태의 암호화된 메시지가 메아리(echo/broadcast)되어 돌아오는지 확인.
2. **TTL 확인**: Redis CLI (`redis-cli`)에서 `TTL [key]` 명령어를 통해 새로 입력된 메시지의 잔여 수명이 86400초 부근으로 떨어지고 있는지 확인.
3. **신고 API 테스트**: Postman을 사용해 올바른 서명과 틀린 서명을 각각 `POST /api/reports`로 전송해보고, 400 Bad Request와 200 OK(DB 저장 완료)가 의도대로 떨어지는지 확인.
