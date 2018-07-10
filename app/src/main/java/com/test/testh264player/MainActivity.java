package com.test.testh264player;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.test.testh264player.bean.Frame;
import com.test.testh264player.decode.DecodeThread;
import com.test.testh264player.interf.OnAcceptBuffListener;
import com.test.testh264player.interf.OnAcceptTcpStateChangeListener;
import com.test.testh264player.mediacodec.VIdeoMediaCodec;
import com.test.testh264player.server.TcpServer;
import com.yuri.xlog.XLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import qiu.niorgai.StatusBarCompat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private SurfaceView mSurface = null;
    private DecodeThread mDecodeThread;

    private NormalPlayQueue mPlayqueue;
    private TcpServer tcpServer;
    private VIdeoMediaCodec VIdeoMediaCodec;
    private FileOutputStream fos;

    private ImageView mTestImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

//        StatusBarCompat.translucentStatusBar(this);

        XLog.startSaveToFile();

        mSurface = findViewById(R.id.surfaceview);
        mTestImageView = findViewById(R.id.iv_test_image);

        findViewById(R.id.button1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTestImageView.bringToFront();
            }
        });

        findViewById(R.id.button2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSurface.bringToFront();
            }
        });

        startLoad();


        SurfaceHolder surfaceHolder = mSurface.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initialMediaCodec(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                try {
                    if (VIdeoMediaCodec != null) VIdeoMediaCodec.release();
                } catch (final Exception e) {
                    e.printStackTrace();
                    XLog.d("Error:" + e.getMessage());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MyApplication.mInstance.getApplicationContext(), "surfaceDestroyed.error:" + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},12);
        }
    }

    private void startLoad() {
        XLog.d();
        initialFIle();
        startServer();
    }

    private void initialMediaCodec(SurfaceHolder holder) {
        VIdeoMediaCodec = new VIdeoMediaCodec(holder, null, null);
        XLog.d("create decode thread");
        mDecodeThread = new DecodeThread(VIdeoMediaCodec.getCodec(), mPlayqueue);
        XLog.d("media codec start");
        VIdeoMediaCodec.start();
        XLog.d("mDecodeThread start");
        mDecodeThread.start();
    }

    private void startServer() {
        mPlayqueue = new NormalPlayQueue();
        tcpServer = new TcpServer();
        tcpServer.setOnAccepttBuffListener(new MyAcceptH264Listener());
        tcpServer.setOnTcpConnectListener(new MyAcceptTcpStateListener());
        tcpServer.startServer();
    }

    //接收到H264buff的回调
    class MyAcceptH264Listener implements OnAcceptBuffListener {

        @Override
        public void acceptBuff(Frame frame) {
            XLog.d();
//            if (frame.getType() == Frame.AUDIO_FRAME) {
//                try {
//                    fos.write(frame.getBytes());
//                } catch (IOException e) {
//                    Log.e("MAInActivity", "Exception =" + e.toString());
//                }
//                return;
//            }
            mPlayqueue.putByte(frame);
        }
    }

    //客户端Tcp连接状态的回调...
    class MyAcceptTcpStateListener implements OnAcceptTcpStateChangeListener {

        @Override
        public void acceptTcpConnect() {    //接收到客户端的连接...
            XLog.e( "accept a tcp connect...");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "accept a tcp connect", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void acceptTcpDisConnect(final Exception e) {  //客户端的连接断开...
            XLog.e("acceptTcpConnect exception = " + e.toString());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "acceptTcpConnect exception = " + e.toString(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    public void finish() {
        super.finish();
        try {
            if (tcpServer != null) tcpServer.stopServer();
            if (mPlayqueue != null) mPlayqueue.stop();
            if (VIdeoMediaCodec != null) VIdeoMediaCodec.release();
            if (mDecodeThread != null) mDecodeThread.shutdown();
        } catch (Exception e) {
            String message = e.getMessage();
            Toast.makeText(getApplicationContext(), "finish exception = " + message, Toast.LENGTH_SHORT).show();
            XLog.d("ERROR:" + message);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (tcpServer != null) {
            tcpServer.stopServer();
        }
    }

    private void initialFIle() {
        XLog.d();
        File file = new File(Environment.getExternalStorageDirectory(), "test.aac");
        if (file.exists()) {
            file.delete();
        }
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
        } catch (IOException e) {
            e.printStackTrace();
            XLog.d("" + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        XLog.stopAndSave();
    }
}
