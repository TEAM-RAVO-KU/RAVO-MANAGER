package org.ravo.client.controller;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/ui")
@Slf4j
@RequiredArgsConstructor
public class UiLockController {

    private final AtomicBoolean locked = new AtomicBoolean(false);
    private final DataSource liveDataSource;

    @PostMapping("/lock")
    public void lock() {
        locked.set(true);
    }

    @PostMapping("/unlock")
    public void unlock() {

        // --- 커넥션 풀 리셋 ---
        try {
            if (liveDataSource instanceof HikariDataSource hikari) {
                hikari.getHikariPoolMXBean().softEvictConnections();
                log.info("Live DataSource pool cleared successfully.");
            }
        } catch (Exception e) {
            log.error("Failed to evict live datasource connections", e);
        }

        locked.set(false);
        log.info("UI LOCK -> UNLOCK");
    }

    /** FE 폴링용 **/
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of("locked", locked.get()));
    }
}
