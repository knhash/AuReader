package obsidian.aureader;

import android.app.Activity;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.epub.EpubReader;
import synesketch.emotion.Emotion;
import synesketch.emotion.EmotionalState;
import synesketch.emotion.Empathyscope;

public class ReaderActivity extends Activity {

    StringBuilder bookString=new StringBuilder();
    long spinesRead=0;

    Book book;
    Spine spine;

    TextSwitcher textSwitcher;
    Button nextButton;

    int screenWidth, screenHeight, maxLineCount, lineHeight;
    float densityMultiplier;
    int curStart=0, curEnd=0;

    //ALLOW USER TO CHANGE
    float textSize=20;

    long thresholdRead=5000;

    Paint paint;

    private EmotionalState state;
    private int emotionType;
    String debugTag="he was a hero";
    int lastEmotion=-1;

    //Music of stuff
    public MediaPlayer player;
    AudioManager audioManager;
    int[] names;
    boolean fadeOutMusic=false;

    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_activity);

        player = new MediaPlayer();
        audioManager = (AudioManager) getSystemService(this.AUDIO_SERVICE);
        names = new int[]{R.raw.peace, R.raw.peace1,              //0
                R.raw.anger, R.raw.anger1, R.raw.anger2,        //2
                R.raw.creepy, R.raw.creepy1, R.raw.creepy2,      //5
                R.raw.happy, R.raw.happy1, R.raw.happy2,         //8
                R.raw.sad, R.raw.sad1, R.raw.sad2,              //11
                R.raw.suspense, R.raw.suspense1};             //14

        initBook();
        initUI();
        readNextLines(thresholdRead);
        showNextPage();

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((bookString.toString().length() - curEnd) < thresholdRead / 5) {
                    readNextLines(thresholdRead);
                    Log.i("lalalalalala", "READING NEXT LINES!!");
                }
                showNextPage();
                fadeOutMusic = true;
                detectEmotion();
                playMusic(emotionType);
            }
        });
    }

    @Override
    protected void onPause() {
        player.stop();
        player.release();
        super.onPause();
    }

    public void fade(final int duration) {
        final float deviceVolume = getDeviceVolume();
        final Handler h = new Handler();
        h.postDelayed(new Runnable() {
            private float time = 0.0f;
            private float volume = 0.0f;
            private float timeend = duration;
            boolean startedTrack = false;
            boolean finishedTrack = false;

            @Override
            public void run() {
                try {
                    //if(!finishedTrack) {
                    if (player.getCurrentPosition() < 4000) {
                        if (!startedTrack) {
                            player.start();
                            Log.i("mad potato", "!startedtrack");
                            startedTrack = true;
                        }
                        Log.i("mad potato", "first 5 sec");
                        time += 50;
                        volume = (deviceVolume * time) / duration;
                        player.setVolume(volume, volume);
                        if (time < duration)
                            h.postDelayed(this, 100);
                    }
                    //}

                    else if (((player.getDuration() - player.getCurrentPosition()) < 4000) || (fadeOutMusic)) {
                        Log.i("mad potato", "last 5 sec");
                        fadeOutMusic = false;
                        timeend -= 50;
                        volume = (deviceVolume * timeend) / duration;
                        player.setVolume(volume, volume);
                        Log.i("mad potato", "tenD: " + timeend);
                        if (timeend > 0) {
                            h.postDelayed(this, 100);
                        } else {
                            finishedTrack = true;
                            player.stop();
                            if (fadeOutMusic)
                                player.release();
                            Log.i("mad potato", "stopped, released");
                        }
                    } else if (!finishedTrack) {
                        Log.i("mad potato", "last else");
                        h.postDelayed(this, 100);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 100);
    }

    public float getDeviceVolume() {
        int volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int maxLevel = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return (float) volumeLevel/maxLevel;
    }

    private void fadeOut(final int duration) {
        //final Handler handler=new Handler();
        handler=new Handler();
        handler.postDelayed(new Runnable() {

            private float volume=0.0f;
            private float timeEnd=duration;
            private float deviceVolume=getDeviceVolume();

            @Override
            public void run() {
                timeEnd=-50;
                volume = (deviceVolume * timeEnd) / duration;
                player.setVolume(volume, volume);

                if (timeEnd > 0) {
                    handler.postDelayed(this, 100);
                } else {
                    player.stop();
                    //player.release();
                }
            }
        }, 100);
    }

    private void playMusic(final int emoId) {

        if(lastEmotion!=emoId) {

            lastEmotion=emoId;

            if(player.isPlaying()) {
                player.stop();
                //fadeOut(2500);
                //player.release();
            }

            switch (emoId) {
                case Emotion.NEUTRAL:
                    Log.i(debugTag, "NEUTRAL");
                    player = MediaPlayer.create(getApplicationContext(), names[0]);
                    break;

                case Emotion.ANGER:
                    Log.i(debugTag, "ANGER");
                    player = MediaPlayer.create(getApplicationContext(), names[2]);
                    break;

                case Emotion.DISGUST:
                case Emotion.FEAR:
                    Log.i(debugTag, "FEAR");
                    player = MediaPlayer.create(getApplicationContext(), names[5]);
                    break;

                case Emotion.HAPPINESS:
                    Log.i(debugTag, "HAPPINESS");
                    player = MediaPlayer.create(getApplicationContext(), names[8]);
                    break;

                case Emotion.SADNESS:
                    Log.i(debugTag, "SADNESS");
                    player = MediaPlayer.create(getApplicationContext(), names[11]);
                    break;

                case Emotion.SURPRISE:
                    Log.i(debugTag, "SURPRISE");
                    player = MediaPlayer.create(getApplicationContext(), names[14]);
                    break;

            }

            fade(2500);
        }
    }

    private void detectEmotion() {

        try {
            state= Empathyscope.getInstance().feel(bookString.substring((int)curStart, (int)curEnd));
        } catch (IOException e) {
            e.printStackTrace();
        }

        emotionType=state.getStrongestEmotion().getType();
    }

    private void showNextPage() {

        int numChars=0;

        for(int k=0; k<maxLineCount; ++k) {
            numChars+=paint.breakText(bookString.toString().substring((int)(curEnd+numChars), bookString.length()), true, screenWidth, null);
            if(spinesRead>0)
                for(;bookString.toString().charAt(numChars)!=' '/*&&(numChars>0)*/;--numChars);
            Log.i("blip bleep", "numChars "+k+": "+numChars);
        }

        curStart=curEnd;
        curEnd+=numChars;

        Log.i("blip bleep", "curStart: " +curStart);
        Log.i("blip bleep", "curEnd: " +curEnd);

        textSwitcher.setText(bookString.substring((int)curStart, (int)curEnd));
        Log.i("man went ", "substring: " + bookString.substring((int) curStart, (int) curEnd));
    }

    private void readNextLines(long minReadSize) {

        int initSize=bookString.length();

        while(spinesRead<spine.getSpineReferences().size()) {

            Resource res=spine.getSpineReferences().get((int) spinesRead).getResource();

            try {
                InputStream is = res.getInputStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(is));

                String line="";
                while ((line = r.readLine()) != null) {
                    line = Html.fromHtml(line).toString();
                    Log.i("mew bruno", line);
                    bookString.append(line);
                }

            } catch(IOException e) {
                e.printStackTrace();
            }

            ++spinesRead;

            Log.i("violent crystal banana", "bookString length: "+bookString.length());
            Log.i("violent crystal banana", "spinesRead: "+spinesRead);

            if((bookString.length()-initSize)>minReadSize)
                break;
        }
    }

    private void initUI() {

        textSwitcher=(TextSwitcher) findViewById(R.id.textSwitcher);
        nextButton=(Button) findViewById(R.id.nextButton);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;
        screenHeight= dm.heightPixels;

        paint = new Paint();
        densityMultiplier=dm.density;
        Log.i("density", "is: " + densityMultiplier);

        paint.setTextSize(textSize*densityMultiplier);

        textSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                TextView textView = new TextView(ReaderActivity.this);
                textView.setTextSize(textSize);
                lineHeight=textView.getLineHeight();
                maxLineCount=screenHeight/lineHeight;
                //textView.setMaxLines(maxLineCount);
                textView.setLines(maxLineCount);
                textView.setMaxHeight(screenHeight);
                //textView.setEllipsize(null);
                return textView;
            }
        });

        Log.i("blip bleep", "lineHeight: "+lineHeight);
        Log.i("blip bleep", "maxLineCount: " + maxLineCount);

    }

    private void initBook() {
        try {
            String filePath="";
            Bundle bundle=getIntent().getExtras();
            if(bundle!=null)
                filePath=bundle.getString("filePath");

            FileInputStream stream=new FileInputStream(filePath);
            book=new EpubReader().readEpub(stream);
            spine=book.getSpine();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
