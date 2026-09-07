package com.demo.gitlabscanner.service;

import com.demo.gitlabscanner.model.Issue;
import com.demo.gitlabscanner.model.ScanResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ContentScannerService {
	
	// exact sensitive filenames
	private static final Set<String> SENSITIVE_FILES = Set.of(
		".env", ".htpasswd", "id_rsa", "id_dsa", "id_ecdsa", "id_ed25519",
		"config.json", "secrets.yml", "secrets.yaml", "credentials.json",
		"keystore.jks", "private.key"
	);
	
	// sensitive extensions
	private static final Set<String> SENSITIVE_EXTENSIONS = Set.of(
		".env", ".pem", ".key", ".p12", ".pfx", ".crt", ".cer", ".der",
		".ppk", ".keystore", ".jks"
	);
	
	// sensitive filename patterns
	private static final List<String> SENSITIVE_PATTERNS = List.of(
		"config.json", "secrets.", "credentials.", ".htpasswd", "id_rsa", "id_dsa"
	);
	
	// secret detection regexes
	private static final List<Pattern> SECRET_PATTERNS = List.of(
		// aws
		Pattern.compile("AKIA[0-9A-Z]{16}"),
		Pattern.compile("aws_secret_access_key\\s*=\\s*['\"]?[A-Za-z0-9/+=]{40}['\"]?"),
		// git
		Pattern.compile("ghp_[a-zA-Z0-9]{36}"),
		Pattern.compile("glpat-[a-zA-Z0-9\\-]{20}"),
		Pattern.compile("sk-[a-zA-Z0-9]{20,}"),
		// google cloud
		Pattern.compile("AIza[0-9A-Za-z_-]{35}"),
		// jwt
		Pattern.compile("eyJ[a-zA-Z0-9_-]*\\.eyJ[a-zA-Z0-9_-]*\\.[a-zA-Z0-9_-]*"),
		// db url (mongodb, mysql, postgres, redis)
		Pattern.compile("(mongodb|mysql|postgresql|postgres|redis)://[^:]+:[^@]+@[^/\\s\"']+"),
		// generic password
		Pattern.compile("(password|passwd|pwd)\\s*=\\s*['\"][^'\"]+['\"]", Pattern.CASE_INSENSITIVE),
		// generic secret / api key
		Pattern.compile("(secret|private_key|api_key)\\s*=\\s*['\"][^'\"]+['\"]", Pattern.CASE_INSENSITIVE),
		// generic token
		Pattern.compile("(token|bearer)\\s*=\\s*['\"][^'\"]+['\"]", Pattern.CASE_INSENSITIVE),
		Pattern.compile("bearer\\s+[a-zA-Z0-9_-]{20,}", Pattern.CASE_INSENSITIVE)
	);
	
	public List<Issue> scanProject(Long projectId, String projectName, String projectUrl,
		List<String> files, GitLabApiService apiService, String token) {
		List<Issue> issues = new ArrayList<>();
		Set<String> rootFiles = new HashSet<>();
		
		for (String filePath : files) {
			String fileName = filePath.contains("/")
										? filePath.substring(filePath.lastIndexOf('/') + 1)
										: filePath;
			String lowerFileName = fileName.toLowerCase();
			rootFiles.add(lowerFileName);
			
			boolean isSensitiveFile = isSensitiveFile(lowerFileName);
			
			if (isSensitiveFile) {
				issues.add(new Issue(
					ScanResult.IssueType.SENSITIVE_FILE,
					ScanResult.Severity.HIGH,
					"Found sensitive file: " + filePath
				));
			}
			
			boolean shouldScanContent = isTextFile(fileName) || isSensitiveFile;
			
			if (shouldScanContent) {
				String content = apiService.getFileContent(projectId, filePath, token);
				if (content != null && !content.isBlank()) {
					List<String> foundSecrets = scanContentForSecrets(content);
					if (!foundSecrets.isEmpty()) {
						for(String s: foundSecrets){
							issues.add(new Issue(
								ScanResult.IssueType.EXPOSED_SECRET,
								ScanResult.Severity.MEDIUM,
//							"Exposed secret in: " + filePath + " (" + String.join(", ", foundSecrets) + ")"
								"Exposed secret in: " + filePath + " ( " +  s + " )"
							));
						}
					}
				}
			}
		}
		
		boolean hasReadme = rootFiles.stream().anyMatch(f -> f.startsWith("readme"));
		boolean hasLicense = rootFiles.stream()
												.anyMatch(f -> f.startsWith("license") ||
																		f.startsWith("copying") ||
																		f.startsWith("licence"));
		
		if (!hasReadme) {
			issues.add(new Issue(
				ScanResult.IssueType.MISSING_METADATA,
				ScanResult.Severity.LOW,
				"Missing README.md"
			));
		}
		if (!hasLicense) {
			issues.add(new Issue(
				ScanResult.IssueType.MISSING_METADATA,
				ScanResult.Severity.LOW,
				"Missing LICENSE"
			));
		}
		
		return issues;
	}
	
	// Check if filename indicates a sensitive file
	private boolean isSensitiveFile(String lowerFileName) {
		if (SENSITIVE_FILES.contains(lowerFileName)) return true;
		if (SENSITIVE_EXTENSIONS.stream().anyMatch(lowerFileName::endsWith)) return true;
		for (String pattern : SENSITIVE_PATTERNS) {
			if (lowerFileName.contains(pattern)) return true;
		}
		return false;
	}
	
	// Scan file content and return list of matched secret types
	private List<String> scanContentForSecrets(String content) {
		List<String> found = new ArrayList<>();
		if (content == null || content.isBlank())
			return found;
		
		if (Pattern.compile("AKIA[0-9A-Z]{16}").matcher(content).find()) {
			found.add("AWS Access Key");
		}
		
		if (Pattern.compile("aws_secret_access_key\\s*=\\s*['\"]?[A-Za-z0-9/+=]{40}['\"]?").matcher(content).find()) {
			found.add("AWS Secret Key");
		}
		
		if (Pattern.compile("ghp_[a-zA-Z0-9]{36}").matcher(content).find()) {
			found.add("GitHub PAT");
		}
		
		if (Pattern.compile("glpat-[a-zA-Z0-9\\-]{20}").matcher(content).find()) {
			found.add("GitLab PAT");
		}
		
		if (Pattern.compile("sk-[a-zA-Z0-9]{20,}").matcher(content).find()) {
			found.add("OpenAI Key");
		}
		
		if (Pattern.compile("AIza[0-9A-Za-z_-]{35}").matcher(content).find()) {
			found.add("Google Cloud Key");
		}
		
		if (Pattern.compile("eyJ[a-zA-Z0-9_-]*\\.eyJ[a-zA-Z0-9_-]*\\.[a-zA-Z0-9_-]*").matcher(content).find()) {
			found.add("JWT Token");
		}
		
		if (Pattern.compile("(mongodb|mysql|postgresql|postgres|redis)://[^:]+:[^@]+@[^/\\s\"']+").matcher(content)
					  .find()) {
			found.add("Database Password/URL");
		}
		
		if (Pattern.compile("(password|passwd|pwd)\\s*=\\s*['\"][^'\"]+['\"]", Pattern.CASE_INSENSITIVE)
					  .matcher(content).find()) {
			found.add("Hardcoded Password");
		}
		
		if (Pattern.compile("(secret|private_key|api_key)\\s*=\\s*['\"][^'\"]+['\"]", Pattern.CASE_INSENSITIVE)
					  .matcher(content).find()) {
			found.add("Secret/Private Key");
		}
		
		if (Pattern.compile("(token|bearer)\\s*=\\s*['\"][^'\"]+['\"]", Pattern.CASE_INSENSITIVE).matcher(content)
					  .find() ||
				 Pattern.compile("bearer\\s+[a-zA-Z0-9_-]{20,}", Pattern.CASE_INSENSITIVE).matcher(content).find()) {
			found.add("API Token");
		}
		
		return found;
	}
	
	//	private List<String> scanContentForAWSSecrets(String content) {
//		if (Pattern.compile("aws_secret_access_key\\s*=\\s*['\"]?[A-Za-z0-9/+=]{40}['\"]?").matcher(content).find()) {
//			found.add("AWS Secret Key");
//		}
//	}
	private boolean isTextFile(String fileName) {
		String lower = fileName.toLowerCase();
		return lower.endsWith(".txt") || lower.endsWith(".json") || lower.endsWith(".yml")
					 || lower.endsWith(".yaml") || lower.endsWith(".properties") || lower.endsWith(".xml")
					 || lower.endsWith(".js") || lower.endsWith(".java") || lower.endsWith(".py")
					 || lower.endsWith(".md") || lower.endsWith(".sh") || lower.endsWith(".env")
					 || lower.endsWith(".conf") || lower.endsWith(".cfg") || !lower.contains(".");
	}
	
}