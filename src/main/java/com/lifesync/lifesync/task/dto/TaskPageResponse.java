package com.lifesync.lifesync.task.dto;

import com.lifesync.lifesync.common.PageResponse;
import java.util.List;

public class TaskPageResponse extends PageResponse<TaskResponse> {
    public TaskPageResponse() { }
    public TaskPageResponse(List<TaskResponse> content, int page, int size, long totalElements, int totalPages, boolean last) {
        super(content, page, size, totalElements, totalPages, last);
    }
}
