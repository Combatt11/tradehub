package com.nexusmarket.adapters.rest.controllers;

import com.nexusmarket.domain.ports.in.GetSalesReportUseCase;
import com.nexusmarket.domain.ports.in.SalesReportDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * API REST de reportes administrativos.
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reportes", description = "Reportes consolidados para administración")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class ReportController {

    private final GetSalesReportUseCase getSalesReportUseCase;

    public ReportController(GetSalesReportUseCase getSalesReportUseCase) {
        this.getSalesReportUseCase = Objects.requireNonNull(
                getSalesReportUseCase, "getSalesReportUseCase must not be null");
    }

    @GetMapping("/sales")
    @Operation(summary = "Obtiene el reporte de ventas de un rango de fechas")
    public ResponseEntity<SalesReportDTO> getSalesReport(
            @Parameter(description = "Fecha y hora inicial en formato ISO-8601", required = true)
            @RequestParam("start")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime start,
            @Parameter(description = "Fecha y hora final en formato ISO-8601", required = true)
            @RequestParam("end")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime end
    ) {
        return ResponseEntity.ok(getSalesReportUseCase.getSalesReport(start, end));
    }
}
