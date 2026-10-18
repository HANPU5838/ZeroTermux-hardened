package com.termux.zerocore.ftp.new_ftp.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

public class NetworkStatusMonitor {
    private static final Handler handler = new Handler(Looper.getMainLooper());
    // CopyOnWriteArraySet makes iteration from the posted callback runnables safe even when a
    // callback registers or unregisters itself concurrently from another thread.
    private static final Set<NetworkStatusCallback> callbacks = new CopyOnWriteArraySet<>();
    /** Guards {@link #init(Context)} / {@link #release(Context)} so receivers are registered at most once. */
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final BroadcastReceiver apReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("android.net.wifi.WIFI_AP_STATE_CHANGED".equalsIgnoreCase(intent.getAction())) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                if (state == 11) {//AP关闭
                    //Log.e("111","热点关闭");
                    sendStatusChangedToCallbacks();
                    sendToCallbacks(false, NetworkType.AP);
                }
                if (state == 13) {//AP打开
                    //Log.e("111","热点打开");
                    sendStatusChangedToCallbacks();
                    sendToCallbacks(true, NetworkType.AP);
                }
            }
        }
    };
    //private static final @TargetApi(21) MyNetworkCallback myNetworkCallback=new MyNetworkCallback();
    private static ConnectivityManager connectivityManager;
    private static final BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        private boolean wifi = false;
        private boolean ethernet = false;
        private boolean cell = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equalsIgnoreCase(intent.getAction())) {
                sendStatusChangedToCallbacks();
                if (connectivityManager == null) return;
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if (networkInfo == null) {
                    if (wifi) {
                        wifi = false;
                        //Log.e("111","Wifi断开");
                        sendToCallbacks(false, NetworkType.WIFI);

                    }
                    if (ethernet) {
                        ethernet = false;
                        //Log.e("111","有线网络断开");
                        sendToCallbacks(false, NetworkType.ETHERNET);
                    }
                    if (cell) {
                        cell = false;
                        //Log.e("111","蜂窝网断开");
                        sendToCallbacks(false, NetworkType.CELLULAR);
                    }
                    return;
                }
                if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                    ethernet = true;
                    if (wifi) {
                        wifi = false;
                        sendToCallbacks(false, NetworkType.WIFI);
                    }
                    if (cell) {
                        cell = false;
                        sendToCallbacks(false, NetworkType.CELLULAR);
                    }
                    sendToCallbacks(true, NetworkType.ETHERNET);
                }
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    wifi = true;
                    if (ethernet) {
                        ethernet = false;
                        sendToCallbacks(false, NetworkType.ETHERNET);
                    }
                    if (cell) {
                        cell = false;
                        sendToCallbacks(false, NetworkType.CELLULAR);
                    }
                    //Log.e("111","Wifi连接");
                    sendToCallbacks(true, NetworkType.WIFI);
                }
                if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    cell = true;
                    if (wifi) {
                        wifi = false;
                        sendToCallbacks(false, NetworkType.WIFI);
                    }
                    if (ethernet) {
                        ethernet = false;
                        sendToCallbacks(false, NetworkType.ETHERNET);
                    }
                    //Log.e("111","蜂窝网连接");
                    sendToCallbacks(true, NetworkType.CELLULAR);
                }
            }
        }
    };

    public static void init(@NonNull Context context) {
        final Context appContext = context.getApplicationContext();
        synchronized (NetworkStatusMonitor.class) {
            // Registering the receivers twice would deliver every network event twice and leak a
            // registration that can never be removed, so only the first init() wins.
            if (!initialized.compareAndSet(false, true)) return;
            connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            appContext.registerReceiver(apReceiver, new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED"));
            appContext.registerReceiver(networkReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    /** Unregister the receivers registered by {@link #init(Context)}. Safe to call repeatedly. */
    public static void release(@NonNull Context context) {
        final Context appContext = context.getApplicationContext();
        synchronized (NetworkStatusMonitor.class) {
            if (!initialized.compareAndSet(true, false)) return;
            try {
                appContext.unregisterReceiver(apReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            try {
                appContext.unregisterReceiver(networkReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            connectivityManager = null;
        }
    }

    public static void addNetworkStatusCallback(@NonNull NetworkStatusCallback callback) {
        callbacks.add(callback);
    }

    public static void removeNetworkStatusCallback(@NonNull NetworkStatusCallback callback) {
        callbacks.remove(callback);
    }

    private static void sendToCallbacks(final boolean isConnected, final NetworkType networkType) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    for (NetworkStatusCallback callback : callbacks) {
                        callback.onNetworkConnected(networkType);
                    }
                } else {
                    for (NetworkStatusCallback callback : callbacks) {
                        callback.onNetworkDisconnected(networkType);
                    }
                }
            }
        });
    }

    /*@TargetApi(21)
    private static class MyNetworkCallback extends ConnectivityManager.NetworkCallback{

        private boolean wifi=false;
        private boolean ethernet=false;
        private boolean cell=false;

        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            sendStatusChangedToCallbacks();
            if(connectivityManager==null)return;
            NetworkCapabilities networkCapabilities=connectivityManager.getNetworkCapabilities(network);
            if(networkCapabilities==null)return;
            if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                wifi=true;
                //Log.e("111","WiFI已连接");
                sendToCallbacks(true,NetworkType.WIFI);
            }
            if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)){
                ethernet=true;
                //Log.e("111","有线网已连接");
                sendToCallbacks(true,NetworkType.ETHERNET);
            }
            if(networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)){
                cell=true;
                //Log.e("111","蜂窝网已连接");
                sendToCallbacks(true,NetworkType.CELLULAR);
            }
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            sendStatusChangedToCallbacks();
            if(connectivityManager==null)return;
            NetworkCapabilities networkCapabilities=connectivityManager.getNetworkCapabilities(network);
            if(networkCapabilities==null){
                if(wifi){
                    wifi=false;
                    //Log.e("111","WiFI已断开");
                    sendToCallbacks(false,NetworkType.WIFI);
                }
                if(ethernet){
                    ethernet=false;
                    //Log.e("111","有线已断开");
                    sendToCallbacks(false,NetworkType.ETHERNET);
                }
                if(cell){
                    cell=false;
                    //Log.e("111","蜂窝网已断开");
                    sendToCallbacks(false,NetworkType.CELLULAR);
                }
                return;
            }
            if(!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)&&wifi){
                wifi=false;
                //Log.e("111","WiFI已断开");
                sendToCallbacks(false,NetworkType.WIFI);
            }
            if(!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)&&ethernet){
                ethernet=false;
                //Log.e("111","有线网已断开");
                sendToCallbacks(false,NetworkType.ETHERNET);
            }
            if(!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)&&cell){
                cell=false;
                //Log.e("111","蜂窝网已断开");
                sendToCallbacks(false,NetworkType.ETHERNET);
            }
        }
    }*/

    private static void sendStatusChangedToCallbacks() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                for (NetworkStatusCallback callback : callbacks) {
                    callback.onNetworkStatusRefreshed();
                }
            }
        });
    }

    public enum NetworkType {
        WIFI, AP, ETHERNET, CELLULAR
    }

    public interface NetworkStatusCallback {
        /**
         * 当网络状态发生变化时回调
         */
        void onNetworkStatusRefreshed();

        /**
         * 网络连接回调，在主线程
         *
         * @param networkType 参考{@link NetworkType}
         */
        void onNetworkConnected(NetworkType networkType);

        /**
         * 网络断开回调，在主线程
         *
         * @param networkType 参考{@link NetworkType}
         */
        void onNetworkDisconnected(NetworkType networkType);
    }
}
