package com.dobots.bpp;

import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

public abstract class Callback<T> {
    final private static String TAG = "BoomPinPoint Callback";

    final private static Object defaultRef = new Object();
    private final WeakReference<Object> remote;
    private final TimerTask timeoutTask = new TimerTask() {
        @Override
        public void run() {
            Log.w(TAG, "Timeout in callback");
            stop();
            onError(new Error("Timeout in callback"));
        }
    };

    public void sendResult(T result) {
        timeoutTask.cancel();
        this.onResult(result);
    }

    public void sendError(Error error) {
        timeoutTask.cancel();
        this.onError(error);
    }

    public abstract void onResult(T result);

    public abstract void onError(Error error);

    public Callback() {
        this.remote = new WeakReference<>(defaultRef);
    }

    public Callback(final Integer timeout) {
        this.remote = new WeakReference<>(defaultRef);
        new Timer("CallbackTimeout", true).schedule(timeoutTask, timeout);
    }

    public Callback(final Object reference) {
        if (reference == null) {
            this.remote = new WeakReference<>(defaultRef);
        } else {
            this.remote = new WeakReference<>(reference);
        }
    }

    public Callback(final Integer timeout, final Object reference) {
        if (reference == null) {
            this.remote = new WeakReference<>(defaultRef);
        } else {
            this.remote = new WeakReference<>(reference);
        }
        new Timer("CallbackTimeout", true).schedule(timeoutTask, timeout);
    }


    public void stop() {
        this.remote.clear();
        this.timeoutTask.cancel();
    }

    public boolean isStopped() {
        return remote.get() == null;
    }
}
