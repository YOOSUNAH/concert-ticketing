package com.concertticketing.domain.payment.service;

import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.entity.BookingStatus;
import com.concertticketing.domain.booking.repository.BookingRepository;
import com.concertticketing.domain.payment.entity.Payment;
import com.concertticketing.domain.payment.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    PaymentRepository paymentRepository;

    @Mock
    BookingRepository bookingRepository;

    @InjectMocks
    PaymentService paymentService;

    @Test
    @DisplayName("결제 성공 - PENDING 상태 + 금액 일치")
    void confirmPayment_success() {
        // given - 1매 단가 121000
        Long bookingId = 999L;
        Booking booking = new Booking(1L, 1L, "BK20250801001", 101L, 121000);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when - 실결제액 116000 + 포인트 5000 = 총 121000
        Payment payment = paymentService.confirmPayment(
                bookingId, "toss_key", "order_uuid", 116000, 5000, "CARD");

        // then
        assertEquals(116000, payment.getAmount());
        assertEquals(5000, payment.getPointUsed());
        assertEquals(BookingStatus.PAID, booking.getStatus());  // 예매 상태가 PAID로 변경됨
        assertNotNull(payment.getPaidAt());
        verify(paymentRepository).save(any(Payment.class));
        verify(bookingRepository).save(booking);
    }

    @Test
    @DisplayName("결제 실패 - 이미 결제된 예매")
    void confirmPayment_alreadyPaid_throwsException() {
        // given - PAID 상태인 예매
        Long bookingId = 999L;
        Booking booking = new Booking(1L, 1L, "BK20250801001", 101L, 121000);
        booking.markAsPaid(); // 이미 결제됨
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // when & then
        assertThrows(IllegalStateException.class,
                () -> paymentService.confirmPayment(
                        bookingId, "toss_key", "order_uuid", 121000, 0, "CARD"));

        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("결제 실패 - 금액 불일치")
    void confirmPayment_amountMismatch_throwsException() {
        // given - 예매 금액 121000인데
        Long bookingId = 999L;
        Booking booking = new Booking(1L, 1L, "BK20250801001", 101L, 121000);
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        // when & then - 100000 + 5000 = 105000 ≠ 121000
        assertThrows(IllegalArgumentException.class,
                () -> paymentService.confirmPayment(
                        bookingId, "toss_key", "order_uuid", 100000, 5000, "CARD"));

        verify(paymentRepository, never()).save(any());
    }
}
