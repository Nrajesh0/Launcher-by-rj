package dev.dothome.ui;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Portable copies of owned wallpaper/collection images only, with bounded decoding. */
public final class DotBackupMedia {
  private static final int TOTAL=20*1024*1024,SINGLE=8*1024*1024;
  public static JSONObject export(Context context) throws Exception {
    JSONObject media=new JSONObject();File owned=new File(context.getFilesDir(),"collections").getCanonicalFile();int total=0;
    Set<File> files=new LinkedHashSet<>();File wallpaper=new File(context.getFilesDir(),"wallpaper-local.jpg");if(wallpaper.isFile())files.add(wallpaper);
    for(Object value:context.getSharedPreferences("dot-tiles",0).getAll().values())if(value instanceof String text)try{JSONObject item=new JSONObject(text);List<String> uris=new ArrayList<>();uris.add(item.optString("uri"));JSONArray images=item.optJSONArray("images");if(images!=null)for(int n=0;n<images.length();n++)uris.add(images.getString(n));for(String candidate:uris){Uri uri=Uri.parse(candidate);if(!"file".equals(uri.getScheme()))continue;File file=new File(uri.getPath()).getCanonicalFile();if(owned.equals(file.getParentFile())&&file.isFile())files.add(file);}}catch(JSONException ignored){}
    for(File file:files){long size=file.length();if(size>SINGLE||size+total>TOTAL||files.size()>128)throw new IOException("Photo backup exceeds size limit");byte[] bytes=Files.readAllBytes(file.toPath());total+=bytes.length;String key=file.equals(wallpaper)?"wallpaper-local.jpg":"collections/"+file.getName();media.put(key,Base64.encodeToString(bytes,Base64.NO_WRAP));}return media;
  }
  public static Map<String,byte[]> validate(JSONObject media) throws Exception {
    Map<String,byte[]> result=new LinkedHashMap<>();if(media==null)return result;if(media.length()>128)throw new IOException("Too many images");int total=0;
    for(var keys=media.keys();keys.hasNext();){String key=keys.next();if(!key.equals("wallpaper-local.jpg")&&!key.matches("collections/[0-9a-fA-F-]{36}\\.(png|webp|jpg)"))throw new IOException("Invalid image name");String value=media.getString(key);if(value.length()>SINGLE*4L/3+8)throw new IOException("Image is too large");byte[] bytes=Base64.decode(value,Base64.DEFAULT);total+=bytes.length;if(bytes.length>SINGLE||total>TOTAL)throw new IOException("Images exceed backup limit");BitmapFactory.Options info=new BitmapFactory.Options();info.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,info);if(info.outWidth<1||info.outHeight<1||info.outWidth>8192||info.outHeight>8192)throw new IOException("Invalid image");result.put(key,bytes);}return result;
  }
  public static void restore(Context context,Map<String,byte[]> media,JSONObject preferences) throws Exception {
    // Validate first. Names never come from an arbitrary path in the document.
    for(var entry:media.entrySet()){File target=new File(context.getFilesDir(),entry.getKey());target.getParentFile().mkdirs();File temporary=File.createTempFile("restore-",".tmp",target.getParentFile());try{Files.write(temporary.toPath(),entry.getValue());Files.move(temporary.toPath(),target.toPath(),StandardCopyOption.REPLACE_EXISTING);}finally{temporary.delete();}}
    JSONObject tiles=preferences.optJSONObject("dot-tiles");if(tiles==null)return;
    for(var keys=tiles.keys();keys.hasNext();){String key=keys.next();if(!(tiles.opt(key) instanceof String value))continue;JSONObject tile;try{tile=new JSONObject(value);}catch(JSONException invalid){continue;}
      String uri=remap(context,tile.optString("uri"),media);if(uri!=null)tile.put("uri",uri);JSONArray images=tile.optJSONArray("images");if(images!=null)for(int n=0;n<images.length();n++){String mapped=remap(context,images.optString(n),media);if(mapped!=null)images.put(n,mapped);}tiles.put(key,tile.toString());
    }
  }
  private static String remap(Context context,String value,Map<String,byte[]> media){Uri uri=Uri.parse(value);if(!"file".equals(uri.getScheme())||uri.getPath()==null)return null;String name=new File(uri.getPath()).getName();String key="collections/"+name;return media.containsKey(key)?Uri.fromFile(new File(context.getFilesDir(),key)).toString():null;}
}
