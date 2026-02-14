package com.urbanclean.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Event triggered when a task transitions to RESUELTO state
 * Used to notify the citizen who reported the issue
 */
@Getter
public class TaskResolvedEvent extends ApplicationEvent {

    private final UUID taskId;
    private final String citizenEmail;
    private final String taskCategory;
    private final String taskDescription;

    public TaskResolvedEvent(Object source, UUID taskId, String citizenEmail, 
                            String taskCategory, String taskDescription) {
        super(source);
        this.taskId = taskId;
        this.citizenEmail = citizenEmail;
        this.taskCategory = taskCategory;
        this.taskDescription = taskDescription;
    }
}
