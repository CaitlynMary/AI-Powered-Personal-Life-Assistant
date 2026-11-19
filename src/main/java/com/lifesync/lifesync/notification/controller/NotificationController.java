package com.lifesync.lifesync.notification.controller;
import com.lifesync.lifesync.notification.dto.NotificationPageResponse;
import com.lifesync.lifesync.notification.dto.UnreadCountResponse;
import com.lifesync.lifesync.notification.service.NotificationService;
import com.lifesync.lifesync.security.principal.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service){this.service=service;}
    @GetMapping public NotificationPageResponse getAll(@AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(defaultValue="false") boolean unreadOnly){return service.getAll(user.getId(),page,size,unreadOnly);}
    @GetMapping("/unread-count") public UnreadCountResponse unreadCount(@AuthenticationPrincipal CustomUserDetails user){return new UnreadCountResponse(service.unreadCount(user.getId()));}
    @PatchMapping("/{id}/read") public ResponseEntity<Void> markRead(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){service.markRead(user.getId(),id);return ResponseEntity.noContent().build();}
    @PatchMapping("/read-all") public ResponseEntity<Void> markAllRead(@AuthenticationPrincipal CustomUserDetails user){service.markAllRead(user.getId());return ResponseEntity.noContent().build();}
}
