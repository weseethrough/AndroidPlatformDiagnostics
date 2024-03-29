
package com.glassfitgames.glassfitplatformdemo;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.glassfitgames.glassfitplatform.gpstracker.AbstractTracker;
import com.glassfitgames.glassfitplatform.gpstracker.GPSTracker;
import com.glassfitgames.glassfitplatform.gpstracker.LifeFitnessTracker;
import com.glassfitgames.glassfitplatform.gpstracker.TargetTracker;
import com.glassfitgames.glassfitplatform.gpstracker.Helper;
import com.glassfitgames.glassfitplatform.models.Orientation;
import com.glassfitgames.glassfitplatform.models.Position;
import com.glassfitgames.glassfitplatform.models.Track;
import com.glassfitgames.glassfitplatform.points.PointsHelper;
import com.glassfitgames.glassfitplatform.utils.FileUtils;
import com.roscopeco.ormdroid.Entity;
import com.roscopeco.ormdroid.ORMDroidApplication;


/**
 * Demo use-case for the GPSTracker and TargetTracker classes in GlassFitPlatform. Understanding
 * this is a good place to start if you want to build games that rely on the GPS/target
 * functionality of the platform.
 * <p>
 * Displays simple buttons to initialize GPS/target trackers, start/stop tracking and display
 * current speed/distance/time metric of the device vs. the target.
 */
public class GpsTestActivity extends Activity {

    private Context context;
    
    private Helper helper;
    private AbstractTracker gpsTracker;
    private TargetTracker targetTracker;
    private PointsHelper pointsHelper;

    private TextView testLocationText;

    private Button initGpsButton;
    private Button initFakeGpsButton;
    private Button startTrackingButton;
    private Button stopTrackingButton;
    private Button resetButton;
    private Button syncButton;
    
    private Timer timer = new Timer();
    private GpsTask task;
    
    BufferedWriter out;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.testgps);
        context = this.getApplicationContext();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        testLocationText = (TextView)findViewById(R.id.testLocationText);
        initGpsButton = (Button)findViewById(R.id.initGpsButton);
        initFakeGpsButton = (Button)findViewById(R.id.initFakeGpsButton);
        startTrackingButton = (Button)findViewById(R.id.startTrackingButton);
        stopTrackingButton = (Button)findViewById(R.id.stopTrackingButton);
        resetButton = (Button)findViewById(R.id.resetButton);
        syncButton = (Button)findViewById(R.id.SyncButton);

        initGpsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                
                helper = Helper.getInstance(context);
                pointsHelper = PointsHelper.getInstance(context);
                pointsHelper.setBaseSpeed(2.5f);
                try {
                    gpsTracker = LifeFitnessTracker.getInstance();
                    gpsTracker.init(getApplicationContext());
                    //lfTracker.connect();
                    gpsTracker.startNewTrack();
                    gpsTracker.startTracking();
                    
                    //gpsTracker = helper.getGPSTracker();
                    //gpsTracker.init(getApplicationContext());
                } catch (Exception e) {
                    testLocationText.setText("Couldn't instatiate GPS tracker");
                }
                targetTracker = helper.getFauxTargetTracker(1.8f);
                
                // start polling for data
                if (task != null) task.cancel();
                task = new GpsTask();
                timer.scheduleAtFixedRate(task, 1000, 30); 
                
            }
        });

        initFakeGpsButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gpsTracker == null) {
                    testLocationText.setText("No GPS tracker object, please press init GPS");
                    return;
                }
                gpsTracker.setIndoorMode(!gpsTracker.isIndoorMode());
            }
        });

        startTrackingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                if (gpsTracker == null) {
                    testLocationText.setText("No GPS tracker object, please press init GPS");
                    return;
                }
                
                if (!gpsTracker.hasPosition()) {
                    testLocationText.setText(((LifeFitnessTracker)(gpsTracker)).getScreenLog());
                    //testLocationText.setText("GPS position not yet accurate enough, please wait");
                    return;
                }
                
                // open a CSV file to record data to
                if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                    // We can't write to the SD card
                    Log.w("GpsTestActivity","SD card not writable!");
                } 

                try {
                    SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HHmmss");
                    String datestamp = sdfDate.format(new Date());
                    File file = new File(getExternalFilesDir(null), "LFPositionData_" + datestamp + ".csv");
                    file.getParentFile().mkdirs();
                    if (!file.exists()) file.createNewFile();
                    Log.i("GpsTestActivity","Writing position data to " + file.getAbsolutePath());
                    
                    FileWriter fstream = new FileWriter(file);
                    out = new BufferedWriter(fstream);
                    out.write("Time, ");
//                    out.write("State, ");
//                    out.write("Device AccX, ");
//                    out.write("Device AccY, ");
//                    out.write("Device AccZ, ");
//                    out.write("Real-world AccX, ");
//                    out.write("Real-world AccY, ");
//                    out.write("Real-world AccZ, ");
//                    out.write("Forward Acceleration, ");
//                    out.write("Total Acceleration, ");
//                    out.write("Mean dFa, ");
//                    out.write("dTA 5-point mean, ");
//                    out.write("SD Ta, ");
//                    out.write("Max dTa, ");
                    out.write("Sample Speed, ");
//                    out.write("Smoothed Speed, ");
//                    out.write("Device Yaw, ");
//                    out.write("Bearing, ");
//                    out.write("Bearing to Avatar, ");
                    out.write("Sample Distance, ");
                    out.write("Smoothed Distance, ");
//                    out.write("GPS lat, ");
//                    out.write("GPS lng, ");
//                    out.write("GPS bearing\n");
                    out.write("\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
                gpsTracker.startTracking();
            }
        });

        stopTrackingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                
                if (gpsTracker == null) {
                    testLocationText.setText("No GPS tracker object, please press init GPS");
                    return;
                }
                
                gpsTracker.stopTracking();
                try {
                    out.close();
                } catch (IOException e) {
                    // If we've not yet opened the output stream, this is not an issue
                    e.printStackTrace();
                } catch (NullPointerException e) {
                    // out might be null if we've not started tracking
                    // no need to worry about closing it
                }
            }
        });

        resetButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (gpsTracker == null) {
                    testLocationText.setText("No GPS tracker object, please press init GPS");
                    return;
                }
                
                gpsTracker.startNewTrack();
            }
        });
        
        syncButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Helper.getInstance(context).exportDatabaseToCsv();
            }
        });

    }
    
    @Override
    public void onContentChanged() {
        Log.e("GPSTestActivity", "Content changed");
        super.onContentChanged();
        View view = this.findViewById(android.R.id.content);
        view.setKeepScreenOn(true);
    }
    
    public void onDestroy() {
        
        super.onDestroy();
        
        if (task != null) {
            task.cancel();
        }
        if (out != null) {
            try {
                out.close();
            } catch (IOException e) {
                // If we've not opened the output stream yet, this is not a problem
                e.printStackTrace();
            }
        }
    }
    
    public void onPause() {
        
        super.onPause();
        //gpsTracker.onPause();  // don't want to stop tracking when the screen turns off
        
        if (out != null) {
            try {
                out.flush();
            } catch (IOException e) {
                // Probably out is already closed. Can't check for that, but should ignore the error
                //e.printStackTrace();
            }
        }
    }
    
    public void onResume() {
        
        super.onResume();
        
        //gpsTracker.onResume();
        
    }
    
    private void updateLocationText() {
        
        if (gpsTracker == null) {
            testLocationText.setText("GPSTracker is null, please press init");
            return;
        }

        if (!gpsTracker.hasPosition()) {
            //testLocationText.setText("GPS position not yet accurate enough to start tracking, please wait");
            testLocationText.setText(((LifeFitnessTracker)(gpsTracker)).getScreenLog());
            return;
        }
        
        if (targetTracker == null) {
            testLocationText.setText("TargetTracker is null, please press init");
            return;
        }

        DecimalFormat zeroDp = new DecimalFormat("###");
        DecimalFormat twoDp = new DecimalFormat("+#.##;-#.##");
        
        String text = 
                
                  "Device state: " + gpsTracker.toString() + "\n"                  
//                + "Sample distance = " + twoDp.format(gpsTracker.getSampleDistance()) + "m (\u00b1" 
//                          + zeroDp.format(gpsTracker.getCurrentPosition().getEpe()) + "m.)\n"
                + "Sample distance = " + twoDp.format(gpsTracker.getSampleDistance()) + "m.\n"
                + "Smoothed distance = " + twoDp.format(gpsTracker.getElapsedDistance()) + "m.\n"
//                +"Target elapsed distance = " + twoDp.format(targetDistance) + "m.\n"
                + "Distance to avatar = " + twoDp.format(targetTracker.getCumulativeDistanceAtTime(gpsTracker.getElapsedTime()) 
                                                       - gpsTracker.getElapsedDistance()) + "m.\n"

//                + "Device Yaw = " + zeroDp.format(-gpsTracker.getYaw() % 360) + " degrees\n\n"
//                + "GPS bearing = " + zeroDp.format(lastBearing) + " degrees\n\n"
//                + "Relative bearing to avatar = " + zeroDp.format(getBearingToAvatar()) + " degrees\n\n"

                + "Smoothed speed = " + twoDp.format(gpsTracker.getCurrentSpeed()) + "m/s.\n"
                + "Sample speed = " + twoDp.format(gpsTracker.getSampleSpeed()) + "m/s.\n"
                + "Avatar current speed = " + twoDp.format(targetTracker.getCurrentSpeed(gpsTracker.getElapsedTime())) + "m/s.\n\n"
                
                
                + "GPS elapsed time = " + twoDp.format((double)gpsTracker.getElapsedTime()/1000.0) + "s.\n"
                + "Points scored during current activity = " + pointsHelper.getCurrentActivityPoints() + "\n\n"
                
//                + "Device acceleration X = " + twoDp.format((double)gpsTracker.getDeviceAcceleration()[0]) + "ms-2.\n"
//                + "Device acceleration Y = " + twoDp.format((double)gpsTracker.getDeviceAcceleration()[1]) + "ms-2.\n"
//                + "Device acceleration Z = " + twoDp.format((double)gpsTracker.getDeviceAcceleration()[2]) + "ms-2.\n\n"
//                
//                + "Real-world acceleration X = " + twoDp.format((double)gpsTracker.getRealWorldAcceleration()[0]) + "ms-2.\n"
//                + "Real-world acceleration Y = " + twoDp.format((double)gpsTracker.getRealWorldAcceleration()[1]) + "ms-2.\n"
//                + "Real-world acceleration Z = " + twoDp.format((double)gpsTracker.getRealWorldAcceleration()[2]) + "ms-2.\n\n"
//                + "RMS Forward speed = " + twoDp.format((double)gpsTracker.getMeanDfa()) + "ms-2.\n"
//                + "RMS Total speed = " + twoDp.format((double)gpsTracker.getMeanDta()) + "ms-2.\n"
//                + "Forward acceleration = " + twoDp.format((double)gpsTracker.getForwardAcceleration()) + "ms-2.\n\n"
//                + "Derived sensor speed = " + twoDp.format((double)gpsTracker.updateSensorSpeed()) + "ms-1.\n"
        
//                + "Device yaw = " + zeroDp.format((double)gpsTracker.getYaw()) + "\n"
//                + "Forward vector east-west = " + twoDp.format((double)gpsTracker.getForwardVector()[0]) + "\n"
//                + "Forward vector north-sth = " + twoDp.format((double)gpsTracker.getForwardVector()[1]) + "\n";
                
//                + "GlassFit yaw = " + zeroDp.format(Math.toDegrees(helper.getGlassfitQuaternion().toYpr()[0])) + "\n"
//                + "GlassFit pitch = " + zeroDp.format(Math.toDegrees(helper.getGlassfitQuaternion().toYpr()[1])) + "\n"
//                + "GlassFit roll = " + zeroDp.format(Math.toDegrees(helper.getGlassfitQuaternion().toYpr()[2])) + "\n\n"
                
//                + "GlassFit yaw = " + zeroDp.format(Math.toDegrees(helper.getGameYpr()[0])) + "\n"
//                + "GlassFit pitch = " + zeroDp.format(Math.toDegrees(helper.getGameYpr()[1])) + "\n"
//                + "GlassFit roll = " + zeroDp.format(Math.toDegrees(helper.getGameYpr()[2])) + "\n\n" 
//                
//                + "dYaw = " + zeroDp.format(Math.toDegrees(helper.getDeltaQuaternion().toYpr()[0])) + "\n"
//                + "dPitch = " + zeroDp.format(Math.toDegrees(helper.getDeltaQuaternion().toYpr()[1])) + "\n"
//                + "dRoll = " + zeroDp.format(Math.toDegrees(helper.getDeltaQuaternion().toYpr()[2])) + "\n\n"
//                
//                 "accPitch = " + zeroDp.format(Math.toDegrees(helper.getAccPitch())) + "\n"
//                + "accRoll = " + zeroDp.format(Math.toDegrees(helper.getAccRoll())) + "\n"
                
//                + "fusedPitch = " + zeroDp.format(Math.toDegrees(helper.getFusedPitch())) + "\n"
//                + "fusedRoll = " + zeroDp.format(Math.toDegrees(helper.getFusedRoll())) + "\n\n" 
                        
//                  "Device X = " + twoDp.format(helper.getDeviceAccelerationVector().getX()) + "\n"
//                + "Device Y = " + twoDp.format(helper.getDeviceAccelerationVector().getY()) + "\n"
//                + "Device Z = " + twoDp.format(helper.getDeviceAccelerationVector().getZ()) + "\n"
//                
//                + "RW X = " + twoDp.format(helper.getRealWorldAccelerationVector().getX()) + "\n"
//                + "RW Y = " + twoDp.format(helper.getRealWorldAccelerationVector().getY()) + "\n"
//                + "RW Z = " + twoDp.format(helper.getRealWorldAccelerationVector().getZ()) + "\n"
                
//                  "GlassFit yaw = " + zeroDp.format(Math.toDegrees(helper.getGlassfitQuaternion().toYpr()[0])) + "\n"
//                + "GlassFit pitch = " + zeroDp.format(Math.toDegrees(helper.getGlassfitQuaternion().toYpr()[1])) + "\n"
//                + "GlassFit roll = " + zeroDp.format(Math.toDegrees(helper.getGlassfitQuaternion().toYpr()[2])) + "\n\n"      
//                
//                + "GlassFit W = " + twoDp.format(helper.getGlassfitQuaternion().getW()) + "\n"
//                + "GlassFit X = " + twoDp.format(helper.getGlassfitQuaternion().getX()) + "\n"
//                + "GlassFit Y = " + twoDp.format(helper.getGlassfitQuaternion().getY()) + "\n"
//                + "GlassFit Z = " + twoDp.format(helper.getGlassfitQuaternion().getZ()) + "\n\n" 
                
//                + "Accel yaw = " + zeroDp.format(Math.toDegrees(helper.getCorrection().toYpr()[0])) + "\n"
//                + "Accel pitch = " + zeroDp.format(Math.toDegrees(helper.getCorrection().toYpr()[1])) + "\n"
//                + "Accel roll = " + zeroDp.format(Math.toDegrees(helper.getCorrection().toYpr()[2])) + "\n\n"                   
//                
//                + "Accel W = " + twoDp.format(helper.getCorrection().getW()) + "\n"
//                + "Accel X = " + twoDp.format(helper.getCorrection().getX()) + "\n"
//                + "Accel Y = " + twoDp.format(helper.getCorrection().getY()) + "\n"
//                + "Accel Z = " + twoDp.format(helper.getCorrection().getZ()) + "\n\n"                 
                
//                + "gyroDroid yaw = " + zeroDp.format(Math.toDegrees(helper.getGyroDroidQuaternion().toYpr()[0])) + "\n"
//                + "gyroDroid pitch = " + zeroDp.format(Math.toDegrees(helper.getGyroDroidQuaternion().toYpr()[1])) + "\n"
//                + "gyroDroid roll = " + zeroDp.format(Math.toDegrees(helper.getGyroDroidQuaternion().toYpr()[2])) + "\n\n"
//                
//                + "android yaw = " + zeroDp.format(Math.toDegrees(helper.getAndroidQuaternion().toYpr()[0])) + "\n"
//                + "android pitch = " + zeroDp.format(Math.toDegrees(helper.getAndroidQuaternion().toYpr()[1])) + "\n"
//                + "android roll = " + zeroDp.format(Math.toDegrees(helper.getAndroidQuaternion().toYpr()[2])) + "\n"
                + "";
                
        testLocationText.setText(text);
    }
    
//    /**
//     * Calculates the relative bearing from the user's yaw to the smoothed GPS
//     * bearing. The avatar is placed on the smoothed GOS bearing, so this number
//     * can be interpreted as: + means avatar to the right, minus is avatar to the
//     * left.
//     */
//    private float lastBearing = -999.0f;
//    private float getBearingToAvatar() {
//        float yaw = -gpsTracker.getYaw() % 360;
//        if (lastBearing == -999.0f) lastBearing = yaw;
//        float newBearing = gpsTracker.getCurrentBearing();
//        if (newBearing != -999.0f) {
//            lastBearing = newBearing;
//            return (newBearing - yaw + 180) % 360 - 180;
//        } else {
//            return (lastBearing - yaw + 180) % 360 - 180;
//        }
//    }
    
    private class GpsTask extends TimerTask {
        public void run() {
            runOnUiThread(new Runnable() {
                public void run() {
                    
                    updateLocationText();
                    
//                    if (gpsTracker != null) {
//                        // update everything!
//                        gpsTracker.tick.run();
//                    }

                    if (out != null && gpsTracker != null && gpsTracker.isTracking()) {
                        // write values to file
                        try {
                            out.write((float)gpsTracker.getElapsedTime()/1000.0f + ", ");
//                            out.write(gpsTracker.toString() + ", ");
                            
//                            out.write(gpsTracker.getDeviceAcceleration()[0] + ", ");
//                            out.write(gpsTracker.getDeviceAcceleration()[1] + ", ");
//                            out.write(gpsTracker.getDeviceAcceleration()[2] + ", ");
//                            out.write(gpsTracker.getRealWorldAcceleration()[0] + ", ");
//                            out.write(gpsTracker.getRealWorldAcceleration()[1] + ", ");
//                            out.write(gpsTracker.getRealWorldAcceleration()[2] + ", ");
//                            out.write(gpsTracker.getForwardAcceleration() + ", ");
//                            out.write(gpsTracker.getTotalAcceleration() + ", ");                         
//                            out.write(gpsTracker.getMeanDfa() + ", ");
//                            out.write(gpsTracker.getMeanDta() + ", ");
//                            out.write(gpsTracker.getSdTotalAcc() + ", ");
//                            out.write(gpsTracker.getMaxDta() + ", ");
                            out.write(gpsTracker.getSampleSpeed() + ", ");
                            out.write(gpsTracker.getCurrentSpeed() + ", ");
                            
//                            out.write(-gpsTracker.getYaw() % 360 + ", ");
//                            out.write(gpsTracker.getCurrentBearing() + ", ");

//                            out.write(getBearingToAvatar() + ", ");
                            out.write(gpsTracker.getSampleDistance() + ", ");
                            out.write(gpsTracker.getElapsedDistance() + ", ");
//                            out.write(gpsTracker.getCurrentPosition().getLatx() + ", ");
//                            out.write(gpsTracker.getCurrentPosition().getLatx() + ", ");
//                            out.write(gpsTracker.getCurrentPosition().getBearing());
                            out.write("\n");
                            
                        } catch (IOException e) {
                            // Problem writing to the file!
                            e.printStackTrace();
                        }
                    }
                                        
                }
            });
        }
    }

}