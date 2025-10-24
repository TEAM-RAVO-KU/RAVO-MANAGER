package org.ravo.client.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ravo.client.domain.User;
import org.ravo.client.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> login(String userId, String password) {
        userRepository.findAll().forEach(user -> {
            log.info("user.userId : {}, user.password : {}", user.getUserId(), user.getPassword());
        });

        return userRepository.findByUserId(userId)
                .filter(user -> user.getPassword().equals(password));
    }

    /** 회원가입 시 기본 계좌 1개 자동 발급 */
    @Transactional
    public boolean register(String userId, String password, String name) {
        if (userRepository.existsByUserId(userId)) {
            return false; // 이미 존재하는 아이디
        }

        User user = new User();
        user.setUserId(userId);
        user.setPassword(password);
        user.setName(name);
        user.setAccountNo(generateAccountNo());
        user.setBalance(0L); // NOT NULL 요구사항

        userRepository.save(user);
        return true;
    }

    private String generateAccountNo() {
        String tail = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "RAVO-" + tail;
    }
}
