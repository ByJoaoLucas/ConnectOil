package com.oilcollection.service;

import com.oilcollection.dto.CollectionScheduleDTO;
import com.oilcollection.exception.BusinessException;
import com.oilcollection.exception.ResourceNotFoundException;
import com.oilcollection.model.CollectionPoint;
import com.oilcollection.model.CollectionSchedule;
import com.oilcollection.model.User;
import com.oilcollection.repository.CollectionPointRepository;
import com.oilcollection.repository.CollectionScheduleRepository;
import com.oilcollection.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class CollectionScheduleServiceTest {

    @Mock
    private CollectionScheduleRepository scheduleRepository;

    @Mock
    private CollectionPointRepository pointRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private SystemConfigService systemConfigService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private CollectionScheduleService scheduleService;

    private User establishmentUser;
    private User otherEstablishmentUser;
    private User collectorUser;
    private User adminUser;
    private CollectionPoint establishmentPoint;
    private CollectionPoint otherEstablishmentPoint;
    private CollectionScheduleDTO createRequest;

    @BeforeEach
    void setUp() {
        lenient().when(httpRequest.getHeader("User-Agent")).thenReturn("test-agent");
        lenient().when(httpRequest.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(systemConfigService.getCancellationHoursBefore()).thenReturn(2);
        lenient().when(systemConfigService.getBaseOilPricePerLiter()).thenReturn(BigDecimal.valueOf(5.0));

        establishmentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Estabelecimento Teste")
                .email("estab@test.com")
                .cnpj("12345678000199")
                .role(User.UserRole.ESTABLISHMENT)
                .isActive(true)
                .build();

        otherEstablishmentUser = User.builder()
                .id(UUID.randomUUID())
                .name("Outro Estabelecimento")
                .email("estab2@test.com")
                .cnpj("98765432000188")
                .role(User.UserRole.ESTABLISHMENT)
                .isActive(true)
                .build();

        collectorUser = User.builder()
                .id(UUID.randomUUID())
                .name("Coletor Teste")
                .email("collector@test.com")
                .cpf("12345678901")
                .role(User.UserRole.COLLECTOR)
                .isActive(true)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .name("Admin")
                .email("admin@test.com")
                .role(User.UserRole.ADMIN)
                .isActive(true)
                .build();

        establishmentPoint = CollectionPoint.builder()
                .id(UUID.randomUUID())
                .name("Meu Posto")
                .address("Rua A, 100")
                .city("São Paulo")
                .state("SP")
                .status(CollectionPoint.PointStatus.ACTIVE)
                .establishment(establishmentUser)
                .build();

        otherEstablishmentPoint = CollectionPoint.builder()
                .id(UUID.randomUUID())
                .name("Posto Outro")
                .address("Rua B, 200")
                .city("Rio de Janeiro")
                .state("RJ")
                .status(CollectionPoint.PointStatus.ACTIVE)
                .establishment(otherEstablishmentUser)
                .build();

        createRequest = new CollectionScheduleDTO();
        createRequest.setCollectionPointId(establishmentPoint.getId());
        createRequest.setScheduledDate(LocalDate.now().plusDays(1));
        createRequest.setStartTime(LocalTime.of(10, 0));
        createRequest.setEndTime(LocalTime.of(12, 0));
        createRequest.setEstimatedVolumeLiters(100);
        createRequest.setNotes("Teste");
        createRequest.setSaleType(CollectionSchedule.SaleType.SALE);
        createRequest.setTimeWindow("MORNING");
        createRequest.setBasePricePerLiter(BigDecimal.valueOf(5.0));
    }

    // ===== TESTS FOR ESTABLISHMENT CREATING SCHEDULE WITHOUT COLLECTOR =====

    @Test
    void createSchedule_Establishment_WithoutCollector_Success() {
        when(pointRepository.findById(establishmentPoint.getId())).thenReturn(Optional.of(establishmentPoint));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditLogService).logCreate(anyString(), any(), any(), anyString(), anyString(), anyString());

        CollectionScheduleDTO result = scheduleService.createSchedule(createRequest, establishmentUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(establishmentPoint.getId(), result.getCollectionPointId());
        assertNull(result.getCollectorId());
        assertEquals(CollectionSchedule.ScheduleStatus.PENDING, result.getStatus());
        assertEquals(CollectionSchedule.SaleType.SALE, result.getSaleType());
        assertEquals("MORNING", result.getTimeWindow());
        verify(scheduleRepository).save(any());
    }

    @Test
    void createSchedule_Establishment_WithOtherEstablishmentPoint_ThrowsException() {
        createRequest.setCollectionPointId(otherEstablishmentPoint.getId());
        when(pointRepository.findById(otherEstablishmentPoint.getId())).thenReturn(Optional.of(otherEstablishmentPoint));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.createSchedule(createRequest, establishmentUser, "127.0.0.1", "test-agent"));

        assertEquals("POINT_NOT_OWNED", exception.getErrorCode());
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void createSchedule_Establishment_WithPointWithoutEstablishment_ThrowsException() {
        CollectionPoint pointWithoutEstablishment = CollectionPoint.builder()
                .id(UUID.randomUUID())
                .name("Ponto Sem Dono")
                .address("Rua C, 300")
                .city("Belo Horizonte")
                .state("MG")
                .status(CollectionPoint.PointStatus.ACTIVE)
                .establishment(null)
                .build();

        // Create a request with this point's ID
        CollectionScheduleDTO requestForPoint = new CollectionScheduleDTO();
        requestForPoint.setCollectionPointId(pointWithoutEstablishment.getId());
        requestForPoint.setScheduledDate(LocalDate.now().plusDays(1));
        requestForPoint.setStartTime(LocalTime.of(10, 0));
        requestForPoint.setEndTime(LocalTime.of(12, 0));
        requestForPoint.setEstimatedVolumeLiters(100);
        requestForPoint.setNotes("Teste");
        requestForPoint.setSaleType(CollectionSchedule.SaleType.SALE);
        requestForPoint.setTimeWindow("MORNING");
        requestForPoint.setBasePricePerLiter(BigDecimal.valueOf(5.0));

        when(pointRepository.findById(pointWithoutEstablishment.getId())).thenReturn(Optional.of(pointWithoutEstablishment));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.createSchedule(requestForPoint, establishmentUser, "127.0.0.1", "test-agent"));

        assertEquals("POINT_NOT_OWNED", exception.getErrorCode());
    }

    @Test
    void createSchedule_Collector_RequiresCollectorId() {
        createRequest.setCollectorId(null);
        when(pointRepository.findById(establishmentPoint.getId())).thenReturn(Optional.of(establishmentPoint));
        // Point has no collector assigned, so it should pass ownership check for collector
        establishmentPoint.setCollector(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.createSchedule(createRequest, collectorUser, "127.0.0.1", "test-agent"));

        assertEquals("COLLECTOR_REQUIRED", exception.getErrorCode());
    }

    @Test
    void createSchedule_Admin_RequiresCollectorId() {
        createRequest.setCollectorId(null);
        when(pointRepository.findById(establishmentPoint.getId())).thenReturn(Optional.of(establishmentPoint));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.createSchedule(createRequest, adminUser, "127.0.0.1", "test-agent"));

        assertEquals("COLLECTOR_REQUIRED", exception.getErrorCode());
    }

    // ===== TESTS FOR ESTABLISHMENT CANCELLATION (2-HOUR RULE) =====

    @Test
    void cancelSchedule_Establishment_OwnPoint_MoreThan2Hours_Success() {
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollectionPoint(establishmentPoint);
        schedule.setScheduledDate(LocalDate.now().plusDays(1));
        schedule.setStartTime(LocalTime.of(10, 0));

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditLogService).logStatusChange(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString());

        CollectionScheduleDTO result = scheduleService.cancelSchedule(schedule.getId(), "Emergência", establishmentUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(CollectionSchedule.ScheduleStatus.CANCELLED, result.getStatus());
        assertEquals("Emergência", result.getCancellationReason());
    }

    @Test
    void cancelSchedule_Establishment_LessThan2Hours_ThrowsException() {
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollectionPoint(establishmentPoint);

        // Use America/Sao_Paulo timezone for test data to match business logic
        // Schedule for 1 hour from now (rounded up to next hour) to ensure < 2h gap
        // Handle midnight crossover by using tomorrow's date if needed
        ZoneId saoPauloZone = ZoneId.of("America/Sao_Paulo");
        LocalDateTime nowSaoPaulo = LocalDateTime.now(saoPauloZone);

        // Target: 1 hour from now, rounded up to next hour boundary
        // e.g., now=10:30 -> target=11:00 (30 min), now=10:00 -> target=11:00 (1h), now=23:30 -> target=00:00 next day (30 min)
        LocalDateTime target = nowSaoPaulo.plusHours(1).withMinute(0).withSecond(0).withNano(0);

        schedule.setScheduledDate(target.toLocalDate());
        schedule.setStartTime(target.toLocalTime());

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.cancelSchedule(schedule.getId(), "Emergência", establishmentUser, "127.0.0.1", "test-agent"));

        assertEquals("CANCELLATION_TOO_LATE", exception.getErrorCode());
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void cancelSchedule_Establishment_OtherEstablishmentPoint_ThrowsException() {
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollectionPoint(otherEstablishmentPoint);
        schedule.setScheduledDate(LocalDate.now().plusDays(1));
        schedule.setStartTime(LocalTime.of(10, 0));

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.cancelSchedule(schedule.getId(), "Emergência", establishmentUser, "127.0.0.1", "test-agent"));

        assertEquals("POINT_NOT_OWNED", exception.getErrorCode());
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void cancelSchedule_Establishment_WithNullCollector_NoNPE() {
        // Schedule without collector (created by establishment)
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollectionPoint(establishmentPoint);
        schedule.setCollector(null); // No collector assigned
        schedule.setScheduledDate(LocalDate.now().plusDays(1));
        schedule.setStartTime(LocalTime.of(10, 0));

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditLogService).logStatusChange(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString());

        // Should not throw NPE
        CollectionScheduleDTO result = scheduleService.cancelSchedule(schedule.getId(), "Emergência", establishmentUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(CollectionSchedule.ScheduleStatus.CANCELLED, result.getStatus());
    }

    // ===== TESTS FOR COLLECTOR CANCELLATION (ANYTIME WITH REASON) =====

    @Test
    void cancelSchedule_Collector_OwnSchedule_AnyTime_Success() {
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollector(collectorUser);
        schedule.setScheduledDate(LocalDate.now());
        schedule.setStartTime(LocalTime.now().plusMinutes(30)); // 30 minutes from now

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditLogService).logStatusChange(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString());

        CollectionScheduleDTO result = scheduleService.cancelSchedule(schedule.getId(), "Imprevisto", collectorUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(CollectionSchedule.ScheduleStatus.CANCELLED, result.getStatus());
        assertEquals("Imprevisto", result.getCancellationReason());
    }

    @Test
    void cancelSchedule_Collector_WithoutReason_ThrowsException() {
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollector(collectorUser);

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.cancelSchedule(schedule.getId(), "", collectorUser, "127.0.0.1", "test-agent"));

        assertEquals("REASON_REQUIRED", exception.getErrorCode());
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void cancelSchedule_Collector_OtherCollectorSchedule_ThrowsException() {
        User otherCollector = User.builder()
                .id(UUID.randomUUID())
                .name("Outro Coletor")
                .email("collector2@test.com")
                .role(User.UserRole.COLLECTOR)
                .isActive(true)
                .build();

        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollector(otherCollector);

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.cancelSchedule(schedule.getId(), "Imprevisto", collectorUser, "127.0.0.1", "test-agent"));

        assertEquals("UNAUTHORIZED_ACTION", exception.getErrorCode());
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void cancelSchedule_Collector_WithNullCollector_NoNPE() {
        // This shouldn't happen in practice but test defensive code
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollector(null); // No collector

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.cancelSchedule(schedule.getId(), "Imprevisto", collectorUser, "127.0.0.1", "test-agent"));

        assertEquals("UNAUTHORIZED_ACTION", exception.getErrorCode());
    }

    // ===== TESTS FOR OTHER OPERATIONS WITH NULL COLLECTOR =====

    @Test
    void completeSchedule_WithNullCollector_NoNPE() {
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollector(null);
        schedule.setStatus(CollectionSchedule.ScheduleStatus.IN_PROGRESS);
        schedule.setCollectionPoint(establishmentPoint);

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditLogService).logStatusChange(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString());

        // Should not throw NPE
        CollectionScheduleDTO result = scheduleService.completeSchedule(schedule.getId(), collectorUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(CollectionSchedule.ScheduleStatus.COMPLETED, result.getStatus());
    }

    @Test
    void completeSchedule_WithCollector_NotifiesBothCollectorAndEstablishment() {
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollector(collectorUser);
        schedule.setStatus(CollectionSchedule.ScheduleStatus.IN_PROGRESS);
        schedule.setCollectionPoint(establishmentPoint);

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditLogService).logStatusChange(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString());

        CollectionScheduleDTO result = scheduleService.completeSchedule(schedule.getId(), collectorUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(CollectionSchedule.ScheduleStatus.COMPLETED, result.getStatus());

        // Verify notification to collector
        verify(notificationService).notifyScheduleCompleted(collectorUser.getId(), schedule.getId());
        // Verify notification to establishment
        verify(notificationService).notifyScheduleCompleted(establishmentUser.getId(), schedule.getId());
    }

    @Test
    void completeSchedule_WithNullCollector_NotifiesOnlyEstablishment() {
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollector(null); // No collector assigned
        schedule.setStatus(CollectionSchedule.ScheduleStatus.IN_PROGRESS);
        schedule.setCollectionPoint(establishmentPoint);

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditLogService).logStatusChange(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString());

        CollectionScheduleDTO result = scheduleService.completeSchedule(schedule.getId(), collectorUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(CollectionSchedule.ScheduleStatus.COMPLETED, result.getStatus());

        // Verify notification to establishment only
        verify(notificationService).notifyScheduleCompleted(establishmentUser.getId(), schedule.getId());
        // Verify notifyScheduleCompleted was called exactly once (only establishment)
        verify(notificationService, times(1)).notifyScheduleCompleted(any(), any());
    }

    @Test
    void startSchedule_WithNullCollector_NoNPE() {
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollector(null);
        schedule.setStatus(CollectionSchedule.ScheduleStatus.PENDING);
        schedule.setCollectionPoint(establishmentPoint);

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditLogService).logStatusChange(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString());

        // Should not throw NPE
        CollectionScheduleDTO result = scheduleService.startSchedule(schedule.getId(), collectorUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(CollectionSchedule.ScheduleStatus.EN_ROUTE, result.getStatus());
    }

    @Test
    void arriveAtPoint_WithNullCollector_NoNPE() {
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollector(null);
        schedule.setStatus(CollectionSchedule.ScheduleStatus.EN_ROUTE);
        schedule.setCollectionPoint(establishmentPoint);

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditLogService).logStatusChange(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString());

        // Should not throw NPE
        CollectionScheduleDTO result = scheduleService.arriveAtPoint(schedule.getId(), collectorUser, "127.0.0.1", "test-agent");

        assertNotNull(result);
        assertEquals(CollectionSchedule.ScheduleStatus.IN_PROGRESS, result.getStatus());
    }

    @Test
    void confirmSchedule_WithNullCollector_ThrowsException() {
        // Collector cannot confirm a schedule without being assigned
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollector(null);
        schedule.setStatus(CollectionSchedule.ScheduleStatus.PENDING);

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.confirmSchedule(schedule.getId(), collectorUser, "127.0.0.1", "test-agent"));

        assertEquals("UNAUTHORIZED_ACTION", exception.getErrorCode());
    }

    // ===== TESTS FOR US03 - ESTABLISHMENT SCHEDULE FILTERING =====

    @Test
    void getSchedulesByEstablishmentAndDate_ReturnsOnlyOwnSchedules() {
        LocalDate date = LocalDate.now().plusDays(1);
        CollectionSchedule schedule1 = createScheduleEntity();
        schedule1.setId(UUID.randomUUID());
        schedule1.setScheduledDate(date);

        CollectionSchedule schedule2 = createScheduleEntity();
        schedule2.setId(UUID.randomUUID());
        schedule2.setCollectionPoint(otherEstablishmentPoint);
        schedule2.setScheduledDate(date);

        when(userRepository.findById(establishmentUser.getId())).thenReturn(Optional.of(establishmentUser));
        when(scheduleRepository.findByCollectionPoint_EstablishmentAndScheduledDate(establishmentUser, date))
                .thenReturn(List.of(schedule1));

        List<CollectionScheduleDTO> result = scheduleService.getSchedulesByEstablishmentAndDate(establishmentUser.getId(), date);

        assertEquals(1, result.size());
        assertEquals(schedule1.getId(), result.get(0).getId());
        verify(scheduleRepository).findByCollectionPoint_EstablishmentAndScheduledDate(establishmentUser, date);
    }

    @Test
    void getSchedulesByEstablishmentAndDate_EmptyWhenNoSchedules() {
        LocalDate date = LocalDate.now().plusDays(1);
        when(userRepository.findById(establishmentUser.getId())).thenReturn(Optional.of(establishmentUser));
        when(scheduleRepository.findByCollectionPoint_EstablishmentAndScheduledDate(establishmentUser, date))
                .thenReturn(List.of());

        List<CollectionScheduleDTO> result = scheduleService.getSchedulesByEstablishmentAndDate(establishmentUser.getId(), date);

        assertTrue(result.isEmpty());
    }

    @Test
    void getSchedulesByEstablishmentAndDate_InvalidRole_ThrowsException() {
        when(userRepository.findById(collectorUser.getId())).thenReturn(Optional.of(collectorUser));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> scheduleService.getSchedulesByEstablishmentAndDate(collectorUser.getId(), LocalDate.now()));

        assertEquals("INVALID_ESTABLISHMENT_ROLE", exception.getErrorCode());
    }

    // ===== TESTS FOR NOTIFICATIONS ON CANCELLATION =====

    @Test
    void cancelSchedule_Establishment_NotifiesBothEstablishmentAndCollector() {
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollectionPoint(establishmentPoint);
        schedule.setCollector(collectorUser);
        schedule.setScheduledDate(LocalDate.now().plusDays(1));
        schedule.setStartTime(LocalTime.of(10, 0));

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditLogService).logStatusChange(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString());

        scheduleService.cancelSchedule(schedule.getId(), "Emergência", establishmentUser, "127.0.0.1", "test-agent");

        // Verify notification to collector
        verify(notificationService).notifyScheduleCancelled(collectorUser.getId(), schedule.getId(), "Emergência");
        // Verify notification to establishment
        verify(notificationService).notifyScheduleCancelled(establishmentUser.getId(), schedule.getId(), "Emergência");
    }

    @Test
    void cancelSchedule_Establishment_WithoutCollector_NotifiesOnlyEstablishment() {
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollectionPoint(establishmentPoint);
        schedule.setCollector(null); // No collector assigned
        schedule.setScheduledDate(LocalDate.now().plusDays(1));
        schedule.setStartTime(LocalTime.of(10, 0));

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditLogService).logStatusChange(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString());

        scheduleService.cancelSchedule(schedule.getId(), "Emergência", establishmentUser, "127.0.0.1", "test-agent");

        // Verify notification to establishment (only notification since collector is null)
        verify(notificationService).notifyScheduleCancelled(establishmentUser.getId(), schedule.getId(), "Emergência");
        // Verify notifyScheduleCancelled was called exactly once (only establishment)
        verify(notificationService, times(1)).notifyScheduleCancelled(any(), any(), anyString());
    }

    @Test
    void cancelSchedule_Collector_NotifiesBothCollectorAndEstablishment() {
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollectionPoint(establishmentPoint);
        schedule.setCollector(collectorUser);
        schedule.setScheduledDate(LocalDate.now()); // Today - collector can cancel anytime

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditLogService).logStatusChange(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString());

        scheduleService.cancelSchedule(schedule.getId(), "Imprevisto", collectorUser, "127.0.0.1", "test-agent");

        // Verify notification to collector
        verify(notificationService).notifyScheduleCancelled(collectorUser.getId(), schedule.getId(), "Imprevisto");
        // Verify notification to establishment
        verify(notificationService).notifyScheduleCancelled(establishmentUser.getId(), schedule.getId(), "Imprevisto");
    }

    // ===== TEST FOR ASSIGN SCHEDULE NOTIFIES ESTABLISHMENT =====

    @Test
    void assignSchedule_NotifiesEstablishment() {
        CollectionSchedule schedule = createScheduleEntity();
        schedule.setCollector(null);
        schedule.setStatus(CollectionSchedule.ScheduleStatus.PENDING);

        when(scheduleRepository.findById(schedule.getId())).thenReturn(Optional.of(schedule));
        when(userRepository.findById(collectorUser.getId())).thenReturn(Optional.of(collectorUser));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(auditLogService).logStatusChange(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString());

        scheduleService.assignSchedule(schedule.getId(), collectorUser.getId(), adminUser, "127.0.0.1", "test-agent");

        // Verify notification to collector
        verify(notificationService).notifyScheduleAssigned(collectorUser.getId(), schedule.getId(), collectorUser.getName());
        // Verify notification to establishment
        verify(notificationService).notifyScheduleAssigned(establishmentUser.getId(), schedule.getId(), collectorUser.getName());
    }

    // ===== HELPER METHODS =====

    private CollectionSchedule createScheduleEntity() {
        CollectionSchedule schedule = new CollectionSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setCollectionPoint(establishmentPoint);
        schedule.setCollector(collectorUser);
        schedule.setScheduledDate(LocalDate.now().plusDays(1));
        schedule.setStartTime(LocalTime.of(10, 0));
        schedule.setEndTime(LocalTime.of(12, 0));
        schedule.setStatus(CollectionSchedule.ScheduleStatus.PENDING);
        schedule.setEstimatedVolumeLiters(100);
        schedule.setNotes("Teste");
        schedule.setSaleType(CollectionSchedule.SaleType.SALE);
        schedule.setTimeWindow("MORNING");
        schedule.setBasePricePerLiter(BigDecimal.valueOf(5.0));
        schedule.setCalculatedValue(BigDecimal.valueOf(500.0));
        return schedule;
    }
}