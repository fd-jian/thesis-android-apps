package com.example.myapplication;

import android.app.Application;
import lombok.Getter;

@Getter
public class CustomApplication extends Application {
   private ApplicationContext context = new ApplicationContext();
}
