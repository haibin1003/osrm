package com.osrm.domain.subscription.repository;

import com.osrm.domain.subscription.entity.DownloadToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DownloadTokenRepository extends JpaRepository<DownloadToken, Long> {

    Optional<DownloadToken> findBySubscriptionId(Long subscriptionId);

    Optional<DownloadToken> findByToken(String token);
}
