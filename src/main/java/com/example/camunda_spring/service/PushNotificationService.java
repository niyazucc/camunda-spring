package com.example.camunda_spring.service;

import com.onesignal.client.ApiException;
import com.onesignal.client.api.DefaultApi;
import com.onesignal.client.model.CreateNotificationSuccessResponse;
import com.onesignal.client.model.LanguageStringMap;
import com.onesignal.client.model.Notification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PushNotificationService {

    private final DefaultApi oneSignalApi;
    private final String appId;

    public PushNotificationService(DefaultApi oneSignalApi, @Value("${onesignal.app-id}") String appId) {
        this.oneSignalApi = oneSignalApi;
        this.appId = appId;
    }

    public CreateNotificationSuccessResponse sendToAll(String title, String message) throws ApiException {
        Notification notification = baseNotification(title, message)
                .includedSegments(List.of("All"));

        return oneSignalApi.createNotification(notification);
    }

    public CreateNotificationSuccessResponse sendToSubscription(
            String subscriptionId,
            String title,
            String message
    ) throws ApiException {
        Notification notification = baseNotification(title, message)
                .includeSubscriptionIds(List.of(subscriptionId));

        return oneSignalApi.createNotification(notification);
    }

    public CreateNotificationSuccessResponse sendToExternalUser(
            String externalUserId,
            String title,
            String message
    ) throws ApiException {
        return sendToExternalUsers(List.of(externalUserId), title, message);
    }

    public CreateNotificationSuccessResponse sendToExternalUsers(
            List<String> externalUserIds,
            String title,
            String message
    ) throws ApiException {
        Notification notification = baseNotification(title, message)
                .targetChannel(Notification.TargetChannelEnum.PUSH)
                .includeAliases(Map.of("external_id", externalUserIds));

        return oneSignalApi.createNotification(notification);
    }

    private Notification baseNotification(String title, String message) {
        return new Notification()
                .appId(appId)
                .headings(new LanguageStringMap().en(title))
                .contents(new LanguageStringMap().en(message));
    }
}
