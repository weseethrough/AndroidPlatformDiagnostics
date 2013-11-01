
package com.glassfitgames.glassfitplatformdemo;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.glassfitgames.glassfitplatform.models.Orientation;
import com.glassfitgames.glassfitplatform.sensors.SensorService;
import com.glassfitgames.glassfitplatform.utils.FileUtils;
import com.roscopeco.ormdroid.Entity;

import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class OrientationDiagnostics extends Activity {
    
    private final boolean WRITE_TO_CSV = false;
    private final boolean WRITE_TO_DATABASE = false;
    private final boolean CLEAR_DATABASE_ON_START = false;    
    
    private SensorService sensorService;
    private TextView orientationText;
    
    private Timer timer;
    private OrientationTask task;
    private List<Orientation> orientationCache = new ArrayList<Orientation>();    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orientation_diagnostics);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        orientationText = (TextView)findViewById(R.id.orientationTextview);
        
        // clear existing orientations from database
        if (CLEAR_DATABASE_ON_START) {
            List<Orientation> existingOrientations = Entity.query(Orientation.class).executeMulti();
            for (Orientation o : existingOrientations) {
                o.delete();
            }
        }        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.orientation_diagnostics, menu);
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        bindService(new Intent(this, SensorService.class), sensorServiceConnection,
                        Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (task != null) task.cancel();
        unbindService(sensorServiceConnection);
        if (WRITE_TO_CSV) {
            try {
                File f = FileUtils.createSdCardFile(this.getApplicationContext(), "OrientationData.csv");
                (new Orientation()).headersToCsv(f);
                for (Orientation o : orientationCache) {
                    o.toCsv(f);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (WRITE_TO_DATABASE) {
            for (Orientation o : orientationCache) {
                o.save();
            }
        }
        orientationCache.clear();        
    }
    
    private class OrientationTask extends TimerTask {
        public void run() {
            runOnUiThread(new Runnable() {
                public void run() {
                    getCurrentOrientation();
                }
            });
        }
    }
    
    private ServiceConnection sensorServiceConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            sensorService = ((SensorService.SensorServiceBinder)binder).getService();
            Log.d("GlassFitPlatform", "OrientationHelper has bound to SensorService");
            
            // start polling the sensors
            timer = new Timer();
            task = new OrientationTask();
            timer.scheduleAtFixedRate(task, 0, 50);
        }

        public void onServiceDisconnected(ComponentName className) {
            sensorService = null;
            Log.d("GlassFitPlatform", "OrientationHelper has unbound from SensorService");
        }
    };
    
    @SuppressWarnings("unused")
    public void getCurrentOrientation() {

        Log.v("GlassFitPlatform", "Accel: x:" + sensorService.getAccValues()[0] + ", y:"
                        + sensorService.getAccValues()[1] + ", z:"
                        + sensorService.getAccValues()[2] + "m/s/s.");
        Log.v("GlassFitPlatform", "Gyro: x:" + sensorService.getGyroValues()[0] + ", y:"
                        + sensorService.getGyroValues()[1] + ", z:"
                        + sensorService.getGyroValues()[2] + "rad.");
        Log.v("GlassFitPlatform", "Mag: x:" + sensorService.getMagValues()[0] + ", y:"
                        + sensorService.getMagValues()[1] + ", z:"
                        + sensorService.getMagValues()[2] + "uT.");

        String oText = new String();
        DecimalFormat df = new DecimalFormat("+000.00");

        oText += "Accel:   x:" + df.format(sensorService.getAccValues()[0]) + ",   y:"
                        + df.format(sensorService.getAccValues()[1]) + ",   z:"
                        + df.format(sensorService.getAccValues()[2]) + "m/s/s.\n";
        oText += "Gyro:   x:" + df.format(sensorService.getGyroValues()[0]) + ",   y:"
                        + df.format(sensorService.getGyroValues()[1]) + ",   z:"
                        + df.format(sensorService.getGyroValues()[2]) + "rad.\n";
        oText += "Mag:   x:" + df.format(sensorService.getMagValues()[0]) + ",   y:"
                        + df.format(sensorService.getMagValues()[1]) + ",   z:"
                        + df.format(sensorService.getMagValues()[2]) + "uT.\n";
        oText += "\n";

        oText += "Fusion YPR: "
                        + df.format(Math.toDegrees(sensorService.getGlassfitQuaternion().toYpr()[0]))
                        + " / "
                        + df.format(Math.toDegrees(sensorService.getGlassfitQuaternion().toYpr()[1]))
                        + " / "
                        + df.format(Math.toDegrees(sensorService.getGlassfitQuaternion().toYpr()[2]))
                        + "\n";

        oText += "GyroDroid YPR: "
                        + df.format(Math.toDegrees(sensorService.getGyroDroidQuaternion().toYpr()[0]))
                        + " / "
                        + df.format(Math.toDegrees(sensorService.getGyroDroidQuaternion().toYpr()[1]))
                        + " / "
                        + df.format(Math.toDegrees(sensorService.getGyroDroidQuaternion().toYpr()[2]))
                        + "\n";

        orientationText.setText(oText);

        if (WRITE_TO_CSV || WRITE_TO_DATABASE) {
            Orientation o = new Orientation();
            o.setAccelerometer(sensorService.getAccValues());
            o.setGyroscope(sensorService.getGyroValues());
            o.setMagnetometer(sensorService.getMagValues());
            o.setYawPitchRoll(sensorService.getGlassfitQuaternion().toYpr());
            o.setLinearAcceleration(sensorService.getLinAccValues());
            o.setTimestamp(System.currentTimeMillis());
            orientationCache.add(o);
        }
    }

}
