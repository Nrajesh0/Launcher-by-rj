package dev.dothome.widgets;
import org.json.JSONObject;
/** Pure state transition: never infer steps before the first reading of a boot/day. */
final class StepCounterState {
  static int update(JSONObject state,String day,int boot,float reading,long now)throws org.json.JSONException {
    boolean sameDay=day.equals(state.optString("day"));float baseline=(float)state.optDouble("baseline",reading);
    int carried=state.optInt("carried",0);
    if(!sameDay){baseline=reading;carried=0;state.put("since",now);}
    else if(state.optInt("boot",-2)!=boot||reading<baseline){baseline=reading;carried=Math.max(0,state.optInt("count",0));}
    int count=(int)Math.min(Integer.MAX_VALUE,carried+Math.max(0,Math.round(reading-baseline))*(long)1);
    state.put("day",day).put("boot",boot).put("baseline",baseline).put("carried",carried).put("count",count);return count;
  }
}
