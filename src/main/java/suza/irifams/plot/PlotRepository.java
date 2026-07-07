package suza.irifams.plot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlotRepository
        extends JpaRepository<Plot, Long> {

    boolean existsByPlotNo(String plotNo);
    long countByFarmer_Id(Long farmerId);

    List<Plot> findByFarmerId(Long farmerId);
    List<Plot> findByBlock(String block);

    List<Plot> findByFarmerBlockName(String blockName);

    List<Plot> findByBlockOrderByIdDesc(String block);
    Long countByBlock(String block);
}