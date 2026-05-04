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

    /**
     * 환불 처리
     * 1. bookingId로 결제 정보 조회
     * 2. 결제 환불 처리 (PAID → REFUNDED, 환불액 기록)
     *
     * 예매 상태(CANCELLED) 변경은 호출 측(BookingService.cancelBooking)에서 처리
     * TODO: PG사 환불 API 호출 (Toss cancel)
     * TODO: User.point 환원 (Step 5에서 연결)
     */
    public Payment refund(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalStateException("결제 정보가 없습니다."));

        payment.refund();
        return paymentRepository.save(payment);
    }
}
