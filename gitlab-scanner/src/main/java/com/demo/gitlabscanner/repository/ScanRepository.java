package com.demo.gitlabscanner.repository;

import com.demo.gitlabscanner.model.ScanResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanRepository extends JpaRepository<ScanResult, Long> {
	
	@Query("SELECT s FROM ScanResult s WHERE s.gitlabUsername = ?1 " +
				 "AND s.scanTimestamp = (SELECT MAX(s2.scanTimestamp) FROM ScanResult s2 WHERE s2.gitlabUsername = " +
				 "?1)")
	List<ScanResult> findLatestScanByUsername(String username);
	
	List<ScanResult> findByGitlabUsernameOrderByScanTimestampDesc(String username);
	
}