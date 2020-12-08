package com.github.cobr123.bar_o_timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private int nextId = 1000;

    private int genId() {
        final int id = nextId;
        nextId += 1;
        return id;
    }

    private final Map<Integer, CountDownTimer> timers = new ConcurrentHashMap<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand " + intent.getAction());
        if (intent != null) {
            if ("STOP_DURATION_TIMER".equals(intent.getAction())) {
                final int id = intent.getIntExtra("ID", -1);
                if (timers.containsKey(id)) {
                    timers.get(id).cancel();
                    timers.remove(id);
                }
                NotificationManagerCompat.from(TimerService.this)
                        .cancel(id);
            } else if ("START_DURATION_TIMER".equals(intent.getAction())) {
                final int id = genId();
                final long duration_millis = intent.getLongExtra("DURATION", 0) * 1000;
                final String title = intent.getStringExtra("TITLE");
                Log.d(TAG, "duration_millis = " + duration_millis + ", id = " + id + ", title = " + title);
                final long when_to_stop = System.currentTimeMillis() + duration_millis;

                final Intent stopIntent = new Intent(TimerService.this, TimerService.class);
                stopIntent.setAction("STOP_DURATION_TIMER");
                stopIntent.putExtra("ID", id);

                final NotificationCompat.Builder builder = new NotificationCompat.Builder(TimerService.this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_baseline_timer_24)
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .setContentTitle(title)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setWhen(when_to_stop)
                        .addAction(R.drawable.ic_baseline_timer_off_24, "Stop", PendingIntent.getService(TimerService.this, id, stopIntent, 0))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                timers.put(id, new CountDownTimer(duration_millis, 1000) {

                    public void onTick(long millisUntilFinished) {
                        builder.setContentText("seconds remaining: " + millisUntilFinished / 1000);
                        NotificationManagerCompat.from(TimerService.this)
                                .notify(id, builder.build());
                    }

                    public void onFinish() {
                        builder.setContentText("Time!");
                        NotificationManagerCompat.from(TimerService.this)
                                .notify(id, builder.build());
                    }
                }.start());


                NotificationManagerCompat.from(TimerService.this)
                        .notify(id, builder.build());
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}