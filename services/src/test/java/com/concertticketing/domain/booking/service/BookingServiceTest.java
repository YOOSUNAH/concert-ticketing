package com.concertticketing.domain.booking.service;

import com.concertticketing.domain.booking.entity.Booking;
import com.concertticketing.domain.booking.entity.BookingStatus;
import com.concertticketing.domain.booking.repository.BookingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @InjectMocks BookingService bookingService;

    @Test
    @DisplayName("예매 생성")
    void create_success() {
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        Booking booking = bookingService.create(1L, 1L, List.of(101L, 102L), 242000,
                "10cm 콘서트", LocalDate.of(2026, 8, 1), LocalTime.of(19, 0), "올림픽공원");

        assertThat(booking.getTotalAmount()).isEqualTo(242000);
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.PENDING);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("예매 상세 조회 - 본인")
    void getBookingDetail_owner() {
        Booking booking = createBooking(1L);
        when(bookingRepository.findById(999L)).thenReturn(Optional.of(booking));

        Booking result = bookingService.getBookingDetail(999L, 1L);
        assertThat(result.getUserId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("예매 상세 조회 - 타인이면 예외")
    void getBookingDetail_notOwner() {
        Booking booking = createBooking(1L);
        when(bookingRepository.findById(999L)).thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> bookingService.getBookingDetail(999L, 2L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("예매 조회 - 존재하지 않으면 예외")
    void getBooking_notFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBooking(999L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("예매된 좌석 수 조회")
    void countBookedSeats() {
        when(bookingRepository.countSeatsByUserIdAndScheduleId(1L, 1L)).thenReturn(2);

        assertThat(bookingService.countBookedSeats(1L, 1L)).isEqualTo(2);
    }

    private Booking createBooking(Long userId) {
        return new Booking(userId, 1L, "BK20260801001", List.of(101L), 121000,
                "10cm 콘서트", LocalDate.of(2026, 8, 1), LocalTime.of(19, 0), "올림픽공원");
    }
}
