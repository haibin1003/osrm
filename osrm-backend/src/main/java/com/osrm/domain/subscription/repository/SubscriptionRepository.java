package com.osrm.domain.subscription.repository;

import com.osrm.domain.subscription.entity.Subscription;
import com.osrm.domain.subscription.entity.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Page<Subscription> findByUserId(Long userId, Pageable pageable);

    Page<Subscription> findByStatus(SubscriptionStatus status, Pageable pageable);

    boolean existsByPackageIdAndBusinessSystemIdAndStatus(
            Long packageId, Long businessSystemId, SubscriptionStatus status);

    Page<Subscription> findByBusinessSystemIdAndStatus(
            Long businessSystemId, SubscriptionStatus status, Pageable pageable);

    /**
     * 查询指定日期范围内的订阅记录（用于趋势统计，内存中分组以兼容不同数据库）
     */
    List<Subscription> findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(LocalDateTime startDate);

    /**
     * 使用数据库特定函数进行趋势查询（生产环境 MySQL/PostgreSQL 使用）
     * H2 数据库不支持此查询，开发环境使用内存分组方式
     */
    @Query(value = "SELECT DATE(created_at) as date, COUNT(*) as cnt FROM t_subscription " +
                   "WHERE created_at >= :startDate GROUP BY DATE(created_at) ORDER BY DATE(created_at)",
           nativeQuery = true)
    List<Object[]> findTrendByDateRange(@Param("startDate") LocalDateTime startDate);

    List<Subscription> findByCreatedAtGreaterThanEqual(LocalDateTime createdAt);

    Integer countByCreatedAtGreaterThanEqual(LocalDateTime createdAt);

    Integer countByCreatedAtBefore(LocalDateTime createdAt);

    Integer countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Phase 2.2 使用追踪 - 关联图查询
    List<Subscription> findByBusinessSystemId(Long businessSystemId);

    List<Subscription> findByPackageId(Long packageId);

    List<Subscription> findByStatusAndBusinessSystemId(SubscriptionStatus status, Long businessSystemId);

    List<Subscription> findByStatusAndPackageId(SubscriptionStatus status, Long packageId);
}
