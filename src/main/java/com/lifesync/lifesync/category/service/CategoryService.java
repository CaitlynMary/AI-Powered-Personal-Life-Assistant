package com.lifesync.lifesync.category.service;
import com.lifesync.lifesync.category.dto.CategoryRequest;
import com.lifesync.lifesync.category.dto.CategoryResponse;
import java.util.List;
public interface CategoryService {
    CategoryResponse create(Long userId, CategoryRequest request);
    List<CategoryResponse> getAll(Long userId);
    CategoryResponse getById(Long userId, Long id);
    CategoryResponse update(Long userId, Long id, CategoryRequest request);
    void delete(Long userId, Long id);
}
