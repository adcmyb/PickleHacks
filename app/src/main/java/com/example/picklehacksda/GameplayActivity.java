package com.example.picklehacksda;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.FlingAnimation;

import com.devbrackets.android.exomedia.BuildConfig;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLException;
import com.yausername.youtubedl_android.YoutubeDLRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import nl.dionsegijn.konfetti.KonfettiView;

public class GameplayActivity extends AppCompatActivity {

    private VideoView videoView;
    private ProgressBar pbLoading;
    private Button guess1, guess2, guess3, guess4;
    private TextView scoreView;
    private boolean hasFocus = true;
    private String guess;
    private KonfettiView konfettiView;
    private String uniqueID;
    private int numOfGames;
    private LocalDateTime createTime;
    private View root;
    private LocalDateTime start;
    private LocalDateTime end;
    private int runningTotal = 0;
    private int score = 0;
    private Duration total = null;
    private boolean correct = false;
    private RelativeLayout relativeLayout;


    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private static final String TAG = "StreamingExample";

    public GameplayActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        createTime = LocalDateTime.now();
        super.onCreate(savedInstanceState);
        numOfGames = 0;
        uniqueID = UUID.randomUUID().toString();

        try {
            YoutubeDL.getInstance().init(getApplication());
        } catch (YoutubeDLException e) {
            Log.e(TAG, "failed to initialize youtubedl-android", e);
        }

        setContentView(R.layout.activity_gameplay);

        initViews();
        initListeners();
        Context context = getApplicationContext();
        float px = 100 * ((float) context.getResources().getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);

        System.out.println(px);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) videoView.getLayoutParams();
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
        guess1 = findViewById(R.id.guess1);
        guess2 = findViewById(R.id.guess2);
        guess3 = findViewById(R.id.guess3);
        guess4 = findViewById(R.id.guess4);
        konfettiView = findViewById(R.id.viewKonfetti);
        root = videoView.getRootView();
        scoreView = findViewById(R.id.score);
        relativeLayout = findViewById(R.id.relativeLayout);
    }

    private void addEndTime() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final String current_user_id = mAuth.getUid();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

        Map<String, Object> game = new HashMap<>();
        game.put("end_" + numOfGames, dtf.format(now));

        db.collection("users").document(current_user_id + "/games/" + dtf.format(createTime) + "/")
                .set(game, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        System.out.println("DocumentSnapshot added with ID: " + current_user_id);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("Error adding document" + e);
                    }
                });
    }

    private void initListeners() {
        guess1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (guess1.getText().toString().equals(guess)) {
                    videoView.stopPlayback();
                    root.setBackgroundColor(getResources().getColor(R.color.green));
                    correct = true;
//                    konfettiView.build()
//                            .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
//                            .setDirection(0.0, 359.0)
//                            .setSpeed(1f, 5f)
//                            .setFadeOutEnabled(true)
//                            .setTimeToLive(2000L)
//                            .addShapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
//                            .addSizes(new Size(12, 5f))
//                            .setPosition(-50f, konfettiView.getWidth() + 50f, -50f, -50f)
//                            .streamFor(300, 5000L);

                }
                else {
                    correct = false;
                    root.setBackgroundColor(getResources().getColor(R.color.red));
                }
                end = LocalDateTime.now();
                startStream();
            }
        });

        guess2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (guess2.getText().toString().equals(guess)) {
                    correct = true;
                    videoView.stopPlayback();
                    root.setBackgroundColor(getResources().getColor(R.color.green));
//                    konfettiView.build()
//                            .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
//                            .setDirection(0.0, 359.0)
//                            .setSpeed(1f, 5f)
//                            .setFadeOutEnabled(true)
//                            .setTimeToLive(2000L)
//                            .addShapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
//                            .addSizes(new Size(12, 5f))
//                            .setPosition(-50f, konfettiView.getWidth() + 50f, -50f, -50f)
//                            .streamFor(300, 5000L);
                }
                else {
                    correct = false;
                    root.setBackgroundColor(getResources().getColor(R.color.red));
                }
                end = LocalDateTime.now();
                startStream();
            }
        });

        guess3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (guess3.getText().toString().equals(guess)) {
                    correct = true;
                    videoView.stopPlayback();
                    root.setBackgroundColor(getResources().getColor(R.color.green));
//                    konfettiView.build()
//                            .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
//                            .setDirection(0.0, 359.0)
//                            .setSpeed(1f, 5f)
//                            .setFadeOutEnabled(true)
//                            .setTimeToLive(2000L)
//                            .addShapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
//                            .addSizes(new Size(12, 5f))
//                            .setPosition(-50f, konfettiView.getWidth() + 50f, -50f, -50f)
//                            .streamFor(300, 5000L);
                }
                else {
                    correct = false;
                    root.setBackgroundColor(getResources().getColor(R.color.red));
                }
                end = LocalDateTime.now();
                startStream();
            }
        });

        guess4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (guess4.getText().toString().equals(guess)) {
                    correct = true;
                    videoView.stopPlayback();
                    root.setBackgroundColor(getResources().getColor(R.color.green));
//                    konfettiView.build()
//                            .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
//                            .setDirection(0.0, 359.0)
//                            .setSpeed(1f, 5f)
//                            .setFadeOutEnabled(true)
//                            .setTimeToLive(2000L)
//                            .addShapes(Shape.Square.INSTANCE, Shape.Circle.INSTANCE)
//                            .addSizes(new Size(12, 5f))
//                            .setPosition(-50f, konfettiView.getWidth() + 50f, -50f, -50f)
//                            .streamFor(300, 5000L);
                }
                else {
                    correct = false;
                    root.setBackgroundColor(getResources().getColor(R.color.red));
                }
                end = LocalDateTime.now();
                startStream();

            }
        });

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

        if (numOfGames >= 10) {
            Intent intent = new Intent(this, ScoreSingleplayerActivity.class);
            intent.putExtra("SCORE", score);
            startActivity(intent);
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final String current_user_id = mAuth.getUid();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");

        if (numOfGames >= 1) {
            ObjectAnimator animation = ObjectAnimator.ofFloat(videoView, "translationY", -3000f);
            animation.setDuration(1000);
            ObjectAnimator reset = ObjectAnimator.ofFloat(videoView, "translationY", 0);
            reset.setDuration(0);
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.play(animation).before(reset);
            animatorSet.start();
            total = Duration.between(start, end);
            if (correct) {
                if (total.getSeconds() == 0) {
                    score += 100 + 100 / 1;
                }
                else {
                    score += 100 + 100 / total.getSeconds();
                }
                scoreView.setText(Integer.toString(score));
            }
            Map<String, Object> game = new HashMap<>();
//            game.put("total_" + numOfGames, total.getSeconds());
            runningTotal += total.getSeconds();
//            game.put("total", runningTotal);
            game.put("score", score);

            db.collection("users").document(current_user_id + "/games/" + dtf.format(createTime) + "/")
                    .set(game, SetOptions.merge())
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            System.out.println("DocumentSnapshot added with ID: " + current_user_id);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            System.out.println("Error adding document" + e);
                        }
                    });
        }

        start = LocalDateTime.now();
        numOfGames++;

        List<List<String>> urls = new ArrayList<>();
        List<String> guesses = new ArrayList<>();
        Random rand = new Random();

//        Map<String, Object> game = new HashMap<>();
//        game.put("start_" + numOfGames, dtf.format(start));
//
//        db.collection("users").document(current_user_id + "/games/" + dtf.format(createTime) + "/")
//                .set(game, SetOptions.merge())
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        System.out.println("DocumentSnapshot added with ID: " + current_user_id);
//                    }
//                })
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        System.out.println("Error adding document" + e);
//                    }
//                });

        int min = 0;
        int max = 3;
        int randomButton = rand.nextInt(max - min + 1) + min;

        db.collection("advertisers")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                urls.add((List<String>) document.getData().get("urls"));
                                guesses.add(document.getData().get("guess").toString());
                            }
                            int min = 0;
                            int max = urls.size() - 1;
                            int randAd = rand.nextInt(max - min + 1) + min;
                            int wrong1 = rand.nextInt(max - min + 1) + min;
                            int wrong2 = rand.nextInt(max - min + 1) + min;
                            int wrong3 = rand.nextInt(max - min + 1) + min;

                            while (wrong1 == randAd || wrong2 == randAd || wrong3 == randAd || wrong1 == wrong2 || wrong1 == wrong3 || wrong2 == wrong3) {
                                wrong1 = rand.nextInt(max - min + 1) + min;
                                wrong2 = rand.nextInt(max - min + 1) + min;
                                wrong3 = rand.nextInt(max - min + 1) + min;
                            }

                            Button[] buttons = {guess1, guess2, guess3, guess4};
                            String[] wrongGuesses = {guesses.get(wrong1), guesses.get(wrong2), guesses.get(wrong3)};
                            guess = guesses.get(randAd);

                            int j = 0;
                            for (int i = 0; i < 4; i++) {
                                if (randomButton != i) {
                                    buttons[i].setText(wrongGuesses[j]);
                                    j++;
                                }
                            }

                            buttons[randomButton].setText(guess);

                            min = 0;
                            max = urls.get(randAd).size() - 1;
                            int randUrl = rand.nextInt(max - min + 1) + min;

                            String url = urls.get(randAd).get(randUrl);
                            System.out.println(url);
                            pbLoading.setVisibility(View.VISIBLE);
                            root.setBackgroundColor(getResources().getColor(R.color.black));
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
                                        if (numOfGames == 1) {
                                            numOfGames--;
                                        }
                                        numOfGames--;
                                        correct = false;
                                        startStream();
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
