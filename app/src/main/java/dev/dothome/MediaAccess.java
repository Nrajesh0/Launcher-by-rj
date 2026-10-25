package dev.dothome;
import android.app.Notification;
import android.content.Intent;
import android.service.notification.*;
import java.util.*;
/** Local package-level dots only. Notification bodies are never persisted. */
public final class MediaAccess extends NotificationListenerService {
 @Override public void onListenerConnected(){update();}
 @Override public void onNotificationPosted(StatusBarNotification notification){update();}
 @Override public void onNotificationRemoved(StatusBarNotification notification){update();}
 @Override public void onListenerDisconnected(){getSharedPreferences("runtime",0).edit().remove("badgePackages").apply();sendBroadcast(new Intent("dev.dothome.NOTIFICATION_CHANGED").setPackage(getPackageName()));}
 void update(){try{Set<String> packages=new HashSet<>();StatusBarNotification[] active=getActiveNotifications();if(active!=null)for(StatusBarNotification n:active)if((n.getNotification().flags&Notification.FLAG_ONGOING_EVENT)==0)packages.add(n.getPackageName());getSharedPreferences("runtime",0).edit().putStringSet("badgePackages",packages).apply();sendBroadcast(new Intent("dev.dothome.NOTIFICATION_CHANGED").setPackage(getPackageName()));}catch(SecurityException ignored){}}
}
