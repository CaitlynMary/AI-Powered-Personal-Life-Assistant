package com.lifesync.lifesync.milestone.dto;

import com.lifesync.lifesync.milestone.entity.MilestoneStatus;
import jakarta.validation.constraints.NotNull;

public class MilestoneStatusRequest {
    @NotNull private MilestoneStatus status;
    public MilestoneStatusRequest() { }
    public MilestoneStatusRequest(MilestoneStatus status){this.status=status;}
    public MilestoneStatus getStatus(){return status;}
    public void setStatus(MilestoneStatus status){this.status=status;}
}
