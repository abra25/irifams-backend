package suza.irifams.report;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDto {

    private long totalFarmers;
    private long totalPlots;
    private long totalRequests;
    private long approvedRequests;
    private long completedRequests;
    private long totalPayments;
    private double totalRevenue;
    private long totalInputs;
    private long distributedInputs;
    private long totalWaterSchedules;
}