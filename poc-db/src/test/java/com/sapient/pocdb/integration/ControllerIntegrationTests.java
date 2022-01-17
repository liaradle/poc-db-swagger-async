package com.sapient.pocdb.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapient.pocdb.data.Placeholder;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ControllerIntegrationTests {
    
    @Autowired 
    TestRestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Order(1)
    @ParameterizedTest
    @ValueSource(strings = {"/allplaceholdersfuturejoin", "/allplaceholdersnormal"})
    void getAllSucceeds(String url) throws JsonProcessingException {
        Placeholder[] expected = new Placeholder[100];
        for(long i = 1; i < 101; ++i) {
            Placeholder p = Placeholder.builder().id(i).sampleField("test").build();
            expected[(int) (i - 1)] = p;
        }

        ResponseEntity<Placeholder[]> responseEntity = restTemplate.getForEntity(url, Placeholder[].class);
        Placeholder[] objects = responseEntity.getBody();
        String expectedString = objectMapper.writeValueAsString(expected); 
        String actualAsString = objectMapper.writeValueAsString(objects);
        
        assertEquals(expectedString, actualAsString);
    }

    @Order(2)
    @ParameterizedTest
    @ValueSource(strings={"/placeholderbyidfuturejoin?id={id}","/placeholderbyidnormal?id={id}"})
    void getByIdSucceeds(String url) throws JsonProcessingException {
        Placeholder p = Placeholder.builder().id(1L).sampleField("test").build();

        ResponseEntity<Placeholder> responseEntity = restTemplate.getForEntity(url, Placeholder.class, 1L);
        Placeholder actual = responseEntity.getBody();
        String expectedString = objectMapper.writeValueAsString(p);
        String actualString = objectMapper.writeValueAsString(actual);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(expectedString, actualString);
    }

    @Order(3)
    @ParameterizedTest
    @ValueSource(strings={"/placeholderbyidfuturejoin?id={id}","/placeholderbyidnormal?id={id}"})
    void getByIdReturnsNotFound(String url) {
        ResponseEntity<Placeholder> responseEntity = restTemplate.getForEntity(url, Placeholder.class, -3L);
        
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Order(4)
    @ParameterizedTest
    @ValueSource(strings={"/placeholderbyidfuturejoin?id={id}","/placeholderbyidnormal?id={id}"})
    void getByIdReturnsBadRequest(String url) {
        ResponseEntity<Placeholder> responseEntity = restTemplate.getForEntity(url, Placeholder.class, "a");

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }

    @Order(5)
    @ParameterizedTest
    @ValueSource(strings={"/postplaceholdernormal?id={id}&sampleField=Test","/postplaceholderfuturejoin?id={id}&sampleField=Test"})
    void postPlaceholder(String url) {
        long id = 101;
        if(url.contains("join"))
            id = 102;
        url = url.replace("{id}", Long.toString(id));
        System.out.println(url);
        Placeholder p = Placeholder.builder().id(id).sampleField("Test").build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Placeholder> responseEntity = restTemplate.postForEntity(url, null, Placeholder.class);
        System.out.println(responseEntity.getStatusCode());
        assert(responseEntity.getStatusCode() == HttpStatus.CREATED);
        assert(responseEntity.getBody().equals(p));
    }

    @Order(6)
    @ParameterizedTest
    @ValueSource(strings={"/putplaceholdernormal?id=-1&sampleField=Test","/putplaceholderfuturejoin?id=-1&sampleField=Test"})
    void putPlaceholderNotFound(String url) {
        ResponseEntity<Placeholder> responseEntity = restTemplate.exchange(url, HttpMethod.PUT, null, Placeholder.class);
        System.out.println(responseEntity.getStatusCode());
        assert(responseEntity.getStatusCode() == HttpStatus.NOT_FOUND);
    }

    @Order(7)
    @ParameterizedTest
    @ValueSource(strings={"/putplaceholdernormal?id={id}&sampleField={sampleField}","/putplaceholderfuturejoin?id={id}&sampleField={sampleField}"})
    void putPlaceholderSucceeds(String url) {
        Placeholder p = Placeholder.builder().id(1L).sampleField("Alteration").build();
        url = url.replace("{id}", Long.toString(p.getId())).replace("{sampleField}", p.getSampleField());

        ResponseEntity<Placeholder> responseEntity = restTemplate.exchange(url, HttpMethod.PUT, null, Placeholder.class);

        assert(responseEntity.getStatusCode() == HttpStatus.OK);
        assert(responseEntity.getBody().equals(p));
    }


    @Order(8)
    @ParameterizedTest
    @ValueSource(strings={"/deleteplaceholderbyidnormal?id=1","/deleteplaceholderbyidfuturejoin?id=2"})
    void deleteByIdWorks(String url) {
        ResponseEntity<Placeholder> responseEntity = restTemplate.exchange(url, HttpMethod.DELETE, null, Placeholder.class);
        
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Order(9)
    @ParameterizedTest
    @ValueSource(strings={"/deleteplaceholderbyidnormal?id=-1","/deleteplaceholderbyidfuturejoin?id=-1"})
    void deleteByIdNotFound(String url) {
        ResponseEntity<Placeholder> responseEntity = restTemplate.exchange(url, HttpMethod.DELETE, null, Placeholder.class);
        
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Order(10)
    @ParameterizedTest
    @ValueSource(strings={"/deleteplaceholderbyidnormal?id=a","/deleteplaceholderbyidfuturejoin?id=a"})
    void deleteByIdBadRequest(String url) {
        ResponseEntity<Placeholder> responseEntity = restTemplate.exchange(url, HttpMethod.DELETE, null, Placeholder.class);
        
        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
    }
}
