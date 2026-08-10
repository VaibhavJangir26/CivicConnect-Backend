package com.bluewave.civicconnect.profilemanagement;

import com.bluewave.civicconnect.utils.constants.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfileManagementRepo extends JpaRepository<ProfileManagement, String> {
    List<ProfileManagement> findByAccountStatus(AccountStatus status);
}