package eu.danman.zidostreamer.zidostreamer;

import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.IBinder;
import android.system.ErrnoException;
import android.util.Log;

import com.mstar.hdmirecorder.HdmiRecorder;

import java.io.File;

import static android.system.Os.mkfifo;

public class StreamService extends Service {

    private HdmiRecorder mHdmiRecorder = null;

    String path;

    public StreamService() {

        //path=this.getApplicationContext().getFilesDir().getAbsolutePath() + "/stream";

        path = "/data/data/eu.danman.zidostreamer.zidostreamer/stream.ts";
        path = "/mnt/sdcard/stream.ts";


        /*
        int res = 0;
        try {
            mkfifo(path, 00007);
        } catch (ErrnoException e) {
            e.printStackTrace();
        }
        if (res != 0)
        {
            Log.d("fifo", "Error while creating a pipe (return:%d, errno:%d)");
            return;
        }

        */

        /*
        UdpThread ut;

        ut = new UdpThread(path);

        //ut.openFile("/mnt/usb/sda1/HdmiRecorder/video20151205210104.ts");

        ut.start();

        */
        Log.d("fifo", "Error while creating a pipe (return:%d, errno:%d)");

    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something useful



        Log.d("Streamer", "instantiate Recorder");
        mHdmiRecorder = new HdmiRecorder();
        Log.d("Streamer", "set error listener");

        mHdmiRecorder.setOnErrorListener(moErrorListener);
        Log.d("Streamer", "set info listener");

        mHdmiRecorder.setOnInfoListener(mOnInfoListener);
        Log.d("Streamer", "set file path");

        mHdmiRecorder.set_output_file_path(path);
        Log.d("Streamer", "set format");

        mHdmiRecorder.set_output_format(HdmiRecorder.FORMAT_TS);
        Log.d("Streamer", "set W&H");

        mHdmiRecorder.native_set_video_high(1080);
        mHdmiRecorder.native_set_video_wigth(1920);
        Log.d("Streamer", "set bitrate");

        mHdmiRecorder.native_set_video_encoder_bitrate(5 * 1024 * 1024);

        Log.d("Streamer", "set framerate");

        mHdmiRecorder.native_set_video_framerate(30);

        Log.d("Streamer", "set travel mode??");

        mHdmiRecorder.native_set_video_travelingMode(4);

        Log.d("Streamer", "set sub source?");

        mHdmiRecorder.native_set_video_subSource(23);

        Log.d("Streamer", "start recording");

        boolean ret = mHdmiRecorder.start();

        return Service.START_NOT_STICKY;

    }

    HdmiRecorder.OnInfoListener	mOnInfoListener	= new HdmiRecorder.OnInfoListener() {

        @Override
        public void onInfo(com.mstar.hdmirecorder.HdmiRecorder mr, int what, int extra) {
            System.out.println("bob--mOnInfoListener what-===" + what);
            if (what == HdmiRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                mHdmiRecorder.native_stop();
            } else if (what == HdmiRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
            }

        }
    };

    HdmiRecorder.OnErrorListener	moErrorListener	= new HdmiRecorder.OnErrorListener() {

        @Override
        public void onError(com.mstar.hdmirecorder.HdmiRecorder mr, int what, int extra) {
            System.out.println("bob--moErrorListener what-===" + what);
            mHdmiRecorder.native_stop();
            // Toast.makeText(mContext,
            // "error occur when recording what-=== "
            // + what,
            // Toast.LENGTH_LONG).show();
            if (what == HdmiRecorder.MEDIA_RECORDER_ERROR_UNKNOWN) {
                // We may have
                // run out of
                // space on the
                // sdcard.
                // Show the
                // toast.
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
