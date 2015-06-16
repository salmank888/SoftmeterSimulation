package com.example.skhalid.softmetersimulation;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import com.crashlytics.android.Crashlytics;

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

	}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mActivityMessenger = intent.getParcelableExtra("Messenger");

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
	public void onDestroy() {
		super.onDestroy();
        if(floatingImage != null)
		floatingImage.destroy(); //  now its an efficient way to destroy the imageView
	}

    private class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case Constants.MSG_SOFTMETER_POWER_ON:
                    floatingImage = new FloatingImage(getApplicationContext());
                    break;

                case Constants.MSG_SOFTMETER_POWER_OFF:
                    floatingImage.destroy();
                    floatingImage = null;
                    break;

                case Constants.MSG_MON_RSP:
                    floatingImage.hired();
                    break;

                case Constants.MSG_TOFF_RSP:
                    floatingImage.timeOff();
                    break;

                case Constants.MSG_TON_RSP:
                    floatingImage.timeOn();
                    break;

                case Constants.MSG_MOFF_RSP:
                    floatingImage.meterOff();
                    break;
                default:
                    break;
            }
        }
    }

    public static void sendMessageToLauncherActivity(Message msg){
        Message lMsg = new Message();
        switch (msg.what){
            case Constants.MSG_MON:
                lMsg = msg;
                break;
            case Constants.MSG_TOFF:
                lMsg = msg;
                break;
            case Constants.MSG_MOFF:
                lMsg = msg;
                break;
            case Constants.MSG_TON:
                lMsg = msg;
                break;
            default:
                break;
        }

            try {
                mActivityMessenger.send(lMsg);
            } catch (RemoteException e) {
                e.printStackTrace();
                Crashlytics.logException(e);
            }
    }


}
