package com.github.cobr123.bar_o_timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.provider.Settings;
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
    private final Map<Integer, MediaPlayer> alerts = new ConcurrentHashMap<>();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand " + intent.getAction());
        if (intent != null) {
            if ("STOP_ALERT".equals(intent.getAction())) {
                final int id = intent.getIntExtra("ID", -1);
                if (alerts.containsKey(id)) {
                    alerts.get(id).stop();
                    alerts.remove(id);
                }
            } else if ("STOP_DURATION_TIMER".equals(intent.getAction())) {
                final int id = intent.getIntExtra("ID", -1);
                if (timers.containsKey(id)) {
                    timers.get(id).cancel();
                    timers.remove(id);
                }
                alerts.remove(id);
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

                alerts.put(id, MediaPlayer.create(TimerService.this, Settings.System.DEFAULT_ALARM_ALERT_URI));

                timers.put(id, new CountDownTimer(duration_millis, 1000) {

                    int PROGRESS_MAX = 100;
                    int PROGRESS_CURRENT = 0;

                    public void onTick(long millisUntilFinished) {
                        int seconds = (int) (millisUntilFinished / 1000);
                        int minutes = seconds / 60;
                        int hours = minutes / 60;
                        minutes = minutes % 60;
                        seconds = seconds % 60;
                        PROGRESS_CURRENT = (int) ((double) millisUntilFinished / (double) duration_millis * 100.0);
                        builder.setContentText(String.format("%02d:%02d:%02d", hours, minutes, seconds))
                                .setProgress(PROGRESS_MAX, PROGRESS_CURRENT, false);
                        NotificationManagerCompat.from(TimerService.this)
                                .notify(id, builder.build());
                    }

                    public void onFinish() {
                        final Intent deleteIntent = new Intent(TimerService.this, TimerService.class);
                        deleteIntent.setAction("STOP_ALERT");
                        deleteIntent.putExtra("ID", id);

                        builder.setContentText("Time!")
                                .setProgress(0, 0, false)
                                .setOngoing(false)
                                .setDeleteIntent(PendingIntent.getService(TimerService.this, id, deleteIntent, 0));
                        NotificationManagerCompat.from(TimerService.this)
                                .notify(id, builder.build());
                        alerts.get(id).start();
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