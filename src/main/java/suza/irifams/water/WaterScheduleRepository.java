package suza.irifams.water;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WaterScheduleRepository
        extends JpaRepository<WaterSchedule, Long> {

    List<WaterSchedule> findByPlotId(Long plotId);

    List<WaterSchedule> findByPlot_Block(String block);
    List<WaterSchedule> findByPlot_Farmer_IdOrderByIrrigationDateAsc(Long farmerId);
    List<WaterSchedule>
    findTop5ByPlot_Farmer_IdOrderByIrrigationDateAsc(
            Long farmerId
    );
}