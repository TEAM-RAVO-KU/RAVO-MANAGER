# RAVO-MANAGER

RAVO-MANAGER는 RAVO의 웹 기반 제어 및 모니터링 대시보드입니다. </br>
RAVO-MANAGER는 DR 솔루션의 작동 상태를 검증하고 시각화하는 두 가지 주요 기능을 제공합니다.

- Scenario Test Client (/client)
  - 금융권의 입출금/송금 기능을 모방한 테스트용 웹 클라이언트를 제공합니다.
  - 사용자가 직접 Active DB에 트랜잭션을 발생시킬 수 있습니다.
  - 의도적으로 Active DB 장애 상황을 트리거하여, RAVO-AGENT의 Auto-Failover 및 Auto-Recover 기능이 정상 작동하는지 검증할 수 있습니다.

- Monitoring Dashboard (/dashboard)
  - Active DB와 Standby DB의 현재 상태 및 주요 메트릭을 실시간으로 시각화합니다.
  - CDC 기반 Live Sync의 상태와 데이터 동기화 일치율을 차트로 제공합니다.
  - Failover 또는 Recover가 진행 중일 때 현재 프로세스 상태를 추적하여 보여줍니다
