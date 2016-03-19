package eu.danman.zidostreamer.zidostreamer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

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
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.security.Provider;


public class StreamService extends Service {

    String myPath;

    String streamFifo;

    String ffmpegBin;

    SharedPreferences settings;

    public StreamService() {

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

    }

    private static void copyAsset(String assetPath, String localPath, Context context) {
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

    public void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
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

        settings = PreferenceManager.getDefaultSharedPreferences(this);


        myPath = this.getApplicationContext().getFilesDir().getAbsolutePath();
        //myPath = "/data/data/eu.danman.zidostreamer.zidostreamer/files/";

        ffmpegBin = myPath + "/ffmpeg";

        // copy ffmpeg from assets to data folder
        File fmBin = new File(ffmpegBin);

        if (!fmBin.exists()){

            try {
                copyFile(new File("/mnt/sdcard/ffmpeg"), new File(ffmpegBin));

                fmBin = new File(ffmpegBin);
                fmBin.setExecutable(true);

            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        // try cleanup first
        stopFFMPEG();
        releaseMediaRecorder();
        releaseCamera();


        //make a pipe containing a read and a write parcelfd
        ParcelFileDescriptor[] fdPair = new ParcelFileDescriptor[0];
        try {
            fdPair = ParcelFileDescriptor.createPipe();
        } catch (IOException e) {
            e.printStackTrace();

            return Service.START_NOT_STICKY;
        }

        //get a handle to your read and write fd objects.
        ParcelFileDescriptor readFD = fdPair[0];
        ParcelFileDescriptor writeFD = fdPair[1];

        // Start ffmpeg process
        String cmd = settings.getString("ffmpeg_cmd", "");



//        String cmd = "/system/xbin/tail -c100000000 -f " + path + " | /mnt/sdcard/ffmpeg -i - -codec:v copy -codec:a copy -bsf:v dump_extra -f mpegts udp://239.255.0.1:1234?pkt_size=1316";
//        String cmd =  ffmpegBin + " -i udp://:12345 -codec:v copy -codec:a copy -bsf:v dump_extra -f mpegts udp://239.255.0.1:1234?pkt_size=1316";
//        String cmd =  ffmpegBin + " -i - -codec:v copy -codec:a copy -bsf:v dump_extra -f mpegts udp://239.255.0.1:1234?pkt_size=1316";
//        String cmd =  ffmpegBin + " -i - -codec:v copy -codec:a copy -bsf:a aac_adtstoasc -f flv rtmp://a.rtmp.youtube.com/live2/daniel.kucera.eu6p-a3wb-ew7h-06ks";
        //String cmd =  ffmpegBin + " -i - -codec:v copy -codec:a copy -bsf:a aac_adtstoasc -f flv rtmp://a.rtmp.youtube.com/live2/daniel.kucera.eu6p-a3wb-ew7h-06ks";
//        String cmd =  ffmpegBin + " -i -  -strict -2 -codec:v copy -codec:a aac -b:a 128k -f flv -metadata streamName=myStream tcp://bonsai.danman.eu:6666";
//        String cmd =  ffmpegBin + " -i -  -strict -2 -codec:v copy -codec:a aac -b:a 128k -f flv rtmp://a.rtmp.youtube.com/live2/daniel.kucera.eu6p-a3wb-ew7h-06ks";
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
            return Service.START_NOT_STICKY;

        } catch (RuntimeException e){
            e.printStackTrace();
            Toast.makeText(this, "You need to edit settings first!", Toast.LENGTH_LONG).show();
            return Service.START_NOT_STICKY;
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
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);


        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        //mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        int audio_bitrate;
        int video_bitrate;
        int video_framerate;

        audio_bitrate =  Integer.parseInt(settings.getString("audio_bitrate", "128")) * 1024;
        video_bitrate =  Integer.parseInt(settings.getString("video_bitrate", "4500")) * 1024;
        video_framerate =  Integer.parseInt(settings.getString("video_framerate", "30"));

        // set TS
        mMediaRecorder.setOutputFormat(8);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setAudioSamplingRate(44100);
        mMediaRecorder.setAudioEncodingBitRate(audio_bitrate);

        mMediaRecorder.setVideoSize(1920, 1080);
        mMediaRecorder.setVideoFrameRate(video_framerate);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoEncodingBitRate(video_bitrate);


        // Step 4: Set output file
        mMediaRecorder.setOutputFile(writeFD.getFileDescriptor());


        // create reader thread

        //next create an input stream to read from the read side of the pipe.


        //final BufferedInputStream bufferedReader = new BufferedInputStream(reader);

        final ParcelFileDescriptor finalreadFD = readFD;

        Thread readerThread = new Thread() {
            @Override
            public void run() {

                byte[] buffer = new byte[8192];
                int read = 0;

                OutputStream ffmpegInput = ffmpegProcess.getOutputStream();

                //while (true) {

                    final FileInputStream reader = new FileInputStream(finalreadFD.getFileDescriptor());

                    try {

                        while (true) {

                            //if (reader.available() > 0){

                            read = reader.read(buffer);

                            ffmpegInput.write(buffer, 0, read);

                            //}

                        }

                    } catch (IOException e) {
                        e.printStackTrace();

                        onDestroy();

                    }

                //}
            }
        };

        readerThread.start();


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

        try {

            mMediaRecorder.start();

        } catch (Exception e){
            Log.d("mediarecorder", "recording failed to start");
            Toast.makeText(this, "Failed to start recording",Toast.LENGTH_LONG);
        }

        Toast.makeText(this, "Recording started",Toast.LENGTH_LONG);

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
