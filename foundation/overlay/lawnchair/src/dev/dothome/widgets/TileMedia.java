package dev.dothome.widgets;

import android.content.*;
import android.media.*;
import android.media.session.*;
import android.provider.Settings;
import java.util.*;

/** Live media metadata stays in memory and uses only the optional listener grant. */
final class TileMedia {
  final MediaController controller;
  final String title, artist;
  final android.graphics.Bitmap artwork;
  final boolean playing;
  final boolean denied;
  final long actions;
  TileMedia(MediaController controller,String title,String artist,android.graphics.Bitmap artwork,boolean playing) {
    this.controller=controller;this.title=title;this.artist=artist;this.artwork=artwork;this.playing=playing;denied=controller==null&&title.equals("Connect media");actions=controller!=null&&controller.getPlaybackState()!=null?controller.getPlaybackState().getActions():0;
  }
  static TileMedia read(Context context) {
    try {
      List<MediaController> sessions=context.getSystemService(MediaSessionManager.class).getActiveSessions(new ComponentName(context,dev.dothome.MediaAccess.class));
      MediaController current=null;
      for(MediaController candidate:sessions) {
        if(current==null)current=candidate;
        if(candidate.getPlaybackState()!=null && candidate.getPlaybackState().getState()==PlaybackState.STATE_PLAYING) {current=candidate;break;}
      }
      if(current==null)return new TileMedia(null,"Nothing playing","Open your music app",null,false);
      MediaMetadata metadata=current.getMetadata();
      String title=metadata==null?"Media":metadata.getString(MediaMetadata.METADATA_KEY_TITLE);
      String artist=metadata==null?"":metadata.getString(MediaMetadata.METADATA_KEY_ARTIST);
      android.graphics.Bitmap art=metadata==null?null:metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
      if(art==null && metadata!=null)art=metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
      boolean playing=current.getPlaybackState()!=null && current.getPlaybackState().getState()==PlaybackState.STATE_PLAYING;
      return new TileMedia(current,title==null?"Media":title,artist==null?"":artist,art,playing);
    } catch(SecurityException denied) { return new TileMedia(null,"Connect media","Tap to allow notification access",null,false); }
  }
  boolean supports(int action){return controller==null||((actions&(action<0?PlaybackState.ACTION_SKIP_TO_PREVIOUS:action>0?PlaybackState.ACTION_SKIP_TO_NEXT:(PlaybackState.ACTION_PLAY_PAUSE|(playing?PlaybackState.ACTION_PAUSE:PlaybackState.ACTION_PLAY))))!=0);}
  void action(Context context,int action) {
    try {
      if(controller==null) {
        if(denied)context.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        else {Intent music=Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,Intent.CATEGORY_APP_MUSIC).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);if(music.resolveActivity(context.getPackageManager())!=null)context.startActivity(music);else android.widget.Toast.makeText(context,"Start playback in your music app",android.widget.Toast.LENGTH_SHORT).show();}return;
      }
      if(!supports(action))return;
      var controls=controller.getTransportControls();
      if(action<0) controls.skipToPrevious();
      else if(action>0) controls.skipToNext();
      else if(playing) controls.pause(); else controls.play();
    } catch(Exception unavailable) {android.widget.Toast.makeText(context,"Media control is unavailable",android.widget.Toast.LENGTH_SHORT).show();}
  }
}
