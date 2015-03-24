package com.example.skhalid.softmetersimulation;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.widget.Toast;

public class FloatingService extends Service {
    private FloatingImage floatingImage;
    private Messenger mServiceMessenger;
    private static Messenger mActivityMessenger;


    /**
	 * @param intent
	 */
	@Override
	public IBinder onBind(Intent intent) {
        return mServiceMessenger.getBinder();
	}

	public void onCreate() {
		super.onCreate();
        mServiceMessenger = new Messenger(new IncomingHandler());
//		floatingImage = new FloatingImage(this);
		
	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mActivityMessenger = intent.getParcelableExtra("Messenger");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
	public void onDestroy() {
		super.onDestroy();
//		floatingImage.destroy(); //  now its an efficient way to destroy the imageView
	}

    private class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MSG_SOFTMETER_ON:
                    floatingImage = new FloatingImage(getApplicationContext());
                    break;

                case Constants.MSG_SOFTMETER_OFF:
                    floatingImage.destroy();
                    break;

                case Constants.MSG_LOCATION_CHANGED:
                    floatingImage.onLocationChanged();
                    break;
                default:
                    break;
            }
        }
    }

    public static void sendMessageToLauncherActivity(int msg){
        Message lMsg = new Message();
        switch (msg){
            case Constants.MSG_DISABLE_FIELDS:
                lMsg.what = msg;
                break;
            case Constants.MSG_ENABLE_FIELDS:
                lMsg.what = msg;
                break;
            default:
                break;
        }

            try {
                mActivityMessenger.send(lMsg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
    }
}