package com.urbanclean.event;

import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Event published when a task is assigned to an operator
 */
public class TaskAssignedEvent extends ApplicationEvent {

    private final UUID taskId;
    private final UUID operatorId;
    private final String category;
    private final String location;
    private final Double priorityScore;

    public TaskAssignedEvent(Object source, UUID taskId, UUID operatorId, 
                            String category, String location, Double priorityScore) {
        super(source);
        this.taskId = taskId;
        this.operatorId = operatorId;
        this.category = category;
        this.location = location;
        this.priorityScore = priorityScore;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getOperatorId() {
        return operatorId;
    }

    public String getCategory() {
        return category;
    }

    public String getLocation() {
        return location;
    }

    public Double getPriorityScore() {
        return priorityScore;
    }
}
