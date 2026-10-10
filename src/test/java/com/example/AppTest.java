package com.example;

import com.example.model.KinshipResult;
import com.example.model.KinshipResult.CommonAncestor;
import com.example.model.KinshipResult.Direction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for simple App.
 */
public class AppTest {
    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Test
    public void kinshipJsonUsesFrontendPropertyNames() throws Exception {
        KinshipResult result = new KinshipResult();
        result.setAToB(new Direction("chú", "chú ruột", "cháu", ""));
        result.setBToA(new Direction("cháu", "cháu", "chú", ""));
        result.setCommonAncestors(java.util.List.of(new CommonAncestor("P001", 2, 1)));

        JsonNode json = new ObjectMapper().valueToTree(result);

        assertEquals("chú", json.path("aToB").path("call").asText());
        assertEquals("cháu", json.path("bToA").path("call").asText());
        assertEquals("P001", json.path("commonAncestors").get(0).path("personId").asText());
    }
}
