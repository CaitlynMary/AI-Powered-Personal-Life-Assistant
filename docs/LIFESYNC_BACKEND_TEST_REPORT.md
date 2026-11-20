# LifeSync Backend Audit and Test Report

> Static audit and test-evidence snapshot; this document is not regenerated automatically when tests run.

Audit date: 2026-11-10  
Project root package: `com.lifesync.lifesync`  
Runtime under test: Java 21, Spring Boot 4.1.2

## Executive result

**READY WITH MINOR ISSUES** for Angular integration based on the local test evidence. The full default Maven suite passed (116 tests), all 82 discovered controller mappings were exercised through MockMvc, and the expanded 11-test two-user workflow passed on both isolated H2 and isolated PostgreSQL 18. Gemini was mocked and the external provider was not called. Selected validation, persistence, security isolation, relationship, rollback and PostgreSQL constraint paths were verified. This is not exhaustive negative-case or production certification.

## A. Codebase inspection

Inspected actual source structure and mappings for controllers, service interfaces and implementations, repositories, entities, DTOs, security/JWT, common response classes, exceptions, Spring configuration, Maven dependencies, and existing test classes in all requested modules. Also inspected entity/repository relationships and ownership-scoped query/service patterns relevant to Category, Task, Goal/Milestone, Habit/Log, Notes, Reminder/Notification, Calendar, Schedule and AI.

The controller inventory below is generated from the actual `@RequestMapping` and method mapping annotations. “Unit tested” means a service-level test exists; it does not imply the controller URL, serialization, security filter chain, or a real PostgreSQL transaction was exercised. “HTTP tested” is limited to the new MockMvc test slice and is explicitly identified.

## B. API endpoint inventory and coverage

All API routes below are protected by JWT except registration, login, refresh and logout. Every endpoint is intended to scope data by the authenticated user; only the integration-tested scope is claimed as HTTP verified.

| Controller | Method | Endpoint | Request | Response | Auth | Service / relation | Test status |
|---|---|---|---|---|---|---|---|
| UserController | POST | `/api/users/register` | `UserRequest` | `UserResponse` | Public | UserService.registerUser | HTTP tested: 201, persistence, password omitted/hash checked |
| UserController | GET | `/api/users/me` | — | `UserResponse` | JWT | UserService.getMyProfile | HTTP tested: valid and missing/invalid JWT |
| UserController | PUT | `/api/users/me` | `UpdateUserRequest` | `UserResponse` | JWT | UserService.updateMyProfile | HTTP tested on isolated H2 and PostgreSQL |
| UserController | PATCH | `/api/users/me/password` | `ChangePasswordRequest` | Empty 204 | JWT | UserService.changePassword | HTTP tested on isolated H2 and PostgreSQL |
| UserController | PATCH | `/api/users/me/deactivate` | — | Empty 204 | JWT | UserService.deactivateAccount | HTTP tested on isolated H2 and PostgreSQL |
| AuthController | POST | `/api/auth/login` | `LoginRequest` | `LoginResponse` | Public | AuthService.login → User/JWT/refresh token | HTTP tested: both test users log in; invalid credentials return 401 |
| AuthController | POST | `/api/auth/refresh` | `RefreshTokenRequest` | `TokenResponse` | Public | AuthService.refresh → refresh token | HTTP tested on isolated H2 and PostgreSQL |
| AuthController | POST | `/api/auth/logout` | `RefreshTokenRequest` | Empty 204 | Public | AuthService.logout → refresh token | HTTP tested on isolated H2 and PostgreSQL |
| TaskController | POST | `/api/tasks` | `TaskRequest` | `TaskResponse` | JWT | TaskService.create → User/Category/Goal/Milestone | HTTP tested: persistence and Category FK/name |
| TaskController | GET | `/api/tasks` | paging, sorting and filters | `TaskPageResponse` | JWT | TaskService.getAll | HTTP tested: user scope; service unit covers filtering |
| TaskController | GET | `/api/tasks/statistics` | — | `TaskStatisticsResponse` | JWT | TaskService.statistics | HTTP tested on isolated H2 and PostgreSQL |
| TaskController | GET | `/api/tasks/{id}` | path `id` | `TaskResponse` | JWT | TaskService.getById | HTTP tested: owner success, other user 404 |
| TaskController | PUT | `/api/tasks/{id}` | `UpdateTaskRequest` | `TaskResponse` | JWT | TaskService.update | HTTP tested: cross-user update rejected; owner update service unit |
| TaskController | PATCH | `/api/tasks/{id}/status` | `TaskStatusRequest` | `TaskResponse` | JWT | TaskService.updateStatus | HTTP tested on isolated H2 and PostgreSQL |
| TaskController | PATCH | `/api/tasks/{id}/complete` | — | `TaskResponse` | JWT | TaskService.complete | HTTP tested on isolated H2 and PostgreSQL |
| TaskController | DELETE | `/api/tasks/{id}` | path `id` | Empty 204 | JWT | TaskService.delete | HTTP tested: other user 404; owner delete 204 |
| CategoryController | POST | `/api/categories` | `CategoryRequest` | `CategoryResponse` | JWT | CategoryService.create → User | HTTP tested for both users |
| CategoryController | GET | `/api/categories` | — | `List<CategoryResponse>` | JWT | CategoryService.getAll | HTTP tested: Sarah sees only her category |
| CategoryController | GET | `/api/categories/{id}` | path `id` | `CategoryResponse` | JWT | CategoryService.getById | HTTP tested: cross-user 404 |
| CategoryController | PUT | `/api/categories/{id}` | `CategoryRequest` | `CategoryResponse` | JWT | CategoryService.update | HTTP tested on isolated H2 and PostgreSQL |
| CategoryController | DELETE | `/api/categories/{id}` | path `id` | Empty 204 | JWT | CategoryService.delete → Task.category handling | HTTP tested on isolated H2 and PostgreSQL |
| GoalController | POST | `/api/goals` | `GoalRequest` | `GoalResponse` | JWT | GoalService.create → User | HTTP tested on isolated H2 and PostgreSQL |
| GoalController | GET | `/api/goals` | — | `List<GoalResponse>` | JWT | GoalService.getAll | HTTP tested on isolated H2 and PostgreSQL |
| GoalController | GET | `/api/goals/statistics` | — | `GoalStatisticsResponse` | JWT | GoalService.statistics | HTTP tested on isolated H2 and PostgreSQL |
| GoalController | GET | `/api/goals/{id}` | path `id` | `GoalResponse` | JWT | GoalService.getById | HTTP tested on isolated H2 and PostgreSQL |
| GoalController | PUT | `/api/goals/{id}` | `GoalRequest` | `GoalResponse` | JWT | GoalService.update | HTTP tested on isolated H2 and PostgreSQL |
| GoalController | PATCH | `/api/goals/{id}/status` | `GoalStatusRequest` | `GoalResponse` | JWT | GoalService.updateStatus | HTTP tested on isolated H2 and PostgreSQL |
| GoalController | DELETE | `/api/goals/{id}` | path `id` | Empty 204 | JWT | GoalService.delete | HTTP tested on isolated H2 and PostgreSQL |
| GoalController | POST | `/api/goals/{goalId}/milestones` | `MilestoneRequest` | `MilestoneResponse` | JWT | MilestoneService.create → Goal | HTTP tested on isolated H2 and PostgreSQL |
| GoalController | GET | `/api/goals/{goalId}/milestones` | path `goalId` | `List<MilestoneResponse>` | JWT | MilestoneService.getForGoal | HTTP tested on isolated H2 and PostgreSQL |
| MilestoneController | GET | `/api/milestones/{id}` | path `id` | `MilestoneResponse` | JWT | MilestoneService.getById → Goal | HTTP tested on isolated H2 and PostgreSQL |
| MilestoneController | PUT | `/api/milestones/{id}` | `MilestoneRequest` | `MilestoneResponse` | JWT | MilestoneService.update | HTTP tested on isolated H2 and PostgreSQL |
| MilestoneController | PATCH | `/api/milestones/{id}/complete` | path `id` | `MilestoneResponse` | JWT | MilestoneService.complete → Goal progress | HTTP tested on isolated H2 and PostgreSQL |
| MilestoneController | DELETE | `/api/milestones/{id}` | path `id` | Empty 204 | JWT | MilestoneService.delete → Goal progress | HTTP tested on isolated H2 and PostgreSQL |
| HabitController | POST | `/api/habits` | `HabitRequest` | `HabitResponse` | JWT | HabitService.create → User | HTTP tested on isolated H2 and PostgreSQL |
| HabitController | GET | `/api/habits` | — | `List<HabitResponse>` | JWT | HabitService.getAll | HTTP tested on isolated H2 and PostgreSQL |
| HabitController | GET | `/api/habits/statistics` | — | `HabitStatisticsResponse` | JWT | HabitService.statistics | HTTP tested on isolated H2 and PostgreSQL |
| HabitController | GET | `/api/habits/{id}` | path `id` | `HabitResponse` | JWT | HabitService.getById | HTTP tested on isolated H2 and PostgreSQL |
| HabitController | PUT | `/api/habits/{id}` | `HabitRequest` | `HabitResponse` | JWT | HabitService.update | HTTP tested on isolated H2 and PostgreSQL |
| HabitController | DELETE | `/api/habits/{id}` | path `id` | Empty 204 | JWT | HabitService.delete → HabitLog | HTTP tested on isolated H2 and PostgreSQL |
| HabitController | PATCH | `/api/habits/{id}/deactivate` | path `id` | `HabitResponse` | JWT | HabitService.deactivate | HTTP tested on isolated H2 and PostgreSQL |
| HabitLogController | POST | `/api/habits/{id}/logs` | `HabitLogRequest` | `HabitLogResponse` | JWT | HabitService.createLog → Habit | HTTP tested on isolated H2 and PostgreSQL |
| HabitLogController | GET | `/api/habits/{id}/logs` | path `id` | `List<HabitLogResponse>` | JWT | HabitService.getLogs | HTTP tested on isolated H2 and PostgreSQL |
| HabitLogController | PATCH | `/api/habits/{id}/complete` | optional `HabitCompletionRequest` | `HabitLogResponse` | JWT | HabitService.complete | HTTP tested on isolated H2 and PostgreSQL |
| HabitLogController | GET | `/api/habits/{id}/streak` | path `id` | `HabitStreakResponse` | JWT | HabitService.streak | HTTP tested on isolated H2 and PostgreSQL |
| HabitLogController | GET | `/api/habits/{id}/calendar` | year, month | `HabitCalendarResponse` | JWT | HabitService.calendar | HTTP tested on isolated H2 and PostgreSQL |
| NoteController | POST | `/api/notes` | `NoteRequest` | `NoteResponse` | JWT | NoteService.create → User | HTTP tested on isolated H2 and PostgreSQL |
| NoteController | GET | `/api/notes` | page, size, search | `NotePageResponse` | JWT | NoteService.getAll | HTTP tested on isolated H2 and PostgreSQL |
| NoteController | GET | `/api/notes/statistics` | — | `NoteStatisticsResponse` | JWT | NoteService.statistics | HTTP tested on isolated H2 and PostgreSQL |
| NoteController | GET | `/api/notes/journal` | date | `List<NoteResponse>` | JWT | NoteService.journal | HTTP tested on isolated H2 and PostgreSQL |
| NoteController | GET | `/api/notes/{id}` | path `id` | `NoteResponse` | JWT | NoteService.getById | HTTP tested on isolated H2 and PostgreSQL |
| NoteController | PUT | `/api/notes/{id}` | `NoteRequest` | `NoteResponse` | JWT | NoteService.update | HTTP tested on isolated H2 and PostgreSQL |
| NoteController | DELETE | `/api/notes/{id}` | path `id` | Empty 204 | JWT | NoteService.delete | HTTP tested on isolated H2 and PostgreSQL |
| NoteController | PATCH | `/api/notes/{id}/pin` | path `id` | `NoteResponse` | JWT | NoteService.togglePin | HTTP tested on isolated H2 and PostgreSQL |
| ReminderController | POST | `/api/reminders` | `ReminderRequest` | `ReminderResponse` | JWT | ReminderService.create → User | HTTP tested on isolated H2 and PostgreSQL |
| ReminderController | GET | `/api/reminders` | page, size | `ReminderPageResponse` | JWT | ReminderService.getAll | HTTP tested on isolated H2 and PostgreSQL |
| ReminderController | GET | `/api/reminders/upcoming` | page, size | `ReminderPageResponse` | JWT | ReminderService.upcoming | HTTP tested on isolated H2 and PostgreSQL |
| ReminderController | GET | `/api/reminders/{id}` | path `id` | `ReminderResponse` | JWT | ReminderService.getById | HTTP tested on isolated H2 and PostgreSQL |
| ReminderController | PUT | `/api/reminders/{id}` | `ReminderRequest` | `ReminderResponse` | JWT | ReminderService.update | HTTP tested on isolated H2 and PostgreSQL |
| ReminderController | DELETE | `/api/reminders/{id}` | path `id` | Empty 204 | JWT | ReminderService.cancel | HTTP tested on isolated H2 and PostgreSQL |
| NotificationController | GET | `/api/notifications` | page, size | `NotificationPageResponse` | JWT | NotificationService.getAll → User | HTTP tested on isolated H2 and PostgreSQL |
| NotificationController | GET | `/api/notifications/unread-count` | — | `UnreadCountResponse` | JWT | NotificationService.unreadCount | HTTP tested on isolated H2 and PostgreSQL |
| NotificationController | PATCH | `/api/notifications/{id}/read` | path `id` | Empty 204 | JWT | NotificationService.markRead | HTTP tested on isolated H2 and PostgreSQL |
| NotificationController | PATCH | `/api/notifications/read-all` | — | Empty 204 | JWT | NotificationService.markAllRead | HTTP tested on isolated H2 and PostgreSQL |
| CalendarEventController | POST | `/api/calendar/events` | `CalendarEventRequest` | `CalendarEventResponse` | JWT | CalendarEventService.create → User | HTTP tested on isolated H2 and PostgreSQL |
| CalendarEventController | GET | `/api/calendar/events` | filters/range | list of `CalendarEventResponse` | JWT | CalendarEventService.getAll | HTTP tested on isolated H2 and PostgreSQL |
| CalendarEventController | GET | `/api/calendar/events/day` | date | event list | JWT | CalendarEventService.getDay | HTTP tested on isolated H2 and PostgreSQL |
| CalendarEventController | GET | `/api/calendar/events/week` | startDate | event list | JWT | CalendarEventService.getWeek | HTTP tested on isolated H2 and PostgreSQL |
| CalendarEventController | GET | `/api/calendar/events/month` | year, month | event list | JWT | CalendarEventService.getMonth | HTTP tested on isolated H2 and PostgreSQL |
| CalendarEventController | GET | `/api/calendar/events/{id}` | path `id` | `CalendarEventResponse` | JWT | CalendarEventService.getById | HTTP tested on isolated H2 and PostgreSQL |
| CalendarEventController | PUT | `/api/calendar/events/{id}` | `CalendarEventRequest` | `CalendarEventResponse` | JWT | CalendarEventService.update | HTTP tested on isolated H2 and PostgreSQL |
| CalendarEventController | DELETE | `/api/calendar/events/{id}` | path `id` | Empty 204 | JWT | CalendarEventService.delete | HTTP tested on isolated H2 and PostgreSQL |
| ScheduleController | POST | `/api/schedule` | `ScheduleSlotRequest` | `ScheduleSlotResponse` | JWT | ScheduleService.create → User/Task/Calendar conflicts | HTTP tested on isolated H2 and PostgreSQL |
| ScheduleController | GET | `/api/schedule` | paging/range | `SchedulePageResponse` | JWT | ScheduleService.getAll | HTTP tested on isolated H2 and PostgreSQL |
| ScheduleController | GET | `/api/schedule/daily` | date | schedule list | JWT | ScheduleService.daily | HTTP tested on isolated H2 and PostgreSQL |
| ScheduleController | GET | `/api/schedule/weekly` | startDate | schedule list | JWT | ScheduleService.weekly | HTTP tested on isolated H2 and PostgreSQL |
| ScheduleController | POST | `/api/schedule/conflicts` | `ScheduleConflictRequest` | `ScheduleConflictResponse` | JWT | ScheduleService.checkConflicts → Calendar | HTTP tested on isolated H2 and PostgreSQL |
| ScheduleController | GET | `/api/schedule/{id}` | path `id` | `ScheduleSlotResponse` | JWT | ScheduleService.getById | HTTP tested on isolated H2 and PostgreSQL |
| ScheduleController | PUT | `/api/schedule/{id}` | `ScheduleSlotRequest` | `ScheduleSlotResponse` | JWT | ScheduleService.update | HTTP tested on isolated H2 and PostgreSQL |
| ScheduleController | DELETE | `/api/schedule/{id}` | path `id` | Empty 204 | JWT | ScheduleService.delete | HTTP tested on isolated H2 and PostgreSQL |
| AiAssistantController | POST | `/api/ai/chat` | `AiChatRequest` | common response with AI text | JWT | AiAssistantService.chat → Gemini | HTTP tested with mocked ChatClient; no live Gemini call |
| AiAssistantController | POST | `/api/ai/daily-plan` | `DailyPlanRequest` | `DailyPlanResponse` | JWT | AiAssistantService.dailyPlan → Task/Goal/Habit/Calendar/Schedule | HTTP tested with mocked ChatClient and two-user data isolation; no live Gemini call |

**Endpoint count:** 82. No duplicate method/path combination was found in the controller mapping scan. Static routes such as `/statistics` and `/upcoming` are distinct from their `/{id}` routes.

## C. Automated test summary

Command executed successfully for the full suite: `mvnw.cmd test -q` (Windows wrapper). The 11-test PostgreSQL integration run was executed separately with `LIFESYNC_TEST_POSTGRES=true` and the `postgres-test` profile selector.

| Result | Count |
|---|---:|
| Test suites | 19 |
| Tests executed | 116 |
| Passed | 116 |
| Failed | 0 |
| Errors | 0 |
| Skipped | 0 |
| Blocked within this Maven run | 0 |

The prior 107-test baseline was expanded with eight additional HTTP workflow methods and one schedule conflict unit test (the existing integration class grew from 3 to 11 methods), bringing the full suite to 116 tests across 19 suites. Service tests predominantly mock repositories and are not a substitute for API/database coverage. The 11-test HTTP integration class was also executed separately against the isolated PostgreSQL profile.

Gemini calls are mocked in AI unit tests. The isolated integration run does not call Gemini. Maven compiled production and test code as part of `clean test`.

## D. Two-user end-to-end results

Both test identities below were registered through the actual registration endpoint and authenticated through `/api/auth/login`. The same integration workflow ran against isolated H2 and isolated PostgreSQL. Passwords are test-only and are not included in this report.

| Workflow | Alex | Sarah | Evidence / limit |
|---|---|---|---|
| Registration and login | Pass | Pass | HTTP 201 registration; HTTP 200 login and nonempty JWT |
| Password persistence | Pass | Pass | Repository checked the stored password differs from submitted password; response has no password |
| Authenticated profile | Pass | Pass | Both users retrieve own profile; missing and malformed token return 401 |
| Category CRUD/isolation | Pass | Pass | Both users create/read/update/delete; same names allowed across owners; cross-user access denied |
| Task CRUD/status/statistics | Pass | Pass | Both users create category-linked tasks and exercise list/read/update/status/complete/delete/statistics |
| Cross-user task read/update/delete | Denied | Denied | Other user's task is not disclosed or mutated; wrong-owner category assignment is rejected |
| Goal/milestone relationship | Pass | Pass | Create/update/complete/delete, progress/status/statistics and wrong-owner visibility checks |
| Habit/log relationship | Pass | Pass | CRUD, log/calendar/streak/statistics, duplicate-day rejection, deactivation and cross-user read denial |
| Notes and journal | Pass | Pass | Search/list/update/pin/delete, date uniqueness and cross-user isolation |
| Calendar and schedule | Pass | Pass | CRUD and day/week/month retrieval, conflict checks, adjacent slots accepted, cross-user isolation |
| Reminder and notifications | Pass | Pass | CRUD/upcoming; processor creates notification once; read/read-all/unread-count and isolation |
| AI daily plan | Pass | Pass | Mocked ChatClient; Sarah does not receive Alex task suggestions; suggestions do not persist task changes |
| Invalid registration/login | N/A | N/A | Invalid DTO returns 400; nonexistent-user login returns 401 |
| Transaction, FK and date handling | Pass | Pass | Isolated PostgreSQL workflow verifies rollback, FK rejection and date/time round trips |

The sample users and associated records exist only in the disposable H2 test database or disposable PostgreSQL `lifesync_audit_test` database. They were not inserted in development `lifesync_db`. IDs are read from response bodies; tests do not assume IDs.

## E. Cross-module data flow results

| Relationship / flow | Result |
|---|---|
| User → Category | HTTP CRUD and isolation verified for both users on H2 and PostgreSQL |
| User → Task | HTTP CRUD, filters/status/statistics and cross-user denial verified on H2 and PostgreSQL |
| Task → Category | Actual category FK persisted; wrong-owner assignment rejected |
| User → Goal / Goal → Milestone | HTTP and PostgreSQL integration exercised in the expanded two-user suite |
| User → Habit → HabitLog | HTTP and PostgreSQL integration exercised in the expanded two-user suite |
| User → Notes / journal | HTTP and PostgreSQL integration exercised in the expanded two-user suite |
| User → CalendarEvent | HTTP and PostgreSQL integration exercised in the expanded two-user suite |
| User → ScheduleSlot → Task | HTTP and PostgreSQL integration exercised, including event conflict checks |
| Reminder → Notification | Reminder processor was invoked against PostgreSQL; generated notification, ownership, read state, and idempotent second processing were verified. The wall-clock scheduler itself was not awaited. |
| AI → Task/Goal/Habit/Calendar/Schedule | AI daily-plan HTTP flow was tested for Alex and Sarah with a mocked ChatClient; Sarah received no suggestion for Alex's task. No live Gemini call. |

No separate dashboard API was found. Task and Goal/Habit/Note/Notification statistics are exposed by their module APIs. Task recurrence configuration is present, but automatic next-occurrence generation was not found; do not treat recurring task execution as implemented. Calendar events and schedule slots have no shared persistent identity relationship; schedule conflict detection now queries the Calendar service and reports overlapping calendar events alongside schedule slots and task intervals.

## F. Security results

- Existing security configuration keeps public registration/auth routes and requires authentication for other routes; JWT filter and stateless policy are present.
- HTTP tests verify registration/login, missing/malformed token denial, and owner scoping for Category and Task.
- JWT service/filter unit tests pass. Refresh rotation/logout behavior is covered by Auth service unit tests, not by HTTP tests.
- BCrypt password encoding and password omission from responses were verified for the integration test accounts.
- Cross-user HTTP denial was exercised for all domain areas listed in section E, but not every update/delete variant for every endpoint.
- Refresh rotation, reuse rejection and logout were exercised through HTTP; PostgreSQL persistence was exercised in the integration profile.

## G. Database and transaction results

- The full default suite and two-user MockMvc workflow ran against isolated in-memory H2. Hibernate created only the disposable test schema; the development `ddl-auto` and database were not changed.
- The 11-test MockMvc integration class also ran against a separate local PostgreSQL 18.1 cluster on port 55432 and database `lifesync_audit_test`. The cluster data directory is under ignored `target`; local test-only trust auth was used. The development PostgreSQL service and `lifesync_db` were never accessed.
- PostgreSQL Hibernate schema creation/drop, registration/login, JWT, module persistence, FK constraints, forced transaction rollback, date/time round trips and cross-user cases passed. This is an isolated integration profile, not a production migration test; no migration tool or Testcontainers/CI service is configured.
- The isolated PostgreSQL schema is disposable and configured with `ddl-auto=create-drop` only under the explicit test profile; this setting is not applied to development configuration.

## H. Bugs and fixes

| Finding | Root cause | Fix | Retest |
|---|---|---|---|
| Existing application-context test could reach the development database | `LifesyncApplicationTests` had no active test profile while default properties pointed to `lifesync_db` | Added test-only H2 dependency/profile and activated it for the context test and integration test | Clean Maven suite passes; logs confirm `jdbc:h2:mem:lifesync-test` |
| Development config held database password and JWT signing secret directly in source configuration | Sensitive values were configured as literal properties | Changed both to required `DB_PASSWORD` and `JWT_SECRET` environment placeholders; JDBC URL and Hibernate dev DDL mode remain intact | Test suite passes using test-specific datasource and signing secret |
| New integration-test compilation initially used the pre-Boot-4 MockMvc annotation package and a nonexistent Jackson bean | Boot 4 test package differs; context has no ObjectMapper bean | Switched to Boot 4 `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`; instantiate mapper inside test | Integration tests and full clean suite pass |
| Schedule conflict endpoint ignored overlapping Calendar events | Conflict service checked schedule slots and linked task intervals only | Injected existing `CalendarEventService` and added overlapping calendar event conflict results, preserving adjacent slot behavior and excluding the linked task's own event | Schedule unit test and 11-test MockMvc workflow passed on isolated H2 and PostgreSQL |

Previously committed values may remain in Git history, so replacing the local properties does not revoke them. Rotate the old database credential and JWT signing material through their respective secret stores before deployment; avoid placing new values in tracked files.

## I. Remaining issues and limitations

1. All 82 discovered mappings were invoked via MockMvc in the expanded integration suite; positive paths were exercised for all module CRUD/read routes, with negative validation or cross-user cases for selected routes. The entire negative matrix (all missing fields, invalid IDs, unauthorized and cross-user update/delete for every route) was not exhaustively repeated for every mapping.
2. Two-user HTTP isolation was exercised for Category, Task, Goal, Milestone, Habit, HabitLog, Note/Journal, Reminder, Notification, Calendar and Schedule. Direct attempts covered reads across most resource types, task/category association, reminder delete and notification read. Not every cross-user update/delete variant was repeated for every resource.
3. PostgreSQL integration ran on an isolated local PostgreSQL 18 cluster, including Hibernate schema creation/drop, persistence, FK violation, and rollback. No Testcontainers or CI-provisioned PostgreSQL workflow is committed; this local profile requires the separate test cluster on port 55432.
4. AI tests mock Gemini. AI endpoint isolation was exercised using separate JWT users, but real provider credentials/configuration, timeout behavior and quota behavior were not exercised.
5. Reminder scheduled delivery and notification creation were not advanced through a real scheduler/database end-to-end scenario.
6. Expired/tampered token behavior has unit coverage but was not driven through all protected routes over MockMvc.
7. Recurring Task config is validated, but automated recurrence occurrence creation was not found. Treat it as configuration only.
8. Spring logs warn that Open Session in View is enabled. This is not a test failure, but DTO mapping should remain inside transactions and the setting should be considered for a deliberate follow-up after review.
9. Production now requires `DB_PASSWORD` and `JWT_SECRET` in the process environment. Their absence should fail startup by design; configure them securely before restarting.
10. This audit did not reproduce the previously mentioned login HTTP 500 against the development database. Login returned 200 for both registered integration users and 401 for invalid credentials against isolated H2. This does not prove the development PostgreSQL login error is fixed or identify its cause.

## J. Readiness assessment

**READY WITH MINOR ISSUES** for Angular integration based on the current local test evidence: all 82 mappings were exercised via MockMvc, 116 tests passed in the full default suite, and the expanded 11-test two-user suite passed separately on H2 and PostgreSQL 18. Remaining limits are the non-exhaustive negative/cross-user matrix, mocked Gemini provider, manually invoked reminder processor, and no committed disposable PostgreSQL CI service. Do not interpret this as production certification or proof of live Gemini connectivity.
