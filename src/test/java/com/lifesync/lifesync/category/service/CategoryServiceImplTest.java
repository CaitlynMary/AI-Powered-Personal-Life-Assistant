package com.lifesync.lifesync.category.service;

import com.lifesync.lifesync.category.dto.CategoryRequest;
import com.lifesync.lifesync.category.entity.Category;
import com.lifesync.lifesync.category.repository.CategoryRepository;
import com.lifesync.lifesync.category.service.impl.CategoryServiceImpl;
import com.lifesync.lifesync.exception.DuplicateResourceException;
import com.lifesync.lifesync.exception.ResourceNotFoundException;
import com.lifesync.lifesync.task.repository.TaskRepository;
import com.lifesync.lifesync.user.entity.User;
import com.lifesync.lifesync.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {
    @Mock CategoryRepository categories;
    @Mock UserRepository users;
    @Mock TaskRepository tasks;
    private CategoryServiceImpl service;
    private User owner;
    @BeforeEach void setUp(){service=new CategoryServiceImpl(categories,users,tasks);owner=new User();owner.setId(4L);}

    @Test void createsCategoryForOwnerAndAllowsSameNameForDifferentOwner() {
        when(categories.existsByUser_IdAndNameIgnoreCase(4L,"Work")).thenReturn(false);
        when(users.findById(4L)).thenReturn(Optional.of(owner));
        when(categories.save(any(Category.class))).thenAnswer(i->{Category c=i.getArgument(0);c.setId(8L);return c;});
        var response=service.create(4L,new CategoryRequest(" Work ","Job", "#AABBCC"));
        ArgumentCaptor<Category> captor=ArgumentCaptor.forClass(Category.class);verify(categories).save(captor.capture());
        assertEquals(4L,captor.getValue().getUser().getId());assertEquals("Work",response.getName());
        when(categories.existsByUser_IdAndNameIgnoreCase(9L,"Work")).thenReturn(false);
        when(users.findById(9L)).thenReturn(Optional.of(new User()));
        assertDoesNotThrow(()->service.create(9L,new CategoryRequest("Work",null,null)));
    }

    @Test void rejectsDuplicateCategoryNameForSameOwner() {
        when(categories.existsByUser_IdAndNameIgnoreCase(4L,"Work")).thenReturn(true);
        assertThrows(DuplicateResourceException.class,()->service.create(4L,new CategoryRequest("Work",null,null)));
        verify(categories,never()).save(any());
    }

    @Test void hidesCategoryOwnedByAnotherUser() {
        when(categories.findByIdAndUser_Id(8L,4L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,()->service.getById(4L,8L));
    }

    @Test void unlinksTasksBeforeDeletingCategory() {
        Category category=new Category();category.setId(8L);category.setUser(owner);
        when(categories.findByIdAndUser_Id(8L,4L)).thenReturn(Optional.of(category));
        service.delete(4L,8L);
        var order=inOrder(tasks,categories);order.verify(tasks).clearCategory(4L,8L);order.verify(categories).delete(category);
    }
}
