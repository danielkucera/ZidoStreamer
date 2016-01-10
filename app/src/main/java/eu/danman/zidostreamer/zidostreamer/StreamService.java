package eu.danman.zidostreamer.zidostreamer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.util.Log;

import com.mstar.android.camera.MCamera;
import com.mstar.android.tv.TvCommonManager;
import com.mstar.android.tvapi.common.TvManager;
import com.mstar.android.tvapi.common.exception.TvCommonException;
import com.mstar.android.tvapi.common.vo.TvOsType;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.InetAddress;


public class StreamService extends Service {

    String myPath;

    String streamFifo;

    String ffmpegBin;

    public StreamService() {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

        //myPath = this.getApplicationContext().getFilesDir().getAbsolutePath();
        myPath = "/data/data/eu.danman.zidostreamer.zidostreamer/files/";

        streamFifo = myPath + "/stream.ts";
        ffmpegBin = myPath + "/ffmpeg";


        // copy ffmpeg from assets to data folder
        File fmBin = new File(ffmpegBin);

        if (!fmBin.exists()){

            copyFile("ffmpeg", ffmpegBin, this);

            fmBin = new File(ffmpegBin);
            fmBin.setExecutable(true);

        }

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

    boolean isProcessRunning(Process process){

        try {
            process.exitValue();
        } catch (IllegalThreadStateException e){
            return true;
        }

        return false;
    }

    android.hardware.Camera mCamera = null;
    MediaRecorder mMediaRecorder = null;
    Process ffmpegProcess = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO do something

        // try cleanup first
        stopFFMPEG();
        releaseMediaRecorder();
        releaseCamera();

        enableHDMI();

        //make a pipe containing a read and a write parcelfd
        ParcelFileDescriptor[] fdPair = new ParcelFileDescriptor[0];
        try {
            fdPair = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //get a handle to your read and write fd objects.
        ParcelFileDescriptor readFD = fdPair[0];
        ParcelFileDescriptor writeFD = fdPair[1];

        //next create an input stream to read from the read side of the pipe.
        FileInputStream reader = new FileInputStream(readFD.getFileDescriptor());

        BufferedInputStream bufferedReader = new BufferedInputStream(reader);


        // Start ffmpeg process

//        String cmd = "/system/xbin/tail -c100000000 -f " + path + " | /mnt/sdcard/ffmpeg -i - -codec:v copy -codec:a copy -bsf:v dump_extra -f mpegts udp://239.255.0.1:1234?pkt_size=1316";
        String cmd =  ffmpegBin + " -i udp://:12345 -codec:v copy -codec:a copy -bsf:v dump_extra -f mpegts udp://239.255.0.1:1234?pkt_size=1316";
//        String cmd =  ffmpegBin + "k -i udp://:12345 -codec:v copy -codec:a copy -f flv rtmp://a.rtmp.youtube.com/live2/daniel.kucera.eu6p-a3wb-ew7h-06ks";

        Log.d("starting ffmpeg", cmd);

        try {

            ffmpegProcess = Runtime.getRuntime().exec(cmd);

            final BufferedReader in = new BufferedReader(new InputStreamReader(ffmpegProcess.getErrorStream()));

            final Process thisFFMPEG = ffmpegProcess;

            // create logger thread
            Thread thread = new Thread() {
                @Override
                public void run() {

                    String log = null;

                    try {

                        while(isProcessRunning(thisFFMPEG)) {

                            log = in.readLine();

                            if ((log != null) && (log.length() > 0)){
                                Log.d("ffmpeg", log);
                            } else {
                                sleep(100);
                            }

                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            thread.start();

        } catch (IOException e) {
            e.printStackTrace();
        }


        // create output UDP socket
        ParcelFileDescriptor socketWrapper = null;

        try {
            DatagramSocket socket = new DatagramSocket();
            socket.connect(InetAddress.getByName("127.0.0.1"), 12345);
            socketWrapper = ParcelFileDescriptor.fromDatagramSocket(socket);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // initialize recording hardware
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
        mMediaRecorder.setAudioSamplingRate(44100);
        mMediaRecorder.setVideoEncodingBitRate(2000);


        // Step 4: Set output file
        mMediaRecorder.setOutputFile(socketWrapper.getFileDescriptor());

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

    public void onDestroy() {
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
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

    private void stopFFMPEG(){
        if (ffmpegProcess != null){
            ffmpegProcess.destroy();
        }
        ffmpegProcess = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
