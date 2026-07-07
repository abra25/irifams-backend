package suza.irifams.report;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;

import com.itextpdf.layout.properties.TextAlignment;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

import suza.irifams.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import suza.irifams.enums.RequestStatus;
import suza.irifams.input.FarmInputRepository;
import suza.irifams.input.InputDistributionRepository;
import suza.irifams.payment.PaymentRepository;
import suza.irifams.plot.PlotRepository;
import suza.irifams.request.ServiceRequestRepository;
import suza.irifams.user.UserRepository;
import suza.irifams.water.WaterScheduleRepository;

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

    @GetMapping("/summary")
    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR','STAKEHOLDER')")
    public ReportDto getSummaryReport() {

        return ReportDto.builder()

                .totalFarmers(
                        userRepository.countByRole(
                                suza.irifams.enums.Role.FARMER)
                )

                .totalPlots(
                        plotRepository.count()
                )

                .totalRequests(
                        requestRepository.count()
                )

                .approvedRequests(
                        requestRepository.countByStatus(
                                RequestStatus.WAITING_VERIFICATION)
                )

                .completedRequests(
                        requestRepository.countByStatus(
                                RequestStatus.COMPLETED)
                )

                .totalPayments(
                        paymentRepository.count()
                )

                .totalRevenue(
                        paymentRepository.getTotalRevenue()
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

    @GetMapping("/export/pdf")
    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR','STAKEHOLDER')")
    public ResponseEntity<byte[]> exportPdf() throws Exception {

        ByteArrayOutputStream out =
                new ByteArrayOutputStream();

        PdfWriter writer =
                new PdfWriter(out);

        PdfDocument pdf =
                new PdfDocument(writer);

        Document document =
                new Document(pdf);

        // TITLE

        Paragraph title =
                new Paragraph(
                        "IRIFAMS SYSTEM REPORT"
                )

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

        document.add(new Paragraph("\n"));

        // SUMMARY SECTION

        document.add(
                new Paragraph("1. Summary Statistics")
                        .setBold()
                        .setFontSize(16)
        );

        Table table = new Table(2);

        table.addCell("Total Farmers");
        table.addCell(
                String.valueOf(
                        userRepository.countByRole(
                                Role.FARMER)
                )
        );

        table.addCell("Total Farm Plots");
        table.addCell(
                String.valueOf(
                        plotRepository.count()
                )
        );

        table.addCell("Total Requests");
        table.addCell(
                String.valueOf(
                        requestRepository.count()
                )
        );

        table.addCell("Approved Requests");
        table.addCell(
                String.valueOf(
                        requestRepository.countByStatus(
                                RequestStatus.WAITING_PAYMENT)
                )
        );

        table.addCell("Completed Requests");
        table.addCell(
                String.valueOf(
                        requestRepository.countByStatus(
                                RequestStatus.COMPLETED)
                )
        );

        table.addCell("Total Payments");
        table.addCell(
                String.valueOf(
                        paymentRepository.count()
                )
        );

        table.addCell("Total Revenue");
        table.addCell(
                "TZS "
                        + paymentRepository.getTotalRevenue()
        );

        table.addCell("Available Inputs");
        table.addCell(
                String.valueOf(
                        inputRepository.count()
                )
        );

        table.addCell("Distributed Inputs");
        table.addCell(
                String.valueOf(
                        distributionRepository.count()
                )
        );

        table.addCell("Water Schedules");
        table.addCell(
                String.valueOf(
                        waterRepository.count()
                )
        );

        document.add(table);

        // ANALYSIS

        document.add(new Paragraph("\n"));

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
                                + "management of irrigation farming activities. "
                                + "Farmers are able to submit service requests, "
                                + "receive farm inputs, monitor water schedules "
                                + "and make service payments electronically. "
                                + "The report indicates the current operational "
                                + "status of the system."

                )

        );

        // CONCLUSION

        document.add(new Paragraph("\n"));

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
                                + "IRIFAMS has enhanced transparency, "
                                + "efficiency and accountability in "
                                + "irrigation farming management."
                )
        );

        document.add(new Paragraph("\n\n"));

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

        return ResponseEntity.ok()

                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=irifams-report.pdf"
                )

                .contentType(
                        MediaType.APPLICATION_PDF
                )

                .body(out.toByteArray());
    }

}