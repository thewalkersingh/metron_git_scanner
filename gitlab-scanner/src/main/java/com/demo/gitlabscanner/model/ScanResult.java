package com.demo.gitlabscanner.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "scans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanResult {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(name = "gitlab_username", nullable = false)
	private String gitlabUsername;
	
	@Column(name = "project_name", nullable = false)
	private String projectName;
	
	@Column(name = "project_url")
	private String projectUrl;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "issue_type", nullable = false)
	private IssueType issueType;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "severity", nullable = false)
	private Severity severity;
	
	@Column(name = "details", columnDefinition = "TEXT")
	private String details;
	
	@Column(name = "scan_timestamp")
	private LocalDateTime scanTimestamp;
	
	public enum IssueType {
		SENSITIVE_FILE,
		EXPOSED_SECRET,
		MISSING_METADATA
	}
	
	public enum Severity {
		HIGH,
		MEDIUM,
		LOW
	}
	@OneToMany()
	private Solution solution;
}