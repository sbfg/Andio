package android.andio;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class PlayerActivity extends AppCompatActivity
{
    private static final String TAG = PlayerActivity.class.getSimpleName();

    private Handler handler;
    private Record record;
    private MediaPlayer mediaPlayer;
    private Runnable updateStatusViewRunnable;
    private long startTime;
    private boolean isPlaying = false;
    private Button playButton;
    private TextView statusView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        handler = new Handler(Looper.getMainLooper());
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
        record = getIntent().getExtras().getParcelable("record");
        final TextView nameView = findViewById(R.id.nameView);
        nameView.setText(record.getName());
        final TextView durationView = findViewById(R.id.durationView);
        durationView.setText(Utils.formatDuration(record.getDuration()));
        final TextView dateTimeView = findViewById(R.id.dateTimeView);
        dateTimeView.setText(Utils.formatDateTime(record.getDateTimeMillis()));
        playButton = findViewById(R.id.buttonPlay);
        playButton.setOnClickListener(v -> {
            if (isPlaying) {
                stopPlaying();
            } else {
                startPlaying();
            }
        });
        statusView = findViewById(R.id.statusView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPlaying();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startPlaying() {
        try {
            isPlaying = true;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setDataSource(record.getFile().getAbsolutePath());
            mediaPlayer.setOnPreparedListener(mp -> {
                if (!isPlaying) {
                    return;
                }
                updateStatusViewRunnable = () -> {
                    statusView.setText(Utils.formatDuration((int) (System.currentTimeMillis() - startTime)));
                    handler.postDelayed(updateStatusViewRunnable, 1000);
                };
                handler.post(updateStatusViewRunnable);
            });
            mediaPlayer.setOnTimedTextListener((mp, text) -> statusView.setText(text.getText()));
            mediaPlayer.setOnCompletionListener(mp -> stopPlaying());
            mediaPlayer.prepare();
            mediaPlayer.start();
            startTime = System.currentTimeMillis();
            playButton.setText(R.string.Stop);
        } catch (Exception ex) {
            stopPlaying();
            Log.d(TAG, "startPlaying: failed", ex);
        }
    }

    private void stopPlaying() {
        if (!isPlaying) {
            return;
        }
        isPlaying = false;
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Exception ignored) {
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (updateStatusViewRunnable != null) {
            handler.removeCallbacks(updateStatusViewRunnable);
            updateStatusViewRunnable = null;
        }
        playButton.setText(R.string.Play);
        statusView.setText(null);
    }
}
