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
  /** Stage fresh names so a failed restore never overwrites an existing collection. */
  public static final class Stage implements AutoCloseable {
    final Context context;final List<File> created=new ArrayList<>();
    boolean accepted;
    Stage(Context context){this.context=context;}
    public void accept(){accepted=true;}
    @Override public void close() throws IOException {
      if(!accepted){
        for(File file:created)Files.deleteIfExists(file.toPath());
      }

    }
  }
  public static Stage stage(Context context,Map<String,byte[]> media,JSONObject preferences) throws Exception {
    Stage stage=new Stage(context);Map<String,String> paths=new HashMap<>();
    try {
      for(var entry:media.entrySet()){
        File directory=new File(context.getFilesDir(),"collections");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot create collection");
        String suffix=entry.getKey().substring(entry.getKey().lastIndexOf('.'));
        File target=new File(directory,UUID.randomUUID()+suffix);stage.created.add(target);Files.write(target.toPath(),entry.getValue());paths.put(entry.getKey(),Uri.fromFile(target).toString());if(entry.getKey().equals("wallpaper-local.jpg")){JSONObject style=preferences.optJSONObject("dot-style");if(style==null){style=new JSONObject();preferences.put("dot-style",style);}style.put("backupWallpaper",Uri.fromFile(target).toString());}
      }
      JSONObject tiles=preferences.optJSONObject("dot-tiles");if(tiles==null)return stage;
      for(var keys=tiles.keys();keys.hasNext();){String key=keys.next();if(!(tiles.opt(key) instanceof String value))continue;JSONObject tile;try{tile=new JSONObject(value);}catch(JSONException invalid){continue;}
        String uri=remap(tile.optString("uri"),paths);if(uri!=null)tile.put("uri",uri);
        JSONArray images=tile.optJSONArray("images");if(images!=null)for(int n=0;n<images.length();n++){String mapped=remap(images.optString(n),paths);if(mapped!=null)images.put(n,mapped);}tiles.put(key,tile.toString());
      }
      return stage;
    }catch(Exception error){try{stage.close();}catch(Exception ignored){}throw error;}
  }
  private static String remap(String value,Map<String,String> paths){Uri uri=Uri.parse(value);if(!"file".equals(uri.getScheme())||uri.getPath()==null)return null;return paths.get("collections/"+new File(uri.getPath()).getName());}
}
