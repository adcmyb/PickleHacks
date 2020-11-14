package com.example.picklehacksda;

import android.net.Uri;
import android.os.Bundle;
import android.os.health.SystemHealthManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.devbrackets.android.exomedia.BuildConfig;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class GameplayActivity extends AppCompatActivity {

    private VideoView videoView;
    private ProgressBar pbLoading;
    private boolean hasFocus = true;


    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private static final String TAG = "StreamingExample";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            YoutubeDL.getInstance().init(getApplication());
        } catch (YoutubeDLException e) {
            Log.e(TAG, "failed to initialize youtubedl-android", e);
        }

        setContentView(R.layout.activity_gameplay);

        initViews();
        initListeners();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) videoView.getLayoutParams();
        params.width = metrics.widthPixels;
        params.height = metrics.heightPixels;
        params.leftMargin = 0;
        videoView.setLayoutParams(params);

        startStream();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }


    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void initViews() {
        videoView = findViewById(R.id.video_view);
        pbLoading = findViewById(R.id.pb_status);
    }

    private void initListeners() {
        videoView.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared() {
                videoView.start();
            }
        });
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    private void startStream() {
        List<List<String>> advertisers = new ArrayList<>();
        Random rand = new Random();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("advertisers")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                advertisers.add((List<String>) document.getData().get("urls"));
                            }
                            int min = 0;
                            int max = advertisers.size() - 1;
                            int randNum = rand.nextInt(max - min + 1) + min;

                            min = 0;
                            max = advertisers.get(randNum).size() - 1;
                            int randNum2 = rand.nextInt(max - min + 1) + min;

                            String url = advertisers.get(randNum).get(randNum2);
                            System.out.println(url);
                            pbLoading.setVisibility(View.VISIBLE);
                            Disposable disposable = Observable.fromCallable(() -> {
                                YoutubeDLRequest request = new YoutubeDLRequest(url);
                                // best stream containing video+audio
                                request.addOption("-f", "best");
                                return YoutubeDL.getInstance().getInfo(request);
                            })
                                    .subscribeOn(Schedulers.newThread())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(streamInfo -> {
                                        pbLoading.setVisibility(View.GONE);
                                        String videoUrl = streamInfo.getUrl();
                                        if (TextUtils.isEmpty(videoUrl)) {
                                            Toast.makeText(GameplayActivity.this, "failed to get stream url", Toast.LENGTH_LONG).show();
                                        } else {
                                            setupVideoView(videoUrl);
                                        }
                                    }, e -> {
                                        if(BuildConfig.DEBUG) Log.e(TAG,  "failed to get stream info", e);
                                        pbLoading.setVisibility(View.GONE);
                                        Toast.makeText(GameplayActivity.this, "streaming failed. failed to get stream info", Toast.LENGTH_LONG).show();
                                    });
                            compositeDisposable.add(disposable);

                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });


//        String url = etUrl.getText().toString().trim();


    }

    private void setupVideoView(String videoUrl) {
        videoView.setVideoURI(Uri.parse(videoUrl));
    }
}
