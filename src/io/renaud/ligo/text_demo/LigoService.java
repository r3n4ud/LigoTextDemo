package io.renaud.ligo.text_demo;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.os.Bundle;

public class LigoService extends Service {

    private static final int SERVER_PORT = 10000;

    private UsbManager mUsbManager;
    private UsbAccessory mAccessory = null;
    private ParcelFileDescriptor mParcelFileDescriptor = null;
    private ServerSocket mServerSocket = null;
    private FileInputStream mInputStream = null;
    private FileOutputStream mOutputStream = null;

    private Thread mThread = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(Constants.LOGTAG, "LigoService.onCreate()");
        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
    }

    @Override
    public void onDestroy() {
        Log.d(Constants.LOGTAG, "LigoService.onDestroy()");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.LOGTAG, "LigoService.onStartCommand()");
        String action = intent.getAction();

        if (action != null) {
            if (action.equals(Constants.ACTION_ATTACHED)) {

                Log.i(Constants.LOGTAG, "LigoService: processing ACTION_ATTACHED");
                UsbAccessory accessory =
                        (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                if (accessory != null) {
                    mAccessory = accessory;
                    new AccessoryInitializer().execute();
                }
            } else if (action.equals(Constants.ACTION_DETACHED)) {

                Log.i(Constants.LOGTAG, "LigoService: processing ACTION_DETACHED");
                closeAccessory();
                stopSelf();
            } else if (action.equals(Constants.ACTION_WRITE_DATA)) {

                Log.i(Constants.LOGTAG, "LigoService: processing ACTION_WRITE_DATA");
                if (mOutputStream != null) {
                    Bundle bundle = intent.getExtras();
                    byte[] outputBuf = bundle.getByteArray("output_buf");
                    try {
                        mOutputStream.write(outputBuf);
                    } catch(IOException e) {
                        Log.e(Constants.LOGTAG, e.getMessage());
                    }
                } else {
                    Log.e(Constants.LOGTAG, "LigoService: the output stream is null!");
                }
            }
        }

        return START_NOT_STICKY;
    }

    // accessory as arg?
    private void openAccessory() {
        Log.d(Constants.LOGTAG, "LigoService.openAccessory()");
        mParcelFileDescriptor = mUsbManager.openAccessory(mAccessory);

        if (mParcelFileDescriptor != null) {
            Log.d(Constants.LOGTAG, "LigoService: Accessory successfully opened");

            FileDescriptor fd = mParcelFileDescriptor.getFileDescriptor();
            mInputStream  = new FileInputStream(fd);
            mOutputStream = new FileOutputStream(fd);

            Runnable readRunnable = new ReadRunnable(mInputStream);
            mThread = new Thread(readRunnable);
            mThread.start();

        } else {
            Log.e(Constants.LOGTAG, "LigoService: Failed to open the accessory");
        }
    }

    private void closeAccessory() {
        Log.d(Constants.LOGTAG, "LigoService.closeAccessory()");

        if (mAccessory != null && mThread != null) {
            mThread.stop();

            try {
                mParcelFileDescriptor.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mParcelFileDescriptor = null;
            mOutputStream         = null;
            mInputStream          = null;
            mThread               = null;
        }
    }

    private class AccessoryInitializer extends AsyncTask<Void, Void, Void> {
        protected Void doInBackground(Void... arg0) {
            Log.d(Constants.LOGTAG, "AccessoryInitializer.doInBackground()");
            openAccessory();
            return null;
        }
    }

    private class ReadRunnable implements Runnable {

        private static final int BUFFER_SIZE = 1024;
        private final FileInputStream mInputStream;

        public ReadRunnable(FileInputStream inputStream) {
            mInputStream  = inputStream;
        }

        public void run() {
            Log.d(Constants.LOGTAG, "ReadRunnable −− run");

            byte[] inputBuf = new byte[BUFFER_SIZE];
            int n;
            while (true) {

                try {
                    n = mInputStream.read(inputBuf);
                    // if (n < 0) {
                    //     Log.d(Constants.LOGTAG, "End of file reached");
                    // }
                    Log.d(Constants.LOGTAG, "Read some bytes");

                    Intent readIntent = new Intent(Constants.ACTION_READ_DATA);
                    Bundle bundle = new Bundle();
                    bundle.putByteArray("input_buf", inputBuf);
                    readIntent.putExtras(bundle);
                    sendBroadcast(readIntent);

                } catch (IOException e) {
                    Log.e(Constants.LOGTAG, "ReadRunnable −− IOException");
                    break;
                }
            }

        } // run()
    } // ReadRunnable

}
