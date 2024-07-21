package com.service.report.generator;

import com.service.report.generator.dto.APIResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReportGeneratorServiceApplicationTests {

	@Autowired
	private TestRestTemplate testRestTemplate;

	@LocalServerPort
	private int port;

	@Value("${app.test.token}")
	private String token;

	@Test
	void triggerReportGeneration_withRestTemplate_shouldReturnHttpStatusOKAndMessageNotNull() {

		String url = "http://localhost:" + port +"/api/generator/execute/J24072144bf";

		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + token);
		HttpEntity<String> request = new HttpEntity<>(null, headers);

		// Perform the request
		ResponseEntity<APIResponse> responseEntity = testRestTemplate.postForEntity(url, request, APIResponse.class);

		// Assert the response status and message
		assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
		assertNotNull(responseEntity.getBody().getMessage());

	}

}
