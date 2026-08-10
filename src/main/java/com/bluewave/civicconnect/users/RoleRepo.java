package com.bluewave.civicconnect.users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepo extends JpaRepository<Roles,String> {

    Optional<Roles> findByRoleName(String roleName);
}
