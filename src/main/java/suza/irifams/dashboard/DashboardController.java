package suza.irifams.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import suza.irifams.enums.PaymentStatus;
import suza.irifams.enums.RequestStatus;
import suza.irifams.enums.Role;
import suza.irifams.enums.ServiceType;
import suza.irifams.input.FarmInputRepository;
import suza.irifams.payment.PaymentRepository;
import suza.irifams.plot.PlotRepository;
import suza.irifams.request.ServiceRequestRepository;
import suza.irifams.user.UserRepository;
import suza.irifams.water.WaterScheduleRepository;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final PlotRepository plotRepository;
    private final ServiceRequestRepository requestRepository;
    private final PaymentRepository paymentRepository;
    private final FarmInputRepository inputRepository;
    private final WaterScheduleRepository waterRepository;

    @GetMapping("/stats")
    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR','STAKEHOLDER')")
    public DashboardStatsDto getStatistics() {

        return DashboardStatsDto.builder()

                .totalFarmers(
                        userRepository.countByRole(Role.FARMER)
                )

                .totalSupervisors(
                        userRepository.countByRole(Role.SUPERVISOR)
                )

                .totalStakeholders(
                        userRepository.countByRole(Role.STAKEHOLDER)
                )

                .totalPlots(
                        plotRepository.count()
                )

                .totalRequests(
                        requestRepository.count()
                )

                .pendingRequests(
                        requestRepository.countByStatus(
                                RequestStatus.PENDING
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
                        paymentRepository.getTotalRevenue()
                )

                .availableInputs(
                        inputRepository.countByStatus(
                                "Available"
                        )
                )

                .waterSchedules(
                        waterRepository.count()
                )

                .build();
    }

    @GetMapping("/charts")
    @PreAuthorize(
            "hasAnyRole('ADMIN','SUPERVISOR','STAKEHOLDER')")
    public ChartDataDto getChartData() {

        return ChartDataDto.builder()

                .tractorRequests(
                        requestRepository.countByServiceType(
                                ServiceType.TRACTOR_SERVICE
                        )
                )

                .harvestingRequests(
                        requestRepository.countByServiceType(
                                ServiceType.HARVESTING_SERVICE
                        )
                )

                .paidPayments(
                        paymentRepository.countByStatus(
                                PaymentStatus.PAID
                        )
                )

                .pendingPayments(
                        paymentRepository.countByStatus(
                                PaymentStatus.PENDING
                        )
                )

                .build();
    }

}