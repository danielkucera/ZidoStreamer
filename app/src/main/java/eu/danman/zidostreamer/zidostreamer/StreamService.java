package eu.danman.zidostreamer.zidostreamer;

import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.os.SystemClock;
import android.system.ErrnoException;
import android.util.Log;

import com.mstar.android.camera.MCamera;
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

    /** A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(5); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    android.hardware.Camera mCamera;
    MediaRecorder mMediaRecorder;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something



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

//            Runtime.getRuntime().exec(fifoBin + " " + streamFifo + "nic");
            Runtime.getRuntime().exec(fifoBin + " " + streamFifo );
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
//        String cmd =  ffmpegBin + " -i " + streamFifo + " -codec:v copy -codec:a copy -f flv rtmp://a.rtmp.youtube.com/live2/daniel.kucera.eu6p-a3wb-ew7h-06ks";


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
        }


        mCamera = getCameraInstance();
        mMediaRecorder = new MediaRecorder();

        Camera.Parameters camParams = mCamera.getParameters();

        camParams.set(MCamera.Parameters.KEY_TRAVELING_RES, MCamera.Parameters.E_TRAVELING_RES_1920_1080);
        camParams.set(MCamera.Parameters.KEY_TRAVELING_MODE, MCamera.Parameters.E_TRAVELING_ALL_VIDEO);
        camParams.set(MCamera.Parameters.KEY_TRAVELING_MEM_FORMAT, MCamera.Parameters.E_TRAVELING_MEM_FORMAT_YUV422_YUYV);
        camParams.set(MCamera.Parameters.KEY_MAIN_INPUT_SOURCE, MCamera.Parameters.MAPI_INPUT_SOURCE_HDMI);
        camParams.set(MCamera.Parameters.KEY_TRAVELING_FRAMERATE, 30);
        camParams.set(MCamera.Parameters.KEY_TRAVELING_SPEED, MCamera.Parameters.E_TRAVELING_SPEED_FAST);

        mCamera.setParameters(camParams);

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);


        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        //mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));


        // set TS
        mMediaRecorder.setOutputFormat(8);
        mMediaRecorder.setVideoSize(1920, 1080);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoEncodingBitRate(2000);


        // Step 4: Set output file
        mMediaRecorder.setOutputFile(streamFifo);

        // Step 5: Set the preview output
        //mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d("nejde", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();

        } catch (IOException e) {
            Log.d("nejde", "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
        }

        mMediaRecorder.start();


        return Service.START_STICKY;

    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
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
