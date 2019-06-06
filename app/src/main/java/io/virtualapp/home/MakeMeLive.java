package io.virtualapp.home;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import androidx.annotation.Nullable;
import android.util.Log;

import io.virtualapp.R;

public class MakeMeLive extends Service
{
    private final static String TAG = MakeMeLive.class.getSimpleName();
    private MediaPlayer mMediaPlayer;

    public MakeMeLive()
    {
        this.mMediaPlayer = new MediaPlayer();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, TAG + "---->onCreate,启动服务");
        try
        {
            mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.no_notice);
            mMediaPlayer.setLooping(true);
        }
        catch(Throwable e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new Thread(this::startPlayMusic).start();
        return START_STICKY;
    }

    private void startPlayMusic() {
        if (mMediaPlayer != null)
        {
            Log.d(TAG, "启动后台播放音乐");
            mMediaPlayer.start();
        }
    }

    private void stopPlayMusic() {
        if (mMediaPlayer != null) {
            Log.d(TAG, "关闭后台播放音乐");
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPlayMusic();
        Log.d(TAG, TAG + "---->onDestroy,停止服务");
        // 重启自己
        // Intent intent = new Intent(getApplicationContext(), MakeMeLive.class);
        // startService(intent);
    }
}
