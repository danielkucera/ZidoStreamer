package eu.danman.zidostreamer.zidostreamer;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mstar.android.tv.TvCommonManager;
import com.mstar.android.tvapi.common.TvManager;
import com.mstar.android.tvapi.common.exception.TvCommonException;
import com.mstar.android.tvapi.common.vo.TvOsType;

import java.util.Arrays;

public class MainActivity extends ActionBarActivity {


    SurfaceView surfaceView = null;
    SurfaceHolder mSurfaceHolder = null;
    SurfaceHolder.Callback	callback = null;
    TextView textView = null;
    ScrollView mScrollView = null;
    Intent service = null;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = (SurfaceView) this.findViewById(R.id.surfaceView);
        textView = (TextView) findViewById(R.id.textView);
        mScrollView = (ScrollView) findViewById(R.id.scrollView);

        enableHDMI();

        if (activityReceiver != null) {
            //Create an intent filter to listen to the broadcast sent with the action "ACTION_STRING_SERVICE"
            IntentFilter intentFilter = new IntentFilter("ToStreamerActivity");
            //Map the intent filter to the receiver
            registerReceiver(activityReceiver, intentFilter);
        }

        startStreaming();

    }

    private boolean isStreaming() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("eu.danman.zidostreamer.zidostreamer.StreamService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startStreaming(){
            service = new Intent(this, StreamService.class);
            startService(service);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        Intent i = new Intent(this, Settings.class);
        startActivity(i);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        Toast.makeText(this, "pressed "+event.toString(), Toast.LENGTH_SHORT).show();

        switch (keyCode) {
            case KeyEvent.KEYCODE_1:
        /* Sample for handling the Menu button globally */
                startStreaming();
                return true;
            case KeyEvent.KEYCODE_2:
                stopStreaming();
                return true;
        }
        return false;
    }

    private BroadcastReceiver activityReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String log = intent.getExtras().getString("log");

            textView.append(log+"\n");
            mScrollView.smoothScrollTo(0, textView.getBottom());
        }
    };



}
