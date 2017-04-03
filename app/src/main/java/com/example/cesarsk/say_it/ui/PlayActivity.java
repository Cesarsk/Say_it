package com.example.cesarsk.say_it.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.design.widget.Snackbar;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.cesarsk.say_it.R;
import com.example.cesarsk.say_it.utility.ShowTimer;
import com.example.cesarsk.say_it.utility.UtilityRecordings;
import com.example.cesarsk.say_it.utility.Utility;
import com.example.cesarsk.say_it.utility.UtilitySharedPrefs;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence;
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig;

import static android.speech.tts.TextToSpeech.QUEUE_FLUSH;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

public class PlayActivity extends AppCompatActivity {
    public final static String PLAY_WORD = "com.example.cesarsk.say_it.WORD";
    public final static String PLAY_IPA = "com.example.cesarsk.say_it.IPA";
    public static final long UNDO_TIMEOUT = 3000;
    private static final String AUDIO_RECORDER_FILE_EXT_AAC = ".aac";
    private static final String AUDIO_RECORDER_FOLDER = "Say it";
    private MediaRecorder recorder = null;
    private ShowTimer timer;
    private int currentFormat = 0;
    private int output_formats[] = {MediaRecorder.OutputFormat.DEFAULT};
    private String file_exts[] = {AUDIO_RECORDER_FILE_EXT_AAC};
    public static final int RequestPermissionCode = 1;
    MediaPlayer mediaPlayer;
    int N = 10;
    private CharSequence[] history;
    int testa = 0;
    public static String selected_word;
    public static String selected_ipa;
    private boolean slow_mode = false;
    private boolean accent_flag = false;
    private boolean favorite_flag = false;
    Context context = this;
    final int durationMillis = 500;
    AlphaAnimation delete_button_anim, delete_button_anim_reverse;
    Snackbar snackbar;
    Handler handler = new Handler();
    Runnable pendingRemovalRunnable;
    byte[] temp_recording_bytes;
    private boolean isRecording = false;
    CountDownTimer countDownTimer;
    CountDownTimer minDurationTimer;
    private boolean isMinimumDurationReached = false;
    Button multibutton;
    Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);

        Intent intent = getIntent();
        Bundle args = intent.getExtras();

        multibutton = (Button) findViewById(R.id.recplay_button);
        final TextView selected_word_view = (TextView) findViewById(R.id.selected_word);
        final TextView selected_ipa_view = (TextView) findViewById(R.id.selected_word_ipa);
        final ImageButton delete_button = (ImageButton) findViewById(R.id.delete_button);
        final ImageButton favorite_button = (ImageButton) findViewById(R.id.favorite_button);
        final ImageButton slow_button = (ImageButton) findViewById(R.id.slow_button);
        final ImageButton accent_button = (ImageButton) findViewById(R.id.accent_button);
        final Button play_original_button = (Button) findViewById(R.id.play_original);
        final ImageButton your_recordings = (ImageButton) findViewById(R.id.recordings_button);
        final ImageButton remove_ad = (ImageButton) findViewById(R.id.remove_ads_button);
        final ImageButton search_meaning = (ImageButton) findViewById(R.id.search_meaning_button);
        final TextView timerTextView = (TextView) findViewById(R.id.recordingTimer);
        vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        selected_word = args.getString(PLAY_WORD);
        selected_ipa = args.getString(PLAY_IPA);


        if (MainActivity.DEFAULT_ACCENT.equals("0")) {
            accent_button.setColorFilter(getResources().getColor(R.color.primary_light));
            accent_flag = false;
        } else if (MainActivity.DEFAULT_ACCENT.equals("1")) {
            accent_button.setColorFilter(getResources().getColor(R.color.Yellow600));
            accent_flag = true;
        }

        final Chronometer chronometer = (Chronometer) findViewById(R.id.recording_timer);
        chronometer.setBase(SystemClock.elapsedRealtime());

        timer = new ShowTimer(timerTextView);
        recorder = new MediaRecorder();
        mediaPlayer = new MediaPlayer();
        history = new CharSequence[N];

        delete_button_anim = new AlphaAnimation(1.0f, 0.0f);
        delete_button_anim_reverse = new AlphaAnimation(0.0f, 1.0f);
        delete_button_anim.setDuration(500);
        delete_button_anim_reverse.setDuration(500);
        delete_button_anim_reverse.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                delete_button.setEnabled(true);
                delete_button.setVisibility(VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        delete_button_anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                delete_button.setEnabled(false);
                delete_button.setVisibility(INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        Typeface plainItalic = Typeface.createFromAsset(getAssets(), "fonts/GentiumPlus-I.ttf");
        Typeface plainRegular = Typeface.createFromAsset(getAssets(), "fonts/GentiumPlus-R.ttf");
        selected_word_view.setTypeface(plainRegular);
        selected_ipa_view.setTypeface(plainItalic);
        selected_word_view.setText(selected_word);
        selected_ipa_view.setText(selected_ipa);

        final View.OnClickListener play_listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mediaPlayer.isPlaying()) {
                    UtilityRecordings.playRecording(context, mediaPlayer, selected_word + ".aac");
                    multibutton.setBackground(getDrawable(R.drawable.circle_green_pressed));
                    vibrator.vibrate(50);
                    Log.i("SAY IT!", "" + mediaPlayer.getDuration());
                    new CountDownTimer(mediaPlayer.getDuration(), 1000) {
                        @Override
                        public void onTick(long millisUntilFinished) {

                        }

                        @Override
                        public void onFinish() {
                            multibutton.setBackground(getDrawable(R.drawable.circle_green));
                        }
                    }.start();
                }
            }
        };

        final GestureDetector gestureDetector = new GestureDetector(context, new SimpleTapListener());

        final View.OnTouchListener rec_listener = new View.OnTouchListener() {

            @Override
            public boolean onTouch(View view, MotionEvent event) {

                if(gestureDetector.onTouchEvent(event)){
                    if (!mediaPlayer.isPlaying()) {
                        UtilityRecordings.playRecording(context, mediaPlayer, selected_word + ".aac");
                        multibutton.setBackground(getDrawable(R.drawable.circle_green_pressed));
                        vibrator.vibrate(50);
                        Log.i("SAY IT!", "" + mediaPlayer.getDuration());

                    }

                    return true;
                }

                else {
                    if (UtilityRecordings.checkRecordAudioPermissions(view.getContext())) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                Log.i("Say it!", "Start Recording");

                            /*if (countDownTimer != null) {
                                countDownTimer.cancel();
                                countDownTimer.start();
                            }

                            if (minDurationTimer != null) {
                                isMinimumDurationReached = false;
                                minDurationTimer.cancel();
                                minDurationTimer.start();
                            }*/
                                isRecording = true;
                                vibrator.vibrate(50);

                                multibutton.setBackground(getDrawable(R.drawable.circle_red_pressed));
                                //timer.startTimer();
                                chronometer.setBase(SystemClock.elapsedRealtime());
                                chronometer.start();
                                UtilityRecordings.startRecording(context, recorder, selected_word);
                                return true;

                            case MotionEvent.ACTION_UP:
                                Log.i("Say it!", "Stop Recording");
                            /*timer.stopTimer();
                            if (countDownTimer != null) {
                                countDownTimer.cancel();
                            }

                            if (minDurationTimer != null) {
                                minDurationTimer.cancel();
                            }*/

                                isRecording = false;
                                multibutton.setBackground(getDrawable(R.drawable.circle_red));
                                chronometer.stop();

                                if (UtilityRecordings.stopRecording(context, recorder, selected_word)) {

                                    multibutton.setBackground(getDrawable(R.drawable.circle_color_anim_red_to_green));
                                    delete_button.startAnimation(delete_button_anim_reverse);
                                    multibutton.setOnTouchListener(null);
                                    multibutton.setOnClickListener(play_listener);
                                    TransitionDrawable transition = (TransitionDrawable) multibutton.getBackground();
                                    transition.startTransition(durationMillis);

                                    String counted_time = chronometer.getText().toString();

                                    //TODO da racchiudere in un metodo "checkDuration"
                                    String[] time_units = counted_time.split(":");
                                    int seconds = Integer.parseInt(time_units[1]);
                                    if (seconds < 1) {
                                        Toast.makeText(context, "Minimum not reached!", Toast.LENGTH_SHORT).show();
                                        UtilityRecordings.deleteRecording(context, selected_word + ".aac");
                                    }
                                    return true;

                                /*if (isMinimumDurationReached) {
                                    multibutton.setBackground(getDrawable(R.drawable.circle_color_anim_red_to_green));
                                    delete_button.startAnimation(delete_button_anim_reverse);
                                    multibutton.setOnTouchListener(null);
                                    multibutton.setOnClickListener(play_listener);
                                    TransitionDrawable transition = (TransitionDrawable) multibutton.getBackground();
                                    transition.startTransition(durationMillis);
                                    isMinimumDurationReached = false;
                                    return true;
                                } else {
                                    vibrator.vibrate(50);
                                    Toast.makeText(context, "Minimum duration not reached.", Toast.LENGTH_SHORT).show();
                                    timer.clearTimer();
                                    String filename = context.getFilesDir().getAbsolutePath() + "/" + selected_word + ".aac";
                                    UtilityRecordings.deleteRecording(context, new File(filename));
                                    return true;
                                }*/
                                }
                        }
                    } else {
                        UtilityRecordings.requestRecordAudioPermissions(view.getContext());
                        return false;
                    }
                }
                return false;
            }
        };

        minDurationTimer = new CountDownTimer(1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {

            }

            @Override
            public void onFinish() {
                isMinimumDurationReached = true;
            }
        };

        countDownTimer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                //mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
            }

            public void onFinish() {
                if (isRecording) {
                    timer.stopTimer();
                    Toast.makeText(context, "Maximum length duration reached.", Toast.LENGTH_SHORT).show();
                    vibrator.vibrate(50);
                    multibutton.setBackground(getDrawable(R.drawable.circle_red));
                    if (UtilityRecordings.stopRecording(context, recorder, selected_word)) {
                        multibutton.setBackground(getDrawable(R.drawable.circle_color_anim_red_to_green));
                        delete_button.startAnimation(delete_button_anim_reverse);
                        multibutton.setOnTouchListener(null);
                        multibutton.setOnClickListener(play_listener);
                        TransitionDrawable transition = (TransitionDrawable) multibutton.getBackground();
                        transition.startTransition(durationMillis);
                        vibrator.vibrate(50);
                        return;
                    }
                    return;
                }
            }
        };

        if (UtilityRecordings.checkRecordingFile(context, selected_word)) {
            multibutton.setBackground(getResources().getDrawable(R.drawable.circle_color_anim_green_to_red, null));
            multibutton.setOnClickListener(play_listener);
            int millis = UtilityRecordings.getRecordingDuration(context, mediaPlayer, selected_word);
            SimpleDateFormat formatter = new SimpleDateFormat("ss:SSS", Locale.UK);
            Date date = new Date(millis);
            String result = formatter.format(date);
            timerTextView.setText(result);
            delete_button.startAnimation(delete_button_anim_reverse);
        } else {
            multibutton.setBackground(getResources().getDrawable(R.drawable.circle_red, null));
            multibutton.setOnTouchListener(rec_listener);
            delete_button.setEnabled(false);
            delete_button.setVisibility(INVISIBLE);
        }

        remove_ad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        search_meaning.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utility.searchMeaning(context, selected_word);
            }
        });

        //Gestione Snackbar + UNDO
        snackbar = Snackbar.make(findViewById(R.id.play_activity_coordinator), "Deleted Recording", (int) UNDO_TIMEOUT);
        final Context context = this;

        snackbar.setAction("UNDO", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //handler.removeCallbacks(pendingRemovalRunnable);
                File recovered_file = new File(UtilityRecordings.RECORDINGS_PATH + selected_word + ".aac");
                FileOutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(recovered_file);
                    outputStream.write(temp_recording_bytes);
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //UtilitySharedPrefs.addRecording(context, recovered_file.getAbsolutePath());
                timer.setTimer(timer.getOld_time());
                delete_button.startAnimation(delete_button_anim_reverse);
                multibutton.setOnTouchListener(null);
                multibutton.setOnClickListener(play_listener);
                multibutton.setBackground(getDrawable(R.drawable.circle_color_anim_red_to_green));
                TransitionDrawable transition = (TransitionDrawable) multibutton.getBackground();
                transition.startTransition(durationMillis);
            }
        });

        //Gestione AD (TEST AD)
        MobileAds.initialize(getApplicationContext(), "ca-app-pub-3940256099942544/6300978111");
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        your_recordings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent main_activity_intent = new Intent(v.getContext(), MainActivity.class);
                Bundle b = new Bundle();
                b.putInt("fragment_index", 3); //Your id
                main_activity_intent.putExtras(b); //Put your id to your next Intent
                main_activity_intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                v.getContext().startActivity(main_activity_intent);
                finish(); //distruggiamo il play activity relativo alla parola
            }
        });

        delete_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                timer.clearTimer();
                delete_button.startAnimation(delete_button_anim);
                multibutton.setOnTouchListener(rec_listener);
                multibutton.setBackground(getDrawable(R.drawable.circle_color_anim_green_to_red));
                TransitionDrawable transition = (TransitionDrawable) multibutton.getBackground();
                transition.startTransition(durationMillis);
                String filename = context.getFilesDir().getAbsolutePath() + selected_word + ".aac";
                File recording_file = new File(filename);
                temp_recording_bytes = UtilityRecordings.getRecordingBytesfromFile(recording_file);
                //UtilityRecordings.deleteRecording(context, selected_word);
                UtilityRecordings.deleteRecording(context, filename);
                snackbar.show();
            }
        });

        favorite_flag = UtilitySharedPrefs.checkFavs(this, selected_word);
        if (favorite_flag)
            favorite_button.setColorFilter(getResources().getColor(R.color.RudolphsNose));

        favorite_button.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                if (!favorite_flag) {
                    UtilitySharedPrefs.addFavs(v.getContext(), new Pair<>(selected_word, selected_ipa));
                    favorite_flag = !favorite_flag;
                    Toast.makeText(PlayActivity.this, "Added to favorites!", Toast.LENGTH_SHORT).show();
                    favorite_button.setColorFilter(getResources().getColor(R.color.RudolphsNose));
                } else {
                    favorite_button.setColorFilter(getResources().getColor(R.color.primary_light));
                    Toast.makeText(PlayActivity.this, "Removed from favorites!", Toast.LENGTH_SHORT).show();
                    UtilitySharedPrefs.removeFavs(v.getContext(), new Pair<>(selected_word, selected_ipa));
                    favorite_flag = !favorite_flag;
                }
            }
        });

        slow_button.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                if (!slow_mode) {
                    MainActivity.american_speaker_google.setSpeechRate((float) 0.30);
                    MainActivity.british_speaker_google.setSpeechRate((float) 0.30);
                    slow_mode = !slow_mode;
                    Toast.makeText(PlayActivity.this, "Slow Mode Activated", Toast.LENGTH_SHORT).show();
                    slow_button.setColorFilter(getResources().getColor(R.color.Yellow600));
                } else {
                    MainActivity.american_speaker_google.setSpeechRate((float) 0.90);
                    MainActivity.british_speaker_google.setSpeechRate((float) 0.90);
                    Toast.makeText(PlayActivity.this, "Slow Mode Deactivated", Toast.LENGTH_SHORT).show();
                    slow_button.setColorFilter(getResources().getColor(R.color.primary_light));
                    slow_mode = !slow_mode;
                }
            }
        });

        accent_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!accent_flag) {
                    accent_button.setColorFilter(getResources().getColor(R.color.Yellow600));
                    Toast.makeText(PlayActivity.this, "British Accent selected", Toast.LENGTH_SHORT).show();
                    accent_flag = !accent_flag;
                } else {
                    accent_button.setColorFilter(getResources().getColor(R.color.primary_light));
                    Toast.makeText(PlayActivity.this, "American English selected", Toast.LENGTH_SHORT).show();
                    accent_flag = !accent_flag;
                }
            }

        });

        play_original_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!accent_flag) {
                    MainActivity.american_speaker_google.speak(selected_word, QUEUE_FLUSH, null, null);
                    vibrator.vibrate(50);
                } else if (accent_flag) {
                    MainActivity.british_speaker_google.speak(selected_word, QUEUE_FLUSH, null, null);
                    vibrator.vibrate(50);
                }
            }
        });

        startTutorialPlayActivity(multibutton, play_original_button, accent_button, slow_button);
    }

    private void startTutorialPlayActivity(Button multibutton, Button play_original_button, ImageButton accent_button, ImageButton slow_button) {
        ShowcaseConfig config = new ShowcaseConfig();
        config.setDelay(50); // 50ms between each showcase views
        config.setShapePadding(15);
        config.setRenderOverNavigationBar(true);
        MaterialShowcaseSequence sequence = new MaterialShowcaseSequence(this, "utente");
        sequence.setConfig(config);

        sequence.addSequenceItem(multibutton,
                "HOLD this button to record a word. Release and PRESS it to listen to your recording.", "[DISMISS]");
        sequence.addSequenceItem(play_original_button,
                "PRESS this button to listen to the original pronunciation.", Utility.underlineText("[GO AWAY!]").toString());
        sequence.addSequenceItem(accent_button,
                "Want to switch accent? PRESS this one!", Utility.underlineText("[SERIOUSLY, I'VE GOT THIS!]").toString());
        sequence.addSequenceItem(slow_button,
                "Okay... this is the last one. If you want to slower the word, use the SLOW BUTTON.", Utility.underlineText("[...I will]").toString());
        sequence.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionCode:
                if (grantResults.length > 0) {
                    boolean RecordPermission = grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED;

                    if (RecordPermission) {
                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show();
                    }
                }
                break;
        }
    }

    private class SimpleTapListener extends GestureDetector.SimpleOnGestureListener{
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return true;
        }
    }
}