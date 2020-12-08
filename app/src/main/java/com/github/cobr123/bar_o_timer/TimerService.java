package com.github.cobr123.bar_o_timer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
    private long startTime;
    private Intent stopIntent;
    private ScheduledExecutorService scheduledExecutorService;

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
        stopIntent = new Intent(TimerService.this, TimerService.class);
        stopIntent.setAction("STOP_DURATION_TIMER");
        scheduledExecutorService = Executors.newScheduledThreadPool(3);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (intent != null) {
            if ("STOP_DURATION_TIMER".equals(intent.getAction())) {
                scheduledExecutorService.shutdown();
                stopForeground(true);
            } else if ("START_DURATION_TIMER".equals(intent.getAction())) {
                scheduledExecutorService.shutdown();
                scheduledExecutorService = Executors.newScheduledThreadPool(1);
                final long duration_millis = intent.getLongExtra("DURATION", 0) * 1000;
                final int id = intent.getIntExtra("ID", 1);
                Log.d(TAG, "duration_millis = " + duration_millis + ", id = " + id);
                startTime = System.currentTimeMillis();

                scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
                    @Override
                    public void run() {
                        final long elapsed_millis = System.currentTimeMillis() - startTime;
                        final long remain_millis = duration_millis - elapsed_millis;
                        Log.d(TAG, "remain_millis = " + remain_millis);
                        if (remain_millis <= 0) {
                            scheduledExecutorService.shutdown();
                            stopForeground(true);
                            return;
                        }
                        int seconds = (int) (remain_millis / 1000);
                        int minutes = seconds / 60;
                        int hours = minutes / 60;
                        seconds = seconds % 60;
                        minutes = minutes % 60;

                        final NotificationCompat.Builder builder = new NotificationCompat.Builder(TimerService.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_baseline_timer_24)
                                .setContentTitle(String.format("%02d:%02d:%02d", hours, minutes, seconds))
                                .setAutoCancel(false)
                                .setOngoing(true)
                                .addAction(R.drawable.ic_baseline_timer_off_24, "Stop", PendingIntent.getService(TimerService.this, id, stopIntent, 0))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                        NotificationManagerCompat.from(TimerService.this)
                                .notify(id, builder.build());
                    }
                }, 0, 1000, TimeUnit.MILLISECONDS);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}