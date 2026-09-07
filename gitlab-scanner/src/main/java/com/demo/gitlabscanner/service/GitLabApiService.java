package com.demo.gitlabscanner.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
public class GitLabApiService {
	
	@Value("${gitlab.api.base-url}")
	private String gitlabBaseUrl;
	
	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper mapper = new ObjectMapper();
	
	private String apiGet(String url, String token) {
		try {
			if (token != null && !token.isBlank()) {
				HttpHeaders headers = new HttpHeaders();
				headers.set("PRIVATE-TOKEN", token);
				HttpEntity<String> entity = new HttpEntity<>(headers);
				ResponseEntity<String> response = restTemplate.exchange(url,
					HttpMethod.GET, entity, String.class);
				return response.getBody();
			} else {
				return restTemplate.getForObject(url, String.class);
			}
		} catch (Exception e) {
			throw new RuntimeException("GitLab API request failed: " + e.getMessage(), e);
		}
	}
	
	public UserInfo lookupUser(String username, String token) {
		String url = UriComponentsBuilder.fromUriString(gitlabBaseUrl).path("/users")
													.queryParam("username", username)
													.toUriString();
		try {
			String response = apiGet(url, token);
			JsonNode root = mapper.readTree(response);
			if (root.isArray() && !root.isEmpty()) {
				JsonNode user = root.get(0);
				return new UserInfo(user.get("id").asLong(),
					user.get("username").asString(), user.get("name").asString(),
					user.get("web_url").asString());
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to lookup user: " + username, e);
		}
		return null;
	}
	
	public GroupInfo lookupGroup(String groupPath, String token) {
		String encodedPath = groupPath.replace("/", "%2F");
		String url = UriComponentsBuilder.fromUriString(gitlabBaseUrl).path("/groups/{path}").buildAndExpand(encodedPath)
													.toUriString();
		
		try {
			String response = apiGet(url, token);
			JsonNode group = mapper.readTree(response);
			return new GroupInfo(group.get("id").asLong(),
				group.get("name").asString(),
				group.get("path").asString(),
				group.get("web_url").asString());
		} catch (Exception e) {
			throw new RuntimeException("Failed to lookup group: " + groupPath, e);
		}
	}
	
	public List<ProjectInfo> getUserProjects(Long userId, String token) {
		String url =
			UriComponentsBuilder.fromUriString(gitlabBaseUrl)
									  .path("/users/{id}/projects")
									  .queryParam("per_page", 100)
									  .buildAndExpand(userId).toUriString();
		return fetchProjects(url, token, "user id: " + userId);
	}
	
	public List<ProjectInfo> getGroupProjects(Long groupId, String token) {
		String url =
			UriComponentsBuilder.fromUriString(gitlabBaseUrl)
									  .path("/groups/{id}/projects")
									  .queryParam("per_page", 100)
									  .buildAndExpand(groupId).toUriString();
		return fetchProjects(url, token, "group id: " + groupId);
	}
	
	private List<ProjectInfo> fetchProjects(String url, String token, String context) {
		List<ProjectInfo> projects = new ArrayList<>();
		try {
			String response = apiGet(url, token);
			JsonNode root = mapper.readTree(response);
			for (JsonNode node : root) {
				projects.add(new ProjectInfo(node.get("id").asLong(),
					node.get("name").asString(), node.get("web_url").asString()));
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to fetch projects for " + context, e);
		}
		return projects;
	}
	
	public List<String> getRepositoryFiles(Long projectId, String token) {
		String url = UriComponentsBuilder.fromUriString(gitlabBaseUrl)
													.path("/projects/{id}/repository/tree")
													.queryParam("recursive", true)
													.queryParam("per_page", 100)
													.buildAndExpand(projectId).toUriString();
		
		List<String> files = new ArrayList<>();
		try {
			String response = apiGet(url, token);
			JsonNode root = mapper.readTree(response);
			for (JsonNode node : root) {
				files.add(node.get("path").asString());
			}
		} catch (Exception e) {
			// empty repo, write later
		}
		return files;
	}
	
	public String getFileContent(Long projectId, String filePath, String token) {
		String encodedPath = filePath.replace("/", "%2F");
		String url = UriComponentsBuilder.fromUriString(gitlabBaseUrl)
													.path("/projects/{id}/repository/files/{path}/raw")
													.queryParam("ref", "master")
													.buildAndExpand(projectId, encodedPath)
													.toUriString();
		
		try {
			return apiGet(url, token);
		} catch (Exception e) {
			String urlMain =
				UriComponentsBuilder.fromUriString(gitlabBaseUrl).path("/projects/{id}/repository/files/{path}/raw")
										  .queryParam("ref", "main")
										  .buildAndExpand(projectId, encodedPath)
										  .toUriString();
			try {
				return apiGet(urlMain, token);
			} catch (Exception ex) {
				return "";
			}
		}
	}
	
	public record UserInfo(Long id, String username, String name, String webUrl) {
	}
	
	public record GroupInfo(Long id, String name, String path, String webUrl) {
	}
	
	public record ProjectInfo(Long id, String name, String webUrl) {
	}
	
}