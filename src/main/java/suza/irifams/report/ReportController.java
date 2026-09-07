package suza.irifams.report;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import suza.irifams.enums.RequestStatus;
import suza.irifams.enums.Role;
import suza.irifams.input.FarmInputRepository;
import suza.irifams.input.InputDistributionRepository;
import suza.irifams.payment.PaymentRepository;
import suza.irifams.plot.PlotRepository;
import suza.irifams.request.ServiceRequestRepository;
import suza.irifams.user.UserRepository;
import suza.irifams.water.WaterScheduleRepository;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final UserRepository userRepository;
    private final PlotRepository plotRepository;
    private final ServiceRequestRepository requestRepository;
    private final PaymentRepository paymentRepository;
    private final FarmInputRepository inputRepository;
    private final InputDistributionRepository distributionRepository;
    private final WaterScheduleRepository waterRepository;


    // =========================================================
    // SUMMARY REPORT
    // =========================================================

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ReportDto getSummaryReport() {

        Double totalRevenue =
                paymentRepository.getTotalRevenue();

        return ReportDto.builder()

                .totalFarmers(
                        userRepository.countByRole(Role.FARMER)
                )

                .totalPlots(
                        plotRepository.count()
                )

                .totalRequests(
                        requestRepository.count()
                )

                /*
                 * A request is considered approved when the
                 * supervisor/admin has approved it and the system
                 * has generated the control number.
                 *
                 * Current status:
                 * WAITING_PAYMENT
                 */
                .approvedRequests(
                        requestRepository.countByStatus(
                                RequestStatus.WAITING_PAYMENT
                        )
                )

                .completedRequests(
                        requestRepository.countByStatus(
                                RequestStatus.COMPLETED
                        )
                )

                .totalPayments(
                        paymentRepository.count()
                )

                .totalRevenue(
                        totalRevenue != null
                                ? totalRevenue
                                : 0.0
                )

                .totalInputs(
                        inputRepository.count()
                )

                .distributedInputs(
                        distributionRepository.count()
                )

                .totalWaterSchedules(
                        waterRepository.count()
                )

                .build();
    }


    // =========================================================
    // EXPORT PDF REPORT
    // =========================================================

    @GetMapping("/export/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ResponseEntity<byte[]> exportPdf() throws Exception {

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        PdfWriter writer =
                new PdfWriter(out);

        PdfDocument pdf =
                new PdfDocument(writer);

        Document document =
                new Document(pdf);


        // =====================================================
        // TITLE
        // =====================================================

        Paragraph title =
                new Paragraph("IRIFAMS SYSTEM REPORT")
                        .setBold()
                        .setFontSize(20)
                        .setTextAlignment(
                                TextAlignment.CENTER
                        );

        document.add(title);

        document.add(
                new Paragraph(
                        "Irrigation Rice Farming Management System"
                )
                        .setTextAlignment(
                                TextAlignment.CENTER
                        )
        );

        document.add(
                new Paragraph(
                        "Generated on: "
                                + LocalDate.now()
                )
        );

        document.add(
                new Paragraph("\n")
        );


        // =====================================================
        // SUMMARY SECTION
        // =====================================================

        document.add(
                new Paragraph(
                        "1. Summary Statistics"
                )
                        .setBold()
                        .setFontSize(16)
        );

        Table table = new Table(2);


        // Total Farmers

        table.addCell("Total Farmers");

        table.addCell(
                String.valueOf(
                        userRepository.countByRole(
                                Role.FARMER
                        )
                )
        );


        // Total Plots

        table.addCell("Total Farm Plots");

        table.addCell(
                String.valueOf(
                        plotRepository.count()
                )
        );


        // Total Requests

        table.addCell("Total Requests");

        table.addCell(
                String.valueOf(
                        requestRepository.count()
                )
        );


        // Approved Requests

        table.addCell("Approved Requests");

        table.addCell(
                String.valueOf(
                        requestRepository.countByStatus(
                                RequestStatus.WAITING_PAYMENT
                        )
                )
        );


        // Completed Requests

        table.addCell("Completed Requests");

        table.addCell(
                String.valueOf(
                        requestRepository.countByStatus(
                                RequestStatus.COMPLETED
                        )
                )
        );


        // Total Payments

        table.addCell("Total Payments");

        table.addCell(
                String.valueOf(
                        paymentRepository.count()
                )
        );


        // Total Revenue

        Double totalRevenue =
                paymentRepository.getTotalRevenue();

        table.addCell("Total Revenue");

        table.addCell(
                "TZS "
                        + (
                        totalRevenue != null
                                ? totalRevenue
                                : 0.0
                )
        );


        // Available Inputs

        table.addCell("Available Inputs");

        table.addCell(
                String.valueOf(
                        inputRepository.count()
                )
        );


        // Distributed Inputs

        table.addCell("Distributed Inputs");

        table.addCell(
                String.valueOf(
                        distributionRepository.count()
                )
        );


        // Water Schedules

        table.addCell("Water Schedules");

        table.addCell(
                String.valueOf(
                        waterRepository.count()
                )
        );


        document.add(table);


        // =====================================================
        // ANALYSIS
        // =====================================================

        document.add(
                new Paragraph("\n")
        );

        document.add(
                new Paragraph(
                        "2. System Analysis"
                )
                        .setBold()
                        .setFontSize(16)
        );

        document.add(
                new Paragraph(
                        "The IRIFAMS system continues to improve "
                                + "the management of irrigation farming "
                                + "activities. Farmers are able to submit "
                                + "service requests, receive farm inputs, "
                                + "view water schedules and make service "
                                + "payments through the system. "
                                + "Supervisors and administrators are able "
                                + "to manage farming operations, service "
                                + "requests, payments, farm inputs and "
                                + "irrigation schedules. The report "
                                + "provides a summary of the current "
                                + "operational status of the system."
                )
        );


        // =====================================================
        // CONCLUSION
        // =====================================================

        document.add(
                new Paragraph("\n")
        );

        document.add(
                new Paragraph(
                        "3. Conclusion"
                )
                        .setBold()
                        .setFontSize(16)
        );

        document.add(
                new Paragraph(
                        "Based on the current statistics, "
                                + "IRIFAMS supports transparency, "
                                + "efficiency and accountability in "
                                + "irrigation farming management by "
                                + "centralizing farmers, farm plots, "
                                + "service requests, payments, farm "
                                + "inputs and water scheduling activities."
                )
        );


        // =====================================================
        // SIGNATURES
        // =====================================================

        document.add(
                new Paragraph("\n\n")
        );

        document.add(
                new Paragraph(
                        "Prepared By: ____________________"
                )
        );

        document.add(
                new Paragraph(
                        "Approved By: ____________________"
                )
        );


        document.close();


        // =====================================================
        // RESPONSE
        // =====================================================

        return ResponseEntity.ok()

                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=irifams-report.pdf"
                )

                .contentType(
                        MediaType.APPLICATION_PDF
                )

                .body(
                        out.toByteArray()
                );
    }
}