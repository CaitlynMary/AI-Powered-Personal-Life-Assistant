package com.lifesync.lifesync.habit.controller;
import com.lifesync.lifesync.habit.dto.*;
import com.lifesync.lifesync.habit.service.HabitService;
import com.lifesync.lifesync.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/habits")
public class HabitLogController {
    private final HabitService service;
    public HabitLogController(HabitService service){this.service=service;}
    @PostMapping("/{id}/logs") public ResponseEntity<HabitLogResponse> createLog(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id,@Valid @RequestBody HabitLogRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.createLog(user.getId(),id,request));}
    @GetMapping("/{id}/logs") public List<HabitLogResponse> getLogs(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){return service.getLogs(user.getId(),id);}
    @PatchMapping("/{id}/complete") public HabitLogResponse complete(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id,@Valid @RequestBody(required=false) HabitCompletionRequest request){return service.complete(user.getId(),id,request);}
    @GetMapping("/{id}/streak") public HabitStreakResponse streak(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){return service.streak(user.getId(),id);}
    @GetMapping("/{id}/calendar") public HabitCalendarResponse calendar(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id,@RequestParam int year,@RequestParam int month){return service.calendar(user.getId(),id,year,month);}
}
