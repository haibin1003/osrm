package com.osrm.domain.h5.repository;

import com.osrm.domain.h5.entity.H5AccessToken;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface H5AccessTokenRepository extends JpaRepository<H5AccessToken, Long> {

    Optional<H5AccessToken> findByToken(String token);

    Page<H5AccessToken> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
