package com.service.report.generator;

import com.service.report.generator.dto.APIResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReportGeneratorServiceApplicationTests {

	@Autowired
	private TestRestTemplate testRestTemplate;

	@Autowired
	private MockMvc mockMvc;

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


	@Test
	void uploadFile_withMultipartFiles_shouldReturnHttpStatusOKAndMessageNotNull() throws Exception {
		// Prepare files
		MockMultipartFile mainfile = new MockMultipartFile("main", "main.csv", "text/csv", new ClassPathResource("test-data/da85b0e5c197466b99f290ea8d0904ac-2024-07-21-16-03-51.csv").getInputStream());
		MockMultipartFile reference1 = new MockMultipartFile("ref-1", "reference1.csv", "text/csv", new ClassPathResource("test-data/db133aace1cb4fae9f4bd0e3517655bc-2024-07-21-16-03-51.csv").getInputStream());
		MockMultipartFile reference2 = new MockMultipartFile("ref-2", "reference2.csv", "text/csv", new ClassPathResource("test-data/d110371313994fc3a4dad8d504e1aa1a-2024-07-21-16-03-51.csv").getInputStream());

		// Prepare TokenValidationResponse as JSON
		String tokenValidationResponseJson = "{\"pid\": 1, \"uid\": \"test-uid\", \"adminRole\": true}";

		mockMvc.perform(multipart("/api/generator/upload-file")
						.file(mainfile)
						.file(reference1)
						.file(reference2)
						.param("validationResponse", tokenValidationResponseJson)
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.MULTIPART_FORM_DATA))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message", notNullValue()));
	}


}
