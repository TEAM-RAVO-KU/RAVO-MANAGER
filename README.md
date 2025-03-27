# RAVO-MANAGER
<img width="449" alt="image" src="https://github.com/user-attachments/assets/a97a129c-8cf2-43d5-bd1c-7c1d82ca9f1c" />
<br/> <br/>
<img width="435" alt="image" src="https://github.com/user-attachments/assets/469ff1be-838f-4ffa-8d98-66675e3d0c3a" />
<br/>

Manager for RAVO project that fetch data and check integrity, also health checks DB Server

## 선행 요구사항
```sql
CREATE TABLE integrity_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data VARCHAR(255),
    checked_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO integrity_data (data, checked_at) VALUES ('초기 데이터', NOW());
```
Integrity Check 기능을 사용하기 위해서는 최초 1회는 위 `integrity_data` TABLE과 데이터를 Live/Standby DB에 추가해주어야 합니다.

## RAVO-MANAGER 검사 항목
### MySQL Server Health
현재 연결된 두 개의 DB가 정상적으로 구동 가능하며, 연결이 가능한가?

### Data Integrity (데이터 무결성)
데이터 자체가 손상되지 않고, 비즈니스 로직 및 기술적 조건에 따라 유효한 형태로 유지되는 상태 <br/>
"DB 안의 데이터가 비즈니스나 기술적 조건을 잘 충족하여 신뢰할 수 있는 상태인가?" <br/>
ex. <br/>
- null이 허용되지 않는 필드가 비어있지 않음
- 외래키 참조가 깨지지 않고 정상 유지
- 애플리케이션에서 정의한 데이터 포맷 조건 충족(날짜 형식, 숫자 범위 등)


### Data Consistency (데이터 일관성)
복수의 DB 혹은 시스템 간 데이터가 일치하며 서로 동일한 상태를 유지하는지 확인하는 상태 <br/>
"두 개 이상의 DB 또는 시스템이 서로 데이터의 차이 없이 동일하게 유지되고 있는가?" <br/>
ex. <br/>
- Master-Replica 구조에서 Master DB의 데이터가 Replica DB에 정확히 복제됨
- 백업된 DB와 Live DB의 데이터가 동일함
- Live-Standby 구조에서 두 DB가 완벽히 일치하는지 여부
