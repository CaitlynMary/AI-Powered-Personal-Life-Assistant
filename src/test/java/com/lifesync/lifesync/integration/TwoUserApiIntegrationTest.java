package com.lifesync.lifesync.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lifesync.lifesync.ai.dto.AiPlanDraft;
import com.lifesync.lifesync.ai.dto.AiPlanItemDraft;
import com.lifesync.lifesync.category.repository.CategoryRepository;
import com.lifesync.lifesync.category.entity.Category;
import com.lifesync.lifesync.reminder.service.ReminderService;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@SpringBootTest(properties = "app.jwt.secret=VGhpcy1pcy1vbmx5LWEtc2FmZS10ZXN0LXNpZ25pbmcta2V5LWZvci1saWZlc3luYw==")
@AutoConfigureMockMvc
@ActiveProfiles(resolver = TestProfileResolver.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TwoUserApiIntegrationTest {
    private static final String ALEX_EMAIL = "alex.johnson.test@example.com";
    private static final String ALEX_PASSWORD = "Alex@LifeSync2026";
    private static final String SARAH_EMAIL = "sarah.williams.test@example.com";
    private static final String SARAH_PASSWORD = "Sarah@LifeSync2026";

    @Autowired private MockMvc mvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private ReminderService reminderService;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoBean private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec aiRequest;
    private ChatClient.CallResponseSpec aiResponse;

    private static String alexToken;
    private static String sarahToken;

    @BeforeAll
    void registerAndAuthenticateTwoIsolatedTestUsers() throws Exception {
        register("Alex Johnson", ALEX_EMAIL, ALEX_PASSWORD, "9876543210");
        register("Sarah Williams", SARAH_EMAIL, SARAH_PASSWORD, "9876543211");
        alexToken = login(ALEX_EMAIL, ALEX_PASSWORD);
        sarahToken = login(SARAH_EMAIL, SARAH_PASSWORD);
    }

    @BeforeEach
    void stubAiProviderWithoutCallingExternalProvider() {
        aiRequest = mock(ChatClient.ChatClientRequestSpec.class);
        aiResponse = mock(ChatClient.CallResponseSpec.class);
        when(chatClient.prompt()).thenReturn(aiRequest);
        when(aiRequest.system(anyString())).thenReturn(aiRequest);
        when(aiRequest.user(anyString())).thenReturn(aiRequest);
        when(aiRequest.call()).thenReturn(aiResponse);
        when(aiResponse.content()).thenReturn("A mocked productivity response");
    }

    @Test
    void registrationPersistsOnlyPasswordHashAndProfileRequiresAuthentication() throws Exception {
        assertEquals(2, userRepository.count());
        assertNotEquals(ALEX_PASSWORD, userRepository.findByEmail(ALEX_EMAIL).orElseThrow().getPassword());
        assertNotEquals(SARAH_PASSWORD, userRepository.findByEmail(SARAH_EMAIL).orElseThrow().getPassword());
        mvc.perform(get("/api/users/me").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(ALEX_EMAIL))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/ai/daily-plan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-11-11\",\"dayStartTime\":\"09:00:00\",\"dayEndTime\":\"17:00:00\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/ai/daily-briefing"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/ai/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hello\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/ai/daily-plan").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"2026-11-11\",\"dayStartTime\":\"09:00:00\",\"dayEndTime\":\"17:00:00\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void authenticatedProfileUpdatePasswordChangeAndDeactivateEndpointsWork() throws Exception {
        mvc.perform(put("/api/users/me").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Alex Johnson Updated\",\"phoneNumber\":\"9876543210\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Alex Johnson Updated"));
        mvc.perform(patch("/api/users/me/password").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Alex@LifeSync2026\",\"newPassword\":\"Alex@LifeSync2027\",\"confirmPassword\":\"Alex@LifeSync2027\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(patch("/api/users/me/deactivate").header("Authorization", bearer(alexToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void refreshRotatesAndLogoutRevokesRefreshToken() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ALEX_EMAIL + "\",\"password\":\"Alex@LifeSync2026\"}"))
                .andExpect(status().isOk()).andReturn();
        String initialRefresh = objectMapper.readTree(login.getResponse().getContentAsString()).get("refreshToken").asText();
        MvcResult rotated = mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + initialRefresh + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty()).andReturn();
        String replacement = objectMapper.readTree(rotated.getResponse().getContentAsString()).get("refreshToken").asText();
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + initialRefresh + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + replacement + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + replacement + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void categoriesAndTasksPersistAndRemainIsolatedBetweenUsers() throws Exception {
        String alexCategoryId = createCategory(alexToken, "Alex Work");
        String sarahCategoryId = createCategory(sarahToken, "Sarah Work");
        assertEquals(2, categoryRepository.count());
        mvc.perform(get("/api/categories/{id}", alexCategoryId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Alex Work"));
        mvc.perform(get("/api/categories").header("Authorization", bearer(sarahToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Sarah Work"));
        mvc.perform(get("/api/categories/{id}", alexCategoryId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/categories/{id}", sarahCategoryId).header("Authorization", bearer(sarahToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Sarah Personal\",\"description\":\"updated\",\"color\":\"#AABBCC\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Sarah Personal"));
        mvc.perform(delete("/api/categories/{id}", sarahCategoryId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/categories/{id}", sarahCategoryId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNoContent());
        MvcResult sameNamedCategory = mvc.perform(post("/api/categories").header("Authorization", bearer(sarahToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alex Work\",\"description\":\"same name, different owner\",\"color\":\"#123456\"}"))
                .andExpect(status().isCreated()).andReturn();
        mvc.perform(delete("/api/categories/{id}", id(sameNamedCategory)).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNoContent());

        MvcResult created = mvc.perform(post("/api/tasks").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Alex private task","description":"Integration check","categoryId":%s,"priority":"HIGH","recurring":false,"recurrenceType":"NONE"}
                                """.formatted(alexCategoryId)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Alex private task"))
                .andReturn();
        JsonNode task = objectMapper.readTree(created.getResponse().getContentAsString());
        String taskId = task.get("id").asText();
        assertEquals(Long.parseLong(alexCategoryId), task.get("categoryId").asLong());
        assertEquals("Alex private task", taskRepository.findById(Long.valueOf(taskId)).orElseThrow().getTitle());

        mvc.perform(get("/api/tasks/{id}", taskId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Alex private task"));
        mvc.perform(put("/api/tasks/{id}", taskId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Alex private task updated\",\"description\":\"Updated\",\"categoryId\":" + alexCategoryId + ",\"priority\":\"HIGH\",\"recurring\":false,\"recurrenceType\":\"NONE\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Alex private task updated"));
        mvc.perform(get("/api/tasks/{id}", taskId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/tasks/{id}/status", taskId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mvc.perform(patch("/api/tasks/{id}/complete", taskId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/tasks?status=COMPLETED&priority=HIGH", taskId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/tasks/statistics").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completedTasks").value(1));
        mvc.perform(post("/api/tasks").header("Authorization", bearer(sarahToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Wrong owner category link\",\"priority\":\"LOW\",\"categoryId\":" + alexCategoryId + ",\"recurring\":false,\"recurrenceType\":\"NONE\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/tasks/{id}", taskId).header("Authorization", bearer(sarahToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Hijacked","description":"No","categoryId":%s,"priority":"LOW","recurring":false,"recurrenceType":"NONE"}
                                """.formatted(sarahCategoryId)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/tasks/{id}", taskId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());
        assertEquals("Alex private task updated", taskRepository.findById(Long.valueOf(taskId)).orElseThrow().getTitle());

        mvc.perform(get("/api/tasks").header("Authorization", bearer(sarahToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(0));
        mvc.perform(delete("/api/tasks/{id}", taskId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    void deletingTaskPreservesLinkedReminderAndClearsItsTaskReference() throws Exception {
        java.time.LocalDateTime deadline = java.time.LocalDateTime.now().plusDays(3).withNano(0);
        MvcResult taskResult = mvc.perform(post("/api/tasks").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Reminder-linked task\",\"priority\":\"MEDIUM\",\"dueDate\":\""
                                + deadline + "\",\"recurring\":false,\"recurrenceType\":\"NONE\"}"))
                .andExpect(status().isCreated()).andReturn();
        String taskId = id(taskResult);
        String reminderDateTime = deadline.minusDays(1).toString();
        MvcResult reminderResult = mvc.perform(post("/api/reminders").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Keep this reminder\",\"message\":\"Preserve this user data\",\"reminderDateTime\":\""
                                + reminderDateTime + "\",\"reminderType\":\"GENERAL\",\"taskId\":" + taskId + "}"))
                .andExpect(status().isCreated()).andReturn();
        String reminderId = id(reminderResult);
        mvc.perform(patch("/api/tasks/{id}/complete", taskId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        Long notificationId = jdbcTemplate.queryForObject(
                "select id from notifications where task_id = ?", Long.class, Long.valueOf(taskId));

        mvc.perform(delete("/api/tasks/{id}", taskId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isNoContent());

        assertFalse(taskRepository.findById(Long.valueOf(taskId)).isPresent());
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from reminders where id = ?", Integer.class, Long.valueOf(reminderId)));
        assertNull(jdbcTemplate.queryForObject(
                "select task_id from reminders where id = ?", Long.class, Long.valueOf(reminderId)));
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from notifications where id = ?", Integer.class, notificationId));
        assertNull(jdbcTemplate.queryForObject(
                "select task_id from notifications where id = ?", Long.class, notificationId));
    }

    @Test
    @Transactional
    void deletingTaskLinkedToPlannerScheduleReturnsMeaningfulConflictAndPreservesBothRecords() throws Exception {
        MvcResult taskResult = mvc.perform(post("/api/tasks").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Scheduled task\",\"priority\":\"MEDIUM\",\"recurring\":false,\"recurrenceType\":\"NONE\"}"))
                .andExpect(status().isCreated()).andReturn();
        String taskId = id(taskResult);
        java.time.LocalDateTime start = java.time.LocalDateTime.now().plusDays(90).withNano(0);
        MvcResult slotResult = mvc.perform(post("/api/schedule").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Preserve this schedule\",\"startDateTime\":\"" + start
                                + "\",\"endDateTime\":\"" + start.plusHours(1) + "\",\"taskId\":" + taskId + "}"))
                .andExpect(status().isCreated()).andReturn();
        String slotId = id(slotResult);

        mvc.perform(delete("/api/tasks/{id}", taskId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "This task is linked to a planner schedule. Remove or unlink the schedule entry before deleting the task."));

        assertTrue(taskRepository.findById(Long.valueOf(taskId)).isPresent());
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from schedule_slots where id = ?", Integer.class, Long.valueOf(slotId)));
    }

    @Test
    @Transactional
    void deletingTaskLinkedToCalendarEventReturnsMeaningfulConflictAndPreservesBothRecords() throws Exception {
        MvcResult taskResult = mvc.perform(post("/api/tasks").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Calendar-linked task\",\"priority\":\"MEDIUM\",\"recurring\":false,\"recurrenceType\":\"NONE\"}"))
                .andExpect(status().isCreated()).andReturn();
        String taskId = id(taskResult);
        java.time.LocalDateTime start = java.time.LocalDateTime.now().plusDays(120).withNano(0);
        MvcResult eventResult = mvc.perform(post("/api/calendar/events").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Preserve this event\",\"eventType\":\"MEETING\",\"allDay\":false,\"startDateTime\":\""
                                + start + "\",\"endDateTime\":\"" + start.plusHours(1)
                                + "\",\"recurrenceType\":\"NONE\",\"taskId\":" + taskId + "}"))
                .andExpect(status().isCreated()).andReturn();
        String eventId = id(eventResult);

        mvc.perform(delete("/api/tasks/{id}", taskId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "This task is linked to a calendar event. Remove or unlink the event before deleting the task."));

        assertTrue(taskRepository.findById(Long.valueOf(taskId)).isPresent());
        assertEquals(1, jdbcTemplate.queryForObject(
                "select count(*) from calendar_events where id = ?", Integer.class, Long.valueOf(eventId)));
    }

    @Test
    void rejectsInvalidRegistrationAndLoginCredentials() throws Exception {
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Bad\",\"email\":\"not-an-email\",\"password\":\"short\",\"phoneNumber\":\"123\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.com\",\"password\":\"SomePassword123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Transactional
    void taskSearchMatchesTitleAndDescriptionIgnoringCaseAndWhitespace() throws Exception {
        MvcResult task = mvc.perform(post("/api/tasks").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Learn Coding SearchNeedleQz\",\"description\":\"Findable DescriptionNeedleQz\",\"priority\":\"LOW\",\"recurring\":false,\"recurrenceType\":\"NONE\"}"))
                .andExpect(status().isCreated()).andReturn();
        String taskId = id(task);

        for (String search : List.of(
                "Learn Coding SearchNeedleQz",
                "learn coding searchneedleqz",
                "LEARN CODING SEARCHNEEDLEQZ",
                "Learn",
                "COD",
                "NeedleQz",
                "  Learn Coding SearchNeedleQz  ",
                "DESCRIPTIONNEEDLEQZ")) {
            assertTaskSearchContains(search, taskId, alexToken);
        }

        mvc.perform(get("/api/tasks").header("Authorization", bearer(alexToken))
                        .param("page", "0").param("size", "100").param("search", "NoSuchSearchNeedleQz"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/tasks").header("Authorization", bearer(sarahToken))
                        .param("page", "0").param("size", "100").param("search", "SearchNeedleQz"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        assertTaskSearchContains("  ", taskId, alexToken);
    }

    @Test
    @Transactional
    void noteSearchMatchesTitleAndContentIgnoringCaseAndWhitespace() throws Exception {
        MvcResult note = mvc.perform(post("/api/notes").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Study Angular SearchNoteQz\",\"content\":\"A thoughtful reflection about component testing\",\"noteType\":\"GENERAL\"}"))
                .andExpect(status().isCreated()).andReturn();
        String noteId = id(note);

        for (String search : List.of(
                "Study Angular SearchNoteQz",
                "study angular searchnoteqz",
                "STUDY ANGULAR SEARCHNOTEQZ",
                "Study",
                "ANG",
                "NoteQz",
                "  Study Angular SearchNoteQz  ",
                "COMPONENT TEST")) {
            assertNoteSearchContains(search, noteId, alexToken);
        }

        mvc.perform(get("/api/notes").header("Authorization", bearer(alexToken))
                        .param("page", "0").param("size", "100").param("search", "NoSuchSearchNoteQz"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/notes").header("Authorization", bearer(sarahToken))
                        .param("page", "0").param("size", "100").param("search", "SearchNoteQz"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        assertNoteSearchContains("  ", noteId, alexToken);
    }

    @Test
    void goalsMilestonesAndTaskAssociationsPersistWithOwnerIsolation() throws Exception {
        MvcResult goalResult = mvc.perform(post("/api/goals").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Learn PostgreSQL\",\"description\":\"Practice relational modeling\",\"goalType\":\"YEARLY\",\"startDate\":\"2026-11-10\",\"targetDate\":\"2027-11-10\"}"))
                .andExpect(status().isCreated()).andReturn();
        String goalId = id(goalResult);
        mvc.perform(get("/api/goals").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(Long.valueOf(goalId)));
        mvc.perform(get("/api/goals/{id}", goalId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/goals/{id}", goalId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());

        MvcResult milestoneResult = mvc.perform(post("/api/goals/{goalId}/milestones", goalId)
                        .header("Authorization", bearer(alexToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Build schema\",\"description\":\"First checkpoint\",\"targetDate\":\"2026-12-10\"}"))
                .andExpect(status().isCreated()).andReturn();
        String milestoneId = id(milestoneResult);
        mvc.perform(get("/api/goals/{goalId}/milestones", goalId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(Long.valueOf(milestoneId)));
        mvc.perform(get("/api/milestones/{id}", milestoneId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/milestones/{id}", milestoneId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        mvc.perform(put("/api/milestones/{id}", milestoneId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Schema checkpoint\",\"description\":\"Updated\",\"targetDate\":\"2026-12-11\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Schema checkpoint"));
        mvc.perform(patch("/api/milestones/{id}/complete", milestoneId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());

        MvcResult taskResult = mvc.perform(post("/api/tasks").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Implement schema\",\"priority\":\"HIGH\",\"goalId\":" + goalId + ",\"milestoneId\":" + milestoneId + ",\"recurring\":false,\"recurrenceType\":\"NONE\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.goalId").value(Long.valueOf(goalId)))
                .andExpect(jsonPath("$.milestoneId").value(Long.valueOf(milestoneId))).andReturn();
        String taskId = id(taskResult);
        mvc.perform(get("/api/milestones/{id}", milestoneId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.completedAt").value(org.hamcrest.Matchers.nullValue()));
        MvcResult secondTaskResult = mvc.perform(post("/api/tasks").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Review schema\",\"priority\":\"HIGH\",\"goalId\":" + goalId + ",\"milestoneId\":" + milestoneId + ",\"recurring\":false,\"recurrenceType\":\"NONE\"}"))
                .andExpect(status().isCreated()).andReturn();
        String secondTaskId = id(secondTaskResult);
        mvc.perform(patch("/api/tasks/{id}/complete", taskId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/milestones/{id}", milestoneId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.completedAt").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(patch("/api/tasks/{id}/status", secondTaskId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"MISSED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("MISSED"));
        mvc.perform(get("/api/milestones/{id}", milestoneId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.completedAt").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(patch("/api/tasks/{id}/status", secondTaskId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"TODO\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("TODO"));
        mvc.perform(patch("/api/tasks/{id}/complete", secondTaskId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));
        mvc.perform(get("/api/milestones/{id}", milestoneId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());
        mvc.perform(get("/api/goals/{id}", goalId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressPercentage").value(100));
        mvc.perform(put("/api/goals/{id}", goalId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Learn relational databases\",\"description\":\"Updated goal\",\"goalType\":\"YEARLY\",\"startDate\":\"2026-11-10\",\"targetDate\":\"2027-11-10\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/tasks/{id}", taskId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/goals/{id}/status", goalId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/goals/statistics").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/milestones/{id}", milestoneId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/tasks/{id}", taskId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/tasks/{id}", secondTaskId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/goals/{id}", goalId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    void completedGoalCanBeReversedAndCompletedMilestonesAreReset() throws Exception {
        MvcResult goalResult = mvc.perform(post("/api/goals").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Reversible goal\",\"description\":\"Goal completion test\",\"goalType\":\"YEARLY\",\"startDate\":\"2026-11-10\",\"targetDate\":\"2027-11-10\"}"))
                .andExpect(status().isCreated()).andReturn();
        String goalId = id(goalResult);
        MvcResult firstMilestone = mvc.perform(post("/api/goals/{goalId}/milestones", goalId)
                        .header("Authorization", bearer(alexToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"First step\",\"targetDate\":\"2026-12-10\"}"))
                .andExpect(status().isCreated()).andReturn();
        MvcResult secondMilestone = mvc.perform(post("/api/goals/{goalId}/milestones", goalId)
                        .header("Authorization", bearer(alexToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Second step\",\"targetDate\":\"2027-01-10\"}"))
                .andExpect(status().isCreated()).andReturn();
        String firstMilestoneId = id(firstMilestone);
        String secondMilestoneId = id(secondMilestone);

        mvc.perform(patch("/api/goals/{id}/status", goalId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/goals/{id}", goalId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.progressPercentage").value(0));
        mvc.perform(get("/api/goals/{goalId}/milestones", goalId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("NOT_STARTED"))
                .andExpect(jsonPath("$[1].status").value("NOT_STARTED"));

        mvc.perform(patch("/api/milestones/{id}/complete", firstMilestoneId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/milestones/{id}/complete", secondMilestoneId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/goals/{id}", goalId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressPercentage").value(100));

        mvc.perform(patch("/api/goals/{id}/status", goalId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"NOT_STARTED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.progressPercentage").value(0));
        mvc.perform(get("/api/goals/{id}", goalId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.progressPercentage").value(0));
        mvc.perform(get("/api/goals/{goalId}/milestones", goalId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("NOT_STARTED"))
                .andExpect(jsonPath("$[0].completedAt").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[1].status").value("NOT_STARTED"))
                .andExpect(jsonPath("$[1].completedAt").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(get("/api/goals/{id}", goalId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void milestoneStatusCanBeUncheckedAndRecheckedWithPersistedGoalProgress() throws Exception {
        MvcResult goalResult = mvc.perform(post("/api/goals").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Milestone toggle goal\",\"description\":\"Toggle test\",\"goalType\":\"YEARLY\",\"startDate\":\"2026-11-10\",\"targetDate\":\"2027-11-10\"}"))
                .andExpect(status().isCreated()).andReturn();
        String goalId = id(goalResult);
        MvcResult firstResult = mvc.perform(post("/api/goals/{goalId}/milestones", goalId)
                        .header("Authorization", bearer(alexToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Toggle first step\",\"targetDate\":\"2026-12-10\"}"))
                .andExpect(status().isCreated()).andReturn();
        MvcResult secondResult = mvc.perform(post("/api/goals/{goalId}/milestones", goalId)
                        .header("Authorization", bearer(alexToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Toggle second step\",\"targetDate\":\"2027-01-10\"}"))
                .andExpect(status().isCreated()).andReturn();
        String firstId = id(firstResult);
        String secondId = id(secondResult);

        mvc.perform(patch("/api/milestones/{id}/status", firstId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/milestones/{id}/status", secondId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/goals/{id}", goalId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressPercentage").value(100));

        mvc.perform(patch("/api/milestones/{id}/status", firstId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"NOT_STARTED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NOT_STARTED"))
                .andExpect(jsonPath("$.completedAt").value(org.hamcrest.Matchers.nullValue()));
        mvc.perform(get("/api/goals/{id}", goalId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.progressPercentage").value(50));
        mvc.perform(get("/api/goals/{goalId}/milestones", goalId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("NOT_STARTED"))
                .andExpect(jsonPath("$[1].status").value("COMPLETED"));

        mvc.perform(patch("/api/milestones/{id}/status", firstId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/goals/{id}", goalId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.progressPercentage").value(100));
        mvc.perform(patch("/api/milestones/{id}/status", firstId).header("Authorization", bearer(sarahToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"NOT_STARTED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void habitsAndJournalsPersistWithDuplicateAndCrossUserChecks() throws Exception {
        MvcResult habitResult = mvc.perform(post("/api/habits").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Morning walk\",\"description\":\"Walk outside\",\"frequency\":\"DAILY\",\"targetDaysPerWeek\":7,\"startDate\":\"2026-11-10\"}"))
                .andExpect(status().isCreated()).andReturn();
        String habitId = id(habitResult);
        mvc.perform(get("/api/habits").header("Authorization", bearer(alexToken))).andExpect(status().isOk());
        mvc.perform(get("/api/habits/{id}", habitId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/habits/{id}", habitId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/habits/{id}", habitId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Daily morning walk\",\"frequency\":\"DAILY\",\"targetDaysPerWeek\":7,\"startDate\":\"2026-11-10\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/habits/{id}/logs", habitId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completionDate\":\"2026-11-10\",\"completed\":true,\"notes\":\"Done\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/habits/{id}/logs", habitId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completionDate\":\"2026-11-10\",\"completed\":true}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/habits/{id}/logs", habitId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].completed").value(true));
        mvc.perform(get("/api/habits/{id}/logs", habitId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/habits/{id}/complete", habitId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/habits/{id}/streak", habitId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/habits/{id}/calendar?year=2026&month=11", habitId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.days.length()").value(30));
        mvc.perform(get("/api/habits/statistics").header("Authorization", bearer(alexToken))).andExpect(status().isOk());
        mvc.perform(patch("/api/habits/{id}/deactivate", habitId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
        mvc.perform(delete("/api/habits/{id}", habitId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isNoContent());

        MvcResult journalResult = mvc.perform(post("/api/notes").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Daily reflection\",\"content\":\"A productive day\",\"noteType\":\"JOURNAL\",\"journalDate\":\"2026-11-10\"}"))
                .andExpect(status().isCreated()).andReturn();
        String noteId = id(journalResult);
        mvc.perform(get("/api/notes/journal?date=2026-11-10").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(Long.valueOf(noteId)));
        mvc.perform(get("/api/notes/{id}", noteId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/notes").header("Authorization", bearer(alexToken)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Duplicate\",\"content\":\"Same date\",\"noteType\":\"JOURNAL\",\"journalDate\":\"2026-11-10\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/notes/{id}", noteId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());
        mvc.perform(put("/api/notes/{id}", noteId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Reflection updated\",\"content\":\"Updated text\",\"noteType\":\"JOURNAL\",\"journalDate\":\"2026-11-10\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/notes/{id}/pin", noteId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pinned").value(true));
        mvc.perform(get("/api/notes?page=0&size=10&search=Reflection", noteId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/notes/statistics").header("Authorization", bearer(alexToken))).andExpect(status().isOk());
        mvc.perform(delete("/api/notes/{id}", noteId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    @Transactional
    void habitCompletionCanBeUndoneAndDateEligibilityIsEnforced() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(8);
        LocalDate pastDate = today.minusDays(1);
        LocalDate outsideDate = start.minusDays(1);
        LocalDate futureDate = today.plusDays(1);
        long baseline = objectMapper.readTree(mvc.perform(get("/api/habits/statistics")
                        .header("Authorization", bearer(alexToken)))
                .andReturn().getResponse().getContentAsString()).path("totalCompletedDays").asLong();

        MvcResult habitResult = mvc.perform(post("/api/habits").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Reversible walk\",\"frequency\":\"DAILY\",\"targetDaysPerWeek\":7,\"startDate\":\""
                                + start + "\",\"endDate\":\"" + today + "\"}"))
                .andExpect(status().isCreated()).andReturn();
        String habitId = id(habitResult);

        mvc.perform(patch("/api/habits/{id}/complete", habitId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completionDate\":\"" + pastDate + "\",\"completed\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completed").value(true));
        MvcResult completedLogs = mvc.perform(get("/api/habits/{id}/logs", habitId)
                        .header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andReturn();
        String logId = objectMapper.readTree(completedLogs.getResponse().getContentAsString()).get(0).path("id").asText();
        assertEquals(baseline + 1, objectMapper.readTree(mvc.perform(get("/api/habits/statistics")
                        .header("Authorization", bearer(alexToken)))
                .andReturn().getResponse().getContentAsString()).path("totalCompletedDays").asLong());
        mvc.perform(get("/api/habits/{id}/streak", habitId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCompletedDays").value(1));
        JsonNode completedCalendar = objectMapper.readTree(mvc.perform(get("/api/habits/{id}/calendar", habitId)
                        .param("year", String.valueOf(pastDate.getYear()))
                        .param("month", String.valueOf(pastDate.getMonthValue()))
                        .header("Authorization", bearer(alexToken)))
                .andReturn().getResponse().getContentAsString());
        assertTrue(completedCalendar.path("days").get(pastDate.getDayOfMonth() - 1).path("completed").asBoolean());

        mvc.perform(patch("/api/habits/{id}/complete", habitId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completionDate\":\"" + pastDate + "\",\"completed\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(Long.valueOf(logId)))
                .andExpect(jsonPath("$.completed").value(false));
        MvcResult undoneLogs = mvc.perform(get("/api/habits/{id}/logs", habitId)
                        .header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].completed").value(false)).andReturn();
        assertEquals(logId, objectMapper.readTree(undoneLogs.getResponse().getContentAsString()).get(0).path("id").asText());
        assertEquals(baseline, objectMapper.readTree(mvc.perform(get("/api/habits/statistics")
                        .header("Authorization", bearer(alexToken)))
                .andReturn().getResponse().getContentAsString()).path("totalCompletedDays").asLong());
        mvc.perform(get("/api/habits/{id}/streak", habitId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCompletedDays").value(0));
        JsonNode undoneCalendar = objectMapper.readTree(mvc.perform(get("/api/habits/{id}/calendar", habitId)
                        .param("year", String.valueOf(pastDate.getYear()))
                        .param("month", String.valueOf(pastDate.getMonthValue()))
                        .header("Authorization", bearer(alexToken)))
                .andReturn().getResponse().getContentAsString());
        assertFalse(undoneCalendar.path("days").get(pastDate.getDayOfMonth() - 1).path("completed").asBoolean());

        mvc.perform(patch("/api/habits/{id}/complete", habitId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completionDate\":\"" + pastDate + "\",\"completed\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(Long.valueOf(logId)));
        mvc.perform(patch("/api/habits/{id}/complete", habitId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completionDate\":\"" + futureDate + "\",\"completed\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/habits/{id}/complete", habitId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completionDate\":\"" + outsideDate + "\",\"completed\":true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/habits/{id}/logs", habitId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"completionDate\":\"" + pastDate + "\",\"completed\":true}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/habits/{id}/logs", habitId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void calendarAndScheduleOperationsHandleOverlapAndOwnership() throws Exception {
        MvcResult eventResult = mvc.perform(post("/api/calendar/events").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Planning meeting\",\"description\":\"Calendar conflict fixture\",\"eventType\":\"MEETING\",\"allDay\":false,\"startDateTime\":\"2026-11-20T09:00:00\",\"endDateTime\":\"2026-11-20T10:00:00\",\"recurrenceType\":\"NONE\"}"))
                .andExpect(status().isCreated()).andReturn();
        String eventId = id(eventResult);
        mvc.perform(get("/api/calendar/events").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].startDateTime").value("2026-11-20T09:00:00"));
        mvc.perform(get("/api/calendar/events/day?date=2026-11-20").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(Long.valueOf(eventId)));
        mvc.perform(get("/api/calendar/events/{id}", eventId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/calendar/events/week?startDate=2026-11-16").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/calendar/events/month?year=2026&month=11").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/calendar/events/{id}", eventId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());

        String conflict = "{\"startDateTime\":\"2026-11-20T09:30:00\",\"endDateTime\":\"2026-11-20T10:30:00\"}";
        mvc.perform(post("/api/schedule/conflicts").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON).content(conflict))
                .andExpect(status().isOk()).andExpect(jsonPath("$.hasConflicts").value(true))
                .andExpect(jsonPath("$.conflicts[0].type").value("CALENDAR_EVENT"));
        mvc.perform(post("/api/schedule").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Overlapping slot\",\"startDateTime\":\"2026-11-20T09:30:00\",\"endDateTime\":\"2026-11-20T10:30:00\"}"))
                .andExpect(status().isConflict());

        MvcResult slotResult = mvc.perform(post("/api/schedule").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Adjacent slot\",\"startDateTime\":\"2026-11-20T10:00:00\",\"endDateTime\":\"2026-11-20T11:00:00\"}"))
                .andExpect(status().isCreated()).andReturn();
        String slotId = id(slotResult);
        mvc.perform(get("/api/schedule/daily?date=2026-11-20").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].startDateTime").value("2026-11-20T10:00:00"));
        mvc.perform(get("/api/schedule?page=0&size=10").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/schedule/weekly?startDate=2026-11-16").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/schedule/{id}", slotId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/schedule/{id}", slotId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk());
        mvc.perform(put("/api/schedule/{id}", slotId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated slot\",\"startDateTime\":\"2026-11-20T11:00:00\",\"endDateTime\":\"2026-11-20T12:00:00\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/schedule/{id}", slotId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isNoContent());
        mvc.perform(put("/api/calendar/events/{id}", eventId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated meeting\",\"eventType\":\"MEETING\",\"allDay\":false,\"startDateTime\":\"2026-11-20T09:00:00\",\"endDateTime\":\"2026-11-20T10:00:00\",\"recurrenceType\":\"NONE\"}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/calendar/events/{id}", eventId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void reminderProcessingCreatesOwnedNotificationsAndSupportsReadOperations() throws Exception {
        MvcResult reminderResult = mvc.perform(post("/api/reminders").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Past due audit reminder\",\"message\":\"Verify scheduled delivery\",\"reminderDateTime\":\"2026-11-09T09:00:00\",\"reminderType\":\"GENERAL\"}"))
                .andExpect(status().isCreated()).andReturn();
        String reminderId = id(reminderResult);
        mvc.perform(get("/api/reminders").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/reminders/upcoming").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/reminders/{id}", reminderId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());
        assertEquals(1, reminderService.processDueReminders());
        mvc.perform(get("/api/reminders/{id}", reminderId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SENT"));
        MvcResult notifications = mvc.perform(get("/api/notifications").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").isNumber()).andReturn();
        JsonNode notificationContent = objectMapper.readTree(notifications.getResponse().getContentAsString()).get("content");
        String notificationId = null;
        for (JsonNode notification : notificationContent) {
            if ("Reminder: Past due audit reminder".equals(notification.path("title").asText())) {
                notificationId = notification.path("id").asText();
                break;
            }
        }
        assertNotNull(notificationId, "The due reminder should have a corresponding notification");
        mvc.perform(patch("/api/notifications/{id}/read", notificationId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/notifications/{id}/read", notificationId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isNoContent());
        mvc.perform(patch("/api/notifications/read-all").header("Authorization", bearer(alexToken)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.unreadCount").value(0));
        assertEquals(0, reminderService.processDueReminders());

        MvcResult futureReminder = mvc.perform(post("/api/reminders").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Future reminder\",\"message\":\"Can be updated\",\"reminderDateTime\":\"2026-11-20T09:00:00\",\"reminderType\":\"GENERAL\"}"))
                .andExpect(status().isCreated()).andReturn();
        String futureId = id(futureReminder);
        mvc.perform(get("/api/reminders/upcoming").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(put("/api/reminders/{id}", futureId).header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated future reminder\",\"message\":\"Updated\",\"reminderDateTime\":\"2026-11-21T09:00:00\",\"reminderType\":\"GENERAL\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated future reminder"));
        mvc.perform(delete("/api/reminders/{id}", futureId).header("Authorization", bearer(sarahToken)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/reminders/{id}", futureId).header("Authorization", bearer(alexToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void databaseEnforcesForeignKeysAndRollsBackPartialWrites() {
        Long alexId = userRepository.findByEmail(ALEX_EMAIL).orElseThrow().getId();
        long before = categoryRepository.count();
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactionManager).execute(status -> {
            Category category = new Category();
            category.setUser(userRepository.findById(alexId).orElseThrow());
            category.setName("Rollback-only category");
            category.setDescription("Must not persist");
            categoryRepository.saveAndFlush(category);
            throw new IllegalStateException("force rollback after insert");
        }));
        assertEquals(before, categoryRepository.count());
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "insert into categories (name, name_key, user_id, created_at, updated_at) values (?, ?, ?, ?, ?)",
                "Orphan row", "orphan row", Long.MAX_VALUE, java.time.LocalDateTime.now(), java.time.LocalDateTime.now()));
    }

    @Test
    void aiEndpointsRequireJwtAndDailyPlanUsesOnlyAuthenticatedUsersTasks() throws Exception {
        mvc.perform(post("/api/ai/chat").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"Help me plan\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("A mocked productivity response"));
        mvc.perform(post("/api/ai/chat").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest());

        MvcResult taskResult = mvc.perform(post("/api/tasks").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Alex only planning task\",\"priority\":\"HIGH\",\"recurring\":false,\"recurrenceType\":\"NONE\"}"))
                .andExpect(status().isCreated()).andReturn();
        Long taskId = Long.valueOf(id(taskResult));
        mvc.perform(get("/api/ai/daily-briefing").header("Authorization", bearer(alexToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingTasks").value(1))
                .andExpect(jsonPath("$.upcomingDeadlines").isArray())
                .andExpect(jsonPath("$.eventsToday").isArray());
        mvc.perform(get("/api/ai/daily-briefing").header("Authorization", bearer(sarahToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingTasks").value(0));
        when(aiResponse.entity(AiPlanDraft.class)).thenReturn(new AiPlanDraft("Focus on the task", List.of(
                new AiPlanItemDraft(taskId, LocalTime.of(9, 0), LocalTime.of(10, 0), "High priority"))));
        String planRequest = "{\"date\":\"" + LocalDate.now().plusDays(1) + "\",\"dayStartTime\":\"08:00:00\",\"dayEndTime\":\"18:00:00\"}";
        mvc.perform(post("/api/ai/daily-plan").header("Authorization", bearer(alexToken))
                        .contentType(MediaType.APPLICATION_JSON).content(planRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.suggestions.length()").value(1))
                .andExpect(jsonPath("$.data.suggestions[0].taskId").value(taskId));
        mvc.perform(post("/api/ai/daily-plan").header("Authorization", bearer(sarahToken))
                        .contentType(MediaType.APPLICATION_JSON).content(planRequest))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.suggestions.length()").value(0));
        assertEquals("TODO", taskRepository.findById(taskId).orElseThrow().getStatus().name());
        verify(aiRequest, atLeastOnce()).user(anyString());
    }

    private void register(String name, String email, String password, String phone) throws Exception {
        mvc.perform(post("/api/users/register").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Registration(name, email, password, phone))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String createCategory(String token, String name) throws Exception {
        MvcResult result = mvc.perform(post("/api/categories").header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"description\":\"test\",\"color\":\"#ABCDEF\"}".formatted(name)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String bearer(String token) { return "Bearer " + token; }

    private String id(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private void assertTaskSearchContains(String search, String taskId, String token) throws Exception {
        MvcResult result = mvc.perform(get("/api/tasks").header("Authorization", bearer(token))
                        .param("page", "0").param("size", "100").param("search", search))
                .andExpect(status().isOk()).andReturn();
        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).path("content");
        boolean found = false;
        for (JsonNode item : content) {
            if (taskId.equals(item.path("id").asText())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected task search to include task " + taskId + " for query: " + search);
    }

    private void assertNoteSearchContains(String search, String noteId, String token) throws Exception {
        MvcResult result = mvc.perform(get("/api/notes").header("Authorization", bearer(token))
                        .param("page", "0").param("size", "100").param("search", search))
                .andExpect(status().isOk()).andReturn();
        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).path("content");
        boolean found = false;
        for (JsonNode item : content) {
            if (noteId.equals(item.path("id").asText())) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected note search to include note " + noteId + " for query: " + search);
    }

    private static final class Registration {
        public String fullName;
        public String email;
        public String password;
        public String phoneNumber;
        private Registration(String fullName, String email, String password, String phoneNumber) {
            this.fullName = fullName; this.email = email; this.password = password; this.phoneNumber = phoneNumber;
        }
    }
}
