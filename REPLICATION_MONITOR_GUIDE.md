# Database Replication Monitor 구현 가이드

## 📋 개요
Active DB와 Standby DB 간의 실시간 데이터 동기화 및 자동 복구 시스템을 모니터링하는 대시보드입니다.

### 1. Backend (DTO, Controller, Service)

- **DTOs**: 
  - `DatabaseStatusDto`: Active/Standby DB 상태 정보
  - `SynchronizationMetricsDto`: 동기화 메트릭 (동기화율, lag, binlog position 등)
  - `BinlogEventDto`: Binlog 이벤트 정보
  - `SystemEventDto`: 시스템 이벤트 (동기화 완료, failover, 경고 등)
  - `ReplicationActivityDto`: 복제 활동 타임라인
  - `KubernetesSelectorDto`: K8s 서비스 셀렉터 정보
  - `DashboardResponseDto`: 전체 대시보드 응답

- **Controller**: `ReplicationMonitorController`
  - `GET /replication/monitor`: 대시보드 페이지
  - `GET /replication/api/dashboard`: 실시간 데이터 API

- **Service**: `ReplicationMonitorService`
  - 현재 Mock 데이터로 구현되어 있음
  - 실제 데이터 소스와 연결이 필요한 메서드들:
    - `getActiveDatabaseStatus()` 
    - `getStandbyDatabaseStatus()`
    - `getSynchronizationMetrics()`
    - `getKubernetesSelector()`
    - `getReplicationActivity()`
    - `getRecentBinlogEvents()`
    - `getSystemEvents()`

### 2. Frontend (HTML 템플릿)
Artifact로 생성한 `replication-monitor.html`을 다음 경로에 저장하세요:
```
src/main/resources/templates/manager/replication-monitor.html
```

## 🔧 구현해야 할 내용

### 1. 실제 데이터베이스 모니터링 연동

#### Active/Standby DB 상태 조회
```java
// ReplicationMonitorService.java
private DatabaseStatusDto getActiveDatabaseStatus() {
    // TODO: 실제 Active DB 연결 및 상태 조회
    // - JDBC 또는 MySQL Exporter API 사용
    // - 현재 연결 수: SHOW STATUS LIKE 'Threads_connected'
    // - QPS 계산
    // - Uptime: SHOW STATUS LIKE 'Uptime'
    // - Latency: 쿼리 응답 시간 측정
}
```

#### 동기화율 계산
```java
private SynchronizationMetricsDto getSynchronizationMetrics() {
    // TODO: 테이블 row 해시값 기반 동기화율 계산
    // 1. Active DB의 각 테이블별 체크섬 계산
    // 2. Standby DB의 각 테이블별 체크섬 계산
    // 3. 일치율 퍼센트 계산
    // 4. Binlog position 조회: SHOW MASTER STATUS / SHOW SLAVE STATUS
    // 5. Replication lag 계산: Seconds_Behind_Master
}
```

#### Kubernetes Selector 조회
```java
private KubernetesSelectorDto getKubernetesSelector() {
    // TODO: K8s API 호출
    // 1. Kubernetes Java Client 사용
    // 2. Service의 selector 조회
    // 3. 현재 타겟팅된 Pod 정보 조회
    // 예시:
    // CoreV1Api api = new CoreV1Api();
    // V1Service service = api.readNamespacedService("mysql-service", "default", null);
    // Map<String, String> selector = service.getSpec().getSelector();
}
```

#### Binlog 이벤트 파싱
```java
private List<BinlogEventDto> getRecentBinlogEvents() {
    // TODO: Binlog 파싱 라이브러리 사용
    // 1. mysql-binlog-connector-java 의존성 추가
    // 2. BinaryLogClient 설정
    // 3. INSERT, UPDATE, DELETE 이벤트 캡처
    // 4. Active와 Standby 모두에서 같은 이벤트 확인
}
```

### 2. 필요한 의존성 추가

`build.gradle`에 추가:
```gradle
dependencies {
    // Kubernetes Java Client
    implementation 'io.kubernetes:client-java:18.0.0'
    
    // MySQL Binlog Connector (binlog 파싱용)
    implementation 'com.github.shyiko:mysql-binlog-connector-java:0.27.2'
    
    // 또는 Debezium (더 강력한 CDC)
    // implementation 'io.debezium:debezium-embedded:2.4.0.Final'
    // implementation 'io.debezium:debezium-connector-mysql:2.4.0.Final'
}
```

### 3. 실시간 업데이트 개선 (선택사항)

현재는 3초마다 폴링 방식이지만, WebSocket으로 개선 가능:

```java
// WebSocket Configuration
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ReplicationMonitorWebSocketHandler(), "/ws/replication")
                .setAllowedOrigins("*");
    }
}

// WebSocket Handler
@Component
public class ReplicationMonitorWebSocketHandler extends TextWebSocketHandler {
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 실시간 데이터 푸시
    }
}
```

## 📊 주요 기능 설명

### 1. Database Status Cards
- Active/Standby DB의 실시간 상태 (UP/DOWN/Standby)
- Uptime, 연결 수, QPS, Latency
- 마지막 heartbeat 시간

### 2. Kubernetes Service Selector
- 현재 K8s 서비스가 가리키는 타겟 (active/standby)
- Endpoint 정보
- 마지막 전환 시간

### 3. Synchronization Metrics
- 동기화율 (퍼센트 + 프로그레스 바)
- Replication Lag
- 전송된 데이터 용량
- Active/Standby Binlog Position

### 4. Replication Activity Chart
- Active DB 쓰기 작업 타임라인
- Standby DB 쓰기 작업 타임라인
- 실시간 그래프 (Chart.js)

### 5. Binlog Events
- 최근 실행된 binlog 이벤트 목록
- UPDATE, INSERT, DELETE 등 이벤트 타입
- Active와 Standby에서 동일하게 실행된 이벤트 표시
- SQL 쿼리문 표시

### 6. System Events
- 동기화 완료
- Heartbeat 수신
- Replication lag 경고
- Active DB 복구 및 재동기화
- Failover 이벤트 등

## 🚀 실행 방법

1. 백엔드 서비스 실행
```bash
./gradlew bootRun
```

2. 브라우저에서 접속
```
http://localhost:8080/replication/monitor
```

3. API 테스트
```
http://localhost:8080/replication/api/dashboard
```

## 📝 TODO 체크리스트

### 필수 구현
- [ ] Active DB 실시간 상태 조회
- [ ] Standby DB 실시간 상태 조회
- [ ] 동기화율 계산 (row 해시값 기반)
- [ ] Binlog position 조회
- [ ] Replication lag 계산
- [ ] Kubernetes Selector API 연동

### 선택적 구현
- [ ] Binlog 이벤트 실시간 파싱
- [ ] 시스템 이벤트 로깅 시스템
- [ ] WebSocket 실시간 푸시
- [ ] 알림 기능 (임계값 초과 시)
- [ ] 히스토리 데이터 저장 및 조회

## 💡 팁

1. **동기화율 계산 최적화**: 
   - 모든 테이블을 매번 체크하지 말고, 중요 테이블만 주기적으로 검사
   - Checksum 결과를 캐싱하여 성능 개선

2. **Binlog 이벤트**:
   - 너무 많은 이벤트를 화면에 표시하지 말고 최근 10~20개만
   - 필터링 기능 추가 (특정 테이블만, 특정 이벤트 타입만)

3. **성능 고려**:
   - 대시보드 데이터 조회 시 병렬 처리
   - 캐싱 전략 적용

4. **에러 처리**:
   - DB 연결 실패 시 적절한 fallback
   - Timeout 설정
   - 재시도 로직

## 🎯 다음 단계

1. `ReplicationMonitorService`의 TODO 메서드들을 실제 구현으로 교체
2. Artifact의 HTML을 `/templates/manager/replication-monitor.html`에 저장
3. 실제 데이터로 테스트
4. 필요시 WebSocket으로 업그레이드
5. 알림 및 히스토리 기능 추가

