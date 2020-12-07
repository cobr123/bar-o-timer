package com.github.cobr123.bar_o_timer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class TimerService extends Service {

    private final String TAG = getClass().getSimpleName();
    private final String CHANNEL_ID = getClass().getCanonicalName();

    public TimerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, importance);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (intent != null) {
            final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
            final long duration_seconds = intent.getLongExtra("DURATION", 0);
            Log.d(TAG, "duration_seconds = " + duration_seconds);
            final int id = intent.getIntExtra("ID", 1);
            final AtomicLong elapsed_seconds = new AtomicLong(0);

             scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    final long remain_seconds = duration_seconds - elapsed_seconds.getAndIncrement();
                    Log.d(TAG, "remain_seconds = " + remain_seconds);
                    if (remain_seconds <= 0) {
                        scheduledExecutorService.shutdown();
                        return;
                    }
                    final Duration remain = Duration.ofSeconds(remain_seconds);
                    long hours = 0;
                    long minutes = 0;
                    long seconds = 0;
                    for (TemporalUnit temporalUnit : remain.getUnits()) {
                        if (temporalUnit == ChronoUnit.HOURS) {
                            hours = remain.get(ChronoUnit.HOURS);
                        } else if (temporalUnit == ChronoUnit.MINUTES) {
                            minutes = remain.get(ChronoUnit.MINUTES);
                        } else if (temporalUnit == ChronoUnit.SECONDS) {
                            seconds = remain.get(ChronoUnit.SECONDS);
                        }
                    }
                    final NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_baseline_timer_24)
                            .setContentTitle(String.format("%02d:%02d:%02d", hours, minutes, seconds))
                            .setAutoCancel(false)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                    NotificationManagerCompat.from(getApplicationContext())
                            .notify(id, builder.build());
                }
            }, 0, 1000, TimeUnit.MILLISECONDS);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}