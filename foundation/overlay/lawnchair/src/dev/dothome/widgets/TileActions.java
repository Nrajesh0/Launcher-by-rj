package dev.dothome.widgets;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.provider.*;
import org.json.JSONObject;

final class TileActions {
  static Activity activity(Context context) {
    while(context instanceof ContextWrapper) { if(context instanceof Activity) return (Activity)context; context=((ContextWrapper)context).getBaseContext(); }
    return context instanceof Activity?(Activity)context:null;
  }
  static void tap(Context context,long id,JSONObject item) {
    Activity home=activity(context); if(home==null) return;
    try {
      switch(item.optString("type")) {
        case "media": TileMedia.read(context).action(context,0); break;
        case "clock": home.startActivity(new Intent(AlarmClock.ACTION_SHOW_ALARMS)); break;
        case "date": home.startActivity(new Intent(Intent.ACTION_VIEW,CalendarContract.CONTENT_URI)); break;
        case "bluetooth": home.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); break;
        case "sound": {
          android.media.AudioManager audio=context.getSystemService(android.media.AudioManager.class);
          try{audio.setRingerMode(audio.getRingerMode()==android.media.AudioManager.RINGER_MODE_NORMAL?android.media.AudioManager.RINGER_MODE_VIBRATE:android.media.AudioManager.RINGER_MODE_NORMAL);}catch(SecurityException needsAccess){home.startActivity(new Intent(Settings.ACTION_SOUND_SETTINGS));}break;
        }
        case "dnd": {
          NotificationManager notifications=context.getSystemService(NotificationManager.class);
          if(!notifications.isNotificationPolicyAccessGranted()){home.startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));break;}
          notifications.setInterruptionFilter(notifications.getCurrentInterruptionFilter()==NotificationManager.INTERRUPTION_FILTER_ALL?NotificationManager.INTERRUPTION_FILTER_PRIORITY:NotificationManager.INTERRUPTION_FILTER_ALL);break;
        }
        case "steps": {
          if(context.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)!=PackageManager.PERMISSION_GRANTED)home.requestPermissions(new String[]{android.Manifest.permission.ACTIVITY_RECOGNITION},23);
          else home.startActivity(new Intent(context,TileConfiguration.class).putExtra("itemId",id).putExtra("initial",item.toString()));break;
        }
        case "headphones": home.startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));break;
        case "usage": case "away": case "breathing": home.startActivity(new Intent(context,TileDetails.class).putExtra("itemId",id).putExtra("initial",item.toString())); break;
        case "agenda":
          if(context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED) home.requestPermissions(new String[]{android.Manifest.permission.READ_CALENDAR},21);
          else home.startActivity(new Intent(Intent.ACTION_VIEW,CalendarContract.CONTENT_URI));
          break;
        case "torch":
          if(context.checkSelfPermission(android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) { home.requestPermissions(new String[]{android.Manifest.permission.CAMERA},22); return; }
          CameraManager manager=context.getSystemService(CameraManager.class);
          for(String camera:manager.getCameraIdList()) if(Boolean.TRUE.equals(manager.getCameraCharacteristics(camera).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
            boolean active=!context.getSharedPreferences("dot-style",0).getBoolean("torchActive",false); manager.setTorchMode(camera,active); item.put("torchActive",active); TileContext.save(context,id,item); break;
          }
          break;
        default: home.startActivity(new Intent(context,TileConfiguration.class).putExtra("itemId",id).putExtra("initial",item.toString()));
      }
    } catch(Exception unavailable) { android.widget.Toast.makeText(context,"This action is unavailable",android.widget.Toast.LENGTH_SHORT).show(); }
  }
}
