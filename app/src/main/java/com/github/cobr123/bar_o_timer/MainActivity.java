package com.github.cobr123.bar_o_timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.view.Menu;
import android.view.MenuItem;

import java.time.Duration;

public class MainActivity extends AppCompatActivity {

    private final String CHANNEL_ID = getClass().getCanonicalName();

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

        //final PendingIntent pendingIntent15s = PendingIntent.getService(MainActivity.this, 4, getNewTimeIntent("15s", Duration.ofSeconds(15).getSeconds()), 0);
        final PendingIntent pendingIntent5m = PendingIntent.getService(MainActivity.this, 1, getNewTimeIntent("5m", Duration.ofMinutes(5).getSeconds()), 0);
        final PendingIntent pendingIntent25m = PendingIntent.getService(MainActivity.this, 2, getNewTimeIntent("25m", Duration.ofMinutes(25).getSeconds()), 0);
        final PendingIntent pendingIntent45m = PendingIntent.getService(MainActivity.this, 3, getNewTimeIntent("45m", Duration.ofMinutes(45).getSeconds()), 0);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_timer_24)
                .setAutoCancel(false)
                .setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                //.addAction(R.drawable.ic_baseline_more_time_24, "15s", pendingIntent15s)
                .addAction(R.drawable.ic_baseline_more_time_24, "5m", pendingIntent5m)
                .addAction(R.drawable.ic_baseline_more_time_24, "25m", pendingIntent25m)
                .addAction(R.drawable.ic_baseline_more_time_24, "45m", pendingIntent45m)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(MainActivity.this)
                .notify(0, builder.build());
    }

    private Intent getNewTimeIntent(final String title, final long seconds) {
        final Intent intent = new Intent(MainActivity.this, TimerService.class);
        intent.setAction(TimerService.START_DURATION_TIMER);
        intent.putExtra(TimerService.DURATION_SECONDS, seconds);
        intent.putExtra(TimerService.TITLE, title);
        return intent;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}