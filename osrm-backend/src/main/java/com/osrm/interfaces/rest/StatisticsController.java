package com.osrm.interfaces.rest;

import com.osrm.application.statistics.dto.response.*;
import com.osrm.application.statistics.service.StatisticsAppService;
import com.osrm.common.model.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 统计接口控制器
 */
@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsAppService statisticsAppService;

    @Autowired
    public StatisticsController(StatisticsAppService statisticsAppService) {
        this.statisticsAppService = statisticsAppService;
    }

    /**
     * 获取统计概览
     */
    @GetMapping("/overview")
    public ApiResponse<StatisticsOverviewDTO> getOverview() {
        return ApiResponse.success(statisticsAppService.getOverview());
    }

    /**
     * 获取趋势数据
     */
    @GetMapping("/trend")
    public ApiResponse<TrendDataDTO> getTrend(
            @RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(statisticsAppService.getTrend(days));
    }

    /**
     * 获取业务系统分布
     */
    @GetMapping("/business-distribution")
    public ApiResponse<BusinessDistributionDTO> getBusinessDistribution(
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.success(statisticsAppService.getBusinessDistribution(limit));
    }

    /**
     * 获取软件包热度排行
     */
    @GetMapping("/popularity")
    public ApiResponse<PopularityRankingDTO> getPopularity(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "subscription_count") String sortBy) {
        return ApiResponse.success(statisticsAppService.getPopularityRanking(limit, sortBy));
    }

    /**
     * 获取软件类型分布
     */
    @GetMapping("/type-distribution")
    public ApiResponse<TypeDistributionDTO> getTypeDistribution() {
        return ApiResponse.success(statisticsAppService.getTypeDistribution());
    }
}
