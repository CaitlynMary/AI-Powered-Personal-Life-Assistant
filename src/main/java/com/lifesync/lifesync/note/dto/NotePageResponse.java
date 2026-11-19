package com.lifesync.lifesync.note.dto;

import com.lifesync.lifesync.common.PageResponse;
import java.util.List;

public class NotePageResponse extends PageResponse<NoteResponse> {
    public NotePageResponse() { }
    public NotePageResponse(List<NoteResponse> content, int page, int size, long totalElements, int totalPages, boolean last) {
        super(content, page, size, totalElements, totalPages, last);
    }
}
