package com.oilcollection.security;

import com.oilcollection.controller.CollectionPointController;
import com.oilcollection.controller.CollectionRecordController;
import com.oilcollection.controller.CollectionScheduleController;
import com.oilcollection.controller.CollectorCoverageController;
import com.oilcollection.controller.ExchangeItemController;
import com.oilcollection.controller.IncidentController;
import com.oilcollection.controller.SystemConfigController;
import com.oilcollection.repository.CollectionPointRepository;
import com.oilcollection.service.CollectionPointService;
import com.oilcollection.service.CollectionRecordService;
import com.oilcollection.service.CollectionScheduleService;
import com.oilcollection.service.CollectorCoverageService;
import com.oilcollection.service.ExchangeItemService;
import com.oilcollection.service.IncidentService;
import com.oilcollection.service.SystemConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        CollectionPointController.class,
        CollectionScheduleController.class,
        CollectionRecordController.class,
        IncidentController.class,
        ExchangeItemController.class,
        SystemConfigController.class,
        CollectorCoverageController.class
})
@Import(SecurityConfig.class)
class EndpointAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private CollectionPointRepository collectionPointRepository;

    @MockBean
    private CollectionPointService collectionPointService;

    @MockBean
    private CollectionScheduleService collectionScheduleService;

    @MockBean
    private CollectionRecordService collectionRecordService;

    @MockBean
    private IncidentService incidentService;

    @MockBean
    private ExchangeItemService exchangeItemService;

    @MockBean
    private SystemConfigService systemConfigService;

    @MockBean
    private CollectorCoverageService collectorCoverageService;

    @Test
    @WithMockUser(roles = "ESTABLISHMENT")
    void establishmentCanCreateCollectionPoint() throws Exception {
        String body = """
                {
                  "name": "Ponto Teste",
                  "address": "Rua Teste, 123",
                  "city": "São Paulo",
                  "state": "SP"
                }
                """;

        mockMvc.perform(post("/api/collection-points")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ESTABLISHMENT")
    void establishmentCanUpdateOwnCollectionPoint() throws Exception {
        String body = """
                {
                  "name": "Ponto Atualizado",
                  "address": "Rua Teste, 123",
                  "city": "São Paulo",
                  "state": "SP"
                }
                """;

        mockMvc.perform(put("/api/collection-points/{id}", UUID.randomUUID())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ESTABLISHMENT")
    void establishmentCanGetMyPoints() throws Exception {
        mockMvc.perform(get("/api/collection-points/my-points"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ESTABLISHMENT")
    void establishmentCanCreateSchedule() throws Exception {
        String body = """
                {
                  "collectionPointId": "%s",
                  "scheduledDate": "2025-12-31",
                  "startTime": "10:00:00",
                  "estimatedVolumeLiters": 100,
                  "saleType": "SALE"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/schedules")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ESTABLISHMENT")
    void establishmentCanCancelScheduleWithTwoHours() throws Exception {
        String body = "reason=Emergência";
        mockMvc.perform(put("/api/schedules/{id}/cancel", UUID.randomUUID())
                        .param("reason", "Emergência"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ESTABLISHMENT")
    void establishmentCanGetMySchedules() throws Exception {
        mockMvc.perform(get("/api/schedules/my-schedules"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ESTABLISHMENT")
    void establishmentCanConfirmCollection() throws Exception {
        mockMvc.perform(put("/api/records/{id}/confirm", UUID.randomUUID())
                        .param("confirmationCode", "123456"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ESTABLISHMENT")
    void establishmentCanGetCollectionCertificate() throws Exception {
        mockMvc.perform(get("/api/records/{id}/certificate", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ESTABLISHMENT")
    void establishmentCanCreateIncident() throws Exception {
        String body = """
                {
                  "collectionPointId":"%s",
                  "reporterId":"%s",
                  "incidentType":"SPILL",
                  "severity":"HIGH"
                }
                """.formatted(UUID.randomUUID(), UUID.randomUUID());

        mockMvc.perform(post("/api/incidents")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }

    // US03 - Establishment can access schedule GET endpoints (filtered by ownership)
    @Test
    @WithMockUser(roles = "ESTABLISHMENT")
    void establishmentCanGetAllSchedules() throws Exception {
        mockMvc.perform(get("/api/schedules"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ESTABLISHMENT")
    void establishmentCanGetAllSchedulesList() throws Exception {
        mockMvc.perform(get("/api/schedules/list"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ESTABLISHMENT")
    void establishmentCanGetScheduleById() throws Exception {
        mockMvc.perform(get("/api/schedules/{id}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ESTABLISHMENT")
    void establishmentCanGetSchedulesByPoint() throws Exception {
        mockMvc.perform(get("/api/schedules/point/{pointId}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ESTABLISHMENT")
    void establishmentCanGetSchedulesByDate() throws Exception {
        mockMvc.perform(get("/api/schedules/date/{date}", "2025-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COLLECTOR")
    void collectorCanGetExchangeItemsBySchedule() throws Exception {
        mockMvc.perform(get("/api/exchange-items/schedule/{scheduleId}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COLLECTOR")
    void collectorCanGetBaseOilPrice() throws Exception {
        when(systemConfigService.getBaseOilPricePerLiter()).thenReturn(BigDecimal.valueOf(5));

        mockMvc.perform(get("/api/system-config/price/base-oil"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COLLECTOR")
    void collectorCanGetCollectorCoverage() throws Exception {
        mockMvc.perform(get("/api/collector-coverage/{id}", UUID.randomUUID()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "COLLECTOR")
    void collectorCanCancelScheduleAnytime() throws Exception {
        mockMvc.perform(put("/api/schedules/{id}/cancel", UUID.randomUUID())
                        .param("reason", "Imprevisto"))
                .andExpect(status().isOk());
    }
}
