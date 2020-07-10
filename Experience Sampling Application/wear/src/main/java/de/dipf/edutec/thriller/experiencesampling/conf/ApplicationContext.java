package de.dipf.edutec.thriller.experiencesampling.conf;

import android.app.NotificationManager;
import android.content.Context;
import de.dipf.edutec.thriller.experiencesampling.R;
import de.dipf.edutec.thriller.experiencesampling.activities.MainActivity;
import de.dipf.edutec.thriller.experiencesampling.foreground.ForegroundNotificationCreator;
import lombok.Getter;

@Getter
public class ApplicationContext {
    // TODO: unsafe certificate validation for development, turn off later
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final String NOTIFICATION_CONTENT_TITLE = "App is still running";
    private static final String NOTIFICATION_CONTENT_TEXT = "Tap to go back to the app.";

    private final ForegroundNotificationCreator foregroundNotificationCreator;

    public ApplicationContext(Context ctx, NotificationManager notificationManager){

        this.foregroundNotificationCreator = new ForegroundNotificationCreator(
                1,
                CHANNEL_ID,
                ctx,
                MainActivity.class,
                notificationManager,
                NOTIFICATION_CONTENT_TITLE,
                NOTIFICATION_CONTENT_TEXT,
                // TODO: long qualifier necessary to use shared resource?
                de.dipf.edutec.thriller.experiencesampling.shared.R.drawable.ic_notifications_black_24dp);
    }
}
