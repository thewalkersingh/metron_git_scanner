package com.demo.gitlabscanner.controller;

import com.demo.gitlabscanner.dto.ScanRequest;
import com.demo.gitlabscanner.dto.ScanResponse;
import com.demo.gitlabscanner.model.ScanResult;
import com.demo.gitlabscanner.service.GitLabApiService;
import com.demo.gitlabscanner.service.ScanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class ScanController {
	
	private final ScanService scanService;
	
	// Helper to read token from header or body
	private String getToken(String headerToken, ScanRequest body) {
		if (headerToken != null && !headerToken.isBlank())
			return headerToken;
		if (body != null && body.getToken() != null)
			return body.getToken();
		return null;
	}
	
	@GetMapping("/lookup/user/{username}")
	public ResponseEntity<GitLabApiService.UserInfo> lookupUser(
		@PathVariable String username,
		@RequestHeader(value = "X-GitLab-Token", required = false) String headerToken) {
		GitLabApiService.UserInfo user = scanService.lookupUser(username, headerToken);
		if (user == null)
			return ResponseEntity.notFound().build();
		return ResponseEntity.ok(user);
	}
	
	@GetMapping("/lookup/group/{groupname}")
	public ResponseEntity<GitLabApiService.GroupInfo> lookupGroup(
		@PathVariable String groupname,
		@RequestHeader(value = "X-GitLab-Token", required = false) String headerToken) {
		GitLabApiService.GroupInfo group = scanService.lookupGroup(groupname, headerToken);
		if (group == null)
			return ResponseEntity.notFound().build();
		return ResponseEntity.ok(group);
	}
	
	@GetMapping("/discover/user/{userId}/projects")
	public ResponseEntity<List<GitLabApiService.ProjectInfo>> discoverUserProjects(
		@PathVariable Long userId,
		@RequestHeader(value = "X-GitLab-Token", required = false) String headerToken) {
		return ResponseEntity.ok(scanService.discoverUserProjects(userId, headerToken));
	}
	
	@GetMapping("/discover/group/{groupId}/projects")
	public ResponseEntity<List<GitLabApiService.ProjectInfo>> discoverGroupProjects(
		@PathVariable Long groupId,
		@RequestHeader(value = "X-GitLab-Token", required = false) String headerToken) {
		return ResponseEntity.ok(scanService.discoverGroupProjects(groupId, headerToken));
	}
	
	@PostMapping("/scan")
	public ResponseEntity<ScanResponse> scan(
		@RequestBody ScanRequest request,
		@RequestHeader(value = "X-GitLab-Token", required = false) String headerToken) {
		String token = getToken(headerToken, request);
		return ResponseEntity.ok(scanService.scanProject(
			request.getGitlabUsername(),
			request.getProjectId(),
			request.getProjectName(),
			request.getProjectUrl(),
			token
		));
	}
	
	@GetMapping("/scan/{username}/latest")
	public ResponseEntity<List<ScanResult>> getLatest(@PathVariable String username) {
		return ResponseEntity.ok(scanService.getLatestScan(username));
	}
	
	@GetMapping("/scan/{username}/history")
	public ResponseEntity<List<ScanResult>> getHistory(@PathVariable String username) {
		return ResponseEntity.ok(scanService.getHistory(username));
	}
	
	@GetMapping("/scan/{username}/export/json")
	public ResponseEntity<List<ScanResult>> exportJson(@PathVariable String username) {
		return ResponseEntity.ok(scanService.getLatestScan(username));
	}
	
}