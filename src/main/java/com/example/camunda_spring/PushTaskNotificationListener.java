package com.example.camunda_spring;

import com.example.camunda_spring.service.PushNotificationService;
import com.onesignal.client.ApiException;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.task.IdentityLink;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component("pushTaskNotificationListener")
public class PushTaskNotificationListener implements TaskListener {

    private static final Logger LOGGER = Logger.getLogger(PushTaskNotificationListener.class.getName());

    private final PushNotificationService pushNotificationService;
    private final IdentityService identityService;

    public PushTaskNotificationListener(PushNotificationService pushNotificationService, IdentityService identityService) {
        this.pushNotificationService = pushNotificationService;
        this.identityService = identityService;
    }

    @Override
    public void notify(DelegateTask task) {
        String assignee = task.getAssignee();

        if (assignee != null && !assignee.trim().isEmpty()) {
            sendNotification(assignee, task);
            return;
        }

        Set<String> groupIds = new LinkedHashSet<>();
        Set<String> userIds = new LinkedHashSet<>();

        for (IdentityLink link : task.getCandidates()) {
            if (link.getGroupId() != null) {
                groupIds.add(link.getGroupId());
                identityService.createUserQuery()
                        .memberOfGroup(link.getGroupId())
                        .list()
                        .stream()
                        .map(User::getId)
                        .forEach(userIds::add);
            }
        }

        if (!groupIds.isEmpty() && !userIds.isEmpty()) {
            LOGGER.info("Task '" + task.getName() + "' is unassigned but available for Groups: "
                    + groupIds + ". Notifying users: " + userIds);
            sendNotifications(List.copyOf(userIds), task);
        } else if (!groupIds.isEmpty()) {
            LOGGER.info("Task '" + task.getName() + "' is available for Groups: "
                    + groupIds + ", but no users were found in those groups.");
        } else {
            LOGGER.info("Task '" + task.getName() + "' has neither an assignee nor any candidate groups.");
        }
    }

    private void sendNotification(String targetUser, DelegateTask task) {
        try {
            pushNotificationService.sendToExternalUser(
                    targetUser,
                    "New Task Assigned",
                    "You have a new task: " + task.getName());
            LOGGER.info("Notification successfully dispatched to user: " + targetUser);
        } catch (ApiException exception) {
            LOGGER.log(Level.WARNING, "Failed to send push notification for task " + task.getId(), exception);
        }
    }

    private void sendNotifications(List<String> targetUsers, DelegateTask task) {
        try {
            pushNotificationService.sendToExternalUsers(
                    targetUsers,
                    "New Task Assigned",
                    "You have a new task: " + task.getName());
            LOGGER.info("Notification successfully dispatched to users: " + targetUsers);
        } catch (ApiException exception) {
            LOGGER.log(Level.WARNING, "Failed to send push notification for task " + task.getId(), exception);
        }
    }
}
