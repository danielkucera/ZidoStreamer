package eu.danman.zidostreamer.zidostreamer;

/**
 * Created by dano on 12/5/15.
 */


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Comparator;


public class UdpThread extends Thread{

    DatagramSocket ds = null;
    DatagramPacket dp;

    InetAddress server = null;

    String path = null;

    int port            = 1234;
//    String serverName   = "10.0.0.149";
    String serverName   = "239.255.0.1";
//    String serverName   = "danman.eu";

//    byte [] addr = new byte[] {(byte)10,(byte)0,(byte)0,(byte)149};

    public UdpThread(String m_path){

        path = m_path;

//        File f = new File("/mnt/usb/sda1/HdmiRecorder");
//        File f = new File("/sdcard/HdmiRecorder");

        /*
        File [] files = f.listFiles();

Arrays.sort(files, new Comparator() {
    public int compare(Object o1, Object o2) {

        if (((File) o1).lastModified() > ((File) o2).lastModified()) {
            return -1;
        } else if (((File) o1).lastModified() < ((File) o2).lastModified()) {
            return +1;
        } else {
            return 0;
        }
    }

});
        file = files[0];

        Log.d("file",file.getAbsolutePath());
*/
    }
/*
    public void openFile(String filename){

        file = new File(filename);

    }
*/

    public void run() {
        File file = new File(path);

        while (server == null){
            try {
                server = InetAddress.getByName(serverName);
//                server = InetAddress.getByAddress(addr);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        while (ds == null){

            try {
                ds = new DatagramSocket();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        String message;
        byte[] lmessage = new byte[1500];
        DatagramPacket packet = new DatagramPacket(lmessage, lmessage.length);


//        dataSend("".getBytes());

        FileInputStream dis;

        try {
            dis = new FileInputStream(file);
            try {

                String tsdata;
                byte[] tsdatab = new byte[1366];

        while (true) {

            /*
            tsdata = "";

            while (tsdata.length() <1366) {


                    tsdata += dis.readByte();

            }
            */

            if (dis.available() > 1365){
                dis.read(tsdatab, 0, 1366);

                dataSend(tsdatab);

            }

        }
        } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }

    public void dataSend(byte[] data) throws IOException {
        dp = new DatagramPacket(data, data.length, server, port);

            ds.send(dp);

    }


}