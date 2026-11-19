package com.lifesync.lifesync.category.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public class CategoryRequest {
    @NotBlank(message = "Category name is required") @Size(max = 100, message = "Category name cannot exceed 100 characters") private String name;
    @Size(max = 500, message = "Description cannot exceed 500 characters") private String description;
    @Pattern(regexp = "^$|^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$", message = "Color must be a 3 or 6 digit hex value such as #2A7FFF") private String color;
    public CategoryRequest() { }
    public CategoryRequest(String name, String description, String color) { this.name = name; this.description = description; this.color = color; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
