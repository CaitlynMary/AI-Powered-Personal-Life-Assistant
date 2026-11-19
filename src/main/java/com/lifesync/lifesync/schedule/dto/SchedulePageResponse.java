package com.lifesync.lifesync.schedule.dto;

import com.lifesync.lifesync.common.PageResponse;
import java.util.List;

public class SchedulePageResponse extends PageResponse<ScheduleSlotResponse> {
    public SchedulePageResponse() { }
    public SchedulePageResponse(List<ScheduleSlotResponse> content, int page, int size, long totalElements, int totalPages, boolean last) {
        super(content, page, size, totalElements, totalPages, last);
    }
}
