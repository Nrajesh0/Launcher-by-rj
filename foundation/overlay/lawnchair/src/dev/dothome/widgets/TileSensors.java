package dev.dothome.widgets;
import android.hardware.*;
import android.hardware.camera2.CameraManager;
import android.os.*;

/** Sensors and torch observation follow the visible widget lifecycle. */
final class TileSensors implements SensorEventListener {
  final TileContext context;
  final String kind;
  final Runnable changed;
  final SensorManager sensors;
  final CameraManager cameras;
  boolean registered;
  final CameraManager.TorchCallback torch=new CameraManager.TorchCallback() {
    @Override public void onTorchModeChanged(String id,boolean enabled) {context.torch=enabled;context.getSharedPreferences("dot-style",0).edit().putBoolean("torchActive",enabled).apply();changed.run();}
    @Override public void onTorchModeUnavailable(String id) {context.torch=false;changed.run();}
  };
  TileSensors(TileContext context,String kind,Runnable changed) {this.context=context;this.kind=kind;this.changed=changed;sensors=context.getSystemService(SensorManager.class);cameras=context.getSystemService(CameraManager.class);}
  void start() {
    if(registered)return;registered=true;
    if(kind.equals("compass") && sensors!=null) {
      Sensor sensor=sensors.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
      if(sensor!=null)sensors.registerListener(this,sensor,SensorManager.SENSOR_DELAY_UI);
    }
    if(kind.equals("torch") && cameras!=null)try {cameras.registerTorchCallback(torch,new Handler(Looper.getMainLooper()));}catch(Exception ignored){}
  }
  void stop() { if(!registered)return;registered=false;if(sensors!=null)sensors.unregisterListener(this);if(cameras!=null)try {cameras.unregisterTorchCallback(torch);}catch(Exception ignored){} }
  @Override public void onSensorChanged(SensorEvent event) {float[] rotation=new float[9],angles=new float[3];SensorManager.getRotationMatrixFromVector(rotation,event.values);SensorManager.getOrientation(rotation,angles);context.heading=(float)((Math.toDegrees(angles[0])+360)%360);changed.run();}
  @Override public void onAccuracyChanged(Sensor sensor,int accuracy) {}
}
