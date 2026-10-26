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
      context.compassAvailable=sensor!=null;
      if(sensor!=null)sensors.registerListener(this,sensor,SensorManager.SENSOR_DELAY_UI);
    }
    if(kind.equals("steps")&&sensors!=null&&context.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)==android.content.pm.PackageManager.PERMISSION_GRANTED){Sensor counter=sensors.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);if(counter!=null)try{sensors.registerListener(this,counter,SensorManager.SENSOR_DELAY_NORMAL);}catch(SecurityException denied){context.steps=-1;}}
    if((kind.equals("torch")||kind.equals("controls")) && cameras!=null)try {cameras.registerTorchCallback(torch,new Handler(Looper.getMainLooper()));}catch(Exception ignored){}
  }
  void stop() { if(!registered)return;registered=false;if(sensors!=null)sensors.unregisterListener(this);if(cameras!=null)try {cameras.unregisterTorchCallback(torch);}catch(Exception ignored){} }
  @Override public void onSensorChanged(SensorEvent event) {
    if(event.sensor.getType()==Sensor.TYPE_STEP_COUNTER){
      var prefs=context.getSharedPreferences("dot-steps",0);String day=java.time.LocalDate.now().toString();int boot=android.provider.Settings.Global.getInt(context.getContentResolver(),android.provider.Settings.Global.BOOT_COUNT,-1);float reading=event.values[0];
      try{org.json.JSONObject state=new org.json.JSONObject(prefs.getString("state","{}"));context.steps=StepCounterState.update(state,day,boot,reading,System.currentTimeMillis());prefs.edit().putString("state",state.toString()).putString("day",day).putInt("count",context.steps).apply();}catch(org.json.JSONException invalid){context.steps=-1;}
      changed.run();return;
    }
    float[] rotation=new float[9],angles=new float[3];SensorManager.getRotationMatrixFromVector(rotation,event.values);SensorManager.getOrientation(rotation,angles);context.heading=(float)((Math.toDegrees(angles[0])+360)%360);changed.run();
  }
  @Override public void onAccuracyChanged(Sensor sensor,int accuracy) {context.sensorAccuracy=accuracy;changed.run();}
}
