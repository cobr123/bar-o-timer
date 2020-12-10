package com.github.cobr123.bar_o_timer;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TimerService extends JobIntentService {

    public static final int JOB_ID = 1;

    public static void enqueueWork(final Context context, final Intent work) {
        enqueueWork(context, TimerService.class, JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(TAG, "onStartCommand " + intent.getAction());
        if (FINISH_DURATION_TIMER.equals(intent.getAction())) {
            final String notify_tag = intent.getStringExtra(NOTIFY_TAG);
            final String title = intent.getStringExtra(TITLE);
            Log.d(TAG, "FINISH_DURATION_TIMER, notify_tag = " + notify_tag + ", title = " + title);
            finishTimer(notify_tag, title);
        } else if (CANCEL_DURATION_TIMER.equals(intent.getAction())) {
            final String notify_tag = intent.getStringExtra(NOTIFY_TAG);
            final boolean resume_music = intent.getBooleanExtra(RESUME_MUSIC, false);
            stopAlert(notify_tag, resume_music);
            stopVibrator(notify_tag);
            cancelAlarm(notify_tag);
            Log.d(TAG, "CANCEL_DURATION_TIMER, notify_tag = " + notify_tag);
            NotificationManagerCompat.from(TimerService.this)
                    .cancel(notify_tag, 0);
        } else if (START_DURATION_TIMER.equals(intent.getAction())) {
            final String notify_tag = String.valueOf(System.currentTimeMillis());
            final long duration_millis = intent.getLongExtra(DURATION_SECONDS, 0) * 1000;
            final String title = intent.getStringExtra(TITLE);
            Log.d(TAG, "duration_millis = " + duration_millis + ", notify_tag = " + notify_tag + ", title = " + title);
            final long when_to_stop = System.currentTimeMillis() + duration_millis;

            final Intent stopIntent = new Intent(TimerService.this, TimerService.class);
            stopIntent.setAction(CANCEL_DURATION_TIMER);
            stopIntent.putExtra(NOTIFY_TAG, notify_tag);

            final NotificationCompat.Builder builder = getNotificationBuilder()
                    .setContentTitle(title)
                    .setUsesChronometer(true)
                    .setWhen(when_to_stop)
                    .addAction(R.drawable.ic_baseline_timer_off_24, "Cancel", PendingIntent.getService(TimerService.this, (int) SystemClock.uptimeMillis(), stopIntent, 0));

            final Intent finishIntent = new Intent(TimerService.this, TimerService.class);
            finishIntent.setAction(FINISH_DURATION_TIMER);
            finishIntent.putExtra(NOTIFY_TAG, notify_tag);
            finishIntent.putExtra(TITLE, title);

            final PendingIntent finishPendingIntent = PendingIntent.getForegroundService(TimerService.this, (int) SystemClock.uptimeMillis(), finishIntent, 0);
            alarms.put(notify_tag, finishPendingIntent);
            getAlarmManager().setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when_to_stop, finishPendingIntent);

            NotificationManagerCompat.from(TimerService.this)
                    .notify(notify_tag, 0, builder.build());
        }
    }

    public static final String DURATION_SECONDS = "DURATION_SECONDS";
    public static final String TITLE = "TITLE";
    public static final String NOTIFY_TAG = "NOTIFY_TAG";
    public static final String RESUME_MUSIC = "RESUME_MUSIC";

    public static final String START_DURATION_TIMER = "START_DURATION_TIMER";
    public static final String FINISH_DURATION_TIMER = "FINISH_DURATION_TIMER";
    public static final String CANCEL_DURATION_TIMER = "CANCEL_DURATION_TIMER";

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
    private final Map<String, Vibrator> vibrators = new ConcurrentHashMap<>();
    private final Map<String, PendingIntent> alarms = new ConcurrentHashMap<>();

    private void stopAlert(final String notify_tag, final boolean resume_music) {
        if (alerts.containsKey(notify_tag)) {
            final MediaPlayer player = alerts.get(notify_tag);
            if (player != null) {
                player.stop();
            }
            alerts.remove(notify_tag);
            if (resume_music) {
                getAudioManager().abandonAudioFocus(null);
            }
        }
    }

    private void stopVibrator(final String notify_tag) {
        if (vibrators.containsKey(notify_tag)) {
            final Vibrator vibrator = vibrators.get(notify_tag);
            if (vibrator != null) {
                vibrator.cancel();
            }
            vibrators.remove(notify_tag);
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

    private AudioManager getAudioManager() {
        return (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    }

    private Vibrator getVibrator() {
        return (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
    }

    private NotificationCompat.Builder getNotificationBuilder() {
        return new NotificationCompat.Builder(TimerService.this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_timer_24)
                .setAutoCancel(false)
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);
    }

    private void finishTimer(final String notify_tag, final String title) {
        final AudioManager audioManager = getAudioManager();
        boolean resume_music;
        boolean can_play;
        if (audioManager.isMusicActive()) {
            if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)) {
                can_play = audioManager.getStreamVolume(AudioManager.STREAM_ALARM) > 0;
                resume_music = can_play;
            } else {
                can_play = false;
                resume_music = false;
            }
        } else {
            resume_music = false;
            can_play = audioManager.getStreamVolume(AudioManager.STREAM_ALARM) > 0;
        }
        switch (audioManager.getRingerMode()) {
            case AudioManager.RINGER_MODE_SILENT:
                Log.d(TAG, "Silent mode");
                can_play = resume_music;
                break;
            case AudioManager.RINGER_MODE_VIBRATE:
                Log.d(TAG, "Vibrate mode");
                can_play = resume_music;
                break;
            case AudioManager.RINGER_MODE_NORMAL:
                Log.d(TAG, "Normal mode");
                break;
        }
        if (can_play) {
            final MediaPlayer player = MediaPlayer.create(TimerService.this, Settings.System.DEFAULT_ALARM_ALERT_URI);
            player.start();
            alerts.put(notify_tag, player);
        } else {
            final Vibrator vibrator = getVibrator();
            final long[] pattern = {0, 100, 1000, 300};
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 1));
            vibrators.put(notify_tag, vibrator);
        }

        final Intent deleteIntent = new Intent(TimerService.this, TimerService.class);
        deleteIntent.setAction(CANCEL_DURATION_TIMER);
        deleteIntent.putExtra(NOTIFY_TAG, notify_tag);
        deleteIntent.putExtra(RESUME_MUSIC, resume_music);

        final NotificationCompat.Builder builder = getNotificationBuilder()
                .setSmallIcon(R.drawable.ic_baseline_timer_off_24)
                .setContentTitle(title)
                .setContentText("Time!")
                .setAutoCancel(true)
                .setOngoing(false)
                .setDeleteIntent(PendingIntent.getService(TimerService.this, (int) SystemClock.uptimeMillis(), deleteIntent, 0))
                .setPriority(NotificationCompat.PRIORITY_MAX);

        NotificationManagerCompat.from(TimerService.this)
                .notify(notify_tag, 0, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            onHandleWork(intent);
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}