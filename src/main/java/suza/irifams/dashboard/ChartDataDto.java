package suza.irifams.dashboard;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChartDataDto {

    private long tractorRequests;
    private long harvestingRequests;

    private long paidPayments;
    private long pendingPayments;

}