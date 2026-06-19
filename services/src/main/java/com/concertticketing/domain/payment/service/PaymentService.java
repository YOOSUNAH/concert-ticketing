package com.concertticketing.domain.payment.service;

import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.entity.BookingStatus;
import com.concertticketing.domain.booking.repository.BookingRepository;
import com.concertticketing.domain.payment.entity.Payment;
import com.concertticketing.domain.payment.event.PaymentConfirmedEvent;
import com.concertticketing.domain.payment.gateway.PaymentGateway;
import com.concertticketing.domain.payment.repository.PaymentRepository;
import com.concertticketing.domain.user.entity.User;
import com.concertticketing.domain.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          UserRepository userRepository,
                          PaymentGateway paymentGateway,
                          ApplicationEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 결제 확정
     * 1. 예매 조회 & PENDING 상태 확인
     * 2. 결제 금액 검증
     * 3. 포인트 사용
     * 4. PG사 결제 요청 (실패 시 PaymentGatewayException → 호출 측에서 BookingService.failBooking)
     * 5. 결제 정보 저장
     * 6. 예매 상태를 PAID로 변경
     * 7. 포인트 차감 반영
     *
     * 결제/예매/포인트 save를 한 트랜잭션으로 묶어 부분 커밋을 방지한다.
     * (동시에, 이후 도입할 결제 완료 이벤트의 AFTER_COMMIT 발화 전제가 된다.)
     */
    @Transactional
    public Payment confirmPayment(Long bookingId, String paymentKey, String orderId,
                                  int amount, int pointUsed, String paymentMethod) {
        // 1. 예매 조회 & 상태 확인
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("결제 대기 상태의 예매만 결제할 수 있습니다.");
        }

        // 2. 결제 금액 검증 (실결제액 + 포인트 = 총 금액)
        int expectedAmount = booking.getTotalAmount();
        if (amount + pointUsed != expectedAmount) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        // 3. 포인트 사용 (사용자 확인, 잔액 확인)
        User user = null;
        if (pointUsed > 0) {
            user = userRepository.findById(booking.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
            user.usePoint(pointUsed);
        }

        // 4. PG사 결제 요청 (실패 시 어떤 save도 일어나지 않음)
        paymentGateway.charge(paymentKey, orderId, amount);

        // 5. 결제 정보 저장
        Payment payment = new Payment(bookingId, paymentKey, orderId,
                amount, pointUsed, paymentMethod);
        paymentRepository.save(payment);

        // 6. 예매 상태를 PAID로 변경
        booking.markAsPaid();
        bookingRepository.save(booking);

        // 7. 포인트 차감 반영
        if (user != null) {
            userRepository.save(user);
        }

        // 8. 결제 완료 이벤트 발행 (트랜잭션 안에서 발행 → 소비는 AFTER_COMMIT로 커밋 후 지연)
        eventPublisher.publishEvent(new PaymentConfirmedEvent(
                UUID.randomUUID().toString(),
                bookingId,
                booking.getUserId(),
                amount,
                pointUsed));

        return payment;
    }

    /**
     * 환불 처리
     * 1. bookingId로 결제 정보 조회
     * 2. 결제 환불 처리 (PAID → REFUNDED, 환불액 기록)
     * 3. 포인트 사용했던 결제면 포인트 환원
     *
     * 예매 상태(CANCELLED) 변경은 호출 측(BookingService.cancelBooking)에서 처리
     * TODO: PG사 환불 API 호출 (Toss cancel)
     */
    public Payment refund(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보가 없습니다."));

        payment.refund();

        // 포인트 사용했던 결제면 포인트 환원
        if (payment.getPointUsed() > 0) {
            Booking booking = bookingRepository.findById(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));
            User user = userRepository.findById(booking.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
            user.refundPoint(payment.getPointUsed());
            userRepository.save(user);
        }

        return paymentRepository.save(payment);
    }

    public java.util.Optional<Payment> getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBookingId(bookingId);
    }
}
