package com.nbos.phonebook;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;
 
public class Widget extends AppWidgetProvider {
 
	public static String ACTION_WIDGET_CONFIGURE = "ConfigureWidget";
	public static String ACTION_WIDGET_RECEIVER = "ActionReceiverWidget";
	String tag = "MyAppWidgetProvider";
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		Log.i(tag, "onUpdate()");
		context.startService(new Intent(context, AppService.class));
		/*Toast.makeText(context, "onUpdate", Toast.LENGTH_SHORT).show();
		Log.i(tag, "update my widget");
		RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.main);
		Intent configIntent = new Intent(context, StartActivity.class);
		configIntent.setAction(ACTION_WIDGET_CONFIGURE);
 
		Intent active = new Intent(context, MyAppWidgetProvider.class);
		active.setAction(ACTION_WIDGET_RECEIVER);
		active.putExtra("msg", "Message for Button 1");
		//when you will click button1 the message "Message for Button 1" will appear as a notification
//you can do whatever you want anyway on the press of this button
		PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, 0, active, 0);
		PendingIntent configPendingIntent = PendingIntent.getActivity(context, 0, configIntent, 0);
 
		remoteViews.setOnClickPendingIntent(R.id.button_one, actionPendingIntent);
		remoteViews.setOnClickPendingIntent(R.id.button_two, configPendingIntent);
 
		appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);*/
	}
 
	@Override
	public void onReceive(Context context, Intent intent) {
		//this takes care of managing the widget
		// v1.5 fix that doesn't call onDelete Action
		Log.i(tag, "onReceive():");
		final String action = intent.getAction();
		if (AppWidgetManager.ACTION_APPWIDGET_DELETED.equals(action)) {
			final int appWidgetId = intent.getExtras().getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
				this.onDeleted(context, new int[] { appWidgetId });
			}
		} else {
			// check, if our Action was called
			if (intent.getAction().equals(ACTION_WIDGET_RECEIVER)) {
				String msg = "null";
				try {
					msg = intent.getStringExtra("msg");
				} catch (NullPointerException e) {
					Log.e("Error", "msg = null");
				}
				Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
 
				PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);
				NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
				Notification noty = new Notification(R.drawable.icon, "Button 1 clicked", System.currentTimeMillis());
 
				noty.setLatestEventInfo(context, "Notice", msg, contentIntent);
				notificationManager.notify(1, noty);
				context.startService(new Intent(context, AppService.class));				
			} else if (intent.getAction().equals(ACTION_WIDGET_CONFIGURE)) {
				// do nothing
				Log.i(tag, "Config button clicked");
			}
 
			super.onReceive(context, intent);
		}
	}
	
	public static class AppService extends Service {

		String tag = "MyAppService";
		static String [] messages = {};
		public static String message = "";
		
	    @Override
	    public void onStart(Intent intent, int startId) {
	        // Build the widget update for today
	        RemoteViews updateViews = buildUpdate(this);
	        
	        // Push update for this widget to the home screen
	        ComponentName thisWidget = new ComponentName(this, Widget.class);
	        AppWidgetManager manager = AppWidgetManager.getInstance(this);
	        manager.updateAppWidget(thisWidget, updateViews);
	    }
		
	    public RemoteViews buildUpdate(Context context) {
			Toast.makeText(context, "onUpdate", Toast.LENGTH_SHORT).show();
			Log.i(tag, "update my widget");
			RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.widget);
			Log.i(tag, "got remote widget, message is: "+message);
			// Intent configIntent = new Intent(context, StartActivity.class);
			// configIntent.setAction(MyAppWidgetProvider.ACTION_WIDGET_CONFIGURE);
	 
			Intent active = new Intent(context, Widget.class);
			active.setAction(Widget.ACTION_WIDGET_RECEIVER);
			active.putExtra("msg", "Message for Button 1");
			//when you will click button1 the message "Message for Button 1" will appear as a notification
	//you can do whatever you want anyway on the press of this button
			// PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, 0, active, 0);
			// PendingIntent configPendingIntent = PendingIntent.getActivity(context, 0, configIntent, 0);
			
			remoteViews.setTextViewText(R.id.widget_message, message);
			// remoteViews.setOnClickPendingIntent(R.id.button_one, actionPendingIntent);
			// remoteViews.setOnClickPendingIntent(R.id.button_two, configPendingIntent);
	 
			// appWidgetManager.updateAppWidget(appWidgetIds, remoteViews);
	        return remoteViews;
	    }
	    
		@Override
		public IBinder onBind(Intent intent) {
			// TODO Auto-generated method stub
			return null;
		}

	}
	
}