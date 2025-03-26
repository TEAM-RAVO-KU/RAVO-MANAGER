# RAVO-MANAGER
Manager for RAVO project that fetch data and check integrity, also health checks DB Server

```sql
CREATE TABLE integrity_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data VARCHAR(255),
    checked_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO integrity_data (data, checked_at) VALUES ('초기 데이터', NOW());
```
Integrity Check 기능을 사용하기 위해서는 최초 1회는 위 `integrity_data` TABLE과 데이터를 Live/Standby DB에 추가해주어야 합니다.
