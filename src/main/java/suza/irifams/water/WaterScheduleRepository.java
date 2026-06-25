package suza.irifams.water;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaterScheduleRepository
        extends JpaRepository<WaterSchedule, Long> {

    List<WaterSchedule> findByPlotId(Long plotId);

}