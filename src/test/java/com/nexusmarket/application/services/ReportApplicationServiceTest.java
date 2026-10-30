package com.nexusmarket.application.services;

import com.nexusmarket.domain.models.Money;
import com.nexusmarket.domain.models.Order;
import com.nexusmarket.domain.models.OrderLine;
import com.nexusmarket.domain.models.OrderStatus;
import com.nexusmarket.domain.ports.in.SalesReportDTO;
import com.nexusmarket.domain.ports.out.OrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportApplicationServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    private ReportApplicationService reportApplicationService;

    private final LocalDateTime start = LocalDateTime.of(2026, 10, 1, 0, 0);
    private final LocalDateTime end = LocalDateTime.of(2026, 10, 31, 23, 59, 59);

    @BeforeEach
    void setUp() {
        reportApplicationService = new ReportApplicationService(orderRepository);
    }

    @Test
    void getSalesReportAggregatesPaidOrdersAndCalculatesAverage() {
        when(orderRepository.findAllByCreatedAtBetween(start, end)).thenReturn(List.of(
                order("100.00", "USD", OrderStatus.PAID, start.plusDays(1)),
                order("50.00", "USD", OrderStatus.DELIVERED, start.plusDays(2))
        ));

        SalesReportDTO report = reportApplicationService.getSalesReport(start, end);

        assertEquals(Money.of("150.00", "USD"), report.totalFacturado());
        assertEquals(2, report.numeroOrdenes());
        assertEquals(Money.of("75.00", "USD"), report.ticketPromedio());
        verify(orderRepository).findAllByCreatedAtBetween(start, end);
    }

    @Test
    void getSalesReportIgnoresOrdersWithoutCapturedPayment() {
        when(orderRepository.findAllByCreatedAtBetween(start, end)).thenReturn(List.of(
                order("100.00", "USD", OrderStatus.PENDING_PAYMENT, start.plusDays(1)),
                order("50.00", "USD", OrderStatus.PAYMENT_FAILED, start.plusDays(2)),
                order("10.00", "USD", OrderStatus.PAID, start.plusDays(3))
        ));

        SalesReportDTO report = reportApplicationService.getSalesReport(start, end);

        assertEquals(Money.of("10.00", "USD"), report.totalFacturado());
        assertEquals(1, report.numeroOrdenes());
        assertEquals(Money.of("10.00", "USD"), report.ticketPromedio());
    }

    @Test
    void getSalesReportIncludesRefundedOrdersInGmv() {
        when(orderRepository.findAllByCreatedAtBetween(start, end)).thenReturn(List.of(
                order("20.00", "USD", OrderStatus.REFUNDED, start.plusDays(1))
        ));

        SalesReportDTO report = reportApplicationService.getSalesReport(start, end);

        assertEquals(Money.of("20.00", "USD"), report.totalFacturado());
        assertEquals(1, report.numeroOrdenes());
        assertEquals(Money.of("20.00", "USD"), report.ticketPromedio());
    }

    @Test
    void getSalesReportReturnsZeroWhenThereAreNoPaidOrders() {
        when(orderRepository.findAllByCreatedAtBetween(start, end)).thenReturn(List.of(
                order("30.00", "USD", OrderStatus.PENDING_PAYMENT, start.plusDays(1))
        ));

        SalesReportDTO report = reportApplicationService.getSalesReport(start, end);

        assertEquals(Money.zero("USD"), report.totalFacturado());
        assertEquals(0, report.numeroOrdenes());
        assertEquals(Money.zero("USD"), report.ticketPromedio());
    }

    @Test
    void getSalesReportRejectsInvalidDateRange() {
        LocalDateTime invalidEnd = start.minusSeconds(1);

        assertThrows(
                IllegalArgumentException.class,
                () -> reportApplicationService.getSalesReport(start, invalidEnd)
        );

        verifyNoInteractions(orderRepository);
    }

    @Test
    void getSalesReportRejectsOrdersWithDifferentCurrencies() {
        when(orderRepository.findAllByCreatedAtBetween(start, end)).thenReturn(List.of(
                order("10.00", "USD", OrderStatus.PAID, start.plusDays(1)),
                order("10.00", "EUR", OrderStatus.PAID, start.plusDays(2))
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> reportApplicationService.getSalesReport(start, end)
        );
    }

    private Order order(
            String amount,
            String currency,
            OrderStatus status,
            LocalDateTime createdAt
    ) {
        Money total = Money.of(amount, currency);
        return new Order(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(new OrderLine(UUID.randomUUID(), 1, total)),
                total,
                status,
                createdAt
        );
    }
}
