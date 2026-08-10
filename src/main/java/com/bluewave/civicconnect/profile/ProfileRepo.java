package com.bluewave.civicconnect.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProfileRepo extends JpaRepository<Profile,String> {

    Optional<Profile> findByUsers_Username(String username);
}
