package com.osrm.domain.storage.repository;

import com.osrm.domain.storage.entity.HealthStatus;
import com.osrm.domain.storage.entity.StorageBackend;
import com.osrm.domain.storage.entity.StorageBackendType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 存储后端仓储接口
 */
@Repository
public interface StorageBackendRepository extends JpaRepository<StorageBackend, Long> {

    /**
     * 根据编码查询
     */
    Optional<StorageBackend> findByBackendCode(String backendCode);

    /**
     * 根据名称查询
     */
    Optional<StorageBackend> findByBackendName(String backendName);

    /**
     * 检查编码是否存在
     */
    boolean existsByBackendCode(String backendCode);

    /**
     * 检查名称是否存在
     */
    boolean existsByBackendName(String backendName);

    /**
     * 查询所有启用的存储后端
     */
    List<StorageBackend> findAllByEnabledTrue();

    /**
     * 查询默认存储后端
     */
    Optional<StorageBackend> findByIsDefaultTrue();

    /**
     * 条件分页查询
     */
    @Query("SELECT sb FROM StorageBackend sb WHERE " +
           "(:keyword IS NULL OR sb.backendName LIKE %:keyword% OR sb.backendCode LIKE %:keyword%) AND " +
           "(:type IS NULL OR sb.backendType = :type) AND " +
           "(:status IS NULL OR sb.healthStatus = :status)")
    Page<StorageBackend> findByConditions(@Param("keyword") String keyword,
                                          @Param("type") StorageBackendType type,
                                          @Param("status") HealthStatus status,
                                          Pageable pageable);

    /**
     * 根据类型查询
     */
    List<StorageBackend> findByBackendTypeAndEnabledTrue(StorageBackendType type);

    /**
     * 查询指定状态的存储后端
     */
    List<StorageBackend> findByHealthStatusAndEnabledTrue(HealthStatus healthStatus);
}
