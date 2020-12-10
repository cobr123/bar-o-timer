package com.github.cobr123.bar_o_timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    public static final String CAPTION_1 = "caption1";
    public static final String CAPTION_2 = "caption2";
    public static final String CAPTION_3 = "caption3";

    public static final String TIME_1 = "time1";
    public static final String TIME_2 = "time2";
    public static final String TIME_3 = "time3";

    private final String TAG = getClass().getSimpleName();
    private final String CHANNEL_ID = getClass().getCanonicalName();

    private EditText mCaption1Ed;
    private EditText mCaption2Ed;
    private EditText mCaption3Ed;

    private EditText mTime1Ed;
    private EditText mTime2Ed;
    private EditText mTime3Ed;

    private SharedPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        mPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);

        mCaption1Ed = findViewById(R.id.ed_caption1);
        mCaption1Ed.addTextChangedListener(createTextWatcher(CAPTION_1));
        mCaption2Ed = findViewById(R.id.ed_caption2);
        mCaption2Ed.addTextChangedListener(createTextWatcher(CAPTION_2));
        mCaption3Ed = findViewById(R.id.ed_caption3);
        mCaption3Ed.addTextChangedListener(createTextWatcher(CAPTION_3));

        mTime1Ed = findViewById(R.id.ed_time1);
        mTime1Ed.addTextChangedListener(createTextWatcher(TIME_1));
        mTime2Ed = findViewById(R.id.ed_time2);
        mTime2Ed.addTextChangedListener(createTextWatcher(TIME_2));
        mTime3Ed = findViewById(R.id.ed_time3);
        mTime3Ed.addTextChangedListener(createTextWatcher(TIME_3));

        fillEditors();
        updateMainBar();
    }

    private TextWatcher createTextWatcher(final String preference_key) {
        return new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                mPreferences.edit()
                        .putString(preference_key, s.toString())
                        .apply();
                updateMainBar();
            }
        };
    }

    private void fillEditors() {
        mCaption1Ed.setText(mPreferences.getString(CAPTION_1, "5m"));
        mCaption2Ed.setText(mPreferences.getString(CAPTION_2, "25m"));
        mCaption3Ed.setText(mPreferences.getString(CAPTION_3, "45m"));

        mTime1Ed.setText(mPreferences.getString(TIME_1, "00:05:00"));
        mTime2Ed.setText(mPreferences.getString(TIME_2, "00:25:00"));
        mTime3Ed.setText(mPreferences.getString(TIME_3, "00:45:00"));
    }

    private String getTimeString(final long long_seconds) {
        final StringBuilder sb = new StringBuilder();
        final int seconds = (int) (long_seconds % 60L);
        final int minutes = (int) ((long_seconds / 60L) % 60L);
        final int hours = (int) (long_seconds / 60L / 60L);
        if (hours > 0) {
            sb.append(hours).append("h");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m");
        }
        if (seconds > 0) {
            sb.append(seconds).append("s");
        }
        return sb.toString();
    }

    private long getTimeInSecondsFromString(final CharSequence time) {
        long hours = 0;
        long minutes = 0;
        long seconds = 0;
        final String[] parts = time.toString().trim().split(":");
        if (parts.length == 3) {
            hours = parseLong(parts[0]);
            minutes = parseLong(parts[1]);
            seconds = parseLong(parts[2]);
        } else if (parts.length == 2) {
            minutes = parseLong(parts[0]);
            seconds = parseLong(parts[1]);
        } else if (parts.length == 1) {
            seconds = parseLong(parts[0]);
        }
        return hours * 60 * 60 + minutes * 60 + seconds;
    }

    private long parseLong(final String num) {
        if (num == null || num.trim().equals("")) {
            return 0;
        } else {
            return Long.parseLong(num);
        }
    }

    private void updateMainBar() {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_timer_24)
                .setAutoCancel(false)
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        boolean actionExists = false;
        for (int i = 1; i <= 3; i++) {
            final long seconds = getTimeInSecondsFromString(mPreferences.getString("time" + i, ""));
            if (seconds > 0) {
                String caption = mPreferences.getString("caption" + i, "");
                if ("".equals(caption)) {
                    caption = getTimeString(seconds);
                }
                final PendingIntent pendingIntent = PendingIntent.getService(this, i * (int) SystemClock.uptimeMillis(), createTimeIntent(caption, seconds), 0);
                builder.addAction(R.drawable.ic_baseline_more_time_24, caption, pendingIntent);
                actionExists = true;
            }
        }
        if (actionExists) {
            NotificationManagerCompat.from(this)
                    .notify(0, builder.build());
        } else {
            NotificationManagerCompat.from(this)
                    .cancel(0);
        }
    }

    private Intent createTimeIntent(final String title, final long seconds) {
        final Intent intent = new Intent(this, TimerService.class);
        intent.setAction(TimerService.START_DURATION_TIMER);
        intent.putExtra(TimerService.DURATION_SECONDS, seconds);
        intent.putExtra(TimerService.TITLE, title);
        return intent;
    }

}