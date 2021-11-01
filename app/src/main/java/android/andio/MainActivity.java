package android.andio;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import java.io.File;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity
{
    public static final String TAG = MainActivity.class.getSimpleName();

    private Handler handler;
    private File recordsDir;
    private MediaRecorder mediaRecorder;
    private Record recordingRecord;
    private Runnable updateRecordTextViewRunnable;
    private boolean isRecording = false;

    private TextView timerTextView;
    private Button recordButton;
    private ListAdapter listAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler(Looper.getMainLooper());
        final File filesDir = getFilesDir();
        if (!filesDir.exists() && !filesDir.mkdirs()) {
            throw new RuntimeException();
        }
        recordsDir = new File(filesDir, "records");
        if (!recordsDir.exists() && !recordsDir.mkdirs()) {
            throw new RuntimeException();
        }
        timerTextView = findViewById(R.id.textView);
        timerTextView.setText(Utils.formatDuration(0));
        recordButton = findViewById(R.id.button);
        recordButton.setText(R.string.StartRecording);
        recordButton.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        final File[] files = recordsDir.listFiles();
        if (files != null && files.length > 0) {
            final ArrayList<Record> list = new ArrayList<>();
            final SharedPreferences preferences = getSharedPreferences("records", MODE_PRIVATE);
            for (File file : files) {
                try {
                    final JSONObject json = new JSONObject(preferences.getString(file.getAbsolutePath(), null));
                    final String name = json.getString("name");
                    final int duration = json.getInt("duration");
                    final long dateTimeMillis = json.getLong("dateTimeMillis");
                    list.add(new Record(file, name, duration, dateTimeMillis));
                } catch (Exception ignored) {
                    if (file.exists() && !file.delete()) {
                        file.deleteOnExit();
                    }
                }
            }
            listAdapter = new ListAdapter(list);
        } else {
            listAdapter = new ListAdapter();
        }
        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(RecyclerView.VERTICAL);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerView.setAdapter(listAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
    }

    private void startRecording() {
        if (isRecording) {
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, 1);
            return;
        }
        try {
            File outputFile = new File(recordsDir, String.valueOf(System.currentTimeMillis()));
            isRecording = true;
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioChannels(Config.AUDIO_CHANNELS);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioSamplingRate(Config.AUDIO_BITRATE);
            mediaRecorder.setOutputFile(outputFile.getAbsolutePath());
            mediaRecorder.prepare();
            mediaRecorder.start();
            recordingRecord = new Record(outputFile,
                                         outputFile.getName(),
                                         0,
                                         System.currentTimeMillis());
            recordButton.setText(R.string.StopRecording);
            updateRecordTextViewRunnable = () -> {
                if (!isRecording) {
                    return;
                }
                final int duration = (int) (System.currentTimeMillis() - recordingRecord.getDateTimeMillis());
                timerTextView.setText(Utils.formatDuration(duration));
                handler.postDelayed(updateRecordTextViewRunnable, 1000);
            };
            handler.post(updateRecordTextViewRunnable);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e);
        }
    }

    private void stopRecording() {
        if (!isRecording) {
            return;
        }
        isRecording = false;
        try {
            mediaRecorder.stop();
            recordingRecord.setDuration((int) (System.currentTimeMillis() - recordingRecord.getDateTimeMillis()));
            final JSONObject json = new JSONObject();
            json.put("name", recordingRecord.getName());
            json.put("duration", recordingRecord.getDuration());
            json.put("dateTimeMillis", recordingRecord.getDateTimeMillis());
            final SharedPreferences preferences = getSharedPreferences("records", MODE_PRIVATE);
            preferences.edit().putString(recordingRecord.getFile().getAbsolutePath(), json.toString()).apply();
            listAdapter.addItem(recordingRecord);
            Log.d(TAG, "stopRecording: new record added");
        } catch (Exception ex) {
            Log.d(TAG, "stopRecording: failed", ex);
            if (!recordingRecord.getFile().delete()) {
                recordingRecord.getFile().deleteOnExit();
            }
        }
        mediaRecorder.release();
        handler.removeCallbacks(updateRecordTextViewRunnable);
        updateRecordTextViewRunnable = null;
        recordingRecord = null;
        timerTextView.setText(Utils.formatDuration(0));
        recordButton.setText(R.string.StartRecording);
    }

    private void playRecord(final Record record) {
        final Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("record", record);
        startActivity(intent);
    }

    private void deleteRecord(final Record item) {
        if (!item.getFile().delete()) {
            item.getFile().deleteOnExit();
        }
        final SharedPreferences preferences = getSharedPreferences("records", MODE_PRIVATE);
        preferences.edit().remove(item.getFile().getAbsolutePath()).apply();
        listAdapter.removeItem(item);
    }

    private class ListAdapter extends RecyclerView.Adapter<ListAdapter.Holder>
    {
        private class Holder extends RecyclerView.ViewHolder
        {
            final TextView nameTextView;
            final TextView durationTextView;
            final TextView dateTimeTextView;
            final TextView moreActionView;

            public Holder(@NonNull final View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.nameView);
                dateTimeTextView = itemView.findViewById(R.id.dateView);
                durationTextView = itemView.findViewById(R.id.durationView);
                moreActionView = itemView.findViewById(R.id.actionMore);
                moreActionView.setBackgroundResource(R.drawable.ic_baseline_more_vert_24);
                moreActionView.setFocusable(true);
                moreActionView.setClickable(true);
            }

            void set(final Record record) {
                nameTextView.setText(record.getName());
                dateTimeTextView.setText(Utils.formatDateTime(record.getDateTimeMillis()));
                durationTextView.setText(Utils.formatDuration(record.getDuration()));
                moreActionView.setOnClickListener(v -> {
                    CharSequence[] items = new CharSequence[] {
                            getString(R.string.Play),
                            getString(R.string.Delete)
                    };
                    AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext())
                            .setItems(items, (dialog, which) -> {
                                if (which == 0) {
                                    playRecord(record);
                                } else if (which == 1) {
                                    deleteRecord(record);
                                }
                            });
                    builder.create().show();
                });
                itemView.setOnClickListener(v -> playRecord(record));
            }
        }

        private final ArrayList<Record> itemList;


        ListAdapter() {
            this(new ArrayList<>());
        }

        ListAdapter(ArrayList<Record> list) {
            itemList = list;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
            final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            return new Holder(inflater.inflate(R.layout.view_record, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final Holder holder, final int position) {
            holder.set(itemList.get(position));
        }

        @Override
        public int getItemCount() {
            return itemList.size();
        }

        public void addItem(final Record item) {
            itemList.add(item);
            notifyItemInserted(itemList.size() - 1);
        }

        public void removeItem(final Record item) {
            final int position = itemList.indexOf(item);
            if (position != -1) {
                itemList.remove(position);
                notifyItemRemoved(position);
            }
        }
    }
}