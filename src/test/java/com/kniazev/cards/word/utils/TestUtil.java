package com.kniazev.cards.word.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Assertions;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.web.client.HttpClientErrorException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kniazev.cards.word.api.model.WordDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TestUtil {
    private static final ClassLoader CLASSLOADER = TestUtil.class.getClassLoader();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    public static void doRun(Runnable action) {
        try {
            action.run();
        } catch (HttpClientErrorException e) {
            //Nothing to do
            log.warn("Error in doCall: " + e.getMessage());
        }
    }

    public static <T> T doCall(Callable<T> action) {
        try {
            return action.call();
        } catch (Exception e) {
            log.warn("Error in doCall: " + e.getMessage());
            return null;
        }
    }

    public static void testError(int expectedStatus, Runnable actionToCheck) {
        HttpClientErrorException exception = null;
        try {
            actionToCheck.run();
        } catch (HttpClientErrorException e) {
            exception = e;
        } catch (Exception e) {
            log.error("VIKN: " + e.getMessage() + " " + e);
        }

        assertEquals(expectedStatus, exception.getStatusCode());
    }
    
    public static List<WordDto> parseFile(String filepath, Function<CSVRecord, WordDto> mapper) {
        try (FileReader reader = new FileReader(CLASSLOADER.getResource(filepath).getFile()); 
                CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader("wordType", "ru", "de", "plural", "gender").withDelimiter(';').withFirstRecordAsHeader())) {
            
            return csvParser.getRecords().stream()
                    .map(mapper)
                    .toList();    
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    public static String objectAsJson(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
    
    public static <T> T jsonAsObject(String json, Class<T> clazz) throws JsonMappingException, JsonProcessingException {
        return new ObjectMapper().readValue(json, clazz);
    }
    
    public static void check(String filepath, Class<?> targetClass, String actual) {
        try  {
            Path path = Path.of(CLASSLOADER.getResource(filepath).toURI());
            
            String fileContent = Files.readString(path);
         
            check(fileContent, actual);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void check(String expectedJson, String actualJson) {
        Assertions.assertDoesNotThrow(() -> JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.LENIENT));
    }

}
