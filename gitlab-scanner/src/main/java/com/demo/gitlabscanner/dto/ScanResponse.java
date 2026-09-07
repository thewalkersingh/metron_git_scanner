package com.demo.gitlabscanner.dto;

import com.demo.gitlabscanner.model.ScanResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ScanResponse {
	private Long scanId;
	private String username;
	private int projectsScanned;
	private List<ScanResult> results;
	
}