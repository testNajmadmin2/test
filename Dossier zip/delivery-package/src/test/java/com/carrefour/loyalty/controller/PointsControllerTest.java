package com.carrefour.loyalty.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PointsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldEarnPointsAndReadBalance() throws Exception {
        mockMvc.perform(post("/customers/cust-api/points/earn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purchaseAmountInCents": 12000}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availablePoints").value(120));

        mockMvc.perform(get("/customers/cust-api/points/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availablePoints").value(120));
    }
}
