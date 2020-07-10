package de.dipf.edutec.thriller.experiencesampling.conf;

import android.app.Application;
import android.app.NotificationManager;
import de.dipf.edutec.thriller.experiencesampling.R;
import lombok.Getter;

@Getter
public class CustomApplication extends Application {
   private ApplicationContext context;

   @Override
   public void onCreate() {
      super.onCreate();
      this.context = new ApplicationContext(
              getApplicationContext(),
              getSystemService(NotificationManager.class),
              R.raw.ca);
   }
}
