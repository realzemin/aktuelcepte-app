package com.indirimtrendim.app;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;

public class MainApplication extends Application {
    public static final String ONESIGNAL_APP_ID = "2c0dfdb8-40cf-4d29-9d05-744de782bfd3";

    @Override
    public void onCreate() {
        super.onCreate();

        // OneSignal Başlatma
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);

        // Bildirime tıklandığında dış tarayıcıya (Chrome) gitmeyi engelle, uygulama içinde aç
        OneSignal.getNotifications().addClickListener(event -> {
            event.preventDefault(); // Dış tarayıcının açılmasını engeller
            String launchUrl = event.getNotification().getLaunchURL();
            if (launchUrl == null || launchUrl.trim().isEmpty()) {
                launchUrl = "https://indirimtrendim.com";
            }
            
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.setData(Uri.parse(launchUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }
}
