package com.osrm.domain.inventory.repository;

import com.osrm.domain.inventory.entity.InventoryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryRecordRepository extends JpaRepository<InventoryRecord, Long>, JpaSpecificationExecutor<InventoryRecord> {

    @Query("SELECT i FROM InventoryRecord i LEFT JOIN FETCH i.businessSystem WHERE i.id = :id")
    Optional<InventoryRecord> findById(@Param("id") Long id);

    Optional<InventoryRecord> findByRecordNo(String recordNo);

    boolean existsByRecordNo(String recordNo);

    // 按用户ID查询（数据权限：开发人员只能查看自己的）
    @Query("SELECT i FROM InventoryRecord i LEFT JOIN FETCH i.businessSystem WHERE i.userId = :userId")
    Page<InventoryRecord> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // 按用户ID和状态查询
    @Query("SELECT i FROM InventoryRecord i LEFT JOIN FETCH i.businessSystem WHERE i.userId = :userId AND i.status = :status")
    Page<InventoryRecord> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") InventoryRecord.InventoryStatus status, Pageable pageable);

    // 按状态查询（用于管理员查看待审批列表）
    @Query("SELECT i FROM InventoryRecord i LEFT JOIN FETCH i.businessSystem WHERE i.status = :status")
    Page<InventoryRecord> findByStatus(@Param("status") InventoryRecord.InventoryStatus status, Pageable pageable);

    // 统计用户的存量登记数量
    long countByUserId(Long userId);

    // 统计各种状态的数量
    long countByUserIdAndStatus(Long userId, InventoryRecord.InventoryStatus status);

    long countByStatus(InventoryRecord.InventoryStatus status);

    // 条件查询（管理员使用）
    @Query("SELECT i FROM InventoryRecord i LEFT JOIN FETCH i.businessSystem WHERE " +
           "(:userId IS NULL OR i.userId = :userId) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:packageName IS NULL OR i.packageName LIKE %:packageName%) AND " +
           "(:businessSystemId IS NULL OR i.businessSystemId = :businessSystemId)")
    Page<InventoryRecord> findByConditions(
            @Param("userId") Long userId,
            @Param("status") InventoryRecord.InventoryStatus status,
            @Param("packageName") String packageName,
            @Param("businessSystemId") Long businessSystemId,
            Pageable pageable);
}
