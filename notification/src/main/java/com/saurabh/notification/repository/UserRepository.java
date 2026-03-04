package com.saurabh.notification.repository;

import com.saurabh.notification.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Inherited from JpaRepository:
    // Page<User> findAll(Pageable pageable)  — used for sharding
    // long count()                           — used to calculate total shards
}
