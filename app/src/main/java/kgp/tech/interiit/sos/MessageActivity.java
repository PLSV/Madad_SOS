package kgp.tech.interiit.sos;


import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.media.MediaPlayer;

import com.nineoldandroids.view.ViewHelper;
import com.nineoldandroids.view.ViewPropertyAnimator;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;

import com.github.ksoichiro.android.observablescrollview.ObservableListView;
import com.github.ksoichiro.android.observablescrollview.ObservableScrollViewCallbacks;
import com.github.ksoichiro.android.observablescrollview.ScrollState;
import com.github.ksoichiro.android.observablescrollview.ScrollUtils;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;

import kgp.tech.interiit.sos.Utils.Helper;
import kgp.tech.interiit.sos.Utils.Utils;
import kgp.tech.interiit.sos.Utils.comm;

public class MessageActivity extends BaseActivity implements ObservableScrollViewCallbacks {

    private static final float MAX_TEXT_SCALE_DELTA = 0.3f;

    private View mImageView;
    private View mOverlayView;
    private View mListBackgroundView;
    private RelativeLayout mTitleView;
    private View mFab;
    private int mActionBarSize;
    private int mFlexibleSpaceShowFabOffset;
    private int mFlexibleSpaceImageHeight;
    private int mFabMargin;
    private boolean mFabIsShown;

    ArrayList<Message> messages;
    AwesomeAdapter adapter;
    EditText text;
    TextView desc;
    TextView time;
    static Random rand = new Random();
    static String sender;
    private Toolbar toolbar;
    static String message_incoming = "";
    private ObservableListView listView;
    ImageButton voice, map;
    FrameLayout cont;
    FloatingActionButton sendfab;
    String channelID;
    String sos_creater = "";
    private TextView mTitlehead;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        voice=(ImageButton)findViewById(R.id.btn_voice);
        map=(ImageButton)findViewById(R.id.btn_map);
        cont=(FrameLayout)findViewById(R.id.container);
        sendfab=(FloatingActionButton)findViewById(R.id.second_send_button_sms_view);

        toolbar=(Toolbar)findViewById(R.id.toolbar);

        listView = (ObservableListView) findViewById(R.id.list);
        //text = (EditText) this.findViewById(R.id.text);
        messages = new ArrayList<Message>();

        adapter = new AwesomeAdapter(this, messages);
        listView.setAdapter(adapter);


        mFlexibleSpaceImageHeight = getResources().getDimensionPixelSize(R.dimen.flexible_space_image_height);
        mFlexibleSpaceShowFabOffset = getResources().getDimensionPixelSize(R.dimen.flexible_space_show_fab_offset);
        mActionBarSize = getActionBarSize();
        mImageView = findViewById(R.id.image);
        mOverlayView = findViewById(R.id.overlay);
        //ObservableListView listView = (ObservableListView) findViewById(R.id.list);
        listView.setScrollViewCallbacks(this);

        // Set padding view for ListView. This is the flexible space.
        View paddingView = new View(this);
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT,
                mFlexibleSpaceImageHeight);
        paddingView.setLayoutParams(lp);

        // This is required to disable header's list selector effect
        paddingView.setClickable(true);

        listView.addHeaderView(paddingView);
        //setDummyData(listView);le
        mTitlehead = (TextView) findViewById(R.id.titlehead);
        mTitleView = (RelativeLayout) findViewById(R.id.title);




        setTitle(null);
        mFab = findViewById(R.id.fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(MessageActivity.this, "FAB is clicked", Toast.LENGTH_SHORT).show();
                showPopup(v);

            }
        });
        mFabMargin = getResources().getDimensionPixelSize(R.dimen.margin_standard);
        ViewHelper.setScaleX(mFab, 0);
        ViewHelper.setScaleY(mFab, 0);

        // mListBackgroundView makes ListView's background except header view.
        mListBackgroundView = findViewById(R.id.list_background);

        text = (EditText)findViewById(R.id.text);
        desc = (TextView)findViewById(R.id.desc);
        time = (TextView)findViewById(R.id.time);

        //setting data
        Log.d("Message",getIntent().getStringExtra("username"));
        channelID = getIntent().getStringExtra("channelID");
        sos_creater = getIntent().getStringExtra("username");
        ParseUser user = new ParseUser();
        user.setUsername(sos_creater);
        Helper.GetProfilePic(user, mImageView, MessageActivity.this);
        desc.setText(getIntent().getStringExtra("Description"));
        time.setText(getString(R.string.started_at) +" "+ getIntent().getStringExtra("createdAt"));
        mTitlehead.setText(getIntent().getStringExtra("displayname"));
        sender = getIntent().getStringExtra("displayname");

        history(channelID);
        recieveMessage(channelID);

        SharedPreferences sp = getSharedPreferences("SOS", Context.MODE_APPEND | Context.MODE_PRIVATE);
        setSupportActionBar(toolbar);
        if(sp.getString("sosID", null)!=null)
        {
            Log.d("Message","SOS active");
            setcolorred();
        }
        else{

            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        }

        ParseQuery<ParseObject> pq = ParseQuery.getQuery("SOS");
        pq.whereEqualTo("channelID",channelID);
        pq.getFirstInBackground(new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject parseObject, ParseException e) {
                if (e != null) {
                    e.printStackTrace();
                    return;
                }
                ParseQuery<ParseObject> pq = ParseQuery.getQuery("SOS_Users");
                pq.whereEqualTo("SOSid", parseObject);
                pq.whereEqualTo("hasAccepted", true);
                pq.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> list, ParseException e) {
                        TextView help = (TextView) findViewById(R.id.help);
                        help.setText(list.size() + " " + getString(R.string.coming));
                    }
                });
            }
        });
        //setcolorred();

    }

//    @Override
//    protected int getLayoutResId() {
//        return 0;
//    }
//
//    @Override
//    protected ObservableListView createScrollable() {
//        return null;
//    }

    public void showPopup(View v) {
        IconizedPopup popup = new IconizedPopup(this, v);
        MenuInflater inflater = popup.getMenuInflater();
        SharedPreferences sp = getSharedPreferences("SOS", Context.MODE_APPEND | Context.MODE_PRIVATE);
        setSupportActionBar(toolbar);
        if (sp.getString("sosID", null) != null) {
            inflater.inflate(R.menu.menu_self, popup.getMenu());
            popup.show();

        } else {
            inflater.inflate(R.menu.menu_other, popup.getMenu());
            popup.show();
        }

        IconizedPopup.OnMenuItemClickListener listener = new IconizedPopup.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_map:
                        // User chose the "Settings" item, show the app settings UI...
                        action_openMap();
                        return true;

                    case R.id.action_voice:
                        playAudio();
                        return true;

                    case R.id.action_close:
                        action_cancel_sos();
                        return true;
                }
                return false;

            }
        };
        popup.setOnMenuItemClickListener(listener);
    }

    @Override
    public void onScrollChanged(int scrollY, boolean firstScroll, boolean dragging) {
        // Translate overlay and image
        float flexibleRange = mFlexibleSpaceImageHeight - mActionBarSize;
        int minOverlayTransitionY = mActionBarSize - mOverlayView.getHeight();
        ViewHelper.setTranslationY(mOverlayView, ScrollUtils.getFloat(-scrollY, minOverlayTransitionY, 0));
        ViewHelper.setTranslationY(mImageView, ScrollUtils.getFloat(-scrollY / 2, minOverlayTransitionY, 0));

        // Translate list background
        ViewHelper.setTranslationY(mListBackgroundView, Math.max(0, -scrollY + mFlexibleSpaceImageHeight));

        // Change alpha of overlay
        ViewHelper.setAlpha(mOverlayView, ScrollUtils.getFloat((float) scrollY / flexibleRange, 0, 1));
        //ViewHelper.setAlpha(toolbar, ScrollUtils.getFloat((float) scrollY / flexibleRange, 1, 0));

        // Scale title text
        float scale = 1 + ScrollUtils.getFloat((flexibleRange - scrollY) / flexibleRange, 0, MAX_TEXT_SCALE_DELTA);
        setPivotXToTitle();
        ViewHelper.setPivotY(mTitleView, 0);
        ViewHelper.setScaleX(mTitleView, scale);
        ViewHelper.setScaleY(mTitleView, scale);

        // Translate title text
        int maxTitleTranslationY = (int) (mFlexibleSpaceImageHeight - mTitleView.getHeight() * scale);
        int titleTranslationY = maxTitleTranslationY - scrollY;
        ViewHelper.setTranslationY(mTitleView, titleTranslationY);

        // Translate FAB
        int maxFabTranslationY = mFlexibleSpaceImageHeight - mFab.getHeight() / 2;
        float fabTranslationY = ScrollUtils.getFloat(
                -scrollY + mFlexibleSpaceImageHeight - mFab.getHeight() / 2,
                mActionBarSize - mFab.getHeight() / 2,
                maxFabTranslationY);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            // On pre-honeycomb, ViewHelper.setTranslationX/Y does not set margin,
            // which causes FAB's OnClickListener not working.
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) mFab.getLayoutParams();
            lp.leftMargin = mOverlayView.getWidth() - mFabMargin - mFab.getWidth();
            lp.topMargin = (int) fabTranslationY;
            mFab.requestLayout();
        } else {
            ViewHelper.setTranslationX(mFab, mOverlayView.getWidth() - mFabMargin - mFab.getWidth());
            ViewHelper.setTranslationY(mFab, fabTranslationY);
        }

        // Show FAB
        if (fabTranslationY < mFlexibleSpaceShowFabOffset) {
            hideFab();
        } else {
            showFab();
            //toolbar.setVisibility(View.INVISIBLE);

        }
    }

    @Override
    public void onDownMotionEvent() {
    }

    public void onUpOrCancelMotionEvent(ScrollState scrollState) {
        if (scrollState == ScrollState.UP) {
            // TODO show or hide the ActionBar
            toolbar.setVisibility(View.VISIBLE);
        } else if (scrollState == ScrollState.DOWN) {
            // TODO show or hide the ActionBar
            toolbar.setVisibility(View.INVISIBLE);

        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void setPivotXToTitle() {
        Configuration config = getResources().getConfiguration();
        if (Build.VERSION_CODES.JELLY_BEAN_MR1 <= Build.VERSION.SDK_INT
                && config.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL) {
            ViewHelper.setPivotX(mTitleView, findViewById(android.R.id.content).getWidth());
        } else {
            ViewHelper.setPivotX(mTitleView, 0);
        }
    }

    private void showFab() {
        if (!mFabIsShown) {
            ViewPropertyAnimator.animate(mFab).cancel();
            ViewPropertyAnimator.animate(mFab).scaleX(1).scaleY(1).setDuration(200).start();
           // toolbar.setVisibility(View.INVISIBLE);
            mFabIsShown = true;
            mTitleView.setVisibility(View.VISIBLE);
            map.setVisibility(View.VISIBLE);
            voice.setVisibility(View.VISIBLE);
            getSupportActionBar().setTitle(new SpannableString(""));


        }
    }

    private void hideFab() {
        if (mFabIsShown) {
            ViewPropertyAnimator.animate(mFab).cancel();
            ViewPropertyAnimator.animate(mFab).scaleX(0).scaleY(0).setDuration(200).start();
            //toolbar.setVisibility(View.VISIBLE);
            mTitleView.setVisibility(View.INVISIBLE);
            map.setVisibility(View.INVISIBLE);
            voice.setVisibility(View.INVISIBLE);
            getSupportActionBar().setTitle(new SpannableString(sender));
            mFabIsShown = false;
        }
    }

    public void sendMessage(View v)
    {
        String newMessage = text.getText().toString().trim();
        comm.sendMessage(channelID,newMessage);
        text.setText("");
    }

    void recieveMessage(String channelName)
    {
        try {
            final Pubnub pubnub = new Pubnub("pub-c-f9d02ea4-19f1-4737-b3e1-ef2ce904b94f", "sub-c-3d547124-be29-11e5-8a35-0619f8945a4f");

            pubnub.subscribe(channelName, new Callback() {
                public void successCallback(String channel, Object mes) {

                    try {
                        JSONObject json_mes = new JSONObject(mes.toString());

                        String message = json_mes.getString("message");
                        String username = json_mes.getString("username");
                        String displayname = json_mes.getString("displayname");
                        final Message m = new Message(username,displayname,message,false);
                        if(username.equals(ParseUser.getCurrentUser().getUsername()))
                            m.isMine = true;

                        MessageActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                addNewMessage(m);
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }

                public void errorCallback(String channel, PubnubError error) {
                    System.out.println(error.getErrorString());
                }
            });
        } catch (PubnubException e) {
            e.printStackTrace();
        }
    }

    public void action_cancel_sos()
    {
        Utils.showDialog(this, R.string.cancel_sos, R.string.yes, R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_NEGATIVE:
                        // int which = -2

                        break;
                    case DialogInterface.BUTTON_POSITIVE:
                        SharedPreferences sp = getSharedPreferences("SOS", Context.MODE_PRIVATE);
                        String SOSid = sp.getString("sosID", null);

                        ParseQuery<ParseObject> pq = ParseQuery.getQuery("SOS");
                        pq.getInBackground(SOSid, new GetCallback<ParseObject>() {
                            @Override
                            public void done(ParseObject parseObject, ParseException e) {
                                if (e != null) {
                                    Utils.showDialog(MessageActivity.this, e.getMessage());
                                    return;
                                }
                                parseObject.put("isActive", false);
                                final ProgressDialog dia = ProgressDialog.show(MessageActivity.this, null, getString(R.string.alert_wait));
                                parseObject.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e != null) {
                                            Utils.showDialog(MessageActivity.this, e.getMessage());
                                            return;
                                        }

                                        SharedPreferences sp = getSharedPreferences("SOS", Context.MODE_PRIVATE);
                                        SharedPreferences.Editor editor = sp.edit();
                                        editor.putString("sosID", null);
                                        editor.commit();
                                        dia.dismiss();

                                        Intent intent = new Intent(MessageActivity.this, WelcomeActivity.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                });
                            }
                        });
                        //TODO call the cloud service and make it check if contact uses the app
                        break;
                }
                return;
            }
        });
    }

    public void history(String channelName) {
        final Pubnub pubnub = new Pubnub("pub-c-f9d02ea4-19f1-4737-b3e1-ef2ce904b94f", "sub-c-3d547124-be29-11e5-8a35-0619f8945a4f");
        pubnub.history(channelName, 100, false, new Callback() {
            @Override
            public void successCallback(String channel, final Object message) {
                try {
                    JSONArray json = (JSONArray) message;
                    Log.d("History", json.toString());
                    final JSONArray messages = json.getJSONArray(0);

                    MessageActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < messages.length(); i++) {
                                try {
                                    JSONObject jsonMsg = messages.getJSONObject(i);

                                    String message = jsonMsg.getString("message");
                                    String username = jsonMsg.getString("username");
                                    String displayname = jsonMsg.getString("displayname");
                                    final Message m = new Message(username, displayname, message, false);
                                    if (username.equals(ParseUser.getCurrentUser().getUsername()))
                                        m.isMine = true;

                                    addNewMessage(m);
                                } catch (JSONException e) { // Handle errors silently
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                Log.d("History", error.toString());
            }
        });
    }

    void addNewMessage(Message m)
    {
        messages.add(m);
        adapter.notifyDataSetChanged();
        listView.setSelection(messages.size() - 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        switch(requestCode){
            case 1:
                if(resultCode==RESULT_OK){
                    String FilePath = data.getData().getPath();
                    //textFile.setText(FilePath);
                    Toast.makeText(getApplicationContext(), FilePath,
                            Toast.LENGTH_LONG).show();
                }
                break;

        }
    }

    void setcolorred()//use setcolor R.color.red for Self SOS
    {

        mOverlayView.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
        cont.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.darkred));
        }
        toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
        sendfab.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.red));
        FloatingActionButton f=(FloatingActionButton)findViewById(R.id.fab);
        f.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.darkred));



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    public void openMap(View v)
    {
        action_openMap();
    }
    public void playAudio()
    {
        action_playAudio(channelID);
    }

    void action_playAudio(String name){

        ParseQuery<ParseObject> query = ParseQuery.getQuery("SOS");
        query.whereEqualTo("channelID",channelID);
        Log.d("MessageActivity", channelID);
        query.getFirstInBackground(new GetCallback<ParseObject>() {

            public void done(ParseObject recording, com.parse.ParseException e) {
                if (e != null) {
                    e.printStackTrace();
                    Log.d("MessageActivity", e.getMessage());
                    return;
                } else {
                    ParseFile audioFile = recording.getParseFile("audio");
                    String audioFileURL = audioFile.getUrl();
                    MediaPlayer mediaPlayer = new MediaPlayer();
                    mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                    Log.d("MessageActivity", "Playing Audio");
                    try {
                        mediaPlayer.setDataSource(audioFileURL);
                        mediaPlayer.prepare();
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            mediaPlayer.release();
                            mediaPlayer = null;
                        } else {
                            mediaPlayer.start();
                        }
                        //
                    } catch (IllegalArgumentException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (SecurityException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (IllegalStateException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } catch (IOException e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                }
            }
        });

    }

    void action_openMap()
    {

        SharedPreferences sp = getSharedPreferences("SOS", Context.MODE_APPEND | Context.MODE_PRIVATE);
        if(sp.getString("sosID", null)!=null)
        {
            Intent intent = new Intent(MessageActivity.this, FullMap.class);
            startActivity(intent);
        }
        else
        {
            Intent intent = new Intent(MessageActivity.this, HomeActivity.class);
            startActivity(intent);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_map:
                // User chose the "Settings" item, show the app settings UI...
                action_openMap();
                return true;

            case R.id.action_voice:
                playAudio();
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }
}