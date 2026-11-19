package com.lifesync.lifesync.milestone.controller;
import com.lifesync.lifesync.milestone.dto.MilestoneRequest;
import com.lifesync.lifesync.milestone.dto.MilestoneResponse;
import com.lifesync.lifesync.milestone.dto.MilestoneStatusRequest;
import com.lifesync.lifesync.milestone.service.MilestoneService;
import com.lifesync.lifesync.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {
    private final MilestoneService milestoneService;
    public MilestoneController(MilestoneService milestoneService){this.milestoneService=milestoneService;}
    @GetMapping("/{id}") public MilestoneResponse getById(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){return milestoneService.getById(user.getId(),id);}
    @PutMapping("/{id}") public MilestoneResponse update(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id,@Valid @RequestBody MilestoneRequest request){return milestoneService.update(user.getId(),id,request);}
    @PatchMapping("/{id}/status") public MilestoneResponse updateStatus(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id,@Valid @RequestBody MilestoneStatusRequest request){return milestoneService.updateStatus(user.getId(),id,request.getStatus());}
    @PatchMapping("/{id}/complete") public MilestoneResponse complete(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){return milestoneService.complete(user.getId(),id);}
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){milestoneService.delete(user.getId(),id);return ResponseEntity.noContent().build();}
}
