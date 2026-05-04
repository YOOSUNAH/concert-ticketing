package com.concertticketing.domain.payment.service;

import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.entity.BookingStatus;
import com.concertticketing.domain.booking.repository.BookingRepository;
import com.concertticketing.domain.payment.entity.Payment;
import com.concertticketing.domain.payment.repository.PaymentRepository;

public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
    }

    /**
     * 결제 확정
     * 1. 예매 조회 & PENDING 상태 확인
     * 2. 결제 금액 검증
     * 3. 결제 정보 저장
     * 4. 예매 상태를 PAID로 변경
     */
    public Payment confirmPayment(Long bookingId, String paymentKey, String orderId,
                                  int amount, int pointUsed, String paymentMethod) {
        // 1. 예매 조회 & 상태 확인
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("결제 대기 상태의 예매만 결제할 수 있습니다.");
        }

        // 2. 결제 금액 검증 (실결제액 + 포인트 = 예매 금액)
        int expectedAmount = booking.getAmount();
        if (amount + pointUsed != expectedAmount) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        // 3. 결제 정보 저장
        Payment payment = new Payment(bookingId, paymentKey, orderId,
                amount, pointUsed, paymentMethod);
        paymentRepository.save(payment);

        // 4. 예매 상태를 PAID로 변경
        booking.markAsPaid();
        bookingRepository.save(booking);

        return payment;
    }
}
