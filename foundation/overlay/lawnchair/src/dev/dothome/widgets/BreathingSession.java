package dev.dothome.widgets;
import org.json.JSONObject;

/** Small durable breathing state machine. No alarm, microphone or background service. */
final class BreathingSession {
  static final String[] NAMES={"Focus","Relax","Calm"};
  static final int[][] PACES={{4,4,4,4},{4,0,6,0},{4,7,8,0}};
  final int mode,phase,cycle;
  final float expansion;
  final String label;
  final long elapsed;
  BreathingSession(JSONObject item,long now){
    mode=Math.max(0,Math.min(2,item.optInt("breathMode")));
    long start=item.optLong("breathStarted");elapsed=Math.max(0,item.optLong("breathElapsed")+(start>0?Math.max(0,now-start):0));
    int[] pace=PACES[mode];int total=0;for(int n:pace)total+=n;cycle=total;
    float position=(elapsed/1000f)%total;int at=0;while(at<3 && (pace[at]==0||position>=pace[at])){position-=pace[at];at++;}phase=at;
    float t=pace[at]==0?0:position/pace[at];float smooth=t*t*(3-2*t);
    expansion=at==0?smooth:at==1?1:at==2?1-smooth:0;
    label=at==0?"Breathe in":at==2?"Breathe out":"Hold";
  }
}
