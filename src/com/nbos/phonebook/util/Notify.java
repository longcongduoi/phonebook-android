package com.nbos.phonebook.util;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.nbos.phonebook.R;
import com.nbos.phonebook.WelcomeActivity;
	
public class Notify {
	static int NOTE_ID = 1;
	public static void show(String title, String text, String ticker, Context context) {
		
		int icon = R.drawable.icon;
		// CharSequence tickerText = "Hello";
		long when = System.currentTimeMillis();
		
		NotificationManager notificationManager = (NotificationManager) context
				.getSystemService(Context.NOTIFICATION_SERVICE);

		Notification notification = new Notification(icon, ticker, when);

		RemoteViews contentView = new RemoteViews(context.getPackageName(),
				R.layout.add_group);
		contentView.setTextViewText(R.id.textView1,
				text);
		notification.contentView = contentView;
		Intent notificationIntent = new Intent(context, WelcomeActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, title, text,
				contentIntent);
		notification.flags = Notification.FLAG_AUTO_CANCEL;

		notificationManager.notify(NOTE_ID++, notification);
	}

}
