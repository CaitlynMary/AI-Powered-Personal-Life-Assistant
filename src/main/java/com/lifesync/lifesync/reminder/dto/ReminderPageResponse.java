package com.lifesync.lifesync.reminder.dto;

import com.lifesync.lifesync.common.PageResponse;
import java.util.List;

public class ReminderPageResponse extends PageResponse<ReminderResponse> {
    public ReminderPageResponse() { }
    public ReminderPageResponse(List<ReminderResponse> content, int page, int size, long totalElements, int totalPages, boolean last) {
        super(content, page, size, totalElements, totalPages, last);
    }
}
