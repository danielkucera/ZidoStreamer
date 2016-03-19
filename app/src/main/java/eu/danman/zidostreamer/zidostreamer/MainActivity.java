package eu.danman.zidostreamer.zidostreamer;

import android.app.Activity;
import android.content.Intent;
import android.os.StrictMode;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity {


    SurfaceView surfaceView = null;
    SurfaceHolder mSurfaceHolder = null;
    SurfaceHolder.Callback	callback = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        surfaceView = (SurfaceView) this.findViewById(R.id.surfaceView);

        Intent service = new Intent(this, StreamService.class);
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
            case KeyEvent.KEYCODE_MENU:
        /* Sample for handling the Menu button globally */
                return true;
        }
        return false;
    }

}
