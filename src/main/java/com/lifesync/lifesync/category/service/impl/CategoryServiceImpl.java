package com.lifesync.lifesync.category.service.impl;

import com.lifesync.lifesync.category.dto.CategoryRequest;
import com.lifesync.lifesync.category.dto.CategoryResponse;
import com.lifesync.lifesync.category.entity.Category;
import com.lifesync.lifesync.category.repository.CategoryRepository;
import com.lifesync.lifesync.category.service.CategoryService;
import com.lifesync.lifesync.exception.DuplicateResourceException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    public CategoryServiceImpl(CategoryRepository categoryRepository, UserRepository userRepository, TaskRepository taskRepository) {
        this.categoryRepository=categoryRepository; this.userRepository=userRepository; this.taskRepository=taskRepository;
    }
    @Override @Transactional
    public CategoryResponse create(Long userId, CategoryRequest request) {
        if (categoryRepository.existsByUser_IdAndNameIgnoreCase(userId, request.getName().trim())) throw new DuplicateResourceException("A category with this name already exists");
        Category category = new Category(); category.setUser(findUser(userId)); apply(category, request);
        try { return response(categoryRepository.save(category)); }
        catch (DataIntegrityViolationException ex) { throw new DuplicateResourceException("A category with this name already exists"); }
    }
    @Override @Transactional(readOnly=true)
    public List<CategoryResponse> getAll(Long userId) { return categoryRepository.findAllByUser_IdOrderByNameAsc(userId).stream().map(this::response).toList(); }
    @Override @Transactional(readOnly=true)
    public CategoryResponse getById(Long userId, Long id) { return response(findCategory(userId,id)); }
    @Override @Transactional
    public CategoryResponse update(Long userId, Long id, CategoryRequest request) {
        Category category=findCategory(userId,id);
        if(categoryRepository.existsByUser_IdAndNameIgnoreCaseAndIdNot(userId,request.getName().trim(),id)) throw new DuplicateResourceException("A category with this name already exists");
        apply(category,request);
        try { return response(categoryRepository.save(category)); }
        catch(DataIntegrityViolationException ex) { throw new DuplicateResourceException("A category with this name already exists"); }
    }
    @Override @Transactional
    public void delete(Long userId, Long id) {
        Category category=findCategory(userId,id);
        taskRepository.clearCategory(userId,id);
        categoryRepository.delete(category);
    }
    private User findUser(Long id) { return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User account was not found")); }
    private Category findCategory(Long userId,Long id) { return categoryRepository.findByIdAndUser_Id(id,userId).orElseThrow(() -> new ResourceNotFoundException("Category was not found")); }
    private void apply(Category category,CategoryRequest request) {
        category.setName(request.getName().trim()); category.setDescription(blankToNull(request.getDescription()));
        category.setColor(blankToNull(request.getColor()));
    }
    private String blankToNull(String value) { return value==null||value.isBlank()?null:value.trim(); }
    private CategoryResponse response(Category c) { return new CategoryResponse(c.getId(),c.getName(),c.getDescription(),c.getColor(),c.getCreatedAt(),c.getUpdatedAt()); }
}
