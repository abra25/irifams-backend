package suza.irifams.plot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlotRepository
        extends JpaRepository<Plot, Long> {

    boolean existsByPlotNo(String plotNo);

    List<Plot> findByBlock(String block);

    List<Plot> findByFarmerId(Long farmerId);

}