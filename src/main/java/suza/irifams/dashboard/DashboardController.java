package suza.irifams.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import suza.irifams.audit.AuditLog;
import suza.irifams.audit.AuditLogRepository;
import suza.irifams.enums.PaymentStatus;
import suza.irifams.enums.RequestStatus;
import suza.irifams.enums.Role;
import suza.irifams.enums.ServiceType;
import suza.irifams.input.FarmInputRepository;
import suza.irifams.notification.Notification;
import suza.irifams.notification.NotificationRepository;
import suza.irifams.payment.Payment;
import suza.irifams.payment.PaymentRepository;
import suza.irifams.plot.PlotRepository;
import suza.irifams.request.ServiceRequest;
import suza.irifams.request.ServiceRequestRepository;
import suza.irifams.user.User;
import suza.irifams.user.UserRepository;
import suza.irifams.water.WaterSchedule;
import suza.irifams.water.WaterScheduleRepository;
import org.springframework.security.core.Authentication;

import java.util.List;

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
    private final AuditLogRepository auditRepository;
    private final NotificationRepository notificationRepository;


    // =========================================================
    // GENERAL DASHBOARD STATISTICS
    // ADMIN + SUPERVISOR
    // =========================================================

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public DashboardStatsDto getStatistics() {

        return DashboardStatsDto.builder()

                .totalFarmers(
                        userRepository.countByRole(
                                Role.FARMER
                        )
                )

                .totalSupervisors(
                        userRepository.countByRole(
                                Role.SUPERVISOR
                        )
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


    // =========================================================
    // DASHBOARD CHARTS
    // ADMIN + SUPERVISOR
    // =========================================================

    @GetMapping("/charts")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public ChartDataDto getChartData() {

        return ChartDataDto.builder()

                /*
                 * Land preparation / Kuburugiwa
                 */
                .tractorRequests(
                        requestRepository.countByServiceType(
                                ServiceType.KUBURUGIWA
                        )
                )

                /*
                 * Harvesting / Kuvuna
                 */
                .harvestingRequests(
                        requestRepository.countByServiceType(
                                ServiceType.KUVUNA
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


    // =========================================================
    // RECENT USERS
    // ADMIN + SUPERVISOR
    // =========================================================

    @GetMapping("/recent-users")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<User> recentUsers() {

        return userRepository
                .findTop5ByOrderByIdDesc();
    }


    // =========================================================
    // RECENT ACTIVITIES
    // ADMIN + SUPERVISOR
    // =========================================================

    @GetMapping("/activities")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<AuditLog> recentActivities() {

        return auditRepository
                .findTop5ByOrderByCreatedAtDesc();
    }


    // =========================================================
    // RECENT NOTIFICATIONS
    // ADMIN + SUPERVISOR
    // =========================================================

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    public List<Notification> recentNotifications() {

        return notificationRepository
                .findTop5ByOrderByCreatedAtDesc();
    }


    // =========================================================
    // SUPERVISOR DASHBOARD STATISTICS
    // =========================================================

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/supervisor/stats")
    public DashboardStatsDto getSupervisorStats(
            Authentication authentication
    ) {

        String username =
                authentication.getName();

        User supervisor =
                userRepository
                        .findByUsername(username)
                        .orElseThrow();

        String block =
                supervisor.getBlockName();


        return DashboardStatsDto.builder()

                .totalFarmers(
                        userRepository
                                .countFarmersByBlock(block)
                )

                .totalPlots(
                        plotRepository
                                .countByBlock(block)
                )

                .pendingRequests(
                        requestRepository
                                .countByPlot_BlockAndStatus(
                                        block,
                                        RequestStatus.PENDING
                                )
                )

                .totalPayments(
                        paymentRepository
                                .countByServiceRequest_Plot_BlockAndStatus(
                                        block,
                                        PaymentStatus.WAITING_VERIFICATION
                                )
                )

                .build();
    }


    // =========================================================
    // SUPERVISOR RECENT REQUESTS
    // =========================================================

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/supervisor/recent-requests")
    public List<ServiceRequest> recentSupervisorRequests(
            Authentication authentication
    ) {

        String username =
                authentication.getName();

        User supervisor =
                userRepository
                        .findByUsername(username)
                        .orElseThrow();


        return requestRepository
                .findTop5ByPlot_BlockOrderByCreatedAtDesc(
                        supervisor.getBlockName()
                );
    }


    // =========================================================
    // SUPERVISOR PENDING PAYMENTS
    // =========================================================

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/supervisor/payments")
    public List<Payment> supervisorPayments(
            Authentication authentication
    ) {

        String username =
                authentication.getName();

        User supervisor =
                userRepository
                        .findByUsername(username)
                        .orElseThrow();


        return paymentRepository
                .findTop5ByServiceRequest_Plot_BlockAndStatusOrderByPaymentDateDesc(
                        supervisor.getBlockName(),
                        PaymentStatus.WAITING_VERIFICATION
                );
    }


    // =========================================================
    // SUPERVISOR NOTIFICATIONS
    // =========================================================

    @PreAuthorize("hasRole('SUPERVISOR')")
    @GetMapping("/supervisor/notifications")
    public List<Notification> supervisorNotifications(
            Authentication authentication
    ) {

        String username =
                authentication.getName();

        User supervisor =
                userRepository
                        .findByUsername(username)
                        .orElseThrow();


        return notificationRepository
                .findTop5ByUserIdOrderByCreatedAtDesc(
                        supervisor.getId()
                );
    }


    // =========================================================
    // FARMER DASHBOARD STATISTICS
    // =========================================================

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/farmer/stats")
    public DashboardStatsDto farmerStats(
            Authentication authentication
    ) {

        User farmer =
                userRepository
                        .findByUsername(
                                authentication.getName()
                        )
                        .orElseThrow();


        Double pendingAmount =
                paymentRepository.sumPendingAmount(
                        farmer.getId()
                );


        if (pendingAmount == null) {
            pendingAmount = 0.0;
        }


        return DashboardStatsDto.builder()

                .myPlots(
                        plotRepository.countByFarmer_Id(
                                farmer.getId()
                        )
                )

                .pendingRequests(
                        requestRepository
                                .countByPlot_Farmer_IdAndStatus(
                                        farmer.getId(),
                                        RequestStatus.PENDING
                                )
                )

                .completedRequests(
                        requestRepository
                                .countByPlot_Farmer_IdAndStatus(
                                        farmer.getId(),
                                        RequestStatus.COMPLETED
                                )
                )

                .outstandingPayments(
                        pendingAmount
                )

                .build();
    }


    // =========================================================
    // FARMER RECENT REQUESTS
    // =========================================================

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/farmer/requests")
    public List<ServiceRequest> farmerRequests(
            Authentication authentication
    ) {

        User farmer =
                userRepository
                        .findByUsername(
                                authentication.getName()
                        )
                        .orElseThrow();


        return requestRepository
                .findTop5ByPlot_Farmer_IdOrderByCreatedAtDesc(
                        farmer.getId()
                );
    }


    // =========================================================
    // FARMER WATER SCHEDULES
    // =========================================================

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/farmer/schedules")
    public List<WaterSchedule> farmerSchedules(
            Authentication authentication
    ) {

        User farmer =
                userRepository
                        .findByUsername(
                                authentication.getName()
                        )
                        .orElseThrow();


        return waterRepository
                .findTop5ByPlot_Farmer_IdOrderByIrrigationDateAsc(
                        farmer.getId()
                );
    }


    // =========================================================
    // FARMER NOTIFICATIONS
    // =========================================================

    @PreAuthorize("hasRole('FARMER')")
    @GetMapping("/farmer/notifications")
    public List<Notification> farmerNotifications(
            Authentication authentication
    ) {

        User farmer =
                userRepository
                        .findByUsername(
                                authentication.getName()
                        )
                        .orElseThrow();


        return notificationRepository
                .findTop5ByUserIdOrderByCreatedAtDesc(
                        farmer.getId()
                );
    }

}