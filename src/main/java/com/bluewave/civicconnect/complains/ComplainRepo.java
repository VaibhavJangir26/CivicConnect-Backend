package com.bluewave.civicconnect.complains;

import com.bluewave.civicconnect.complains.constatns.ComplainStatus;
import com.bluewave.civicconnect.profile.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplainRepo extends JpaRepository<Complains, String> {

    // For Managers/Admins filtering
    List<Complains> findByComplainStatus(ComplainStatus status);

    // For Citizens to see only their complaints
    List<Complains> findByProfile(Profile profile);
    List<Complains> findByProfileAndComplainStatus(Profile profile, ComplainStatus status);

    // For Officers to see only complaints assigned to them
    List<Complains> findByAssignedOfficer(Profile officer);
    List<Complains> findByAssignedOfficerAndComplainStatus(Profile officer, ComplainStatus status);
}