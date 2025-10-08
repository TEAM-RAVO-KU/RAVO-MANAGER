package org.ravo.client.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ravo.client.domain.User;
import org.ravo.client.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;

    @Override
    public void run(String... args) {
        long count = userRepository.count();

        if (count == 0) {
            log.info("No user data found. Creating dummy users...");

            List<User> dummyUsers = List.of(
                    createUser("test", "1234", "김규빈", 50000L),
                    createUser("abc", "1234", "노성준", 6000000000L),
                    createUser("def", "1234", "민상연", 100000000000L)
            );
            userRepository.saveAll(dummyUsers);
            log.info("Dummy user data initialized ({} users)", dummyUsers.size());
        } else {
            log.info("User table already contains {} users — skipping initialization", count);
        }
    }

    private User createUser(String userId, String password, String name, Long balance) {
        User user = new User();
        user.setUserId(userId);
        user.setPassword(password);
        user.setName(name);
        user.setBalance(balance);
        return user;
    }
}
