package com.github.cobr123.bar_o_timer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.view.View;

import android.view.Menu;
import android.view.MenuItem;

import java.time.Duration;

public class MainActivity extends AppCompatActivity {

    private final String CHANNEL_ID = getClass().getCanonicalName();
    private final int barNotificationId = 0;

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

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_baseline_timer_24)
                .setAutoCancel(false)
                .setOngoing(true)
                .addAction(R.drawable.ic_baseline_more_time_24, "5m", getNewTimeAction(1, Duration.ofMinutes(5).getSeconds()))
                .addAction(R.drawable.ic_baseline_more_time_24, "15m", getNewTimeAction(2, Duration.ofMinutes(15).getSeconds()))
                .addAction(R.drawable.ic_baseline_more_time_24, "25m", getNewTimeAction(3, Duration.ofMinutes(25).getSeconds()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(MainActivity.this)
                .notify(barNotificationId, builder.build());
    }

    private PendingIntent getNewTimeAction(final int id, final long seconds) {
        final Intent intent = new Intent(MainActivity.this, TimerService.class);
        intent.setAction("START_DURATION_TIMER");
        intent.putExtra("DURATION", seconds);
        intent.putExtra("ID", id);
        return PendingIntent.getService(MainActivity.this, id, intent, 0);
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