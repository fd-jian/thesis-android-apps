package de.dipf.edutec.thriller.experiencesampling.conf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import de.dipf.edutec.thriller.experiencesampling.messageservice.WebSocketService;
import de.dipf.edutec.thriller.experiencesampling.sensorservice.DataLayerListenerService;

public class OnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent i) {
        Intent intent = new Intent(context, DataLayerListenerService.class);
        context.startForegroundService(intent);
    }
}
