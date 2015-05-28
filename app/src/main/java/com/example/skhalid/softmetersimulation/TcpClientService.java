package com.example.skhalid.softmetersimulation;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class TcpClientService extends Service {

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	@Override
	public void onCreate() {
		runTcpClient();
//		this.stopSelf();
	}
	private static final int TCP_SERVER_PORT = 21111;
	private void runTcpClient() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    Socket s = new Socket("192.168.4.56", TCP_SERVER_PORT);
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
                    //send output msg
                    String outMsg = "TCP connecting to " + TCP_SERVER_PORT + System.getProperty("line.separator");
                    out.write(outMsg);
                    out.flush();
                    Log.i("TcpClient", "sent: " + outMsg);
                    //accept server response
                    String inMsg = in.readLine() + System.getProperty("line.separator");
                    Log.i("TcpClient", "received: " + inMsg);
                    //close connection
                    s.close();
                }  catch (Exception e) {
                    e.printStackTrace();
                    Crashlytics.logException(e);
                }

            }
        }).start();

    }
}
