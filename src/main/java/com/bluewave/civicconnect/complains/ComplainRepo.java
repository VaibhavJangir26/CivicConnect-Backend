package com.bluewave.civicconnect.complains;

import com.bluewave.civicconnect.complains.constatns.ComplainStatus;
import com.bluewave.civicconnect.profile.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplainRepo extends JpaRepository<Complains, String> {

    // For Managers/Admins filtering
    List<Complains> findByComplainStatus(ComplainStatus status);
    Page<Complains> findByComplainStatus(ComplainStatus status, Pageable pageable);

    // For Citizens to see only their complaints
    List<Complains> findByProfile(Profile profile);
    Page<Complains> findByProfile(Profile profile, Pageable pageable);
    List<Complains> findByProfileAndComplainStatus(Profile profile, ComplainStatus status);
    Page<Complains> findByProfileAndComplainStatus(Profile profile, ComplainStatus status, Pageable pageable);

    // For Officers to see only complaints assigned to them
    List<Complains> findByAssignedOfficer(Profile officer);
    Page<Complains> findByAssignedOfficer(Profile officer, Pageable pageable);
    List<Complains> findByAssignedOfficerAndComplainStatus(Profile officer, ComplainStatus status);
    Page<Complains> findByAssignedOfficerAndComplainStatus(Profile officer, ComplainStatus status, Pageable pageable);
}