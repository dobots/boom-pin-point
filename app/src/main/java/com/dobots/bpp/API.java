package com.dobots.bpp;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class API extends Service {
    private final static String TAG = "BoomPinPoint API";

    @Override
    public void onCreate() {
        Log.w(TAG, "Service created.(" + Thread.currentThread().getName() + ")");
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

    private void detector_runnable() throws InterruptedException {
        // To Do
//        while (true) {
//            Thread.sleep(1 * 1000);
//        }
            Log.d(TAG, "Detecting..........");
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            // Set file path
            recorder.setOutputFile(getExternalCacheDir().getAbsolutePath()+"/audiorecordtest.3gp");
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                recorder.prepare();
            } catch (IOException e) {
                Log.e(TAG, "MediaRecorder prepare() failed");
            }

            recorder.start();
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
            recorder.stop();
            recorder.reset();    // set state to idle
            recorder.release();
            recorder = null;
            detector_handle.cancel(true);

            next_message = "Starting detection...";
        }

        return next_message;
    }

}