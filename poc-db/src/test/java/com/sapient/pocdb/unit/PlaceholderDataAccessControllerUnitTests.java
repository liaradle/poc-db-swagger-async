package com.sapient.pocdb.unit;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapient.pocdb.controller.PlaceholderDataAccessController;
import com.sapient.pocdb.data.Placeholder;
import com.sapient.pocdb.repository.PlaceholderRepository;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Optional;


@WebMvcTest(PlaceholderDataAccessController.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PlaceholderDataAccessControllerUnitTests {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    PlaceholderRepository placeholderRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    @ParameterizedTest
    @Order(1)
    @ValueSource(strings = {"/allplaceholdersfuturejoin", "/allplaceholdersnormal"})
    void getAllPlaceholders(String url) throws Exception {
        ArrayList<Placeholder> expected = new ArrayList<Placeholder>();
        Placeholder p = Placeholder.builder().id(1L).sampleField("test1").build();
        Placeholder q = Placeholder.builder().id(2L).sampleField("test2").build();
        expected.add(p);
        expected.add(q);

        when(placeholderRepository.findAll()).thenReturn(expected);

        MvcResult responseMvcResult = mockMvc.perform(get(url)).andExpect(status().isOk()).andReturn();
        String actual = responseMvcResult.getResponse().getContentAsString();
        String expectedString = objectMapper.writeValueAsString(expected);
        assertEquals(expectedString, actual);
    }

    @ParameterizedTest
    @Order(2)
    @ValueSource(strings = {"/placeholderbyidnormal", "/placeholderbyidfuturejoin"})
    void getsPlaceholderById(String url) throws Exception {
        Optional<Placeholder> expected = Optional.of(Placeholder.builder().id(1L).sampleField("Test").build());

        when(placeholderRepository.findById(anyLong())).thenReturn(expected);

        MvcResult responseMvcResult = mockMvc.perform(get(url).param("id", Long.toString(expected.get().getId()))).andExpect(status().isOk()).andReturn();
        String actual = responseMvcResult.getResponse().getContentAsString();
        String expectedString = objectMapper.writeValueAsString(expected.get());
        assertEquals(expectedString, actual);
    }

    @ParameterizedTest
    @Order(3)
    @ValueSource(strings = {"/placeholderbyidnormal", "/placeholderbyidfuturejoin"})
    void badRequestOnGetByIdWhenLetterEnteredAsId(String url) throws Exception {
        mockMvc.perform(get(url).param("id", "a")).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @Order(4)
    @ValueSource(strings = {"/placeholderbyidnormal", "/placeholderbyidfuturejoin"})
    void idReturnedWhenNoPlaceholderFoundOnGetById(String url) throws Exception {
        mockMvc.perform(get(url).param("id", "1")).andExpect(status().isNotFound());
    }
    
    @ParameterizedTest
    @Order(5)
    @ValueSource(strings = {"/postplaceholdernormal?id={id}&sampleField={sampleField}", "/postplaceholderfuturejoin?id={id}&sampleField={sampleField}"})
    void postsPlaceholderSuccessfully(String url) throws Exception {
        Placeholder expected = Placeholder.builder().id(1L).sampleField("Test").build();
        String json = objectMapper.writeValueAsString(expected);

        when(placeholderRepository.save(isA(Placeholder.class))).thenReturn(expected);

        MvcResult response = mockMvc.perform(post(url, expected.getId(), expected.getSampleField()).content(json).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andReturn();
        String actual = response.getResponse().getContentAsString();
        assertEquals(json, actual);
    }

    @ParameterizedTest
    @Order(5)
    @ValueSource(strings = {"/postplaceholdernormal", "/postplaceholderfuturejoin"})
    void badRequestOnPostInvalidPlaceholder(String url) throws Exception {
        Placeholder toSend = Placeholder.builder().id(1L).sampleField("Test").build();
        String json = objectMapper.writeValueAsString(toSend);
        json = json.replace("1", "\"a\"");
        
        mockMvc.perform(post(url).content(json).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andReturn();
    }

    @ParameterizedTest
    @Order(6)
    @ValueSource(strings = {"/putplaceholdernormal?id={id}&sampleField={sampleField}", "/putplaceholderfuturejoin?id={id}&sampleField={sampleField}"})
    void putsPlaceholderSuccessfully(String url) throws Exception {
        Placeholder expected = Placeholder.builder().id(1L).sampleField("Test").build();
        String json = objectMapper.writeValueAsString(expected);
        placeholderRepository.save(expected);

        when(placeholderRepository.existsById(anyLong())).thenReturn(true);
        when(placeholderRepository.save(isA(Placeholder.class))).thenReturn(expected);

        MvcResult response = mockMvc.perform(put(url, expected.getId(), expected.getSampleField()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andReturn();
        String actual = response.getResponse().getContentAsString();
        assertEquals(json, actual);
    }

    @ParameterizedTest
    @Order(7)
    @ValueSource(strings = {"/putplaceholdernormal?id={id}&sampleField={sampleField}", "/putplaceholderfuturejoin?id={id}&sampleField={sampleField}"})
    void cannotFindPlaceholderOnPutRequest(String url) throws Exception {
        Placeholder expected = Placeholder.builder().id(-1L).sampleField("Test").build();
        String json = objectMapper.writeValueAsString(expected);

        when(placeholderRepository.existsById(anyLong())).thenReturn(false);

        mockMvc.perform(put(url, expected.getId(), expected.getSampleField()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }
    
    @ParameterizedTest
    @Order(7)
    @ValueSource(strings = {"/deleteplaceholderbyidnormal", "/deleteplaceholderbyidfuturejoin"})
    void deletesSuccessfullyById(String url) throws Exception {
        Placeholder expected = Placeholder.builder().id(1L).sampleField("Test").build();

        when(placeholderRepository.existsById(anyLong())).thenReturn(true);
        when(placeholderRepository.findById(anyLong())).thenReturn(Optional.of(expected));
        doNothing().when(placeholderRepository).deleteById(anyLong());

        mockMvc.perform(delete(url).param("id", Long.toString(expected.getId())).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @ParameterizedTest
    @Order(8)
    @ValueSource(strings = {"/deleteplaceholderbyidnormal", "/deleteplaceholderbyidfuturejoin"})
    void notFoundOnDeleteWhenSearchingById(String url) throws Exception {
        Placeholder toSend = Placeholder.builder().id(1L).sampleField("Test").build();

        when(placeholderRepository.existsById(anyLong())).thenReturn(false);

        mockMvc.perform(delete(url).param("id", Long.toString(toSend.getId())).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @Order(8)
    @ValueSource(strings = {"/deleteplaceholderbyidnormal", "/deleteplaceholderbyidfuturejoin"})
    void badRequestWhenLetterUsedforIdOnDeletion(String url) throws Exception {
        mockMvc.perform(delete(url).param("id", "a").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
    }
}
