package com.fixlink.application.port.in;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface AdminStatisticsUseCase {
    List<Map<String, Object>> getVerifiedOfflineTechnicians(Long areaId, Long categoryId);

    Map<String, Object> getMonthlyStatistics(LocalDate month, Long areaId);
}
