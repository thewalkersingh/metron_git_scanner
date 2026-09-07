package com.demo.gitlabscanner.cli;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

public class CliRunner {
	private static final String API_BASE = "http://localhost:8080/api";
	private static final HttpClient client = HttpClient.newHttpClient();
	
	public static void main(String[] args) throws Exception {
		Scanner scanner = new Scanner(System.in);
		System.out.print("Enter GitLab username to scan: ");
		String username = scanner.nextLine().trim();
		
		System.out.println("Scanning... Please wait.");
		
		String jsonBody = "{\"gitlabUsername\":\"" + username + "\"}";
		HttpRequest request = HttpRequest.newBuilder()
													.uri(URI.create(API_BASE + "/scan"))
													.header("Content-Type", "application/json")
													.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
													.build();
		
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		
		if (response.statusCode() == 200) {
			System.out.println("\n✅ Scan completed successfully!");
			System.out.println(response.body());
		} else {
			System.out.println("\n❌ Scan failed: HTTP " + response.statusCode());
			System.out.println(response.body());
		}
	}
	
}