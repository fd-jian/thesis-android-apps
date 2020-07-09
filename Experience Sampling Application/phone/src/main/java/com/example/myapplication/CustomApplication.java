package com.example.myapplication;

import android.app.Application;
import lombok.Getter;

@Getter
public class CustomApplication extends Application {
   private ApplicationContext context;

   @Override
   public void onCreate() {
      super.onCreate();
      this.context = new ApplicationContext(getApplicationContext(), R.raw.ca);
   }
}
