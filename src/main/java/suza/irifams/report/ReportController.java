package suza.irifams.report;

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
                                RequestStatus.APPROVED)
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

}