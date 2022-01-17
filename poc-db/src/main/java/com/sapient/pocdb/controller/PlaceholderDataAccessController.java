package com.sapient.pocdb.controller;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.*;

import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sapient.pocdb.data.Placeholder;
import com.sapient.pocdb.repository.PlaceholderRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;

@Slf4j
@RestController
@ComponentScan("pocdb/repository")
public class PlaceholderDataAccessController {

    @Autowired
    PlaceholderRepository placeholderRepository;

    @PostConstruct
    void init() {
        log.info("Initialising database");
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        LongStream.range(1, 101).forEach(num -> placeholderRepository.save(new Placeholder(num, "test")));
        stopWatch.stop();
        log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
    }

    @GetMapping("/allplaceholdersnormal")
    public ResponseEntity<List<Placeholder>> getAllPlaceholderInfoNormal() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        List<Placeholder> list = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        placeholderRepository.findAll().forEach((placeholder) -> {
                list.add(placeholder);
            try {
                log.info(objectMapper.writeValueAsString(placeholder));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
        stopWatch.stop();
        log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @GetMapping("/allplaceholdersfuturejoin")
    public ResponseEntity<List<Placeholder>> getAllPlaceholderInfo() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ObjectMapper objectMapper = new ObjectMapper();
        CompletableFuture.runAsync(() -> StreamSupport
                .stream(placeholderRepository.findAll().spliterator(), true).forEach(content ->
                {
                    try {
                        log.info("Item: {}", objectMapper.writeValueAsString(content));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }));
        CompletableFuture<List<Placeholder>> completableFuture = CompletableFuture.supplyAsync(() ->
                StreamSupport.stream(placeholderRepository.findAll().spliterator(), true).collect(toList()));
        stopWatch.stop();
        List<Placeholder> list = completableFuture.join();
        log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
        return new ResponseEntity<>(list, HttpStatus.OK);
    }

    @GetMapping(path="/placeholderbyidfuturejoin")
    public ResponseEntity<Placeholder> getPlaceholderByIdFutureJoin(@RequestParam(name = "id") Long requestParam) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        CompletableFuture<Optional<Placeholder>> completableFuture = CompletableFuture.supplyAsync(() -> {
            Optional<Placeholder> placeholder;
            try {
                placeholder = placeholderRepository.findById(requestParam);
                return placeholder;
            } catch (IllegalArgumentException e) {
                log.info("Bad request received: {}", e.getCause().toString());
                return Optional.empty();
            }
        });
        CompletableFuture.runAsync(() -> {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                Optional<Placeholder> placeholder = placeholderRepository.findById(requestParam);
                if(placeholder.isPresent()) {
                    log.info("Placeholder info: {}", objectMapper.writeValueAsString(placeholder.get()));
                    sleep(1000);
                }
                else {
                    log.info("No such placeholder exists.");
                }
            }
            catch(IllegalArgumentException | JsonProcessingException | InterruptedException e) {
            }
                });
        Optional<Placeholder> placeholder = completableFuture.join();
        if(!placeholder.isPresent()) {
            stopWatch.stop();
            log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        else {
            stopWatch.stop();
            log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
            return new ResponseEntity<>(placeholder.get(), HttpStatus.OK);
        }
    }

    @GetMapping("/placeholderbyidnormal")
    public ResponseEntity<Placeholder> getPlaceholderByIdNormal(@RequestParam(name = "id") Long requestParam) throws JsonProcessingException, InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        Optional<Placeholder> placeholder;
        try {
            placeholder = placeholderRepository.findById(requestParam);
        }
        catch (IllegalArgumentException e) {
            log.info("Bad request received: {}", e.getCause().toString());
            stopWatch.stop();
            log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        if(!placeholder.isPresent()) {
            stopWatch.stop();
            log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        log.info("Placeholder: {}", objectMapper.writeValueAsString(placeholder.get()));
        sleep(1000);
        stopWatch.stop();
        log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
        return new ResponseEntity<>(placeholder.get(), HttpStatus.OK);
    }


    @PostMapping("/postplaceholdernormal")
    public ResponseEntity<Placeholder> postPlaceholderNormal(@RequestParam(name="id") Long id, @RequestParam(name="sampleField") String sampleField) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ObjectMapper objectMapper = new ObjectMapper();
        Placeholder placeholder;
        try {
            placeholder = new Placeholder(id, sampleField);
            if(!placeholderRepository.existsById(id)) {
                placeholderRepository.save(placeholder);
                log.info("Placeholder sent: {}", objectMapper.writeValueAsString(placeholder));
                Thread.sleep(1000);
            }
            else {
                return new ResponseEntity<>(placeholder, HttpStatus.FOUND);
            }
        }
        catch(IllegalArgumentException | InterruptedException | JsonProcessingException e) {
            log.info("Bad request received: {}", e.getCause());
            stopWatch.stop();
            log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        stopWatch.stop();
        log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
        return new ResponseEntity<>(placeholder, HttpStatus.CREATED);
    }

    @PostMapping("/postplaceholderfuturejoin")
    public ResponseEntity<Placeholder> postPlaceholderfuturejoin(@RequestParam(name="id") Long id, @RequestParam(name="sampleField") String sampleField) throws InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        CompletableFuture<ResponseEntity<Placeholder>> completableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Placeholder placeholder = new Placeholder(id, sampleField);
                if(!placeholderRepository.existsById(id)) {
                    placeholderRepository.save(placeholder);
                    return new ResponseEntity<>(placeholder, HttpStatus.CREATED);
                }
                else {
                    return new ResponseEntity<>(placeholder, HttpStatus.FOUND);
                }
            }
            catch(IllegalArgumentException e) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        });
        CompletableFuture.runAsync(() -> {
            try {
                Placeholder placeholder;
                placeholder = new Placeholder(id, sampleField);
                log.info("Placeholder sent: {}", objectMapper.writeValueAsString(placeholder));
                Thread.sleep(1000);
            } catch (JsonProcessingException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        return completableFuture.join();
    }

    @PutMapping("/putplaceholderfuturejoin")
    public ResponseEntity<Placeholder> putPlaceholderFutureJoin(@RequestParam(name="id") long id, @RequestParam(name="sampleField") String sampleField) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ObjectMapper objectMapper = new ObjectMapper();
        CompletableFuture<ResponseEntity<Placeholder>> completableFuture = CompletableFuture.supplyAsync(() -> {
            Placeholder placeholder = new Placeholder(id, sampleField);
            if(placeholderRepository.existsById(id)) {
                placeholderRepository.save(placeholder);
                return new ResponseEntity<>(placeholder, HttpStatus.OK);
            }
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        });
        CompletableFuture.runAsync(() -> {
            try {
                Placeholder placeholder = new Placeholder(id, sampleField);
                log.info("Placeholder: {}", objectMapper.writeValueAsString(placeholder));
                Thread.sleep(1000);
            } catch (InterruptedException | JsonProcessingException e) {
                e.printStackTrace();
            }
        });
        stopWatch.stop();
        log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
        return completableFuture.join();
    }

    @PutMapping("/putplaceholdernormal")
    public ResponseEntity<Placeholder> putPlaceholderNormal(@RequestParam(name="id") long id, @RequestParam(name="sampleField") String sampleField) throws InterruptedException, JsonProcessingException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ObjectMapper objectMapper = new ObjectMapper();
        Placeholder placeholder = new Placeholder(id, sampleField);
        if(!placeholderRepository.existsById(id)) {
            log.info("Attempted PUT on id {}", id);
            stopWatch.stop();
            log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
            return new ResponseEntity<>(placeholder, HttpStatus.NOT_FOUND);
        }
        placeholderRepository.save(placeholder);
        log.info("Placeholder: {}", objectMapper.writeValueAsString(placeholder));
        Thread.sleep(1000);
        stopWatch.stop();
        log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
        return new ResponseEntity<>(placeholder, HttpStatus.OK);
    }

    @DeleteMapping("/deleteplaceholderbyidnormal")
    public ResponseEntity<Placeholder> deletePlaceholderByIdNormal(@RequestParam(name="id") Long requestParam) throws InterruptedException, JsonProcessingException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ObjectMapper objectMapper = new ObjectMapper();
        if(!placeholderRepository.existsById(requestParam)) {
            log.info("Attempted DEL on id {}", requestParam);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        Placeholder placeholder;
        try {
        placeholder = placeholderRepository.findById(requestParam).get();
        placeholderRepository.deleteById(requestParam);
        }
        catch(IllegalArgumentException e) {
            log.info("Attempted DEL on id {} met with error {}", requestParam, e.getCause());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        log.info("Placeholder: {}", objectMapper.writeValueAsString(placeholder));
        Thread.sleep(1000);
        stopWatch.stop();
        log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
        return new ResponseEntity<>(placeholder, HttpStatus.OK);
    }

    @DeleteMapping("/deleteplaceholderbyidfuturejoin")
    public ResponseEntity<Placeholder> deletePlaceholderByIdFutureJoin(@RequestParam(name="id") Long requestParam) {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        ObjectMapper objectMapper = new ObjectMapper();
        CompletableFuture.runAsync(() -> {
            if(placeholderRepository.existsById(requestParam)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        CompletableFuture<Placeholder> completableFuture = CompletableFuture.supplyAsync(() -> {
            Placeholder placeholder;
            if(placeholderRepository.existsById(requestParam)) {
                try {
                    log.info("Placeholder: {}", objectMapper.writeValueAsString(placeholderRepository.findById(requestParam).get()));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                placeholder = placeholderRepository.findById(requestParam).get();
                placeholderRepository.deleteById(requestParam);

                return placeholder;
            }
            return null;
        });
        Placeholder val = completableFuture.join();
        stopWatch.stop();
        log.info("Time taken: {}ms", stopWatch.getTotalTimeMillis());
        if(!(val == null)) {
            return new ResponseEntity<>(val, HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);

    }
    
}
