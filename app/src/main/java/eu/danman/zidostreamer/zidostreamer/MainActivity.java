package eu.danman.zidostreamer.zidostreamer;

import android.app.Activity;
import android.content.Intent;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.mstar.android.tv.TvCommonManager;
import com.mstar.android.tvapi.common.TvManager;
import com.mstar.android.tvapi.common.exception.TvCommonException;
import com.mstar.android.tvapi.common.vo.TvOsType;


public class MainActivity extends ActionBarActivity {


    SurfaceView surfaceView = null;
    SurfaceHolder mSurfaceHolder = null;
    SurfaceHolder.Callback	callback = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

//        StrictMode.setThreadPolicy(policy);


        surfaceView = (SurfaceView) this.findViewById(R.id.surfaceView);


//        showHdmiOnSurfaceView();

        Intent service = new Intent(this, StreamService.class);
        startService(service);

        finish();

//        while (true){
//            ut.dataSend("test".getBytes());
//        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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


    public static boolean isHDMIinput()
    {
        boolean bRet = false;
        try
        {
            changeInputSource(TvOsType.EnumInputSource.E_INPUT_SOURCE_HDMI);
            bRet = TvManager.getInstance().getPlayerManager().isSignalStable();
        } catch (TvCommonException e)
        {
            e.printStackTrace();
        }
        return bRet;
    }

    private void showHdmiOnSurfaceView()
    {
        mSurfaceHolder = surfaceView.getHolder();
        callback = new android.view.SurfaceHolder.Callback()
        {

            @Override
            public void surfaceDestroyed(SurfaceHolder holder)
            {
            }

            @Override
            public void surfaceCreated(SurfaceHolder holder)
            {

                try
                {
                    if (holder == null || holder.getSurface() == null || holder.getSurface().isValid() == false)
                    {
                        return;
                    }
                    if (TvManager.getInstance() != null)
                    {
                        TvManager.getInstance().getPlayerManager().setDisplay(mSurfaceHolder);
                    }
                } catch (TvCommonException e)
                {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
            {
            }
        };
        mSurfaceHolder.addCallback((android.view.SurfaceHolder.Callback) callback);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


    }

}
