package com.nexusmarket.domain.ports.in;

import java.time.LocalDateTime;

/**
 * Puerto de entrada para consultar el reporte administrativo de ventas.
 */
public interface GetSalesReportUseCase {

    /**
     * Calcula el GMV, la cantidad de ordenes pagadas y el ticket promedio para
     * un rango de fechas.
     *
     * @param start inicio inclusivo del rango
     * @param end   fin inclusivo del rango
     * @return reporte consolidado
     */
    SalesReportDTO getSalesReport(LocalDateTime start, LocalDateTime end);
}
