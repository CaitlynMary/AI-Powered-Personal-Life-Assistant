package com.lifesync.lifesync.habit.controller;
import com.lifesync.lifesync.habit.dto.*;
import com.lifesync.lifesync.habit.service.HabitService;
import com.lifesync.lifesync.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/habits")
public class HabitController {
    private final HabitService service;
    public HabitController(HabitService service){this.service=service;}
    @PostMapping public ResponseEntity<HabitResponse> create(@AuthenticationPrincipal CustomUserDetails user,@Valid @RequestBody HabitRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user.getId(),request));}
    @GetMapping public List<HabitResponse> getAll(@AuthenticationPrincipal CustomUserDetails user){return service.getAll(user.getId());}
    @GetMapping("/statistics") public HabitStatisticsResponse statistics(@AuthenticationPrincipal CustomUserDetails user){return service.statistics(user.getId());}
    @GetMapping("/{id}") public HabitResponse getById(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){return service.getById(user.getId(),id);}
    @PutMapping("/{id}") public HabitResponse update(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id,@Valid @RequestBody HabitRequest request){return service.update(user.getId(),id,request);}
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){service.delete(user.getId(),id);return ResponseEntity.noContent().build();}
    @PatchMapping("/{id}/deactivate") public HabitResponse deactivate(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){return service.deactivate(user.getId(),id);}
}
