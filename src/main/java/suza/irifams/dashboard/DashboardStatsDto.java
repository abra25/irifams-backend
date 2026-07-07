package suza.irifams.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardStatsDto {

    private long totalFarmers;
    private long totalSupervisors;
    private long totalStakeholders;

    private long totalPlots;

    private long totalRequests;
    private long pendingRequests;
    private long completedRequests;

    private long totalPayments;
    private Double totalRevenue;

    private long availableInputs;

    private long waterSchedules;

    private long myPlots;


    private Double outstandingPayments;

}