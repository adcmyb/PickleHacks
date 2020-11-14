package com.example.picklehacksda;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

public class ScoreMultiplayerActivity extends AppCompatActivity {

    private static final String TAG = "ScoreMultilplayer";
    private TextView scoreView;
    private TextView scoreView2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int score = getIntent().getIntExtra("SCORE", 0);
        String gameid = getIntent().getStringExtra("GAMEID");
        String uid = getIntent().getStringExtra("UID");
        setContentView(R.layout.activity_score_multiplayer);
        scoreView = findViewById(R.id.score);
        scoreView2 = findViewById(R.id.score2);
        scoreView.setText(Integer.toString(score));

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        final String current_user_id = mAuth.getUid();

        db.collection("multiplayer").document("/"+ gameid + "/")
        .addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot snapshot, @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e);
                    return;
                }

                if (snapshot != null && snapshot.exists()) {
                    System.out.println("Current data: " + snapshot.getData());
                        if (uid.equals("uid1")) {
                            if (snapshot.getBoolean("uid2_done")) {
                                scoreView2.setText(Long.toString(snapshot.getLong("uid2_score")));
                            }
                        }
                        else {
                            if (snapshot.getBoolean("uid1_done")) {
                                scoreView2.setText(Long.toString(snapshot.getLong("uid1_score")));
                            }
                        }
                } else {
                    Log.d(TAG, "Current data: null");
                }
            }
        });
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


}
