package com.concertticketing.domain.payment.service;

import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.entity.BookingStatus;
import com.concertticketing.domain.booking.repository.BookingRepository;
import com.concertticketing.domain.payment.entity.Payment;
import com.concertticketing.domain.payment.entity.PaymentStatus;
import com.concertticketing.domain.payment.repository.PaymentRepository;
import com.concertticketing.domain.user.entity.User;
import com.concertticketing.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    BookingRepository bookingRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    PaymentService paymentService;

    // === 결제 확정 테스트 ===

    @Test
    @DisplayName("결제 성공 - 포인트 5000 차감 + booking PAID")
    void confirmPayment_success() {
        // given
        Long bookingId = 999L;
        Long userId = 1L;
        Booking booking = new Booking(userId, 1L, "BK20250801001", List.of(101L, 102L), 242000);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        User user = new User("test@test.com", "1234");
        user.refundPoint(10000); // 시작 잔액 10000으로 셋업
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when - 실결제액 237000 + 포인트 5000 = 총 242000
        Payment payment = paymentService.confirmPayment(
                bookingId, "toss_key", "order_uuid", 237000, 5000, "CARD");

        // then
        assertEquals(237000, payment.getAmount());
        assertEquals(5000, payment.getPointUsed());
        assertEquals(BookingStatus.PAID, booking.getStatus());
        assertEquals(5000, user.getPoint()); // 10000 - 5000 = 5000
        assertNotNull(payment.getPaidAt());
        verify(paymentRepository).save(any(Payment.class));
        verify(bookingRepository).save(booking);
        verify(userRepository).save(user); // 포인트 차감 영속화
    }

    @Test
    @DisplayName("결제 성공 - 포인트 미사용 시 user 조회 안 함")
    void confirmPayment_zeroPoint_skipsUserLookup() {
        // given
        Long bookingId = 999L;
        Booking booking = new Booking(1L, 1L, "BK20250801001", List.of(101L), 121000);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when - 실결제액 121000 + 포인트 0 = 총 121000
        paymentService.confirmPayment(
                bookingId, "toss_key", "order_uuid", 121000, 0, "CARD");

        // then
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("결제 실패 - 이미 결제된 예매")
    void confirmPayment_alreadyPaid_throwsException() {
        // given - PAID 상태인 예매
        Long bookingId = 999L;
        Booking booking = new Booking(1L, 1L, "BK20250801001", List.of(101L), 121000);
        booking.markAsPaid();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // when & then
        assertThrows(IllegalStateException.class,
                () -> paymentService.confirmPayment(
                        bookingId, "toss_key", "order_uuid", 121000, 0, "CARD"));

        verify(paymentRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("결제 실패 - 금액 불일치")
    void confirmPayment_amountMismatch_throwsException() {
        // given - totalAmount는 242000인데
        Long bookingId = 999L;
        Booking booking = new Booking(1L, 1L, "BK20250801001", List.of(101L, 102L), 242000);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // when & then - 200000 + 5000 = 205000 ≠ 242000
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.confirmPayment(
                        bookingId, "toss_key", "order_uuid", 200000, 5000, "CARD"));

        verify(paymentRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("결제 실패 - 포인트 잔액 부족")
    void confirmPayment_insufficientPoint_throwsException() {
        // given - user.point = 1000인데 5000 사용 시도
        Long bookingId = 999L;
        Long userId = 1L;
        Booking booking = new Booking(userId, 1L, "BK20250801001", List.of(101L, 102L), 242000);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        User user = new User("test@test.com", "1234");
        user.refundPoint(1000); // 잔액 1000
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // when & then
        assertThrows(IllegalStateException.class,
                () -> paymentService.confirmPayment(
                        bookingId, "toss_key", "order_uuid", 237000, 5000, "CARD"));

        // 잔액 부족 → 어떤 save도 호출되지 않음
        verify(paymentRepository, never()).save(any());
        verify(bookingRepository, never()).save(any());
        verify(userRepository, never()).save(any());
        assertEquals(1000, user.getPoint()); // user 잔액 변경 없음
    }

    // === 환불 테스트 ===

    @Test
    @DisplayName("환불 성공 - PAID → REFUNDED + 환불액 기록 + 포인트 환원")
    void refund_success() {
        // given - 실결제 237000 + 포인트 5000 = 총 242000짜리 결제
        Long bookingId = 999L;
        Long userId = 1L;
        Payment payment = new Payment(bookingId, "toss_key", "order_uuid", 237000, 5000, "CARD");
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));

        Booking booking = new Booking(userId, 1L, "BK20250801001", List.of(101L, 102L), 242000);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        User user = new User("test@test.com", "1234");
        user.refundPoint(3000); // 환불 전 잔액 3000 (이전에 일부 적립됐다고 가정)
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Payment refunded = paymentService.refund(bookingId);

        // then
        assertEquals(PaymentStatus.REFUNDED, refunded.getStatus());
        assertEquals(242000, refunded.getRefundedAmount());
        assertNotNull(refunded.getRefundedAt());
        assertEquals(8000, user.getPoint()); // 3000 + 5000 환원
        verify(paymentRepository).save(payment);
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("환불 성공 - 포인트 미사용 결제는 user 조회 안 함")
    void refund_zeroPoint_skipsUserLookup() {
        // given - pointUsed=0 결제
        Long bookingId = 999L;
        Payment payment = new Payment(bookingId, "toss_key", "order_uuid", 121000, 0, "CARD");
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        paymentService.refund(bookingId);

        // then
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any());
        verify(bookingRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("환불 실패 - 결제 정보 없음")
    void refund_paymentNotFound_throwsException() {
        // given
        Long bookingId = 999L;
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.refund(bookingId));

        verify(paymentRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("환불 실패 - 이미 환불된 결제")
    void refund_alreadyRefunded_throwsException() {
        // given - 이미 환불된 결제
        Long bookingId = 999L;
        Payment payment = new Payment(bookingId, "toss_key", "order_uuid", 237000, 5000, "CARD");
        payment.refund();
        when(paymentRepository.findByBookingId(bookingId)).thenReturn(Optional.of(payment));

        // when & then
        assertThrows(IllegalStateException.class,
                () -> paymentService.refund(bookingId));

        verify(paymentRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }
}
