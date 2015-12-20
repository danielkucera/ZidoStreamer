package eu.danman.zidostreamer.zidostreamer;

import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.util.Log;

import com.mstar.android.tv.TvCommonManager;
import com.mstar.android.tvapi.common.PictureManager;
import com.mstar.android.tvapi.common.TvManager;
import com.mstar.android.tvapi.common.exception.TvCommonException;
import com.mstar.android.tvapi.common.vo.EnumScalerWindow;
import com.mstar.android.tvapi.common.vo.TvOsType;
import com.mstar.android.tvapi.common.vo.VideoWindowType;
import com.mstar.hdmirecorder.HdmiRecorder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import static android.system.Os.mkfifo;

public class StreamService extends Service {

    private HdmiRecorder mHdmiRecorder = null;

    String myPath;

    String streamFifo;

    public StreamService() {


        //path = "/data/data/eu.danman.zidostreamer.zidostreamer/stream.ts";
//        path = "/mnt/sdcard/stream.ts";
//        path = "/var/tmp/stream.ts";




        /*
        UdpThread ut;

        ut = new UdpThread(path);

        //ut.openFile("/mnt/usb/sda1/HdmiRecorder/video20151205210104.ts");

        ut.start();

        Log.d("fifo", "Error while creating a pipe (return:%d, errno:%d)");

        */

    }

    public static void changeInputSource(TvOsType.EnumInputSource eis)
    {

        TvCommonManager commonService = TvCommonManager.getInstance();
        if (commonService != null)
        {
            TvOsType.EnumInputSource currentSource = commonService.getCurrentInputSource();
            if (currentSource != null)
            {
                if (currentSource.equals(eis))
                {
                    return;
                }

                commonService.setInputSource(eis);
            }

        }

    }

    public static boolean enableHDMI()
    {
        boolean bRet = false;
        try
        {
            changeInputSource(TvOsType.EnumInputSource.E_INPUT_SOURCE_STORAGE);
            changeInputSource(TvOsType.EnumInputSource.E_INPUT_SOURCE_HDMI);
            bRet = TvManager.getInstance().getPlayerManager().isSignalStable();
        } catch (TvCommonException e)
        {
            e.printStackTrace();
        }
        return bRet;
    }

    private static void copyFile(String assetPath, String localPath, Context context) {
    try {
        InputStream in = context.getAssets().open(assetPath);
        FileOutputStream out = new FileOutputStream(localPath);
        int read;
        byte[] buffer = new byte[4096];
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        out.close();
        in.close();

    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something

        Log.d("Streamer", "instantiate Recorder");
        mHdmiRecorder = new HdmiRecorder();
        Log.d("Streamer", "set error listener");






        myPath=this.getFilesDir().getAbsolutePath();

        streamFifo=myPath + "/stream.ts";

        String fifoBin = myPath + "/mkfifo";


        File fBin = new File(fifoBin);

        if (!fBin.exists()){

            copyFile("mkfifo", fifoBin, this);

            fBin = new File(fifoBin);
            fBin.setExecutable(true);

        }


        //mkfifo
        try {

            File fFile = new File(streamFifo);
            fFile.delete();

            Runtime.getRuntime().exec(fifoBin + " " + streamFifo);
        } catch (IOException e) {
            e.printStackTrace();
        }



        String ffmpegBin = myPath + "/ffmpeg";

        File fmBin = new File(ffmpegBin);

        if (!fmBin.exists()){

            copyFile("ffmpeg", ffmpegBin, this);

            fBin = new File(ffmpegBin);
            fBin.setExecutable(true);

        }


        enableHDMI();



//        String cmd = "/system/xbin/tail -c100000000 -f " + path + " | /mnt/sdcard/ffmpeg -i - -codec:v copy -codec:a copy -bsf:v dump_extra -f mpegts udp://239.255.0.1:1234?pkt_size=1316";
        String cmd =  ffmpegBin + " -i " + streamFifo + " -codec:v copy -codec:a copy -bsf:v dump_extra -f mpegts udp://239.255.0.1:1234?pkt_size=1316";

        Log.d("starting command", cmd);

        Process process = null;

        try {
//            Process process = Runtime.getRuntime().exec("/mnt/sdcard/ffmpeg -re -i " + path + " -codec:v copy -codec:a copy -f mpegts udp://239.255.0.1:1234?pkt_size=1316");
//            Process process = Runtime.getRuntime().exec("tail -/mnt/sdcard/ffmpeg -re -i " + path + " -codec:v copy -codec:a copy -bsf:v dump_extra -f mpegts udp://239.255.0.1:1234?pkt_size=1316");
            process = Runtime.getRuntime().exec(cmd);


            /*
            process = new ProcessBuilder()
                    .command(cmd)
                    .redirectErrorStream(true)
                    .start();
*/


            /*
            while (errstr.ready()){
                Log.d("errstr", errstr.readLine());
            }
            */

        } catch (IOException e) {
            e.printStackTrace();
            mHdmiRecorder.native_stop();
        }


        mHdmiRecorder.setOnErrorListener(moErrorListener);
        Log.d("Streamer", "set info listener");

        mHdmiRecorder.setOnInfoListener(mOnInfoListener);
        Log.d("Streamer", "set file path");

        mHdmiRecorder.set_output_file_path(streamFifo);
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



        String errline;

        /*

        OutputStream stdin = process.getOutputStream();

        InputStream stdout = process.getInputStream();

        BufferedReader errstr = new BufferedReader(new InputStreamReader(stdout));

        while (true) {

            try {

                    errline = errstr.readLine();

                    if (errline.length() > 0){
                        Log.d("errstr", errstr.readLine());
                    }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        */

        return Service.START_STICKY;

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
