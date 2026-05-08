package com.concertticketing.support;

import com.concertticketing.domain.booking.repository.SpringDataBookingRepository;
import com.concertticketing.domain.payment.repository.SpringDataPaymentRepository;
import com.concertticketing.domain.user.repository.SpringDataUserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TestDataReset {

    private final SpringDataPaymentRepository paymentRepository;
    private final SpringDataBookingRepository bookingRepository;
    private final SpringDataUserRepository userRepository;

    public TestDataReset(SpringDataPaymentRepository paymentRepository,
                         SpringDataBookingRepository bookingRepository,
                         SpringDataUserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    /**
     * 테스트 간 격리: 사용자/예매/결제만 제거.
     * concert/schedule/seat은 DataLoader가 주입한 시드 데이터라 보존.
     */
    @Transactional
    public void cleanTransactional() {
        paymentRepository.deleteAllInBatch();
        bookingRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }
}
