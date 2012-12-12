package io.renaud.ligo.text_demo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class LigoTextDemo extends Activity {

    EditText mToUsbEditText;
    TextView mFromUsbTextView;

    private final BroadcastReceiver mEventsReceiver =
            new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();

                    if (android.hardware.usb.UsbManager.ACTION_USB_ACCESSORY_DETACHED
                            .equals(action)) {
                        UsbAccessory accessory =
                                (UsbAccessory)intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                        if (accessory != null) {
                            Intent serviceIntent = null;
                            Log.d(Constants.LOGTAG, "forwarding ACTION_USB_ACCESSORY_DETACHED");
                            serviceIntent = new Intent(Constants.ACTION_DETACHED);
                            serviceIntent.putExtra(UsbManager.EXTRA_ACCESSORY,
                                    getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY));
                            startService(serviceIntent);
                            finish();
                        }
                    } else if (action.equals(Constants.ACTION_READ_DATA)) {
                        Log.d(Constants.LOGTAG, "forwarding ACTION_READ_DATA");
                        Bundle bundle   = intent.getExtras();
                        byte[] inputBuf = bundle.getByteArray("input_buf");
                        mFromUsbTextView.setText(new String(inputBuf, 0, inputBuf.length));
                    }
                }
            };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(Constants.LOGTAG, "onCreate");
        super.onCreate(savedInstanceState);

        IntentFilter eventsFilter = new IntentFilter();
        eventsFilter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
        eventsFilter.addAction(Constants.ACTION_READ_DATA);
        registerReceiver(mEventsReceiver, eventsFilter);

        setContentView(R.layout.main);
        mToUsbEditText   = (EditText) findViewById(R.id.etToUsb);
        mFromUsbTextView = (TextView) findViewById(R.id.tvFromUsb);
    }

    @Override
    public void onStart() {
        Log.d(Constants.LOGTAG, "onStart");
        super.onStart();

        String action = getIntent().getAction();
        Intent serviceIntent = null;

        if (action.equals(UsbManager.ACTION_USB_ACCESSORY_ATTACHED)) {
            Log.d(Constants.LOGTAG, "forwarding ACTION_USB_ACCESSORY_ATTACHED");
            serviceIntent = new Intent(Constants.ACTION_ATTACHED);
            serviceIntent.putExtra(UsbManager.EXTRA_ACCESSORY,
                    getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY));
            startService(serviceIntent);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // Warning : the android::configChanges attribute is set to orientation in the activity
        // manifest, which means that the activity is not destroyed/created when the screen
        // orientation changes. For the moment, nothing needs to be done. However, if the
        // orientation change needs to be handled, it must be done in that method...
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onDestroy() {
        Log.d(Constants.LOGTAG, "onDestroy");
        unregisterReceiver(mEventsReceiver);

        Log.d(Constants.LOGTAG, "sending stopService to LigoService");
        Intent serviceIntent = null;
        serviceIntent = new Intent(this, LigoService.class);
        stopService(serviceIntent);
        super.onDestroy();
    }

    public void onClick(View v) {
        String s = mToUsbEditText.getText().toString();
        Log.d(Constants.LOGTAG, "sending ACTION_WRITE_DATA");
        Intent writeIntent = new Intent(Constants.ACTION_WRITE_DATA);
        Bundle b = new Bundle();
        b.putByteArray("output_buf", s.getBytes());
        writeIntent.putExtras(b);
        startService(writeIntent);
    }

}
