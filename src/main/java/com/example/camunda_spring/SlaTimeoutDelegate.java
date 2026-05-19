package com.example.camunda_spring;

import com.example.camunda_spring.service.PushNotificationService;
import com.onesignal.client.ApiException;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.identity.User;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component("slaTimeoutDelegate")
public class SlaTimeoutDelegate implements JavaDelegate {

    private static final Logger LOGGER = Logger.getLogger(SlaTimeoutDelegate.class.getName());

    private final PushNotificationService pushNotificationService;
    private final IdentityService identityService;

    public SlaTimeoutDelegate(PushNotificationService pushNotificationService, IdentityService identityService) {
        this.pushNotificationService = pushNotificationService;
        this.identityService = identityService;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Set<String> recipientUserIds = new LinkedHashSet<>();

        // 1. DYNAMIC GROUPS: Get the 'slaTargetGroups' parameter defined by the workflow creator
        String targetGroupsStr = (String) execution.getVariable("slaTargetGroups");
        if (targetGroupsStr != null && !targetGroupsStr.trim().isEmpty()) {
            // Split by comma in case the designer put multiple groups (e.g. "supervisor, manager")
            String[] groups = targetGroupsStr.split(",");
            for (String group : groups) {
                String cleanGroup = group.trim();
                if (!cleanGroup.isEmpty()) {
                    // Look up all members of this dynamically passed group name
                    identityService.createUserQuery()
                            .memberOfGroup(cleanGroup)
                            .list()
                            .stream()
                            .map(User::getId)
                            .forEach(recipientUserIds::add);
                }
            }
        }

        // 2. DYNAMIC USERS: Get the 'slaTargetUsers' parameter if the designer targeted explicit people
        String targetUsersStr = (String) execution.getVariable("slaTargetUsers");
        System.out.println("Debug: Retrieved slaTargetUsers variable: '" + targetUsersStr + "'"); // Debug log
        if (targetUsersStr != null && !targetUsersStr.trim().isEmpty()) {
            String[] explicitUsers = targetUsersStr.split(",");
            Arrays.stream(explicitUsers)
                    .map(String::trim)
                    .filter(user -> !user.isEmpty())
                    .forEach(recipientUserIds::add);
        }

        // 3. Dispatch the push alerts to whoever was configured in the BPMN Modeler
        if (!recipientUserIds.isEmpty()) {
            String alertTitle = "🚨 SLA Overdue Notification";
            String alertBody = "A task workflow has breached its designated SLA time limitation.";

            try {
                pushNotificationService.sendToExternalUsers(List.copyOf(recipientUserIds), alertTitle, alertBody);
                LOGGER.info("SLA Alert dynamically sent to BPMN-configured users: " + recipientUserIds);
            } catch (ApiException exception) {
                LOGGER.log(Level.WARNING, "Failed to send dynamic workflow SLA push notification", exception);
            }
        } else {
            LOGGER.warning("SLA Timer fired, but no dynamic target groups or users were defined in the workflow inputs.");
        }
    }
}