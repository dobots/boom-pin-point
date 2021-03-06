package com.dobots.bpp;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class API extends Service {
    private final static String TAG = "BoomPinPoint API";
    // Location Management
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    // Current Location variable
    private Location current_location = null;

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        Log.w(TAG, "Service created.(" + Thread.currentThread().getName() + ")");
        // Create Location Manager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Create Location Request
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Create Location Callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update variable
                    current_location = location;
                }
            }
        };
        // Request location updates
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "Service destroyed.(" + Thread.currentThread().getName() + ")");
    }

    public API() {
        Log.w(TAG, "API constructor called.(" + Thread.currentThread().getName() + ")");
    }

    private final static HashMap<Context, ServiceConnection> bindings = new HashMap<>();

    //Binding API
    public static void bind(final Context ctx, final Callback<API> cb) {
        Log.w(TAG, "Bind called.(" + Thread.currentThread().getName() + ")");
        final Intent intent = new Intent(ctx, API.class);
        ctx.startService(intent);
        ServiceConnection mServiceConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder binder) {
                Log.w(TAG, "Service connected");
                if (!cb.isStopped()) {
                    cb.sendResult(((LocalBinder) binder).getService());
                }
            }

            public void onServiceDisconnected(ComponentName className) {
                Log.w(TAG, "Service disconnected");
                if (!cb.isStopped()) {
                    cb.sendResult(null);
                    cb.stop();
                }
            }
        };
        bindings.put(ctx, mServiceConn);
        ctx.bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    public void unbind(final Context ctx) {
        Log.w(TAG, "Unbind called.(" + Thread.currentThread().getName() + ")");
        final ServiceConnection conn = bindings.get(ctx);
        if (conn != null) {
            ctx.unbindService(conn);
        }
    }

    private final IBinder mBinder = new LocalBinder();

    private class LocalBinder extends Binder {
        API getService() {
            return API.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }


    // HERE you add methods within the service that can be called from the App's screen
    private int active_state = 0; // Detection is OFF Initially
    private long date_time;
    private String next_message = null;
    //  Creating three threads:
    //  - Detection using mic
    //  - Reporting detections to server.
    //  - Listening to server for other detections and triangulating
    private ExecutorService detector_executor = Executors.newSingleThreadExecutor();
    private ExecutorService reporter_executor = Executors.newSingleThreadExecutor();
    private ExecutorService triangulator_executor = Executors.newSingleThreadExecutor();
    // Creating three "Future" handlers for the threads
    private Future detector_handle = null;
    private Future reporter_handle = null;
    private Future triangulator_handle = null;

    // Detector
    private MediaRecorder recorder = null;
    // GPS Logging
    private int log_time_interval_ms = 250;

    @SuppressLint("MissingPermission")
    private void detector_runnable() throws InterruptedException, IOException {

        Log.d(TAG, "Detecting..........");
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
        recorder.setAudioEncodingBitRate(16*44100); // For HQ audio
        recorder.setAudioSamplingRate(44100);
        // Set file path
        recorder.setOutputFile(getExternalCacheDir().getAbsolutePath()+"/"+date_time+".aac");
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "MediaRecorder prepare() failed");
        }

        recorder.start();

        // Request location updates
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());

        // Log location data in a CSV
        FileOutputStream log_stream = new FileOutputStream(getExternalCacheDir().getAbsolutePath()+"/"+date_time+".csv", true);
        OutputStreamWriter log_writer = new OutputStreamWriter(log_stream);
        String log_entry = "Time_ms,Latitude,Longitude,Altitude" + "\n";
        log_writer.append(log_entry);
        log_writer.flush();
        log_stream.flush();

        while (true) {
            Log.d(TAG, "Time_ms: "+ String.valueOf(System.currentTimeMillis()));
            Log.d(TAG, "Latitude: "+ String.valueOf(current_location.getLatitude()));
            Log.d(TAG, "Longitude: "+ String.valueOf(current_location.getLongitude()));
            Log.d(TAG, "Altitude: "+ String.valueOf(current_location.getAltitude()));

            log_entry = String.valueOf(System.currentTimeMillis())+","+String.valueOf(current_location.getLatitude())+","+String.valueOf(current_location.getLongitude())+","+String.valueOf(current_location.getAltitude()) + "\n";
            log_writer.append(log_entry);
            log_writer.flush();
            log_stream.flush();

            Thread.sleep(log_time_interval_ms);
        }

    }

    private void detector_release() {
        recorder.stop();
        recorder.reset();    // set state to idle
        recorder.release();
        recorder = null;

        fusedLocationClient.removeLocationUpdates(locationCallback);

        detector_handle.cancel(true);
    }

    private void reporter_runnable(){
        // To Do
    }

    private void triangulator_runnable(){
        // To Do
    }


    public String HandleButtonPress() {
        Log.d(TAG, "BUTTON PRESSED!");

        if (active_state==0) {
            active_state=1;
            date_time = System.currentTimeMillis();
            // START detection thread
            detector_handle = detector_executor.submit(new Callable(){
                @Override
                public Object call() throws Exception {
                    detector_runnable();
                    return null;
                }
            });

            next_message = "Stopping detection...";
        }
        else if (active_state==1) {
            active_state=0;

            // STOP detection thread
            detector_release();

            next_message = "Starting detection...";
        }

        return next_message;
    }

}