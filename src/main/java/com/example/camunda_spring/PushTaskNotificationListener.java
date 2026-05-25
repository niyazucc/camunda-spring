package com.example.camunda_spring;

import com.example.camunda_spring.service.FcmTokenStore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.identity.User;
import org.camunda.bpm.engine.task.IdentityLink;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component("pushTaskNotificationListener")
public class PushTaskNotificationListener implements TaskListener {

    private static final Logger LOGGER = Logger.getLogger(PushTaskNotificationListener.class.getName());
    private static final String FCM_TOKEN_USER_INFO_KEY = "fcm_token";

    private final IdentityService identityService;
    private final FcmTokenStore fcmTokenStore;

    // We can inject IdentityService directly. FirebaseMessaging handles its own singletons.
    public PushTaskNotificationListener(IdentityService identityService, FcmTokenStore fcmTokenStore) {
        this.identityService = identityService;
        this.fcmTokenStore = fcmTokenStore;
    }

    @Override
    public void notify(DelegateTask task) {
        String assignee = task.getAssignee();

        // Scenario 1: Task explicitly assigned to a single person
        if (assignee != null && !assignee.trim().isEmpty()) {
            sendFirebaseNotification(assignee, task);
            return;
        }

        // Scenario 2: Task is unassigned but has Candidate Groups
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
            sendFirebaseNotificationsToMultipleUsers(List.copyOf(userIds), task);
        } else if (!groupIds.isEmpty()) {
            LOGGER.info("Task '" + task.getName() + "' is available for Groups: "
                    + groupIds + ", but no users were found in those groups.");
        } else {
            LOGGER.info("Task '" + task.getName() + "' has neither an assignee nor any candidate groups.");
        }
    }

    /**
     * Sends a Firebase message to a single user by looking up their token in H2 storage
     */
    private void sendFirebaseNotification(String targetUser, DelegateTask task) {
        String userToken = fcmTokenStore.getToken(targetUser);

        if (userToken == null || userToken.trim().isEmpty()) {
            LOGGER.warning("Could not send push notification. No FCM token registered for user: " + targetUser);
            return;
        }

        try {
            Message message = Message.builder()
                    .setToken(userToken)
                    .setNotification(Notification.builder()
                            .setTitle("New Task Assigned")
                            .setBody("You have a new task: " + task.getName())
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            LOGGER.info("Firebase notification successfully dispatched to " + targetUser + ". Message ID: " + response);

        } catch (FirebaseMessagingException exception) {
            handleFirebaseMessagingException(targetUser, exception);
        } catch (FirebaseMessagingException exception) {
            handleFirebaseMessagingException(targetUser, exception);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Firebase failed to send push notification to user: " + targetUser, exception);
        }
    }

    /**
     * Loops through a list of user IDs, grabs their tokens, and pushes notifications via Firebase
     */
    private void sendFirebaseNotificationsToMultipleUsers(List<String> targetUsers, DelegateTask task) {
        List<String> successfullyNotified = new ArrayList<>();

        for (String targetUser : targetUsers) {
            String userToken = fcmTokenStore.getToken(targetUser);

            if (userToken == null || userToken.trim().isEmpty()) {
                LOGGER.info("Skipping user '" + targetUser + "' - No active web push token found in H2.");
                continue;
            }

            try {
                Message message = Message.builder()
                        .setToken(userToken)
                        .setNotification(Notification.builder()
                                .setTitle("Group Task Available")
                                .setBody("A new group task is waiting: " + task.getName())
                                .build())
                        .build();

                FirebaseMessaging.getInstance().send(message);
                successfullyNotified.add(targetUser);

            } catch (FirebaseMessagingException exception) {
                handleFirebaseMessagingException(targetUser, exception);
            } catch (FirebaseMessagingException exception) {
                handleFirebaseMessagingException(targetUser, exception);
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Firebase failed to send group push notification to user: " + targetUser, exception);
            }
        }

        if (!successfullyNotified.isEmpty()) {
            LOGGER.info("Firebase completely finished processing. Notifications successfully landed for users: " + successfullyNotified);
        }
    }

    private void handleFirebaseMessagingException(String targetUser, FirebaseMessagingException exception) {
        MessagingErrorCode errorCode = exception.getMessagingErrorCode();

        if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
            fcmTokenStore.deleteToken(targetUser);
            LOGGER.warning("Removed stale FCM token for user '" + targetUser + "' after Firebase returned: " + errorCode);
            return;
        }

        LOGGER.log(Level.WARNING,
                "Firebase failed to send push notification to user '" + targetUser + "' with error code: " + errorCode,
                exception);
    }
}
