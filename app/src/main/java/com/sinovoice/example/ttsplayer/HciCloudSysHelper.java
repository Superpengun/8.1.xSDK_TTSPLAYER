package com.sinovoice.example.ttsplayer;


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.sinovoice.hcicloudsdk.api.HciCloudSys;
import com.sinovoice.hcicloudsdk.common.AuthExpireTime;
import com.sinovoice.hcicloudsdk.common.CapabilityItem;
import com.sinovoice.hcicloudsdk.common.CapabilityResult;
import com.sinovoice.hcicloudsdk.common.HciErrorCode;
import com.sinovoice.hcicloudsdk.common.InitParam;

public class HciCloudSysHelper {
	private static final String TAG = HciCloudSysHelper.class.getSimpleName();

	private String mAppKey = "";
	private String mDeveloperKey = "";
	private String mCloudUrl = "";

	private Context mContext;



	private static HciCloudSysHelper instance ;
	private HciCloudSysHelper(){}
	public static HciCloudSysHelper getInstance(){
		if(instance == null) {
			instance = new HciCloudSysHelper();
		}
		return instance;
	}

	/**
	 * HciCloud系统初始化
	 */
	public int init(Context context) {

		if(context == null){
			throw new IllegalArgumentException("context is null");
		}
		mContext = context;

		mAppKey = AccountInfo.getInstance().getAppKey();
		mDeveloperKey = AccountInfo.getInstance().getDeveloperKey();
		mCloudUrl = AccountInfo.getInstance().getCloudUrl();

		// 加载信息,返回InitParam, 获得配置参数的字符串
		InitParam initParam = getInitParam(context);
		String strConfig = initParam.getStringConfig();
		Log.i(TAG, "strConfig value:" + strConfig);

		// 初始化
		int initResult = HciCloudSys.hciInit(strConfig, context);
		if (initResult != HciErrorCode.HCI_ERR_NONE) {
			Log.e(TAG, "hciInit error: " + initResult);

			return initResult;
		} else {
			Log.i(TAG, "hciInit success");
		}

		// 获取授权/更新授权文件 :
		initResult = checkAuth();
		if (initResult != HciErrorCode.HCI_ERR_NONE) {
			// 由于系统已经初始化成功,在结束前需要调用方法hciRelease()进行系统的反初始化
			HciCloudSys.hciRelease();
			return initResult;
		}

		printAllCapkey();

		return HciErrorCode.HCI_ERR_NONE;
	}

	/**
	 * 系统反初始化
	 */
	public int release() {
		int errCode = HciCloudSys.hciRelease();
		Log.i(TAG, "HciCloud release, result = " + errCode);
		return errCode;
	}

	/**
	 * 加载初始化信息
	 * 
	 * @param context
	 *            上下文语境
	 * @return 系统初始化参数
	 */
	private InitParam getInitParam(Context context) {

		if(context == null){
			throw new IllegalArgumentException("context is null");
		}
		// 前置条件：无
		InitParam initparam = new InitParam();

		String sdcardState = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(sdcardState)) {
            String packageName = context.getPackageName();
            String sdPath = Environment.getExternalStorageDirectory().getAbsolutePath();

            String authDirPath = sdPath + File.separator + "sinovoice"
                    + File.separator + packageName + File.separator + "auth"
                    + File.separator;

            // 授权文件地址
            File authfileDir = new File(authDirPath);
            if (!authfileDir.exists()) {
                authfileDir.mkdirs();
            }

            // 授权文件所在路径，此项必填
		    initparam.addParam(InitParam.AuthParam.PARAM_KEY_AUTH_PATH, authDirPath);

		    // 是否自动访问云授权,详见 获取授权/更新授权文件处注释
		    initparam.addParam(InitParam.AuthParam.PARAM_KEY_AUTO_CLOUD_AUTH, "no");

		    // 灵云云服务的接口地址，此项必填
		    initparam.addParam(InitParam.AuthParam.PARAM_KEY_CLOUD_URL, mCloudUrl);

		    // 开发者Key，此项必填，由捷通华声提供
		    initparam.addParam(InitParam.AuthParam.PARAM_KEY_DEVELOPER_KEY, mDeveloperKey);

		    // 应用Key，此项必填，由捷通华声提供
		    initparam.addParam(InitParam.AuthParam.PARAM_KEY_APP_KEY, mAppKey);

			// 配置日志参数
			String logPath = sdPath + File.separator + "sinovoice"
					+ File.separator + packageName + File.separator + "log"
					+ File.separator;

			// 日志文件地址
			File logfileDir = new File(logPath);
			if (!logfileDir.exists()) {
                logfileDir.mkdirs();
			}

			// 日志的路径，可选，如果不传或者为空则不生成日志
			initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_PATH, logPath);

			// 日志数目，默认保留多少个日志文件，超过则覆盖最旧的日志
			initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_COUNT, "50");

			// 日志大小，默认一个日志文件写多大，单位为K
			initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_FILE_SIZE, "10240");

			// 日志等级，0=无，1=错误，2=警告，3=信息，4=细节，5=调试，SDK将输出小于等于logLevel的日志信息
			initparam.addParam(InitParam.LogParam.PARAM_KEY_LOG_LEVEL, "5");
		} else {
			Log.e(TAG, "SdCard不存在!请插入SdCard.");
		}

		return initparam;
	}

	/**
	 * 检查授权
	 */
	private int checkAuth() {
		// 获取系统授权到期时间
		AuthExpireTime objExpireTime = new AuthExpireTime();
		int initResult = HciCloudSys.hciGetAuthExpireTime(objExpireTime);
		if (initResult == HciErrorCode.HCI_ERR_NONE) {
			// 显示授权日期,如用户不需要关注该值,此处代码可忽略
			Date date = new Date(objExpireTime.getExpireTime() * 1000);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
			Log.i(TAG, "expire time: " + sdf.format(date));

			if (objExpireTime.getExpireTime() * 1000 < System.currentTimeMillis()) {
				// 授权已过期，需要调用 hciCheckAuth 重新获取授权
				Log.i(TAG, "authorization is expired");

				initResult = HciCloudSys.hciCheckAuth();
				if (initResult == HciErrorCode.HCI_ERR_NONE) {
					Log.i(TAG, "checkAuth success");
					return initResult;
				} else {
					Log.e(TAG, "checkAuth failed: " + initResult);
					return initResult;
				}
			} else {
				// 授权未过期
				if (isFirstRunAfterUpgraded()) {
					// 如果这是应用升级后第一次运行，则执行一次 hciCheckAuth 以应对授权升级的情况
					initResult = HciCloudSys.hciCheckAuth();
					if (initResult == HciErrorCode.HCI_ERR_NONE) {
						Log.i(TAG, "checkAuth success");
						setFirstRunAfterUpgraded();
						return initResult;
					} else {
						Log.e(TAG, "checkAuth failed: " + initResult);
						return initResult;
					}
				} else
					Log.i(TAG, "checkAuth success");
				return initResult;
			}
		} else if (initResult == HciErrorCode.HCI_ERR_SYS_AUTHFILE_INVALID) {
			// 如果读取Auth文件失败(比如第一次运行,还没有授权文件),则开始获取授权
			Log.i(TAG, "authfile invalid");

			initResult = HciCloudSys.hciCheckAuth();
			if (initResult == HciErrorCode.HCI_ERR_NONE) {
				Log.i(TAG, "checkAuth success");
				return initResult;
			} else {
				Log.e(TAG, "checkAuth failed: " + initResult);
				return initResult;
			}
		} else {
			// 其他失败原因,请根据SDK帮助文档中"常量字段值"中的错误码的含义检查错误所在
			Log.e(TAG, "getAuthExpireTime Error:" + initResult);
			return initResult;
		}
	}

	/**
	 * 检查是不是应用升级后第一次运行
	 * @return
	 */
	private boolean isFirstRunAfterUpgraded() {
		return false; // TODO: 判断当前是不是在应用升级后第一次运行
	}
	static private void setFirstRunAfterUpgraded() {
		// TODO: 升级后已成功运行一次，修改 firstRunAfterUpgraded 状态为 false
	}
	/**
	 * 打印账号全部能力
	 */
	static private void printAllCapkey(){
		Log.d(TAG, "printAllCapkey()");

		CapabilityResult capabilityResult = new CapabilityResult();
		HciCloudSys.hciGetCapabilityList(null, capabilityResult);
		ArrayList<CapabilityItem> capkeyList = capabilityResult.getCapabilityList();
		if (capkeyList != null && capkeyList.size() > 0) {
			for (CapabilityItem capabilityItem : capkeyList) {
				Log.d(TAG, "capability:" + capabilityItem.getCapKey());
			}
		} else {
			Log.e(TAG, "no capability found!");
		}
	}
}
