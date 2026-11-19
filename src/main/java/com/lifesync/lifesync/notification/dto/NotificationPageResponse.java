package com.lifesync.lifesync.notification.dto;

import com.lifesync.lifesync.common.PageResponse;
import java.util.List;

public class NotificationPageResponse extends PageResponse<NotificationResponse> {
    public NotificationPageResponse() { }
    public NotificationPageResponse(List<NotificationResponse> content, int page, int size, long totalElements, int totalPages, boolean last) {
        super(content, page, size, totalElements, totalPages, last);
    }
}
