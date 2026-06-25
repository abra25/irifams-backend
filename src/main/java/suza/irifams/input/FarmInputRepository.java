package suza.irifams.input;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmInputRepository
        extends JpaRepository<FarmInput, Long> {
    long countByStatus(String status);
}