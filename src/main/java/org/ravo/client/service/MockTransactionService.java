package org.ravo.client.service;

import lombok.extern.slf4j.Slf4j;
import org.ravo.client.domain.User;
import org.ravo.client.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class MockTransactionService {

    private final UserRepository userRepository;
    private final Random random = new Random();
    private final AtomicBoolean running = new AtomicBoolean(false);

    // 최근 거래 로그 메모리 저장용 (최대 20개)
    private final List<String> recentLogs = new CopyOnWriteArrayList<>();

    public MockTransactionService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void toggleMockTx() {
        boolean nowRunning = !running.get();
        running.set(nowRunning);
        log.info("Mock Transaction {}", nowRunning ? "Started" : "Stopped");
        appendLog(nowRunning ? "Mock Transaction Started" : "Mock Transaction Stopped");
    }

    // 1초에 한 번씩 거래 생성 (running 상태일 때만)
    @Scheduled(fixedRate = 500)
    public void generateMockTx() {
        if (!running.get()) return;

        List<User> users = userRepository.findAll();
        if (users.size() < 2) return;

        User sender = users.get(random.nextInt(users.size()));
        User receiver = users.get(random.nextInt(users.size()));
        while (receiver.getId().equals(sender.getId())) {
            receiver = users.get(random.nextInt(users.size()));
        }

        long amount = (random.nextInt(5) + 1) * 1000L; // 1000~5000원
        if (sender.getBalance() < amount) return;

        sender.setBalance(sender.getBalance() - amount);
        receiver.setBalance(receiver.getBalance() + amount);
        userRepository.saveAll(List.of(sender, receiver));

        String logMsg = String.format("%s → %s : %,d원 송금 (보낸이 잔액: %,d)",
                sender.getName(), receiver.getName(), amount, sender.getBalance());
        log.debug("[MockTx] {}", logMsg);
        appendLog(logMsg);
    }

    public List<String> getRecentLogs() {
        return recentLogs;
    }

    private void appendLog(String msg) {
        recentLogs.add(0, String.format("[%tT] %s", new Date(), msg));
        if (recentLogs.size() > 20) {
            recentLogs.remove(recentLogs.size() - 1);
        }
    }
}
