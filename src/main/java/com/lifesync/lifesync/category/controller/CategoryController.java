package com.lifesync.lifesync.category.controller;
import com.lifesync.lifesync.category.dto.CategoryRequest;
import com.lifesync.lifesync.category.dto.CategoryResponse;
import com.lifesync.lifesync.category.service.CategoryService;
import com.lifesync.lifesync.security.principal.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;
    public CategoryController(CategoryService categoryService) { this.categoryService=categoryService; }
    @PostMapping public ResponseEntity<CategoryResponse> create(@AuthenticationPrincipal CustomUserDetails principal,@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(principal.getId(),request));
    }
    @GetMapping public List<CategoryResponse> getAll(@AuthenticationPrincipal CustomUserDetails principal) { return categoryService.getAll(principal.getId()); }
    @GetMapping("/{id}") public CategoryResponse getById(@AuthenticationPrincipal CustomUserDetails principal,@PathVariable Long id) { return categoryService.getById(principal.getId(),id); }
    @PutMapping("/{id}") public CategoryResponse update(@AuthenticationPrincipal CustomUserDetails principal,@PathVariable Long id,@Valid @RequestBody CategoryRequest request) {
        return categoryService.update(principal.getId(),id,request);
    }
    @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@AuthenticationPrincipal CustomUserDetails principal,@PathVariable Long id) {
        categoryService.delete(principal.getId(),id); return ResponseEntity.noContent().build();
    }
}
