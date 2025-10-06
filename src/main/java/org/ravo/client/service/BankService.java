package org.ravo.client.service;

import lombok.RequiredArgsConstructor;
import org.ravo.client.domain.User;
import org.ravo.client.repository.UserRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BankService {

    private final UserRepository userRepository;

    /**
     * 잔액 조회
     * @throws DataAccessException DB 접근 실패 시
     */
    public long getBalance(User user) {
        try {
            return userRepository.findById(user.getId())
                    .map(User::getBalance)
                    .orElse(0L);
        } catch (DataAccessException e) {
            // DB 오류 발생 시 예외를 상위로 전파
            // GlobalExceptionHandler에서 처리됨
            throw e;
        }
    }

    /**
     * 입금
     * @throws DataAccessException DB 접근 실패 시
     */
    @Transactional
    public void deposit(User user, long amount) {
        try {
            user.setBalance(user.getBalance() + amount);
            userRepository.save(user);
        } catch (DataAccessException e) {
            throw e;
        }
    }

    /**
     * 출금
     * @throws DataAccessException DB 접근 실패 시
     */
    @Transactional
    public boolean withdraw(User user, long amount) {
        try {
            if (user.getBalance() < amount) {
                return false; // 잔액 부족
            }
            user.setBalance(user.getBalance() - amount);
            userRepository.save(user);
            return true;
        } catch (DataAccessException e) {
            throw e;
        }
    }
}
