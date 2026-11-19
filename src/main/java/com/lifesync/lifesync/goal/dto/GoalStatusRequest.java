package com.lifesync.lifesync.goal.dto;
import com.lifesync.lifesync.goal.entity.GoalStatus;
import jakarta.validation.constraints.NotNull;
public class GoalStatusRequest {
    @NotNull private GoalStatus status;
    public GoalStatusRequest() { }
    public GoalStatusRequest(GoalStatus status){this.status=status;}
    public GoalStatus getStatus(){return status;} public void setStatus(GoalStatus status){this.status=status;}
}
