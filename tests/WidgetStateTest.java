package dev.dothome.widgets;
import org.json.JSONObject;
public final class WidgetStateTest {
  static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
  static BreathingSession breath(int mode,long elapsed)throws Exception{return new BreathingSession(new JSONObject().put("breathMode",mode).put("breathElapsed",elapsed),100000);}
  public static void main(String[] args)throws Exception{
    check(breath(0,0).phase==0&&breath(0,0).expansion==0,"Focus begins at inhale");
    check(breath(0,4000).phase==1&&breath(0,4000).expansion==1,"Focus inhale boundary");
    check(breath(0,8000).phase==2&&breath(0,8000).expansion==1,"Focus exhale boundary");
    check(breath(0,12000).phase==3&&breath(0,12000).expansion==0,"Focus hold boundary");
    check(breath(0,16000).phase==0,"Focus cycles");
    check(breath(1,4000).phase==2,"Relax skips zero-length hold");
    check(breath(1,10000).phase==0,"Relax cycles past zero final hold");
    check(breath(2,11000).phase==2&&breath(2,19000).phase==0,"Calm 4/7/8 pacing");
    BreathingSession active=new BreathingSession(new JSONObject().put("breathElapsed",1500).put("breathStarted",98000),100000);
    check(active.elapsed==3500,"Resume preserves accumulated time");
    check(new BreathingSession(new JSONObject().put("breathStarted",110000),100000).elapsed==0,"Backward wall clock never produces negative time");
    check(breath(1,Long.MAX_VALUE).expansion>=0&&breath(1,Long.MAX_VALUE).expansion<=1,"Long sessions retain bounded phase");
    JSONObject state=new JSONObject();
    check(StepCounterState.update(state,"2026-10-26",1,1000,10)==0,"First reading is baseline, not lifetime steps");
    check(StepCounterState.update(state,"2026-10-26",1,1420,20)==420,"Same boot measures difference");
    check(StepCounterState.update(state,"2026-10-26",2,12,30)==420,"Reboot retains measured steps but never infers missing steps");
    check(StepCounterState.update(state,"2026-10-26",2,22,40)==430,"New boot adds measured difference");
    check(StepCounterState.update(state,"2026-10-27",2,100,50)==0,"New date resets baseline");
    check(StepCounterState.update(state,"2026-10-27",2,125,60)==25,"New day counts normally");
    check(StepCounterState.update(state,"2026-10-27",2,1,70)==25,"Sensor counter reset never makes a negative count");
    System.out.println("PASS: breathing phase, pause/resume and clock changes; step first-use/day/reboot/reset transitions");
  }
}
