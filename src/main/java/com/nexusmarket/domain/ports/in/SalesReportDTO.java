package com.nexusmarket.domain.ports.in;

import com.nexusmarket.domain.models.Money;

import java.util.Objects;

/**
 * Resultado consolidado del reporte de ventas.
 *
 * @param totalFacturado  GMV del periodo
 * @param numeroOrdenes   cantidad de ordenes con pago capturado
 * @param ticketPromedio  GMV dividido entre las ordenes pagadas
 */
public record SalesReportDTO(
        Money totalFacturado,
        long numeroOrdenes,
        Money ticketPromedio
) {

    public SalesReportDTO {
        Objects.requireNonNull(totalFacturado, "totalFacturado must not be null");
        Objects.requireNonNull(ticketPromedio, "ticketPromedio must not be null");

        if (numeroOrdenes < 0) {
            throw new IllegalArgumentException("numeroOrdenes must not be negative");
        }
        if (!totalFacturado.currency().equals(ticketPromedio.currency())) {
            throw new IllegalArgumentException(
                    "totalFacturado and ticketPromedio must use the same currency");
        }
    }
}
