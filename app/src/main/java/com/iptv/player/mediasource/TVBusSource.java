package com.iptv.player.mediasource;

import android.content.Context;
import android.util.Log;

import com.tvbus.engine.TVCore;
import com.tvbus.engine.TVListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class TVBusSource extends LocalServiceSource implements TVListener {
    private static final String TAG = "TVBusSource";

    private TVCore mCore;
    private int mInitCheck;

    private TVService mService = null;

    public TVBusSource(Context context) {
        super();

        mInitCheck = initTVCore(context);
    }

    private int initTVCore(Context context) {
        mCore = new TVCore();
        mCore.setTVListener(this);
        /**
         * 端口号，参考GitHub上的代码
         */
        mCore.setServPort(8902);
        mCore.setPlayPort(4010);

        return mCore.init(context);
    }

    @Override
    public void onInited(String result) {
        Log.d(TAG, "onInited callback " + result);
    }

    @Override
    public void onPrepared(String result) {
        Log.d(TAG, "onPrepared callback " + result);

        mLocalUrl = getPlayUrl(result);
        /**
         * 得到mLocalUrl，解除startService的阻塞
         */
        synchronized (this) {
            notify();
        }
    }

    @Override
    public void onInfo(String result) {
        Log.d(TAG, "onInfo callback " + result);
    }

    @Override
    public void onStart(String result) {
        Log.d(TAG, "onStart callback " + result);
    }

    @Override
    public void onStop(String result) {
        Log.d(TAG, "onStop callback " + result);
    }

    @Override
    public void onQuit(String result) {
        Log.d(TAG, "onQuit callback " + result);
    }

    @Override
    protected void startService(String url) throws IOException {
        if (mInitCheck < 0) {
            throw new IOException("TVCore init fail");
        }

        mService = new TVService();
        mService.start();

        /**
         * 稍等，确保服务已运行
         */
        try {
            Thread.sleep(200);
        }
        catch (InterruptedException e) {
            //ignore
        }

        /**
         * FIXME: 参数accessCode，从GitHub上的代码看应该是指“访问认证码”
         */
        mCore.start(url, "1111");
    }

    @Override
    protected void stopService() {
        if (mService != null) {
            mCore.stop();

            mService.quitSafely();
            mService = null;
        }
    }

    private class TVService extends Thread {
        public TVService() {
            super("TVCore service thread");
        }

        @Override
        public void	run() {
            /**
             * FIXME：从GitHub上的代码逻辑看，TVCore的run操作似乎是阻塞的
             */
            mCore.run();
        }

        public void quitSafely() {
            mCore.quit();

            boolean isTerminated = false;
            do {
                try {
                    join();
                    isTerminated = true;
                }
                catch (InterruptedException e) {
                    //ignore
                }
            }
            while (!isTerminated);
        }
    }

    private static String getPlayUrl(String preparedResult) {
        String url = "";

        try {
            JSONObject rootObj = new JSONObject(preparedResult);
            url = rootObj.getString("hls");
        }
        catch (JSONException e) {
            Log.w(TAG, "parse prepared json error, " + e.getMessage());
        }

        return url;
    }
}
