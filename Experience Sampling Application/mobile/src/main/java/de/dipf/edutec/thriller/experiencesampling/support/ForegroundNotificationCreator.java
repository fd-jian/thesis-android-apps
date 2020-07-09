package de.dipf.edutec.thriller.experiencesampling.support;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import de.dipf.edutec.thriller.experiencesampling.R;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ForegroundNotificationCreator {
    @Getter
    private final int getId;
    private final String channelId;
    private final Context context;
    private final Class<?> intentClass;
    private final NotificationManager notificationManager;
    private final String contentTitle;
    private final String contentText;

    private Notification notification;

    public Notification getNotification() {
        if(notification != null) {
            return notification;
        }

        createChannelIfNecessary();

        Intent notificationIntent = new Intent(context, intentClass);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, notificationIntent, 0);
        notification = new NotificationCompat.Builder(context, channelId)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentIntent(pendingIntent)
                .build();

        return notification;
    }

    public void createChannelIfNecessary() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                notificationManager.getNotificationChannel(channelId) != null) {
            return;
        }

        NotificationChannel serviceChannel = new NotificationChannel(
                channelId,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        notificationManager.createNotificationChannel(serviceChannel);
    }

}
