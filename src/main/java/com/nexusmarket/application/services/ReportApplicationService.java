package com.nexusmarket.application.services;

import com.nexusmarket.domain.models.Money;
import com.nexusmarket.domain.models.Order;
import com.nexusmarket.domain.ports.in.GetSalesReportUseCase;
import com.nexusmarket.domain.ports.in.SalesReportDTO;
import com.nexusmarket.domain.ports.out.OrderRepositoryPort;

import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

/**
 * Servicio de aplicacion para consolidar el reporte de ventas.
 *
 * <p>Solo contabiliza ordenes con pago capturado. El GMV se calcula antes de
 * reembolsos, de acuerdo con el catalogo de metricas de la especificacion.</p>
 */
public final class ReportApplicationService implements GetSalesReportUseCase {

    private static final String DEFAULT_CURRENCY = "USD";

    private final OrderRepositoryPort orderRepository;

    public ReportApplicationService(OrderRepositoryPort orderRepository) {
        this.orderRepository = Objects.requireNonNull(
                orderRepository, "orderRepository must not be null");
    }

    @Override
    public SalesReportDTO getSalesReport(LocalDateTime start, LocalDateTime end) {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");

        if (start.isAfter(end)) {
            throw new IllegalArgumentException("start must be before or equal to end");
        }

        List<Order> paidOrders = orderRepository.findAllByCreatedAtBetween(start, end).stream()
                .filter(ReportApplicationService::hasCapturedPayment)
                .toList();

        if (paidOrders.isEmpty()) {
            Money zero = Money.zero(DEFAULT_CURRENCY);
            return new SalesReportDTO(zero, 0, zero);
        }

        Currency currency = paidOrders.get(0).getTotalAmount().currency();
        Money total = Money.zero(currency);

        for (Order order : paidOrders) {
            if (!order.getTotalAmount().currency().equals(currency)) {
                throw new IllegalArgumentException(
                        "Cannot aggregate orders with different currencies");
            }
            total = total.plus(order.getTotalAmount());
        }

        Money ticketPromedio = total.divide(paidOrders.size());
        return new SalesReportDTO(total, paidOrders.size(), ticketPromedio);
    }

    private static boolean hasCapturedPayment(Order order) {
        return switch (order.getStatus()) {
            case PAID,
                 PREPARING,
                 DISPATCHED,
                 DELIVERED,
                 FINALIZED,
                 RETURN_REQUESTED,
                 RETURN_IN_TRANSIT,
                 RETURN_RECEIVED,
                 RETURNED,
                 REFUND_PENDING,
                 REFUNDED -> true;
            case PENDING_PAYMENT,
                 PAYMENT_FAILED,
                 ON_HOLD,
                 CANCELLED -> false;
        };
    }
}
