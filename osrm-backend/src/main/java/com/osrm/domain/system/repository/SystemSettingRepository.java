package com.osrm.domain.system.repository;

import com.osrm.domain.system.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {
    List<SystemSetting> findByCategory(String category);
    Optional<SystemSetting> findByCategoryAndSettingKey(String category, String settingKey);
}
