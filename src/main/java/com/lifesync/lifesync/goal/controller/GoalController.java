package com.lifesync.lifesync.goal.controller;
import com.lifesync.lifesync.goal.dto.*;
import com.lifesync.lifesync.goal.service.GoalService;
import com.lifesync.lifesync.milestone.dto.MilestoneRequest;
import com.lifesync.lifesync.milestone.dto.MilestoneResponse;
import com.lifesync.lifesync.milestone.service.MilestoneService;
import com.lifesync.lifesync.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class GoalController {
    private final GoalService goalService;
    private final MilestoneService milestoneService;
    public GoalController(GoalService goalService,MilestoneService milestoneService){this.goalService=goalService;this.milestoneService=milestoneService;}
    @PostMapping public ResponseEntity<GoalResponse> create(@AuthenticationPrincipal CustomUserDetails user,@Valid @RequestBody GoalRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(goalService.create(user.getId(),request));}
    @GetMapping public List<GoalResponse> getAll(@AuthenticationPrincipal CustomUserDetails user){return goalService.getAll(user.getId());}
    @GetMapping("/statistics") public GoalStatisticsResponse statistics(@AuthenticationPrincipal CustomUserDetails user){return goalService.statistics(user.getId());}
    @GetMapping("/{id}") public GoalResponse getById(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){return goalService.getById(user.getId(),id);}
    @PutMapping("/{id}") public GoalResponse update(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id,@Valid @RequestBody GoalRequest request){return goalService.update(user.getId(),id,request);}
    @PatchMapping("/{id}/status") public GoalResponse updateStatus(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id,@Valid @RequestBody GoalStatusRequest request){return goalService.updateStatus(user.getId(),id,request.getStatus());}
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){goalService.delete(user.getId(),id);return ResponseEntity.noContent().build();}
    @PostMapping("/{goalId}/milestones") public ResponseEntity<MilestoneResponse> createMilestone(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long goalId,@Valid @RequestBody MilestoneRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(milestoneService.create(user.getId(),goalId,request));}
    @GetMapping("/{goalId}/milestones") public List<MilestoneResponse> getMilestones(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long goalId){return milestoneService.getForGoal(user.getId(),goalId);}
}
