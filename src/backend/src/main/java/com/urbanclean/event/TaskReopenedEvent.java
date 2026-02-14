package com.urbanclean.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Event triggered when a task transitions to REABIERTO state
 * Used to notify the operator assigned to the task
 */
@Getter
public class TaskReopenedEvent extends ApplicationEvent {

    private final UUID taskId;
    private final String operatorEmail;
    private final String taskCategory;
    private final String taskDescription;
    private final String rejectionJustification;

    public TaskReopenedEvent(Object source, UUID taskId, String operatorEmail,
                            String taskCategory, String taskDescription, 
                            String rejectionJustification) {
        super(source);
        this.taskId = taskId;
        this.operatorEmail = operatorEmail;
        this.taskCategory = taskCategory;
        this.taskDescription = taskDescription;
        this.rejectionJustification = rejectionJustification;
    }
}
