package suza.irifams.request;

import org.springframework.data.jpa.repository.JpaRepository;
import suza.irifams.enums.RequestStatus;
import suza.irifams.enums.ServiceType;

import java.util.List;

public interface ServiceRequestRepository
        extends JpaRepository<ServiceRequest, Long> {

    List<ServiceRequest> findByFarmerId(Long farmerId);

    long countByStatus(RequestStatus status);

    long countByServiceType(ServiceType serviceType);
    List<ServiceRequest> findByPlotBlock(String block);
    List<ServiceRequest>
    findByPlotBlockOrderByCreatedAtDesc(
            String block
    );

    Long countByPlot_BlockAndStatus(
            String block,
            RequestStatus status
    );

    List<ServiceRequest>
    findTop5ByPlot_BlockOrderByCreatedAtDesc(
            String block
    );

    long countByPlot_Farmer_IdAndStatus(
            Long farmerId,
            RequestStatus status
    );

    List<ServiceRequest>
    findTop5ByPlot_Farmer_IdOrderByCreatedAtDesc(
            Long farmerId
    );


}