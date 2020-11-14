package com.example.picklehacksda;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.devbrackets.android.exomedia.BuildConfig;
import com.devbrackets.android.exomedia.listener.OnPreparedListener;
import com.devbrackets.android.exomedia.ui.widget.VideoView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
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

public class MultiplayerActivity extends AppCompatActivity {

    private VideoView videoView;
    private ProgressBar pbLoading;
    private Button guess1, guess2, guess3, guess4;
    private TextView scoreView, scoreView2;
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
    private String gameid = "sdsdgdsdgsdggfdgs";
    private String uid;
    private long otherPlayerScore = 0;


    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private static final String TAG = "StreamingExample";

    public MultiplayerActivity() {
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

        setContentView(R.layout.activity_multiplayer);

        initViews();
        initListeners();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final String current_user_id = mAuth.getUid();

        db.collection("multiplayer").document("/"+ gameid + "/")
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                System.out.println(document.getData());
                                Log.d(TAG, "Document exists!");
                                Map<String, Object> game = new HashMap<>();
                                game.put("uid2", current_user_id);
                                game.put("uid2_score", 0);
                                game.put("uid1_score", 0);
                                uid = "uid2";
                                db.collection("multiplayer").document("/"+ gameid + "/")
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
                            } else {
                                Map<String, Object> game = new HashMap<>();
                                game.put("uid1", current_user_id);
                                game.put("uid2_score", 0);
                                game.put("uid1_score", 0);
                                uid = "uid1";
                                db.collection("multiplayer").document("/"+ gameid + "/")
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


                                Log.d(TAG, "Document does not exist!");
                            }
                            startStream();
                        } else {
                            Log.d(TAG, "Failed with: ", task.getException());
                        }
                    }
                });

//        DisplayMetrics metrics = new DisplayMetrics();
//        getWindowManager().getDefaultDisplay().getMetrics(metrics);
//        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) videoView.getLayoutParams();
//        params.width = metrics.widthPixels;
//        params.height = metrics.heightPixels;
//        params.leftMargin = 0;
//        videoView.setLayoutParams(params);
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
        scoreView2 = findViewById(R.id.score2);
    }

    private void addEndTime() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final String current_user_id = mAuth.getUid();

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();

//        Map<String, Object> game = new HashMap<>();
//        game.put(uid + "_end_" + numOfGames, dtf.format(now));
//
//        db.collection("multiplayer").document("/"+ gameid + "/")
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

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final String current_user_id = mAuth.getUid();

        db.collection("multiplayer").document("/"+ gameid + "/")
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                System.out.println(document.getData());
                                Log.d(TAG, "Document exists!");
                                if (uid.equals("uid1")) {
                                    otherPlayerScore = (Long) document.getData().get("uid2_score");
                                } else {
                                    otherPlayerScore = (Long) document.getData().get("uid1_score");
                                }
                            } else {
                                Log.d(TAG, "Document does not exist!");
                            }
                        } else {
                            Log.d(TAG, "Failed with: ", task.getException());
                        }
                        scoreView2.setText(Long.toString(otherPlayerScore));
                    }
                });

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
                score += 100 + 100 / total.getSeconds();
                scoreView.setText(Integer.toString(score));
            }
            Map<String, Object> game = new HashMap<>();
//            game.put(uid + "_total_" + numOfGames, total.getSeconds());
            runningTotal += total.getSeconds();
//            game.put(uid + "_total", runningTotal);
            game.put(uid + "_score", score);

            db.collection("multiplayer").document("/"+ gameid + "/")
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
//        game.put(uid + "_start_" + numOfGames, dtf.format(start));
//
//        db.collection("multiplayer").document("/"+ gameid + "/")
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
                                            Toast.makeText(MultiplayerActivity.this, "failed to get stream url", Toast.LENGTH_LONG).show();
                                        } else {
                                            setupVideoView(videoUrl);
                                        }
                                    }, e -> {
                                        if(BuildConfig.DEBUG) Log.e(TAG,  "failed to get stream info", e);
                                        pbLoading.setVisibility(View.GONE);
                                        if (numOfGames == 1) {
                                            numOfGames--;
                                        }
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
