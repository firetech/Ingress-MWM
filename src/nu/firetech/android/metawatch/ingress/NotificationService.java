package nu.firetech.android.metawatch.ingress;

import java.io.IOException;
import java.io.InputStream;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.provider.Settings.SettingNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.RemoteViews;
import android.widget.TextView;

public class NotificationService extends AccessibilityService {

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		AccessibilityServiceInfo asi = new AccessibilityServiceInfo();
		asi.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
		asi.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
		asi.flags = AccessibilityServiceInfo.DEFAULT;
		asi.notificationTimeout = 100;
		setServiceInfo(asi);
	}

	private static long lastNotificationWhen = 0;

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		/* Acquire details of event. */
		int eventType = event.getEventType();

		String packageName = "";
		try {
			packageName = event.getPackageName().toString();
			if (!packageName.equals("com.nianticproject.ingress")) {
				return;
			}
		} catch (java.lang.NullPointerException e) {
			return;
		}

		if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
			Parcelable p = event.getParcelableData();
			if (p instanceof android.app.Notification == false) {
				return;
			}

			Notification notification = (Notification) p;

			if (lastNotificationWhen == notification.when) {
				return;
			}
			lastNotificationWhen = notification.when;

	        // get the notification text
	        String notificationText = event.getText().toString();
	        // strip the first and last characters which are [ and ]
	        notificationText = notificationText.substring(1, notificationText.length() - 1);
        	if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
        		notificationText += "\n" + getExtraBigData(notification, notificationText.trim());
        	} else {
        		notificationText += "\n" + getExtraData(notification, notificationText.trim());
        	}

    		sendNotification(this, notificationText.trim());
		}
	}

	public static void sendNotification(Context context, String notificationText) {
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

		Intent broadcast = new Intent("org.metawatch.manager.NOTIFICATION");
		Bundle b = new Bundle();
		b.putString("title", "Ingress");
		b.putString("text", notificationText);
		b.putBoolean("sticky", sharedPreferences.getBoolean("sticky", false));

		try {
			InputStream inputStream = context.getAssets().open("notification.bmp");
			Bitmap icon = BitmapFactory.decodeStream(inputStream);
			inputStream.close();

			int pixelArray[] = new int[icon.getWidth() * icon.getHeight()];
			icon.getPixels(pixelArray, 0, icon.getWidth(), 0, 0, icon.getWidth(), icon.getHeight());

			b.putIntArray("icon", pixelArray);
			b.putInt("iconWidth", icon.getWidth());
			b.putInt("iconHeight", icon.getHeight());
			
		} catch (IOException e) {}

		if( sharedPreferences.getBoolean("vibrate", false) )
		{
			b.putInt("vibrate_on", Integer.parseInt(sharedPreferences.getString("vibrate_on", "500")));
			b.putInt("vibrate_off", Integer.parseInt(sharedPreferences.getString("vibrate_off", "500")));
			b.putInt("vibrate_cycles", Integer.parseInt(sharedPreferences.getString("vibrate_cycles", "2")));
		}

		broadcast.putExtras(b);

		context.sendBroadcast(broadcast);
	}

	public static boolean isEnabled(Context context) {
		int accessibilityEnabled = 0;
		final String ACCESSIBILITY_SERVICE_NAME = context.getPackageName()+"/nu.firetech.android.metawatch.ingress.NotificationService";
	
		try {
			accessibilityEnabled = android.provider.Settings.Secure.getInt(context.getContentResolver(),android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
		}
		catch (SettingNotFoundException e) {
			return false;
		}
	
		android.text.TextUtils.SimpleStringSplitter mStringColonSplitter = new android.text.TextUtils.SimpleStringSplitter(':');
	
		if (accessibilityEnabled==1){
			String settingValue = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
			if (settingValue != null) {
				android.text.TextUtils.SimpleStringSplitter splitter = mStringColonSplitter;
				splitter.setString(settingValue);
				while (splitter.hasNext()) {
					String accessabilityService = splitter.next();
					if (accessabilityService.equalsIgnoreCase(ACCESSIBILITY_SERVICE_NAME)){
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void onInterrupt() {
		/* Do nothing */
	}


	// Code below stolen from Pebble Notifier.

	private String getExtraData(Notification notification, String existing_text) {
		RemoteViews views = notification.contentView;
		if (views == null) {
			return "";
		}

		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		try {
			ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
			views.reapply(getApplicationContext(), localView);
			return dumpViewGroup(0, localView, existing_text);
		} catch (android.content.res.Resources.NotFoundException e) {
			return "";
		}
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	private String getExtraBigData(Notification notification, String existing_text) {
		RemoteViews views = null;
		try {
			views = notification.bigContentView;
		} catch (NoSuchFieldError e) {
			return getExtraData(notification, existing_text);
		}
		if (views == null) {
			return getExtraData(notification, existing_text);
		}
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		try {
			ViewGroup localView = (ViewGroup) inflater.inflate(views.getLayoutId(), null);
			views.reapply(getApplicationContext(), localView);
			return dumpViewGroup(0, localView, existing_text);
		} catch (android.content.res.Resources.NotFoundException e) {
			return "";
		}
	}

	private String dumpViewGroup(int depth, ViewGroup vg, String existing_text) {
		String text = "";
		for (int i = 0; i < vg.getChildCount(); ++i) {
			View v = vg.getChildAt(i);
			if (v.getId() == android.R.id.title || v instanceof android.widget.Button
					|| v.getClass().toString().contains("android.widget.DateTimeView")) {
				if (!existing_text.isEmpty() || v.getId() != android.R.id.title) {
					continue;
				}
			}

			if (v instanceof TextView) {
				TextView tv = (TextView) v;
				if (tv.getText().toString() == "..." || isInteger(tv.getText().toString())
						|| tv.getText().toString().trim().equalsIgnoreCase(existing_text)) {
					continue;
				}
				text += tv.getText().toString() + "\n";
			}
			if (v instanceof ViewGroup) {
				text += dumpViewGroup(depth + 1, (ViewGroup) v, existing_text);
			}
		}
		return text;
	}

	public boolean isInteger(String input) {
		try {
			Integer.parseInt(input);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
