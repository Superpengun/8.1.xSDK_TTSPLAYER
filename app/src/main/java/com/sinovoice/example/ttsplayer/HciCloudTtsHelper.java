package com.sinovoice.example.ttsplayer;


import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.sinovoice.hcicloudsdk.common.tts.TtsConfig;
import com.sinovoice.hcicloudsdk.common.tts.TtsInitParam;

import java.io.File;

public class HciCloudTtsHelper {

	private static final String TAG = HciCloudTtsHelper.class.getSimpleName();

	/**
	 *
	 * @param capkey 具体的本地合成能力
	 * @param resPrefix 该能力合成时，在datapath目录下的具体音库
	 * @return 配置串
	 */
	public static String getSynthConfig(String capkey, String resPrefix){
		TtsConfig synthConfig = new TtsConfig();
		synthConfig.addParam(TtsConfig.SessionConfig.PARAM_KEY_CAP_KEY, capkey);
		synthConfig.addParam(TtsConfig.SessionConfig.PARAM_KEY_RES_PREFIX, resPrefix);

		synthConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_SPEED, "5");
		return synthConfig.getStringConfig();

	}

	/**
	 *
	 * @param capkey 具体的云端合成能力
	 * @return 配置串
	 */
	public static String getCloudSynthConfig(String capkey){
		TtsConfig synthConfig = new TtsConfig();
		synthConfig.addParam(TtsConfig.SessionConfig.PARAM_KEY_CAP_KEY, capkey);

		synthConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_SPEED, "5");
		return synthConfig.getStringConfig();

	}


	/**
	 * 获取tts能力初始化时的参数.
	 *
	 * 由于本样例程序可展示本地合成功能，需要传设定本地合成所需的资源路径
	 * @param context
	 * @return
	 */
	public static String getInitConfig(Context context) {
		String dataPath = getDataPath(context);
		Utils.copyAssetsDataFiles(context, dataPath);
		TtsInitParam initParam = new TtsInitParam();
		initParam.addParam(TtsInitParam.PARAM_KEY_DATA_PATH, dataPath);
		Log.w(TAG, "TtsInitParam " + initParam.getStringConfig());
		return initParam.getStringConfig();
	}


	private static String getDataPath(Context context){
		// default datapath = /mnt/sdcard/sinovoice/<packageName>/data/
		return Environment.getExternalStorageDirectory().getAbsolutePath()+ File.separator + "sinovoice"
				+ File.separator + context.getPackageName() + File.separator + "data";
	}



}
