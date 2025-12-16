package com.example.demo.controller;

import com.example.demo.model.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class BookControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withCommand("postgres", "-c", "max_prepared_transactions=100");

    @LocalServerPort
    private int port;

    private final RestTemplate restTemplate = new RestTemplate();

    @Test
    public void testCreateAndReadBook() throws Exception {
        String baseUrl = "http://localhost:" + port;
        
        // Create a new book
        Book book = new Book();
        book.setTitle("Test Book");
        book.setAuthor("Test Author");

        // Test Create operation (POST /books)
        ResponseEntity<Book> createResponse = restTemplate.postForEntity(
                baseUrl + "/books", 
                book, 
                Book.class
        );
        
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().getTitle()).isEqualTo("Test Book");
        assertThat(createResponse.getBody().getAuthor()).isEqualTo("Test Author");
        assertThat(createResponse.getBody().getId()).isNotNull();

        // Test Read operation (GET /books)
        ResponseEntity<List<Book>> readResponse = restTemplate.exchange(
                baseUrl + "/books",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Book>>() {}
        );
        
        assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(readResponse.getBody()).isNotNull();
        assertThat(readResponse.getBody()).hasSize(1);
        assertThat(readResponse.getBody().get(0).getTitle()).isEqualTo("Test Book");
        assertThat(readResponse.getBody().get(0).getAuthor()).isEqualTo("Test Author");
        assertThat(readResponse.getBody().get(0).getId()).isNotNull();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Override the datasource URL to use standard PostgreSQL driver for tests
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
}
