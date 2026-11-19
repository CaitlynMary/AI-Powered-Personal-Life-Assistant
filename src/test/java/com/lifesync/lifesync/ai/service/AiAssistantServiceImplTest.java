package com.lifesync.lifesync.ai.service;

import com.openai.core.http.Headers;
import com.openai.errors.OpenAIServiceException;
import com.lifesync.lifesync.ai.dto.AiPlanDraft;
import com.lifesync.lifesync.ai.dto.AiPlanItemDraft;
import com.lifesync.lifesync.ai.dto.AiChatMessage;
import com.lifesync.lifesync.ai.dto.DailyPlanRequest;
import com.lifesync.lifesync.ai.exception.AiContextException;
import com.lifesync.lifesync.ai.exception.AiProviderException;
import com.lifesync.lifesync.ai.service.impl.AiAssistantServiceImpl;
import com.lifesync.lifesync.calendar.dto.CalendarEventResponse;
import com.lifesync.lifesync.calendar.service.CalendarEventService;
import com.lifesync.lifesync.exception.BadRequestException;
import com.lifesync.lifesync.goal.service.GoalService;
import com.lifesync.lifesync.habit.repository.HabitLogRepository;
import com.lifesync.lifesync.habit.service.HabitService;
import com.lifesync.lifesync.milestone.repository.GoalMilestoneRepository;
import com.lifesync.lifesync.schedule.entity.ScheduleSlot;
import com.lifesync.lifesync.schedule.repository.ScheduleSlotRepository;
import com.lifesync.lifesync.task.entity.Task;
import com.lifesync.lifesync.task.entity.TaskPriority;
import com.lifesync.lifesync.task.entity.TaskStatus;
import com.lifesync.lifesync.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

class AiAssistantServiceImplTest {
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callSpec;
    private TaskRepository taskRepository;
    private GoalService goalService;
    private GoalMilestoneRepository milestoneRepository;
    private HabitService habitService;
    private HabitLogRepository habitLogRepository;
    private CalendarEventService calendarEventService;
    private ScheduleSlotRepository scheduleSlotRepository;
    private AiContextService aiContextService;
    private AiAssistantServiceImpl service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callSpec = mock(ChatClient.CallResponseSpec.class);
        taskRepository = mock(TaskRepository.class);
        goalService = mock(GoalService.class);
        milestoneRepository = mock(GoalMilestoneRepository.class);
        habitService = mock(HabitService.class);
        habitLogRepository = mock(HabitLogRepository.class);
        calendarEventService = mock(CalendarEventService.class);
        scheduleSlotRepository = mock(ScheduleSlotRepository.class);
        aiContextService = mock(AiContextService.class);
        service = new AiAssistantServiceImpl(chatClient, taskRepository, goalService, milestoneRepository,
                habitService, habitLogRepository, calendarEventService, scheduleSlotRepository, aiContextService);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(goalService.getAll(anyLong())).thenReturn(List.of());
        when(habitService.getAll(anyLong())).thenReturn(List.of());
        when(habitLogRepository.findAllByHabit_User_IdAndCompletionDateBetween(anyLong(), any(), any())).thenReturn(List.of());
        when(calendarEventService.day(anyLong(), any())).thenReturn(List.of());
        when(scheduleSlotRepository.findOverlappingRange(anyLong(), any(), any())).thenReturn(List.of());
        when(aiContextService.forQuestion(anyLong(), anyString())).thenReturn("");
    }

    @Test
    void chatReturnsProviderResponseAndRedactsCredentialsBeforeSending() {
        when(callSpec.content()).thenReturn("Here is a plan");
        String response = service.chat(9L, "Help me focus. api_key=AIza123456789012345678901234 and my password is Password123", List.of());
        assertEquals("Here is a plan", response);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(prompt.capture());
        assertTrue(prompt.getValue().contains("api_key=[REDACTED]"));
        assertFalse(prompt.getValue().contains("Password123"));
    }

    @Test
    void emptyMessageIsRejectedWithoutCallingProvider() {
        assertThrows(BadRequestException.class, () -> service.chat(9L, "  ", List.of()));
        verify(chatClient, never()).prompt();
    }

    @Test
    void providerFailureIsReplacedWithSafeException() {
        when(callSpec.content()).thenThrow(new IllegalStateException("provider error with secret"));
        AiProviderException exception = assertThrows(AiProviderException.class, () -> service.chat(9L, "hello", List.of()));
        assertEquals(AiProviderException.FailureType.UNAVAILABLE, exception.getFailureType());
        assertFalse(exception.getMessage().contains("secret"));
    }

    @Test
    void contextRetrievalFailureIsExplicitAndDoesNotCallProvider() {
        when(aiContextService.forQuestion(9L, "What tasks are due?"))
                .thenThrow(new IllegalStateException("database failure"));

        assertThrows(AiContextException.class,
                () -> service.chat(9L, "What tasks are due?", List.of()));

        verifyNoInteractions(chatClient);
    }

    @Test
    void chatIncludesSessionHistoryAndAuthenticatedRelevantContext() {
        when(aiContextService.forQuestion(9L, "What tasks are due?")).thenReturn("TASK RECORDS: report due tomorrow");
        when(callSpec.content()).thenReturn("Your report is due tomorrow.");

        service.chat(9L, "What tasks are due?", List.of(new AiChatMessage(AiChatMessage.Role.USER, "Help me plan.")));

        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).user(prompt.capture());
        assertTrue(prompt.getValue().contains("Help me plan."));
        assertTrue(prompt.getValue().contains("report due tomorrow"));
        verify(aiContextService).forQuestion(9L, "What tasks are due?");
    }

    @Test
    void chatHonorsPersonalizationOptOutAndConciseResponseStyle() {
        when(callSpec.content()).thenReturn("Focus on one small next step.");

        service.chat(9L, "What should I focus on?", List.of(), "CONCISE", false);

        verify(aiContextService, never()).forQuestion(anyLong(), anyString());
        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        verify(requestSpec).system(systemPrompt.capture());
        assertTrue(systemPrompt.getValue().contains("Keep responses concise"));
    }

    @Test
    void preservesAssistantFormattingWhileRemovingCredentials() {
        when(callSpec.content()).thenReturn("First step\n\n- Review the report");

        assertEquals("First step\n\n- Review the report", service.chat(9L, "Help me plan", List.of()));
    }

    @Test
    void classifiesProviderQuotaAndTimeoutFailuresWithoutExposingProviderDetails() {
        when(callSpec.content()).thenThrow(new IllegalStateException("429 RESOURCE_EXHAUSTED: secret quota detail"));
        AiProviderException quota = assertThrows(AiProviderException.class,
                () -> service.chat(9L, "hello", List.of()));
        assertEquals(AiProviderException.FailureType.RATE_LIMIT, quota.getFailureType());
        assertFalse(quota.getMessage().contains("secret quota detail"));

        reset(chatClient, requestSpec, callSpec);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenThrow(new RuntimeException(new SocketTimeoutException("provider timeout")));

        AiProviderException timeout = assertThrows(AiProviderException.class,
                () -> service.chat(9L, "hello", List.of()));
        assertEquals(AiProviderException.FailureType.TIMEOUT, timeout.getFailureType());

        reset(chatClient, requestSpec, callSpec);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenThrow(new RuntimeException(new InterruptedIOException("timeout")));

        AiProviderException callTimeout = assertThrows(AiProviderException.class,
                () -> service.chat(9L, "hello", List.of()));
        assertEquals(AiProviderException.FailureType.TIMEOUT, callTimeout.getFailureType());
    }

    @Test
    void mapsOpenAiCompatibleHttpStatusAndPropagatesRetryAfterWithoutLeakingCredentials() {
        OpenAIServiceException apiException = mock(OpenAIServiceException.class);
        when(apiException.statusCode()).thenReturn(429);
        when(apiException.headers()).thenReturn(Headers.builder().put("Retry-After", "45").build());
        when(apiException.getMessage()).thenReturn(
                "HTTP 429 raw key AIza123456789012345678901234 and api_key=gsk_12345678901234567890123456789012");
        when(callSpec.content()).thenThrow(new RuntimeException("provider call failed", apiException));

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> service.chat(9L, "hello", List.of()));

        assertEquals(AiProviderException.FailureType.RATE_LIMIT, exception.getFailureType());
        assertEquals("45", exception.getRetryAfter());
        assertEquals(429, AiFailureDiagnostics.httpStatus(new RuntimeException(apiException)));
        assertEquals("45", AiFailureDiagnostics.retryAfter(apiException));
        assertFalse(AiFailureDiagnostics.safeMessage(apiException).contains("gsk_"));
        assertFalse(AiFailureDiagnostics.safeMessage(apiException).contains("AIza"));
    }

    @Test
    void classifiesSpringHttpStatusExceptions() {
        assertProviderFailure(HttpStatus.UNAUTHORIZED, AiProviderException.FailureType.CONFIGURATION);
        assertProviderFailure(HttpStatus.FORBIDDEN, AiProviderException.FailureType.CONFIGURATION);
        assertProviderFailure(HttpStatus.REQUEST_TIMEOUT, AiProviderException.FailureType.TIMEOUT);
        assertProviderFailure(HttpStatus.GATEWAY_TIMEOUT, AiProviderException.FailureType.TIMEOUT);
        assertProviderFailure(HttpStatus.TOO_MANY_REQUESTS, AiProviderException.FailureType.RATE_LIMIT);
        assertProviderFailure(HttpStatus.INTERNAL_SERVER_ERROR, AiProviderException.FailureType.UNAVAILABLE);
    }

    @Test
    void structuredProviderPlanOutputParsesTimeFields() {
        AiPlanDraft draft = new BeanOutputConverter<>(AiPlanDraft.class).convert(
                "{\"summary\":\"Start with the report\",\"suggestions\":[{" +
                        "\"taskId\":81,\"startTime\":\"09:00:00\",\"endTime\":\"10:00:00\",\"rationale\":\"High priority\"}]}" );
        assertEquals(LocalTime.of(9, 0), draft.getSuggestions().get(0).getStartTime());
        assertEquals(81L, draft.getSuggestions().get(0).getTaskId());
    }

    @Test
    void dailyPlanReturnsValidatedSuggestionsAndUsesAuthenticatedUsersDataScopes() {
        Task task = task(81L, "Prepare report", TaskPriority.HIGH, 60);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(task)));
        LocalDate planDate = LocalDate.now().plusDays(1);
        when(callSpec.entity(AiPlanDraft.class)).thenReturn(new AiPlanDraft("Focus on the report first.",
                List.of(new AiPlanItemDraft(81L, LocalTime.of(9, 0), LocalTime.of(10, 0), "High priority and due soon."))));

        var response = service.createDailyPlan(9L, new DailyPlanRequest(planDate));

        assertEquals(1, response.getSuggestions().size());
        assertEquals(81L, response.getSuggestions().get(0).getTaskId());
        assertEquals(TaskPriority.HIGH, response.getSuggestions().get(0).getPriority());
        verify(goalService).getAll(9L);
        verify(habitService).getAll(9L);
        verify(habitLogRepository).findAllByHabit_User_IdAndCompletionDateBetween(eq(9L), any(), eq(planDate));
        verify(calendarEventService).day(9L, planDate);
        verify(scheduleSlotRepository).findOverlappingRange(eq(9L), any(), any());
        verify(taskRepository, never()).save(any());
        verify(scheduleSlotRepository, never()).save(any());
    }

    @Test
    void dailyPlanOmitsAiSuggestedTimesThatConflictWithCalendarBusyTime() {
        Task task = task(82L, "Write report", TaskPriority.MEDIUM, 120);
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(task)));
        LocalDate date = LocalDate.now().plusDays(1);
        LocalDateTime slotStart = date.atTime(9, 0);
        CalendarEventResponse event = new CalendarEventResponse(3L, "Meeting", null, null, false,
                slotStart, slotStart.plusHours(1), null, null, null, null, null);
        when(calendarEventService.day(9L, date)).thenReturn(List.of(event));
        when(callSpec.entity(AiPlanDraft.class)).thenReturn(new AiPlanDraft("Plan", List.of(
                new AiPlanItemDraft(82L, LocalTime.of(9, 30), LocalTime.of(10, 30), "Suggested focus time."))));

        var response = service.createDailyPlan(9L, new DailyPlanRequest(date));

        assertTrue(response.getSuggestions().isEmpty());
        assertFalse(response.getWarnings().isEmpty());
        verify(callSpec).entity(AiPlanDraft.class);
    }

    @Test
    void dailyPlanRejectsPastDateAndInvalidWorkingWindow() {
        assertThrows(BadRequestException.class, () -> service.createDailyPlan(9L, new DailyPlanRequest(LocalDate.now().minusDays(1))));
        assertThrows(BadRequestException.class, () -> service.createDailyPlan(9L,
                new DailyPlanRequest(LocalDate.now().plusDays(1), LocalTime.NOON, LocalTime.NOON)));
        verify(taskRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void noPendingTasksReturnsWithoutCallingProvider() {
        when(taskRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        var response = service.createDailyPlan(9L, new DailyPlanRequest(LocalDate.now().plusDays(1)));
        assertTrue(response.getSuggestions().isEmpty());
        verify(chatClient, never()).prompt();
    }

    private Task task(Long id, String title, TaskPriority priority, int minutes) {
        Task task = new Task(); task.setId(id); task.setTitle(title); task.setPriority(priority); task.setStatus(TaskStatus.TODO);
        task.setEstimatedDuration(minutes); task.setDueDate(LocalDateTime.now().plusDays(2)); return task;
    }

    private void assertProviderFailure(HttpStatus status, AiProviderException.FailureType expectedType) {
        reset(chatClient, requestSpec, callSpec);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        RuntimeException responseException = status.is4xxClientError()
                ? new HttpClientErrorException(status)
                : new HttpServerErrorException(status);
        when(callSpec.content()).thenThrow(responseException);

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> service.chat(9L, "hello", List.of()));

        assertEquals(expectedType, exception.getFailureType());
    }
}
