package suza.irifams.input;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InputDistributionRepository
        extends JpaRepository<InputDistribution, Long> {

    List<InputDistribution> findByFarmerId(Long farmerId);

    long count();

}