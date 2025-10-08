package org.ravo.client.bank.service;

import lombok.RequiredArgsConstructor;
import org.ravo.client.bank.domain.User;
import org.ravo.client.bank.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Optional<User> login(String userId, String password) {
        return userRepository.findByUserId(userId)
                .filter(user -> user.getPassword().equals(password));
    }

    public boolean register(String userId, String password, String name) {
        if (userRepository.existsByUserId(userId)) {
            return false; // 이미 존재하는 아이디
        }
        User user = new User();
        user.setUserId(userId);
        user.setPassword(password);
        user.setName(name);
        userRepository.save(user);
        return true;
    }
}
