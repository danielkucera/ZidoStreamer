package eu.danman.zidostreamer.zidostreamer;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.mstar.android.camera.MCamera;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;


public class StreamService extends Service {

    String myPath;

    String ffmpegBin;

    SharedPreferences settings;

    public StreamService() {

    }

    private void sendLog(String log){
        Intent intent = new Intent("ToStreamerActivity");
        intent.putExtra("log", log);
        sendBroadcast(intent);
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

        ffmpegBin = myPath + "/ffmpeg";

        // copy ffmpeg from sdcard to data folder
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
                                sendLog(log);
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


        // get stream settings
        int audio_bitrate;
        int video_bitrate;
        int video_framerate;
        int video_size;
        int video_width = 1920;
        int video_height = 1080;

        audio_bitrate =  Integer.parseInt(settings.getString("audio_bitrate", "128")) * 1024;
        video_bitrate =  Integer.parseInt(settings.getString("video_bitrate", "4500")) * 1024;
        video_framerate =  Integer.parseInt(settings.getString("video_framerate", "30"));
        video_size = Integer.parseInt(settings.getString("video_size", "0"));

        int cam_size;

        switch (video_size){
            default:
            case 0:
                cam_size = MCamera.Parameters.E_TRAVELING_RES_1920_1080;
                video_width = 1920; video_height = 1080;
                break;
            case 1:
                cam_size = MCamera.Parameters.E_TRAVELING_RES_1280_720;
                video_width = 1280; video_height = 720;
                break;
            case 2:
                cam_size = MCamera.Parameters.E_TRAVELING_RES_720_576;
                video_width = 720; video_height = 576;
                break;
            case 3:
                cam_size = MCamera.Parameters.E_TRAVELING_RES_720_480;
                video_width = 720; video_height = 480;
                break;
            case 4:
                cam_size = MCamera.Parameters.E_TRAVELING_RES_640_368;
                video_width = 640; video_height = 368;
                break;
        }

        // initialize recording hardware
        mCamera = getCameraInstance();
        mMediaRecorder = new MediaRecorder();

        Camera.Parameters camParams = mCamera.getParameters();
        camParams.set(MCamera.Parameters.KEY_TRAVELING_RES, cam_size);
        camParams.set(MCamera.Parameters.KEY_TRAVELING_MODE, MCamera.Parameters.E_TRAVELING_ALL_VIDEO);
        camParams.set(MCamera.Parameters.KEY_TRAVELING_MEM_FORMAT, MCamera.Parameters.E_TRAVELING_MEM_FORMAT_YUV422_YUYV);
        camParams.set(MCamera.Parameters.KEY_MAIN_INPUT_SOURCE, MCamera.Parameters.MAPI_INPUT_SOURCE_HDMI);
        camParams.set(MCamera.Parameters.KEY_TRAVELING_FRAMERATE, video_framerate);
        camParams.set(MCamera.Parameters.KEY_TRAVELING_SPEED, MCamera.Parameters.E_TRAVELING_SPEED_FAST);
        mCamera.setParameters(camParams);

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);


        // set TS
        mMediaRecorder.setOutputFormat(8);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mMediaRecorder.setAudioChannels(2);
        mMediaRecorder.setAudioSamplingRate(44100);
        mMediaRecorder.setAudioEncodingBitRate(audio_bitrate);

        mMediaRecorder.setVideoSize(video_width, video_height);
        mMediaRecorder.setVideoFrameRate(video_framerate);
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setVideoEncodingBitRate(video_bitrate);


        // Step 4: Set output file
        mMediaRecorder.setOutputFile(writeFD.getFileDescriptor());


        // create proxy thread to read from mediarecorder and write to ffmpeg stdin
        final ParcelFileDescriptor finalreadFD = readFD;

        Thread readerThread = new Thread() {
            @Override
            public void run() {

                byte[] buffer = new byte[8192];
                int read = 0;

                OutputStream ffmpegInput = ffmpegProcess.getOutputStream();

                final FileInputStream reader = new FileInputStream(finalreadFD.getFileDescriptor());

                try {

                    while (true) {

                        if (reader.available()>0) {
                            read = reader.read(buffer);
                            ffmpegInput.write(buffer, 0, read);
                        } else {
                            sleep(10);
                        }

                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();

                } catch (IOException e) {
                    e.printStackTrace();

                    onDestroy();
                }
            }
        };

        readerThread.start();
        
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
            Toast.makeText(this, "Recording started",Toast.LENGTH_LONG).show();

        } catch (Exception e){
            Log.d("mediarecorder", "recording failed to start");
            teardown();
            Toast.makeText(this, "Failed to start recording",Toast.LENGTH_LONG).show();
        }

        return Service.START_NOT_STICKY;

    }

    private void teardown(){
        stopFFMPEG();
        releaseMediaRecorder();
        releaseCamera();
    }

    public void onDestroy() {
        teardown();
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
