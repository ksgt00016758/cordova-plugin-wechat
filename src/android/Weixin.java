package org.apache.cordova.wechat;

import java.net.URL;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.SendMessageToWX;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.tencent.mm.sdk.openapi.WXImageObject;
import com.tencent.mm.sdk.openapi.WXMediaMessage;
import com.tencent.mm.sdk.openapi.WXWebpageObject;

public class Weixin extends CordovaPlugin {

	public static final String WXAPPID_PROPERTY_KEY = "weixinappid";

	public static final String ERROR_WX_NOT_INSTALLED = "未安装微信";
	public static final String ERROR_ARGUMENTS = "参数错误";

	public static final String KEY_ARG_MESSAGE = "message";
	public static final String KEY_ARG_SCENE = "scene";
	public static final String KEY_ARG_MESSAGE_TITLE = "title";
	public static final String KEY_ARG_MESSAGE_DESCRIPTION = "description";
	public static final String KEY_ARG_MESSAGE_THUMB = "thumb";
	public static final String KEY_ARG_MESSAGE_MEDIA = "media";
	public static final String KEY_ARG_MESSAGE_MEDIA_TYPE = "type";
	public static final String KEY_ARG_MESSAGE_MEDIA_WEBPAGEURL = "webpageUrl";
	public static final String KEY_ARG_MESSAGE_MEDIA_TEXT = "text";

	public static final int TYPE_WX_SHARING_APP = 1;
	public static final int TYPE_WX_SHARING_EMOTION = 2;
	public static final int TYPE_WX_SHARING_FILE = 3;
	public static final int TYPE_WX_SHARING_IMAGE = 4;
	public static final int TYPE_WX_SHARING_MUSIC = 5;
	public static final int TYPE_WX_SHARING_VIDEO = 6;
	public static final int TYPE_WX_SHARING_WEBPAGE = 7;
	public static final int TYPE_WX_SHARING_TEXT = 8;
	

	protected IWXAPI api;
	protected CallbackContext currentCallbackContext;

	@Override
	public boolean execute(String action, JSONArray args,
			CallbackContext callbackContext) throws JSONException {
		
		if (action.equals("share")) {
			// sharing
			return share(args, callbackContext);
		}

		return super.execute(action, args, callbackContext);
	}

	protected IWXAPI getWXAPI() {
		if (api == null) {
			String appId = webView.getProperty(WXAPPID_PROPERTY_KEY, "");
			api = WXAPIFactory.createWXAPI(webView.getContext(), appId, true);
		}

		return api;
	}

	protected boolean share(JSONArray args, CallbackContext callbackContext)
			throws JSONException {
		final IWXAPI api = getWXAPI();

		api.registerApp(webView.getProperty(WXAPPID_PROPERTY_KEY, "wx8e552eaccd33eb8e"));

		// check if installed
		if (!api.isWXAppInstalled()) {
			callbackContext.error(ERROR_WX_NOT_INSTALLED);
			return false;
		}

		// check if # of arguments is correct
		if (args.length() != 1) {
			callbackContext.error(ERROR_ARGUMENTS);
		}

		final JSONObject params = args.getJSONObject(0);
		JSONObject media =params.getJSONObject(KEY_ARG_MESSAGE).getJSONObject(KEY_ARG_MESSAGE_MEDIA);
		int type = media.has(KEY_ARG_MESSAGE_MEDIA_TYPE) ? media
				.getInt(KEY_ARG_MESSAGE_MEDIA_TYPE) : TYPE_WX_SHARING_WEBPAGE;
		switch (type) {
		case TYPE_WX_SHARING_APP:
			break;

		case TYPE_WX_SHARING_EMOTION:
			break;

		case TYPE_WX_SHARING_FILE:
			break;

		case TYPE_WX_SHARING_IMAGE:
			sendImage(api, params);
			break;
			
		case TYPE_WX_SHARING_MUSIC:
			break;

		case TYPE_WX_SHARING_VIDEO:
			break;
			
		case TYPE_WX_SHARING_TEXT:
			break;

		case TYPE_WX_SHARING_WEBPAGE:
			sendWebPage(api, params);
			break;

		}
		
		return true;
	}
	//send image
	public void sendImage(IWXAPI api, JSONObject params) {
		Bitmap bmp = null;
		
		try{
			WXImageObject imgObj = new WXImageObject();
			WXMediaMessage msg = new WXMediaMessage();
			JSONObject message = params.getJSONObject(KEY_ARG_MESSAGE);
			msg.title = message.getString(KEY_ARG_MESSAGE_TITLE);
			msg.description = message
					.getString(KEY_ARG_MESSAGE_DESCRIPTION);
			String data = message.getString(KEY_ARG_MESSAGE_THUMB);
			
			imgObj.imageUrl = data;
			bmp = BitmapFactory.decodeStream(new URL(data).openStream());
			msg.mediaObject = imgObj;
			Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 150, 150, true);
			bmp.recycle();
			msg.thumbData = Util.bmpToByteArray(thumbBmp, true);		
			
			SendMessageToWX.Req req = new SendMessageToWX.Req();
			req.transaction = String.valueOf(System.currentTimeMillis());
			req.message = msg;
			int wxSdkVersion = api.getWXAppSupportAPI();
			if (params.has(KEY_ARG_SCENE)) {
				req.scene = params.getInt(KEY_ARG_SCENE);
			} else {
				req.scene = SendMessageToWX.Req.WXSceneTimeline;
			}
			
			if (wxSdkVersion < 0x21020001) {
				req.scene = SendMessageToWX.Req.WXSceneSession;
			}
			api.sendReq(req);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	//send webpage
	public void sendWebPage(IWXAPI api, JSONObject params) {
		try{
		JSONObject message = params.getJSONObject(KEY_ARG_MESSAGE);
		
		WXWebpageObject webpage = new WXWebpageObject();
		JSONObject media = message.getJSONObject(KEY_ARG_MESSAGE_MEDIA);
		webpage.webpageUrl = media.getString(KEY_ARG_MESSAGE_MEDIA_WEBPAGEURL);
		
		WXMediaMessage msg = new WXMediaMessage(webpage);
		msg.title = message.getString(KEY_ARG_MESSAGE_TITLE);
		msg.description = message
				.getString(KEY_ARG_MESSAGE_DESCRIPTION);
		
		Bitmap bmp = null;
		String imgUrl = message.getString(KEY_ARG_MESSAGE_THUMB);
		bmp = BitmapFactory.decodeStream(new URL(imgUrl).openStream());
		Bitmap thumbBmp = Bitmap.createScaledBitmap(bmp, 150, 150, true);
		bmp.recycle();
		msg.thumbData = Util.bmpToByteArray(thumbBmp, true);

		
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.transaction = String.valueOf(System.currentTimeMillis());
		req.message = msg;
		int wxSdkVersion = api.getWXAppSupportAPI();
		if (params.has(KEY_ARG_SCENE)) {
			req.scene = params.getInt(KEY_ARG_SCENE);
		} else {
			req.scene = SendMessageToWX.Req.WXSceneTimeline;
		}
		
		if (wxSdkVersion < 0x21020001) {
			req.scene = SendMessageToWX.Req.WXSceneSession;
		}
		api.sendReq(req);
		LOG.d("!!!!!!!!!!!!!!!!!!!!", webpage.webpageUrl);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
}

