package com.is.bcs.adapter.out.persistence.admin;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminActivityLogJpaRepository extends JpaRepository<AdminActivityLogJpaEntity, Long> {
}