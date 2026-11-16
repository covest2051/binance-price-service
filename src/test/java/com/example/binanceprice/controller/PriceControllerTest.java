package com.example.binanceprice.controller;

import com.example.binanceprice.config.ClockConfig;
import com.example.binanceprice.dto.request.PeriodRequest;
import com.example.binanceprice.exception.PriceNotFoundException;
import com.example.binanceprice.service.PriceQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PriceController.class)
@Import(ClockConfig.class)
class PriceControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private PriceQueryService priceQueryService;

    @Test
    void missingPeriodBoundIsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/prices/average").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.path").value("/api/v1/prices/average"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.violations[0].field").value("from"));
        verifyNoInteractions(priceQueryService);
    }

    @Test
    void malformedDateIsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/prices/trend").param("from", "yesterday").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("from"))
                .andExpect(jsonPath("$.violations[0].message").value("invalid value 'yesterday'"));
    }

    @Test
    void fromAfterToIsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/prices/average").param("from", "2026-09-02").param("to", "2026-09-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].message").value("from must not be after to"));
    }

    @Test
    void pageSizeOutOfRangeIsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/prices")
                        .param("from", "2026-09-01").param("to", "2026-09-30").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violations[0].field").value("size"));
    }

    @Test
    void nonNumericPageIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/prices")
                        .param("from", "2026-09-01").param("to", "2026-09-30").param("page", "first"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value("/api/v1/prices"))
                .andExpect(jsonPath("$.violations[0].field").value("page"))
                .andExpect(jsonPath("$.violations[0].message").value("invalid value 'first'"));
    }

    @Test
    void applicationExceptionKeepsItsStatusAndMessage() throws Exception {
        PeriodRequest period = new PeriodRequest(LocalDate.parse("2026-08-01"), LocalDate.parse("2026-08-31"));
        when(priceQueryService.getAverage("BNBUSDT", period))
                .thenThrow(new PriceNotFoundException("BNBUSDT", period.from(), period.to()));

        mockMvc.perform(get("/api/v1/prices/average").param("from", "2026-08-01").param("to", "2026-08-31"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("No prices for BNBUSDT from 2026-08-01 to 2026-08-31"));
    }

    @Test
    void unexpectedErrorDoesNotLeakDetails() throws Exception {
        when(priceQueryService.getLatest(eq("BNBUSDT")))
                .thenThrow(new DataAccessResourceFailureException("connection refused to db:5432"));

        mockMvc.perform(get("/api/v1/prices/latest"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @Test
    void unsupportedMethodUsesSameContract() throws Exception {
        mockMvc.perform(post("/api/v1/prices/latest"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.path").value("/api/v1/prices/latest"));
    }

    @Test
    void unknownPathUsesSameContract() throws Exception {
        mockMvc.perform(get("/api/v1/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
        verifyNoInteractions(priceQueryService);
    }
}
