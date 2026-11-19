package com.lifesync.lifesync.reminder.controller;
import com.lifesync.lifesync.reminder.dto.*;
import com.lifesync.lifesync.reminder.service.ReminderService;
import com.lifesync.lifesync.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reminders")
public class ReminderController {
    private final ReminderService service;
    public ReminderController(ReminderService service){this.service=service;}
    @PostMapping public ResponseEntity<ReminderResponse> create(@AuthenticationPrincipal CustomUserDetails user,@Valid @RequestBody ReminderRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user.getId(),request));}
    @GetMapping public ReminderPageResponse getAll(@AuthenticationPrincipal CustomUserDetails user,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.getAll(user.getId(),page,size);}
    @GetMapping("/upcoming") public ReminderPageResponse upcoming(@AuthenticationPrincipal CustomUserDetails user,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.upcoming(user.getId(),page,size);}
    @GetMapping("/{id}") public ReminderResponse getById(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){return service.getById(user.getId(),id);}
    @PutMapping("/{id}") public ReminderResponse update(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id,@Valid @RequestBody ReminderRequest request){return service.update(user.getId(),id,request);}
    @DeleteMapping("/{id}") public ResponseEntity<Void> cancel(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){service.cancel(user.getId(),id);return ResponseEntity.noContent().build();}
}
