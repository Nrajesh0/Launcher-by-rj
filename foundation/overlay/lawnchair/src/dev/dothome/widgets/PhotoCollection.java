package dev.dothome.widgets;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.util.*;

/** Owned, bounded local images survive document-provider revocation and layout backup. */
final class PhotoCollection {
  static final int MAX_IMAGES=24;
  static Uri importImage(Context context,Uri input) throws Exception {
    Bitmap bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(),input),(decoder,info,source)->{decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);decoder.setTargetSampleSize(Math.max(1,(Math.max(info.getSize().getWidth(),info.getSize().getHeight())+1599)/1600));});
    File directory=new File(context.getFilesDir(),"collections");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Photo folder unavailable");
    File image=new File(directory,UUID.randomUUID()+".webp");try(FileOutputStream out=new FileOutputStream(image)){if(!bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY,92,out))throw new IOException("Photo could not be saved");}catch(IOException failed){image.delete();throw failed;}finally{bitmap.recycle();}
    return Uri.fromFile(image);
  }
  static String uri(JSONObject item){JSONArray images=item.optJSONArray("images");if(images==null||images.length()==0)return item.optString("uri");return images.optString(Math.floorMod(item.optInt("imageIndex"),images.length()),item.optString("uri"));}
  static boolean cycle(JSONObject item,int delta){JSONArray images=item.optJSONArray("images");if(images==null||images.length()<2)return false;try{int index=Math.floorMod(item.optInt("imageIndex")+delta,images.length());item.put("imageIndex",index).put("uri",images.getString(index));return true;}catch(JSONException invalid){return false;}}
  static void move(JSONObject item,int delta){JSONArray images=item.optJSONArray("images");if(images==null||images.length()<2)return;int index=Math.floorMod(item.optInt("imageIndex"),images.length()),next=index+delta;if(next<0||next>=images.length())return;try{Object other=images.get(next);images.put(next,images.get(index));images.put(index,other);item.put("imageIndex",next).put("uri",images.getString(next));}catch(JSONException ignored){}}
  static void remove(JSONObject item){JSONArray images=item.optJSONArray("images");if(images==null)images=new JSONArray().put(item.optString("uri"));if(images.length()>0)images.remove(Math.floorMod(item.optInt("imageIndex"),images.length()));try{item.put("images",images).put("imageIndex",0).put("uri",images.optString(0,""));}catch(JSONException ignored){}}
  static void prune(Context context){
    // A grace period protects an editor's unsaved imports and a recent undo.
    TileContext.WORK.execute(()->{File directory=new File(context.getFilesDir(),"collections");File[] files=directory.listFiles();if(files==null)return;Set<String> used=new HashSet<>();
      for(Object value:context.getSharedPreferences("dot-tiles",0).getAll().values())if(value instanceof String text)try{JSONObject item=new JSONObject(text);used.add(item.optString("uri"));JSONArray images=item.optJSONArray("images");if(images!=null)for(int n=0;n<images.length();n++)used.add(images.optString(n));}catch(JSONException ignored){}
      used.add(context.getSharedPreferences("dot-style",0).getString("backupWallpaper",""));
      long older=System.currentTimeMillis()-86400000L;for(File file:files)if(file.lastModified()<older&&!used.contains(Uri.fromFile(file).toString()))file.delete();
    });
  }
  /** Edge-connected colour removal, explicitly offered for plain backgrounds only. */
  static Uri removePlainBackground(Context context,Uri uri,int tolerance) throws Exception {
    Bitmap source=ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(),uri),(d,i,s)->{d.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);d.setTargetSampleSize(Math.max(1,(Math.max(i.getSize().getWidth(),i.getSize().getHeight())+999)/1000));});
    int width=source.getWidth(),height=source.getHeight();int[] pixels=new int[width*height];source.getPixels(pixels,0,width,0,0,width,height);source.recycle();
    int[] queue=new int[pixels.length];boolean[] seen=new boolean[pixels.length];int head=0,tail=0;
    int[] samples={pixels[0],pixels[width-1],pixels[pixels.length-width],pixels[pixels.length-1]};
    for(int y=0;y<height;y++)for(int x=0;x<width;x++)if(y==0||x==0||y==height-1||x==width-1){int index=y*width+x;if(matches(pixels[index],samples,tolerance)){seen[index]=true;queue[tail++]=index;}}
    while(head<tail){int index=queue[head++],x=index%width,y=index/width;pixels[index]=Color.TRANSPARENT;int[] neighbours={x>0?index-1:-1,x+1<width?index+1:-1,y>0?index-width:-1,y+1<height?index+width:-1};for(int next:neighbours)if(next>=0&&!seen[next]){seen[next]=true;if(matches(pixels[next],samples,tolerance))queue[tail++]=next;}}
    Bitmap result=Bitmap.createBitmap(pixels,width,height,Bitmap.Config.ARGB_8888);File output=new File(new File(context.getFilesDir(),"collections"),UUID.randomUUID()+".png");output.getParentFile().mkdirs();try(FileOutputStream out=new FileOutputStream(output)){result.compress(Bitmap.CompressFormat.PNG,100,out);}finally{result.recycle();}return Uri.fromFile(output);
  }
  private static boolean matches(int pixel,int[] samples,int tolerance){for(int sample:samples){int r=Color.red(pixel)-Color.red(sample),g=Color.green(pixel)-Color.green(sample),b=Color.blue(pixel)-Color.blue(sample);if(r*r+g*g+b*b<=tolerance*tolerance*3)return true;}return Color.alpha(pixel)==0;}
}
