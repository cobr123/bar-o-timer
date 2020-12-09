package com.github.cobr123.bar_o_timer;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
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

    public static final String DURATION = "DURATION";
    public static final String TITLE = "TITLE";
    public static final String NOTIFY_TAG = "NOTIFY_TAG";
    public static final String START_DURATION_TIMER = "START_DURATION_TIMER";
    public static final String FINISH_DURATION_TIMER = "FINISH_DURATION_TIMER";
    public static final String STOP_DURATION_TIMER = "STOP_DURATION_TIMER";
    public static final String STOP_ALERT = "STOP_ALERT";

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

    private final Map<String, MediaPlayer> alerts = new ConcurrentHashMap<>();
    private final Map<String, PendingIntent> alarms = new ConcurrentHashMap<>();

    private void stopAlert(final String notify_tag) {
        if (alerts.containsKey(notify_tag)) {
            final MediaPlayer player = alerts.get(notify_tag);
            if (player != null) {
                player.stop();
            }
            alerts.remove(notify_tag);
        }
    }

    private void cancelAlarm(final String notify_tag) {
        if (alarms.containsKey(notify_tag)) {
            final PendingIntent alarm = alarms.get(notify_tag);
            if (alarm != null) {
                getAlarmManager().cancel(alarm);
            }
            alarms.remove(notify_tag);
        }
    }

    private AlarmManager getAlarmManager() {
        return (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
    }

    private NotificationCompat.Builder getNotificationBuilder(final String notify_tag) {
        final Intent stopIntent = new Intent(TimerService.this, TimerService.class);
        stopIntent.setAction(STOP_DURATION_TIMER);
        stopIntent.putExtra(NOTIFY_TAG, notify_tag);

        return new NotificationCompat.Builder(TimerService.this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_timer_24)
                .setAutoCancel(false)
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(R.drawable.ic_baseline_timer_off_24, "Stop", PendingIntent.getService(TimerService.this, 0, stopIntent, 0))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }

    private void finishTimer(final String notify_tag, final String title) {
        final Intent deleteIntent = new Intent(TimerService.this, TimerService.class);
        deleteIntent.setAction(STOP_ALERT);
        deleteIntent.putExtra(NOTIFY_TAG, notify_tag);

        final NotificationCompat.Builder builder = getNotificationBuilder(notify_tag)
                .setSmallIcon(R.drawable.ic_baseline_timer_off_24)
                .setContentTitle(title)
                .setContentText("Time!")
                .setProgress(0, 0, false)
                .setAutoCancel(true)
                .setOngoing(false)
                .setDeleteIntent(PendingIntent.getService(TimerService.this, 0, deleteIntent, 0));

        NotificationManagerCompat.from(TimerService.this)
                .notify(notify_tag, 0, builder.build());

        final MediaPlayer player = MediaPlayer.create(TimerService.this, Settings.System.DEFAULT_ALARM_ALERT_URI);
        player.start();
        alerts.put(notify_tag, player);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            Log.d(TAG, "onStartCommand " + intent.getAction());
            if (FINISH_DURATION_TIMER.equals(intent.getAction())) {
                final String notify_tag = intent.getStringExtra(NOTIFY_TAG);
                final String title = intent.getStringExtra(TITLE);
                finishTimer(notify_tag, title);
            } else if (STOP_ALERT.equals(intent.getAction())) {
                final String notify_tag = intent.getStringExtra(NOTIFY_TAG);
                stopAlert(notify_tag);
                cancelAlarm(notify_tag);
            } else if (STOP_DURATION_TIMER.equals(intent.getAction())) {
                final String notify_tag = intent.getStringExtra(NOTIFY_TAG);
                stopAlert(notify_tag);
                cancelAlarm(notify_tag);
                NotificationManagerCompat.from(TimerService.this)
                        .cancel(notify_tag, 0);
            } else if (START_DURATION_TIMER.equals(intent.getAction())) {
                final String notify_tag = String.valueOf(System.currentTimeMillis());
                final long duration_millis = intent.getLongExtra(DURATION, 0) * 1000;
                final String title = intent.getStringExtra(TITLE);
                Log.d(TAG, "duration_millis = " + duration_millis + ", notify_tag = " + notify_tag + ", title = " + title);
                final long when_to_stop = System.currentTimeMillis() + duration_millis;

                final NotificationCompat.Builder builder = getNotificationBuilder(notify_tag)
                        .setContentTitle(title)
                        .setUsesChronometer(true)
                        .setWhen(when_to_stop);

                final Intent finishIntent = new Intent(TimerService.this, TimerService.class);
                finishIntent.setAction(FINISH_DURATION_TIMER);
                finishIntent.putExtra(NOTIFY_TAG, notify_tag);
                finishIntent.putExtra(TITLE, title);

                final PendingIntent finishPendingIntent = PendingIntent.getService(TimerService.this, 0, finishIntent, 0);
                alarms.put(notify_tag, finishPendingIntent);
                getAlarmManager().setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when_to_stop, finishPendingIntent);

                NotificationManagerCompat.from(TimerService.this)
                        .notify(notify_tag, 0, builder.build());
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}