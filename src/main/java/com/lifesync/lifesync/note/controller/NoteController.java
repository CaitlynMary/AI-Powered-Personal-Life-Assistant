package com.lifesync.lifesync.note.controller;
import com.lifesync.lifesync.note.dto.*;
import com.lifesync.lifesync.note.service.NoteService;
import com.lifesync.lifesync.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    private final NoteService service;
    public NoteController(NoteService service){this.service=service;}
    @PostMapping public ResponseEntity<NoteResponse> create(@AuthenticationPrincipal CustomUserDetails user,@Valid @RequestBody NoteRequest request){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user.getId(),request));}
    @GetMapping public NotePageResponse getAll(@AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) String search,@RequestParam(defaultValue="updatedAt") String sortBy,
            @RequestParam(defaultValue="DESC") String direction){return service.getAll(user.getId(),page,size,search,sortBy,direction);}
    @GetMapping("/statistics") public NoteStatisticsResponse statistics(@AuthenticationPrincipal CustomUserDetails user){return service.statistics(user.getId());}
    @GetMapping("/journal") public List<NoteResponse> journal(@AuthenticationPrincipal CustomUserDetails user,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date){return service.getJournalByDate(user.getId(),date);}
    @GetMapping("/{id}") public NoteResponse getById(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){return service.getById(user.getId(),id);}
    @PutMapping("/{id}") public NoteResponse update(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id,@Valid @RequestBody NoteRequest request){return service.update(user.getId(),id,request);}
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){service.delete(user.getId(),id);return ResponseEntity.noContent().build();}
    @PatchMapping("/{id}/pin") public NoteResponse togglePin(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id){return service.togglePin(user.getId(),id);}
}
