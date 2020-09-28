package com.sinovoice.example.ttsplayer;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.sinovoice.hcicloudsdk.android.tts.player.TTSPlayer;
import com.sinovoice.hcicloudsdk.api.HciCloudSys;
import com.sinovoice.hcicloudsdk.api.tts.HciCloudTts;
import com.sinovoice.hcicloudsdk.common.tts.TtsSynthSyllable;
import com.sinovoice.hcicloudsdk.player.AudioPlayer;
import com.sinovoice.hcicloudsdk.player.PlayerEvent;
import com.sinovoice.hcicloudsdk.player.TTSCommonPlayer;
import com.sinovoice.hcicloudsdk.player.TTSPlayerListener;




public class MainActivity extends Activity {

	TTSPlayer player;
	private String mText_to_speak ;
	private EditText mTtsEditText;
	private TTSPlayerListener ttsPlayerListener;
	private MyTextWatcher myTextWatcher;
	private Button mStopBtn;
	private Button mPauseBtn;
	private Button mResumeBtn;
	private Button mPlayBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Context appContext = getApplicationContext();
		initSDK(appContext);
		initView();
		initDataAndAction();
	}

	private void initSDK(Context appContext) {
		AccountInfo.getInstance().loadAccountInfo(this);
		int errCode = HciCloudSysHelper.getInstance().init(appContext);
		showToast( "sys init code = [" + errCode+"] msg = " + "["+ HciCloudSys.hciGetErrorInfo(errCode)+"]");
		errCode = HciCloudTts.hciTtsInit(HciCloudTtsHelper.getInitConfig(appContext));
		showToast( "tts init code = [" + errCode+"] msg = " + "["+ HciCloudSys.hciGetErrorInfo(errCode)+"]");
	}

	private void initView() {
		mTtsEditText = (EditText) findViewById(R.id.content_tv);
		mPlayBtn = (Button) findViewById(R.id.button1);
		mStopBtn = (Button) findViewById(R.id.button2);
		mPauseBtn = (Button) findViewById(R.id.button3);
		mResumeBtn = (Button) findViewById(R.id.button4);
	}

	private void initDataAndAction() {
		myTextWatcher = new MyTextWatcher();
		mTtsEditText.addTextChangedListener(myTextWatcher);
		mText_to_speak = mTtsEditText.getText().toString();

		ttsPlayerListener = new MyTTSPlayerListener();
		player = new TTSPlayer(ttsPlayerListener);
		player.setContext(getApplicationContext());
		player.setRouteFlag(AudioPlayer.PLAYER_FLAG_SPEAKER);

		mPlayBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				//String capkey = AccountInfo.getInstance().getCapKey();
//				if (capkey.contains("tts.cloud")){
//					if (player.getPlayerState()==TTSCommonPlayer.PLAYER_STATE_PLAYING
//							||player.getPlayerState()==TTSCommonPlayer.PLAYER_STATE_PAUSE){
//						player.stop();
//					}
//					if(player.getPlayerState()==TTSCommonPlayer.PLAYER_STATE_IDLE){
//						player.play(mText_to_speak, HciCloudTtsHelper.getCloudSynthConfig(capkey));
//					}
//
//				}
//					player.pause();

				if (player.getPlayerState() == TTSCommonPlayer.PLAYER_STATE_IDLE){
					String capkey = AccountInfo.getInstance().getCapKey();

					if(capkey.equalsIgnoreCase("tts.local.synth.v9")){
						player.play(mText_to_speak, HciCloudTtsHelper.getSynthConfig(capkey, "v9/RouMeiJuan_Common/"));
					}else if(capkey.equalsIgnoreCase("tts.local.synth")){
						player.play(mText_to_speak, HciCloudTtsHelper.getSynthConfig(capkey, "v8/WangJing_Common/"));
					}else if (capkey.contains("tts.cloud")){
						//player.pause();
						player.play(mText_to_speak, HciCloudTtsHelper.getCloudSynthConfig(capkey));
					}

				}
			}
		});

		mStopBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				player.stop();
			}
		});

		mPauseBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				player.pause();
				int playerstate = player.getPlayerState();
			}
		});

		mResumeBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View arg0) {
				player.resume();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onDestroy() {
		player.stop();
        int errCode = HciCloudTts.hciTtsRelease();
        showToast("tts release code = [" + errCode+"] msg = " + "["+ HciCloudSys.hciGetErrorInfo(errCode)+"]");
		errCode = HciCloudSysHelper.getInstance().release();
		showToast("sys release code = [" + errCode+"] msg = " + "["+ HciCloudSys.hciGetErrorInfo(errCode)+"]");
		super.onDestroy();
	}

	private static class MyTTSPlayerListener implements TTSPlayerListener {

		@Override
        public void onPlayerEventStateChange(PlayerEvent playerEvent) {
            Log.i("TTSPlayer", playerEvent.name());
        }

		@Override
        public void onPlayerEventProgressChange(PlayerEvent playerEvent,
                int playPos, int synthPos, int total) {
            Log.i("TTSPlayer", "progress: " + playPos + ", " + synthPos
                    + ", " + total + ", ");
        }

		@Override
        public void onPlayerEventProgressChange(PlayerEvent playerEvent,
                int textStart, int textEnd, String sentence,
                TtsSynthSyllable syllable) {
            Log.i("TTSPlayer", "syllable: " + textStart + ", " + textEnd
                    + ", " + sentence + ", " + syllable.getText() + ", "
                    + syllable.getPronounciationText());
        }

		@Override
        public void onPlayerEventPlayerError(PlayerEvent playerEvent,
                int errorCode) {
            Log.i("TTSPlayer", playerEvent.name() + " " + errorCode);
        }

		@Override
        public void onPlayerEventSeek(PlayerEvent playerEvent, int seekPos) {
            Log.i("TTSPlayer", playerEvent.name() + " " + seekPos);
        }
	}

	private class MyTextWatcher implements TextWatcher {
		@Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

		@Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

		@Override
        public void afterTextChanged(Editable editable) {
            mText_to_speak = editable.toString();
        }
	}

	private void showToast(String msg){
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}
}
