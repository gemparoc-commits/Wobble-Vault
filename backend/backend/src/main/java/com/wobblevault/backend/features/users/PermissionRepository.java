package com.wobblevault.backend.features.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.wobblevault.backend.entity.Permission;
import com.wobblevault.backend.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    List<Permission> findByUser(User user);

    Optional<Permission> findByUserIdAndPageName(UUID userId, String pageName);

    boolean existsByUserIdAndPageName(UUID userId, String pageName);

    void deleteByUserIdAndPageName(UUID userId, String pageName);
}
