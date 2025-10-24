package org.ravo.client.controller;

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
public class UiLockController {

    private final AtomicBoolean locked = new AtomicBoolean(false);

    @PostMapping("/lock")
    public void lock() {
        locked.set(true);
    }

    @PostMapping("/unlock")
    public void unlock() {
        locked.set(false);
        log.info("UI LOCK -> UNLOCK");
    }

    /** FE 폴링용 **/
    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of("locked", locked.get()));
    }
}
