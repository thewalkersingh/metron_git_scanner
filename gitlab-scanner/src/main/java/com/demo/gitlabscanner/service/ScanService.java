package com.demo.gitlabscanner.service;

import com.demo.gitlabscanner.dto.ScanResponse;
import com.demo.gitlabscanner.model.Issue;
import com.demo.gitlabscanner.model.ScanResult;
import com.demo.gitlabscanner.repository.ScanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScanService {
	
	private final GitLabApiService gitLabApiService;
	private final ContentScannerService contentScannerService;
	private final ScanRepository scanRepository;
	
	public GitLabApiService.UserInfo lookupUser(String username, String token) {
		return gitLabApiService.lookupUser(username, token);
	}
	
	public GitLabApiService.GroupInfo lookupGroup(String groupPath, String token) {
		return gitLabApiService.lookupGroup(groupPath, token);
	}
	
	public List<GitLabApiService.ProjectInfo> discoverUserProjects(Long userId, String token) {
		return gitLabApiService.getUserProjects(userId, token);
	}
	
	public List<GitLabApiService.ProjectInfo> discoverGroupProjects(Long groupId, String token) {
		return gitLabApiService.getGroupProjects(groupId, token);
	}
	
	@Transactional
	public ScanResponse scanProject(String username, Long projectId, String projectName, String projectUrl,
		String token) {
		List<String> files = gitLabApiService.getRepositoryFiles(projectId, token);
		
		if (files == null) {
			throw new RuntimeException("Repository not found or inaccessible");
		}
		
		List<Issue> issues = contentScannerService.scanProject(
			projectId, projectName, projectUrl, files, gitLabApiService, token
		);
		
		LocalDateTime now = LocalDateTime.now();
		List<ScanResult> allResults = new ArrayList<>();
		
		for (Issue issue : issues) {
			ScanResult result = ScanResult.builder()
													.gitlabUsername(username)
													.projectName(projectName)
													.projectUrl(projectUrl)
													.issueType(issue.getType())
													.severity(issue.getSeverity())
													.details(issue.getDetails())
													.scanTimestamp(now)
													.build();
			allResults.add(result);
		}
		
		if (allResults.isEmpty()) {
			allResults.add(ScanResult.builder()
											 .gitlabUsername(username)
											 .projectName(projectName)
											 .projectUrl(projectUrl)
											 .issueType(ScanResult.IssueType.MISSING_METADATA)
											 .severity(ScanResult.Severity.LOW)
											 .details("No issues found in repository: " + projectName)
											 .scanTimestamp(now)
											 .build());
		}
		// sort function
		allResults.sort(Comparator.comparingInt(r -> r.getSeverity().ordinal()));
		scanRepository.saveAll(allResults);
		
		return ScanResponse.builder()
								 .scanId(allResults.get(0).getId())
								 .username(username)
								 .projectsScanned(1)
								 .results(allResults)
								 .build();
	}
	
	public List<ScanResult> getLatestScan(String username) {
		return scanRepository.findLatestScanByUsername(username);
	}
	
	public List<ScanResult> getHistory(String username) {
		return scanRepository.findByGitlabUsernameOrderByScanTimestampDesc(username);
	}
	
}