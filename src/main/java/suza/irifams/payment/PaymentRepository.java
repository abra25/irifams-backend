package suza.irifams.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
}