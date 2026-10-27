package dev.dothome.ui;
import android.content.*;
import android.net.Uri;
import android.provider.OpenableColumns;
import org.json.*;
import java.util.*;

/** Names of explicitly selected documents only. No storage scan or file-content indexing. */
public final class DotDocuments {
  public static final int LIMIT=32;
  public static final class Document {
    public final String uri,name,mime;public final long size,modified;
    Document(JSONObject row){uri=row.optString("uri");name=row.optString("name");mime=row.optString("mime","application/octet-stream");size=row.optLong("size");modified=row.optLong("modified");}
  }
  public static synchronized JSONArray entries(Context context){try{return new JSONArray(context.getSharedPreferences("dot-documents",0).getString("items","[]"));}catch(JSONException invalid){return new JSONArray();}}
  public static void add(Context context,List<Uri> sources){
    for(Uri uri:sources){if(!"content".equals(uri.getScheme()))continue;try{
      String name=null;long size=0,modified=0;
      try(var cursor=context.getContentResolver().query(uri,null,null,null,null)){if(cursor!=null&&cursor.moveToFirst()){int column=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(column>=0)name=cursor.getString(column);column=cursor.getColumnIndex(OpenableColumns.SIZE);if(column>=0&&!cursor.isNull(column))size=Math.max(0,cursor.getLong(column));column=cursor.getColumnIndex("last_modified");if(column>=0&&!cursor.isNull(column))modified=cursor.getLong(column);}}
      if(name==null||name.isBlank())name="Document";if(name.length()>200)name=name.substring(0,200);
      String mime=context.getContentResolver().getType(uri);JSONObject row=new JSONObject().put("uri",uri.toString()).put("name",name).put("size",size).put("modified",modified).put("mime",mime==null?"application/octet-stream":mime);
      synchronized(DotDocuments.class){JSONArray items=entries(context);boolean replaced=false;for(int n=0;n<items.length();n++)if(uri.toString().equals(items.getJSONObject(n).optString("uri"))){items.put(n,row);replaced=true;break;}if(!replaced&&items.length()<LIMIT)items.put(row);store(context,items);}
    }catch(Exception unavailable){android.util.Log.w("DotDocuments","Selected document is unavailable");}}
  }
  private static void store(Context context,JSONArray entries){context.getSharedPreferences("dot-documents",0).edit().putString("items",entries.toString()).apply();}
  public static synchronized void retain(Context context,boolean[] selected){JSONArray before=entries(context),after=new JSONArray();for(int n=0;n<before.length();n++)if(n<selected.length&&selected[n])after.put(before.opt(n));store(context,after);}
  private static boolean readable(Context context,Uri uri){
    if(context.checkUriPermission(uri,android.os.Process.myPid(),android.os.Process.myUid(),Intent.FLAG_GRANT_READ_URI_PERMISSION)==android.content.pm.PackageManager.PERMISSION_GRANTED)return true;
    var provider=context.getPackageManager().resolveContentProvider(uri.getAuthority(),0);return provider!=null&&provider.applicationInfo.uid==android.os.Process.myUid();
  }
  public static List<Document> search(Context context,String query){
    if(query==null||query.isBlank()||!context.getSharedPreferences("dot-style",0).getBoolean("searchDocuments",false))return Collections.emptyList();
    ArrayList<Document> found=new ArrayList<>();String needle=query.toLowerCase(Locale.getDefault());JSONArray items=entries(context);
    for(int n=0;n<items.length();n++)try{Document document=new Document(items.getJSONObject(n));Uri uri=Uri.parse(document.uri);if(document.name.toLowerCase(Locale.getDefault()).contains(needle)&&"content".equals(uri.getScheme())&&readable(context,uri))found.add(document);}catch(Exception invalid){}
    return found;
  }
}
