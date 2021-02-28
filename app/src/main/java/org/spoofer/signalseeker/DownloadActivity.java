package org.spoofer.signalseeker;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.atomic.AtomicBoolean;

public class DownloadActivity extends AppCompatActivity {
    private final Handler guiHandler = new Handler(Looper.getMainLooper());

    private DownloadManager mgr = null;
    private long id;

    private ProgressBar progressBar;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        mgr = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        progressBar = findViewById(R.id.progress_bar);
        progressBar.setMax(100);
        progressBar.setProgress(0, false);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(getIntent().getStringExtra("URL")));

        request
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "UPDATE")
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle("APP update")
                .setDescription("New version " + getIntent().getDoubleExtra("OV", 0.0))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        id = mgr.enqueue(request);

        registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isRunning.getAndSet(true)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    int lastProgress = 0;

                    while (isRunning.get()) {
                        final int progress = getProgress();
                        if (progress < 0) {
                            break;
                        }
                        if (progress > lastProgress) {
                            lastProgress = progress;
                            guiHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    progressBar.setProgress(progress, true);
                                }
                            });
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    isRunning.set(false);
                }
            }).start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        isRunning.set(false);
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(mgr.ACTION_DOWNLOAD_COMPLETE)) {
                unregisterReceiver(receiver);
                isRunning.set(false);
                finishActivity(99);
            }
        }
    };


    private int getProgress() {
        DownloadManager.Query q = new DownloadManager.Query();
        q.setFilterById(id); //filter by id which you have receieved when reqesting download from download manager
        Cursor cursor = mgr.query(q);
        if (!cursor.moveToFirst()) {
            return -1;
        }

        int bytes_total = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
        if (bytes_total <= 0) {
            return -1;
        }
        int bytes_downloaded = cursor.getInt(cursor
                .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
        cursor.close();
        return (int)((double)bytes_downloaded / bytes_total) * 100;
    }

}
