package com.fixlink.repository;

import com.fixlink.entity.TechnicianProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TechnicianProfileRepository extends JpaRepository<TechnicianProfile, Long> {

    Optional<TechnicianProfile> findByUserId(Long userId);
}
