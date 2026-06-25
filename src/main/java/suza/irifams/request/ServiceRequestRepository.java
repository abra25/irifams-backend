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


}