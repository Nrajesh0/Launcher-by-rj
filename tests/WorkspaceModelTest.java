import dev.dothome.WorkspaceModel;
import org.json.*;
public class WorkspaceModelTest {
 public static void main(String[] args)throws Exception{
  JSONArray old=new JSONArray("[{id:'clock',type:'clock',page:0,w:4,h:2},{id:'a',type:'app',page:0,w:1,h:1},{id:'b',type:'app',page:0,w:1,h:1}]");
  JSONArray grid=WorkspaceModel.normalise(old,4,6);
  if(old.getJSONObject(0).has("x"))throw new AssertionError("Migration mutated source");
  JSONObject a=grid.getJSONObject(1);WorkspaceModel.move(grid,a,0,3,5,4,6);
  String once=grid.toString();if(!once.equals(WorkspaceModel.normalise(grid,4,6).toString()))throw new AssertionError("Deliberate empty cells changed");
  try{WorkspaceModel.move(grid,a,0,0,0,4,6);throw new AssertionError("Collision accepted");}catch(JSONException expected){}
  if(a.getInt("x")!=3||a.getInt("y")!=5)throw new AssertionError("Rejected drop changed position");
  if(WorkspaceModel.fits(grid,"a",0,-1,2,1,1,4,6)||WorkspaceModel.fits(grid,"a",0,4,2,1,1,4,6))throw new AssertionError("Bounds accepted");
  JSONArray busy=new JSONArray();for(int i=0;i<100;i++)busy.put(new JSONObject().put("id","item"+i).put("w",1+i%4).put("h",1+i%3).put("page",0));
  JSONArray placed=WorkspaceModel.normalise(busy,4,6);for(int i=0;i<placed.length();i++){JSONObject p=placed.getJSONObject(i);if(!WorkspaceModel.fits(placed,p.getString("id"),p.getInt("page"),p.getInt("x"),p.getInt("y"),p.getInt("w"),p.getInt("h"),4,6))throw new AssertionError("Collision in migrated layout");}
  System.out.println("Workspace migration, empty-space preservation, rejected drops, bounds and 100 mixed placements passed");
 }
}
