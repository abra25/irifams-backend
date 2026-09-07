package suza.irifams.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import suza.irifams.enums.PaymentStatus;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository
        extends JpaRepository<Payment, Long> {

    List<Payment> findByFarmerId(Long farmerId);

    Optional<Payment> findByControlNumber(String controlNumber);

    @Query("""
        SELECT COALESCE(SUM(p.amount),0)
        FROM Payment p
        WHERE p.status='PAID'
        """)
    Double getTotalRevenue();

    long countByStatus(PaymentStatus status);

    List<Payment>
    findByFarmerBlockNameOrderByPaymentDateDesc(
            String blockName
    );

    Long countByServiceRequest_Plot_BlockAndStatus(
            String block,
            PaymentStatus status
    );

    List<Payment>
    findTop5ByServiceRequest_Plot_BlockAndStatusOrderByPaymentDateDesc(
            String block,
            PaymentStatus status
    );

    @Query("""
        SELECT COALESCE(SUM(p.amount),0)
        FROM Payment p
        WHERE p.serviceRequest.plot.farmer.id = :farmerId
        AND p.status='PENDING'
        """)
    Double sumPendingAmount(
            @Param("farmerId")
            Long farmerId
    );
}