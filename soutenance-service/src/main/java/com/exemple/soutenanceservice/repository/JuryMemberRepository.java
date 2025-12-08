package com.exemple.soutenanceservice.repository;

import com.exemple.soutenanceservice.model.JuryMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JuryMemberRepository extends JpaRepository<JuryMember, String> {

    List<JuryMember> findAllBySoutenance_Id(String soutenanceId);

    void deleteAllBySoutenance_Id(String soutenanceId);
}
