package org.ravo.client.service;

import lombok.RequiredArgsConstructor;
import org.ravo.client.domain.TransactionRecord;
import org.ravo.client.domain.User;
import org.ravo.client.repository.TransactionRepository;
import org.ravo.client.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BankService {

    private final UserRepository userRepository;
    private final TransactionRepository txRepository;

    @Transactional(readOnly = true)
    public long getBalance(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("사용자 조회 실패"))
                .getBalance();
    }

    @Transactional(readOnly = true)
    public String getMyAccountNo(User user) {
        return userRepository.findById(user.getId())
                .map(User::getAccountNo)
                .orElse("-");
    }

    @Transactional
    public void deposit(User user, long amount, String requestId) {
        if (amount <= 0) throw new IllegalArgumentException("입금 금액은 1원 이상이어야 합니다.");

        if (txRepository.existsByRequestId(requestId)) return;

        User me = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("사용자 조회 실패"));
        me.setBalance(me.getBalance() + amount);

        TransactionRecord tx = new TransactionRecord();
        tx.setRequestId(requestId);
        tx.setType(TransactionRecord.Type.DEPOSIT);
        tx.setToUser(me);
        tx.setAmount(amount);
        txRepository.save(tx);
    }

    @Transactional
    public boolean withdraw(User user, long amount, String requestId) {
        if (amount <= 0) throw new IllegalArgumentException("출금 금액은 1원 이상이어야 합니다.");
        if (txRepository.existsByRequestId(requestId)) return true;

        User me = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalStateException("사용자 조회 실패"));

        if (me.getBalance() < amount) return false;

        me.setBalance(me.getBalance() - amount);

        TransactionRecord tx = new TransactionRecord();
        tx.setRequestId(requestId);
        tx.setType(TransactionRecord.Type.WITHDRAW);
        tx.setFromUser(me);
        tx.setAmount(amount);
        txRepository.save(tx);
        return true;
    }

    @Transactional
    public boolean transfer(User fromUser, String toAccountNo, long amount, String requestId) {
        if (amount <= 0) throw new IllegalArgumentException("송금 금액은 1원 이상이어야 합니다.");
        if (txRepository.existsByRequestId(requestId)) return true;

        User from = userRepository.findById(fromUser.getId())
                .orElseThrow(() -> new IllegalStateException("보내는 사람 조회 실패"));
        User to = userRepository.findByAccountNo(toAccountNo)
                .orElseThrow(() -> new IllegalArgumentException("수취 계좌번호가 유효하지 않습니다."));

        if (from.getId().equals(to.getId()))
            throw new IllegalArgumentException("동일 계좌로는 송금할 수 없습니다.");

        if (from.getBalance() < amount) return false;

        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);

        TransactionRecord tx = new TransactionRecord();
        tx.setRequestId(requestId);
        tx.setType(TransactionRecord.Type.TRANSFER);
        tx.setFromUser(from);
        tx.setToUser(to);
        tx.setAmount(amount);
        txRepository.save(tx);

        return true;
    }
}
