
package com.mstar.hdmirecorder;

import java.lang.ref.WeakReference;
import java.io.IOException;
import android.util.Log;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;



public class HdmiRecorder{
    private int mNativeContext;
	private EventHandler mEventHandler;
    private OnErrorListener mOnErrorListener;
    private OnInfoListener mOnInfoListener;
	private String mPath;
	private static final String TAG = "java.HdmiRecorder";

	static {
        System.loadLibrary("jni_capture");
        native_init();
    }

	public HdmiRecorder()
	{
        Looper looper;
        if ((looper = Looper.myLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else if ((looper = Looper.getMainLooper()) != null) {
            mEventHandler = new EventHandler(this, looper);
        } else {
            mEventHandler = null;
        }
	
	    mNativeContext = 0;
	    native_setup(new WeakReference<HdmiRecorder>(this));

		//hello();
		//postEventFromNative(0,0,0,0);
	}

    
    public native boolean native_start();
    public native boolean native_stop();
    public native void native_set_video_encoder_bitrate(int bitrate);
    //public native void native_set_video_cameraid(int cameraid);

	//("video-size-values","1920x1080,1280x720,720x576,720x480,640x480,320x240,720x400,640x368,320x176")
    public native void native_set_video_wigth(int video_wight);
    public native void native_set_video_high(int video_hight);
	
    public native void native_set_video_framerate(int video_framerate);
    private native boolean native_set_outputFD(Object fd);
	public native void native_set_video_travelingMode(int travelingMode);
	public native void native_set_video_subSource(int sub);
	private native void native_set_output_format(int format);

	private static native void native_init();
	private native final void native_setup(Object mediarecorder_this);
	private native final void native_finalize();

	@Override
    protected void finalize() { native_finalize(); }

	public void set_output_file_path(String path){
        mPath = path;
	}


    public static final int FORMAT_MP4 = 0;
	public static final int FORMAT_TS = 1;
	
	public void set_output_format(int format){
		native_set_output_format((FORMAT_MP4 == format) ? 2 : 8);
	}

	public boolean start(){
		boolean ret = true;
        if (mPath != null) {
	        Log.v(TAG, "mPath = " + mPath);
			try{
				Log.v(TAG, "start 1");
	            FileOutputStream fos = new FileOutputStream(mPath);
	            try {
					Log.v(TAG, "start 2");
	                ret = native_set_outputFD(fos.getFD());
					if(ret){
						//native_set_video_cameraid(5);
						Log.v(TAG, "start 3");
	                    ret = native_start();
						Log.v(TAG, "start 4");
					}
					else{
	                    Log.e(TAG, "native_set_outputFD failed");
					}
	            } finally {
	                Log.v(TAG, "start 5");
	                fos.close();
	            }
			}catch(IOException e){
			    Log.e(TAG, " handle " + mPath + " error");
				ret = false;
			}
	        
	    } else {
	        Log.e(TAG, "open " + mPath + " failed");
			return false;
	    }

		return ret;
	}










    //contents below are copy from MediaRecorder.java


    /* Do not change this value without updating its counterpart
     * in include/media/mediarecorder.h or mediaplayer.h!
     */
    /** Unspecified media recorder error.
     * @see android.media.MediaRecorder.OnErrorListener
     */
    public static final int MEDIA_RECORDER_ERROR_UNKNOWN = 1;
    /** Media server died. In this case, the application must release the
     * MediaRecorder object and instantiate a new one.
     * @see android.media.MediaRecorder.OnErrorListener
     */
    public static final int MEDIA_ERROR_SERVER_DIED = 100; 
	
	/**
	 * Interface definition for a callback to be invoked when an error
	 * occurs while recording.
	 */
	public interface OnErrorListener
	{
		/**
		 * Called when an error occurs while recording.
		 *
		 * @param mr the MediaRecorder that encountered the error
		 * @param what	  the type of error that has occurred:
		 * <ul>
		 * <li>{@link #MEDIA_RECORDER_ERROR_UNKNOWN}
		 * <li>{@link #MEDIA_ERROR_SERVER_DIED}
		 * </ul>
		 * @param extra   an extra code, specific to the error type
		 */
		void onError(HdmiRecorder mr, int what, int extra);
	}

	/**
	 * Register a callback to be invoked when an error occurs while
	 * recording.
	 *
	 * @param l the callback that will be run
	 */
	public void setOnErrorListener(OnErrorListener l)
	{
		mOnErrorListener = l;
	}

	/* Do not change these values without updating their counterparts
	 * in include/media/mediarecorder.h!
	 */
	/** Unspecified media recorder error.
	 * @see android.media.MediaRecorder.OnInfoListener
	 */
	public static final int MEDIA_RECORDER_INFO_UNKNOWN 			 = 1;
	/** A maximum duration had been setup and has now been reached.
	 * @see android.media.MediaRecorder.OnInfoListener
	 */
	public static final int MEDIA_RECORDER_INFO_MAX_DURATION_REACHED = 800;
	/** A maximum filesize had been setup and has now been reached.
	 * @see android.media.MediaRecorder.OnInfoListener
	 */
	public static final int MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED = 801;

	/** informational events for individual tracks, for testing purpose.
	 * The track informational event usually contains two parts in the ext1
	 * arg of the onInfo() callback: bit 31-28 contains the track id; and
	 * the rest of the 28 bits contains the informational event defined here.
	 * For example, ext1 = (1 << 28 | MEDIA_RECORDER_TRACK_INFO_TYPE) if the
	 * track id is 1 for informational event MEDIA_RECORDER_TRACK_INFO_TYPE;
	 * while ext1 = (0 << 28 | MEDIA_RECORDER_TRACK_INFO_TYPE) if the track
	 * id is 0 for informational event MEDIA_RECORDER_TRACK_INFO_TYPE. The
	 * application should extract the track id and the type of informational
	 * event from ext1, accordingly.
	 *
	 * FIXME:
	 * Please update the comment for onInfo also when these
	 * events are unhidden so that application knows how to extract the track
	 * id and the informational event type from onInfo callback.
	 *
	 * {@hide}
	 */
	public static final int MEDIA_RECORDER_TRACK_INFO_LIST_START		= 1000;
	/** Signal the completion of the track for the recording session.
	 * {@hide}
	 */
	public static final int MEDIA_RECORDER_TRACK_INFO_COMPLETION_STATUS = 1000;
	/** Indicate the recording progress in time (ms) during recording.
	 * {@hide}
	 */
	public static final int MEDIA_RECORDER_TRACK_INFO_PROGRESS_IN_TIME	= 1001;
	/** Indicate the track type: 0 for Audio and 1 for Video.
	 * {@hide}
	 */
	public static final int MEDIA_RECORDER_TRACK_INFO_TYPE				= 1002;
	/** Provide the track duration information.
	 * {@hide}
	 */
	public static final int MEDIA_RECORDER_TRACK_INFO_DURATION_MS		= 1003;
	/** Provide the max chunk duration in time (ms) for the given track.
	 * {@hide}
	 */
	public static final int MEDIA_RECORDER_TRACK_INFO_MAX_CHUNK_DUR_MS	= 1004;
	/** Provide the total number of recordd frames.
	 * {@hide}
	 */
	public static final int MEDIA_RECORDER_TRACK_INFO_ENCODED_FRAMES	= 1005;
	/** Provide the max spacing between neighboring chunks for the given track.
	 * {@hide}
	 */
	public static final int MEDIA_RECORDER_TRACK_INTER_CHUNK_TIME_MS	= 1006;
	/** Provide the elapsed time measuring from the start of the recording
	 * till the first output frame of the given track is received, excluding
	 * any intentional start time offset of a recording session for the
	 * purpose of eliminating the recording sound in the recorded file.
	 * {@hide}
	 */
	public static final int MEDIA_RECORDER_TRACK_INFO_INITIAL_DELAY_MS	= 1007;
	/** Provide the start time difference (delay) betweeen this track and
	 * the start of the movie.
	 * {@hide}
	 */
	public static final int MEDIA_RECORDER_TRACK_INFO_START_OFFSET_MS	= 1008;
	/** Provide the total number of data (in kilo-bytes) encoded.
	 * {@hide}
	 */
	public static final int MEDIA_RECORDER_TRACK_INFO_DATA_KBYTES		= 1009;
	/**
	 * {@hide}
	 */
	public static final int MEDIA_RECORDER_TRACK_INFO_LIST_END			= 2000;


	/**
	 * Interface definition for a callback to be invoked when an error
	 * occurs while recording.
	 */
	public interface OnInfoListener
	{
		/**
		 * Called when an error occurs while recording.
		 *
		 * @param mr the MediaRecorder that encountered the error
		 * @param what	  the type of error that has occurred:
		 * <ul>
		 * <li>{@link #MEDIA_RECORDER_INFO_UNKNOWN}
		 * <li>{@link #MEDIA_RECORDER_INFO_MAX_DURATION_REACHED}
		 * <li>{@link #MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED}
		 * </ul>
		 * @param extra   an extra code, specific to the error type
		 */
		void onInfo(HdmiRecorder mr, int what, int extra);
	}

	/**
	 * Register a callback to be invoked when an informational event occurs while
	 * recording.
	 *
	 * @param listener the callback that will be run
	 */
	public void setOnInfoListener(OnInfoListener listener)
	{
		mOnInfoListener = listener;
	}

	private class EventHandler extends Handler
	{
		private HdmiRecorder mMediaRecorder;

		public EventHandler(HdmiRecorder mr, Looper looper) {
			super(looper);
			mMediaRecorder = mr;
		}

		/* Do not change these values without updating their counterparts
		 * in include/media/mediarecorder.h!
		 */
		private static final int MEDIA_RECORDER_EVENT_LIST_START = 1;
		private static final int MEDIA_RECORDER_EVENT_ERROR 	 = 1;
		private static final int MEDIA_RECORDER_EVENT_INFO		 = 2;
		private static final int MEDIA_RECORDER_EVENT_LIST_END	 = 99;

		/* Events related to individual tracks */
		private static final int MEDIA_RECORDER_TRACK_EVENT_LIST_START = 100;
		private static final int MEDIA_RECORDER_TRACK_EVENT_ERROR	   = 100;
		private static final int MEDIA_RECORDER_TRACK_EVENT_INFO	   = 101;
		private static final int MEDIA_RECORDER_TRACK_EVENT_LIST_END   = 1000;


		@Override
		public void handleMessage(Message msg) {
			if (mMediaRecorder.mNativeContext == 0) {
				Log.w(TAG, "mediarecorder went away with unhandled events");
				return;
			}
			switch(msg.what) {
			case MEDIA_RECORDER_EVENT_ERROR:
			case MEDIA_RECORDER_TRACK_EVENT_ERROR:
				if (mOnErrorListener != null)
					mOnErrorListener.onError(mMediaRecorder, msg.arg1, msg.arg2);

				return;

			case MEDIA_RECORDER_EVENT_INFO:
			case MEDIA_RECORDER_TRACK_EVENT_INFO:
				if (mOnInfoListener != null)
					mOnInfoListener.onInfo(mMediaRecorder, msg.arg1, msg.arg2);

				return;

			default:
				Log.e(TAG, "Unknown message type " + msg.what);
				return;
			}
		}
	}

	/**
	 * Called from native code when an interesting event happens.  This method
	 * just uses the EventHandler system to post the event back to the main app thread.
	 * We use a weak reference to the original MediaRecorder object so that the native
	 * code is safe from the object disappearing from underneath it.  (This is
	 * the cookie passed to native_setup().)
	 */
	private static void postEventFromNative(Object mediarecorder_ref,
                                            int what, int arg1, int arg2, Object obj)
    {
        HdmiRecorder mr = (HdmiRecorder)((WeakReference)mediarecorder_ref).get();
        if (mr == null) {
            return;
        }

        if (mr.mEventHandler != null) {
            Message m = mr.mEventHandler.obtainMessage(what, arg1, arg2, obj);
            mr.mEventHandler.sendMessage(m);
        }
    }

	public void hello(){
        Log.i(TAG, "hello");
	}
	
}



