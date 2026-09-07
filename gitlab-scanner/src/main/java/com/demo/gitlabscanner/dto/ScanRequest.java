package com.demo.gitlabscanner.dto;

import lombok.Data;

@Data
public class ScanRequest {
	private String gitlabUsername;
	private Long projectId;
	private String projectName;
	private String projectUrl;
	private String token;
	
}