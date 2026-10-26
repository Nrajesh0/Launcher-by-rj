package dev.dothome.widgets;
import android.text.format.DateFormat;
import org.json.JSONObject;
import java.util.*;
/** Live values are exposed to Android screen readers without an Accessibility service. */
final class TileSemantics {
  static String describe(TileContext a,JSONObject item){String type=item.optString("type");return switch(type){
    case "clock"->DateFormat.getTimeFormat(a).format(new Date())+", open Clock";
    case "date"->DateFormat.getLongDateFormat(a).format(new Date())+", open calendar";
    case "battery"->"Battery "+a.battery+" percent"+(a.charging?", charging":"");
    case "away"->a.away.available?"Away time, today "+AwayTimeStats.duration(a.away.daily[6])+", view weekly details":"Away time, connect usage history";
    case "usage"->"Screen time, "+a.usage+", view app usage";
    case "agenda"->"Quick Look, "+a.agenda;
    case "notes"->"Note, "+item.optString("text","tap to write")+", edit note";
    case "photo"->{org.json.JSONArray images=item.optJSONArray("images");int count=images==null?1:images.length();yield "Photo "+(count==0?"collection, add photos":(item.optInt("imageIndex")+1)+" of "+count)+(count>1?", swipe along the bottom to change photo":"")+", edit collection";}
    case "steps"->a.steps<0?"Pedometer, activity access or step sensor unavailable":"Measured steps today, "+a.steps+" of "+item.optInt("stepGoal",6000)+", edit goal";
    case "weather"->"Manually entered weather, "+item.optString("location")+", "+item.optString("temperature","not set")+" degrees, "+item.optString("condition")+", edit weather";
    case "torch"->"Flashlight "+(a.torch?"on":"off")+", toggle flashlight";
    case "dnd"->"Do not disturb "+(a.dnd?"on":"off")+", toggle do not disturb";
    case "sound"->"Sound "+(a.ringer==android.media.AudioManager.RINGER_MODE_NORMAL?"on":a.ringer==android.media.AudioManager.RINGER_MODE_VIBRATE?"vibrate":"silent")+", toggle sound";
    case "bluetooth"->"Open Bluetooth settings";
    case "compass"->a.compassAvailable?"Magnetic heading "+Math.round(a.heading)+" degrees, compass settings":"Compass sensor unavailable";
    case "breathing"->"Breathing break, "+BreathingSession.NAMES[Math.max(0,Math.min(2,item.optInt("breathMode")))]+", "+(item.optLong("breathStarted")>0?"running":"paused")+", open guidance";
    case "media"->{TileMedia media=TileMedia.read(a);yield media.title+", "+media.artist+", "+(media.playing?"playing":"paused");}
    case "timer"->{long elapsed=item.optLong("elapsed")+(item.optLong("timerStart")>0?Math.max(0,System.currentTimeMillis()-item.optLong("timerStart")):0);yield "Elapsed timer, "+AwayTimeStats.duration(elapsed)+", "+(item.optLong("timerStart")>0?"running":"paused")+", edit timer";}
    case "countdown"->"Countdown, "+item.optString("name","a day to remember")+(item.optLong("deadline")>0?", "+DateFormat.getLongDateFormat(a).format(new Date(item.optLong("deadline"))):", choose a date");
    case "headphones"->"Audio devices, open Bluetooth settings; device battery unavailable";
    default->type+" widget";
  };}
}
