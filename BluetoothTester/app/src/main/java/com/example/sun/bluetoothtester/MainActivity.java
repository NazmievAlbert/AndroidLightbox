package com.example.sun.bluetoothtester;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

   //TextView statusView;

    private static final String TAG = "MainActivity";
    //private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Default UUID for HC-05

    BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mmDevice = null;
    BTThread btThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


       // statusView = (TextView)findViewById(R.id.textView_status);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Device doesn't support Bluetooth");
        }
        if(checkBTStatus()) {
            Log.d(TAG, "BT is enabled");
            findPairedBTDevice();
            mBluetoothAdapter.cancelDiscovery();
            btThread = new BTThread(mmDevice);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");
        if(!(btThread ==null)) btThread.cancel();

    }


    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "...In onResume()...");
        if(!(btThread ==null))if(!btThread.isrunned) btThread.start();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_OK){
            if(requestCode==77){                                               // bluetooth enabled
                findPairedBTDevice();
                mBluetoothAdapter.cancelDiscovery();
                btThread = new BTThread(mmDevice);
            }
            if(requestCode==99){
                findPairedBTDevice();
                mBluetoothAdapter.cancelDiscovery();
                btThread = new BTThread(mmDevice);
            }

        }else{
            // show error
        }
    }

    public void onButtonClick(View view) {

        switch (view.getId()) {

            case R.id.button_send:                       //send button is pressed
                Log.d(TAG, "send button is pressed");
                if(!(btThread ==null))if(btThread.isrunned)btThread.writeString("THello Hello");
                break;

            case R.id.button_send_colors:

                ColorPickerDialogBuilder
                        .with(this)
                        .setTitle("Choose color")
                        .initialColor(Color.BLUE)
                        .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                        .density(10)
                        .setOnColorSelectedListener(new OnColorSelectedListener() {
                            @Override
                            public void onColorSelected(int selectedColor) {

                                int a,r,g,b;
                                a = (selectedColor >> 24) & 0xff; // or color >>> 24
                                r = (selectedColor >> 16) & 0xff;
                                g = (selectedColor >>  8) & 0xff;
                                b = (selectedColor      ) & 0xff;

                                //toast("onColorSelected: R:" + Integer.toString(r)+" G:" + Integer.toString(g)+" B:"+Integer.toString(b));


                            }
                        })
                        .setPositiveButton("ok", new ColorPickerClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                                int a,r,g,b;
                                a = (selectedColor >> 24) & 0xff; // or color >>> 24
                                r = (selectedColor >> 16) & 0xff;
                                g = (selectedColor >>  8) & 0xff;
                                b = (selectedColor      ) & 0xff;

                                //sendData("T"+"onColorSelected: R:" + Integer.toString(r)+" G:" + Integer.toString(g)+" B:"+Integer.toString(b));
                                btThread.sendColor(r,g,b);
                            }
                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .build()
                        .show();



                break;


        }
    }


    private void toast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();

    }

    private boolean checkBTStatus() {
        boolean result=true;
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 77);

            Log.d(TAG, "Bluetooth is disabled. Sending the request to enable it");
            result= false;
        }
        return result;
    }

    private void findPairedBTDevice() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        Log.d(TAG, "get paired devices list");                                          //getting paired devices list
        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                if (!(deviceName == null))
                    if ("HC-05".equals(deviceName) || "HC-06".equals(deviceName) || "lightbox".equals(deviceName)) {                         //If device name = HC-05 select the device as target
                        mmDevice = device;
                        toast("The device " + mmDevice.getName() + " MAC:" + mmDevice.getAddress() + " was found in paired devices");
                    }
                Log.d(TAG, deviceName + " MAC" + deviceHardwareAddress);
            }
            if (mmDevice == null) findUnpairedBTDevice();

        } else findUnpairedBTDevice();


    }

    private void findUnpairedBTDevice() {
        Intent intentOpenBluetoothSettings = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        intentOpenBluetoothSettings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intentOpenBluetoothSettings, 99);
    }

    private class BTThread extends Thread {
        Long timestamp;
        private BluetoothSocket btSocket;
        private BluetoothDevice btDevice;
        private OutputStream outStream = null;
        public boolean isrunned=false;

        public BTThread(BluetoothDevice device) {

            btDevice = device;
                                }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.

            BluetoothSocket tmp = null;
            ParcelUuid[] supportedUuids = btDevice.getUuids();
            UUID uuid = supportedUuids[0].getUuid();
            try {
                tmp = btDevice.createRfcommSocketToServiceRecord(uuid);
                Log.d(TAG, "Creating the socket");

             } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            btSocket = tmp;


            try {
            Thread.sleep(200);
            } catch (Exception e){
                Log.e(TAG, "Can't wait", e);
            }


            Log.d(TAG, "Difference = " + timestamp.toString());
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                btSocket.connect();
                Log.d(TAG, "Opening the socket");

            } catch (IOException connectException) {
                Log.e(TAG, "Could not open the client socket", connectException);
                                                                                                        //Should add fallback

                    try {
                        btSocket.close();
                        Log.d(TAG, "Closing the socket");
                    } catch (IOException closeException) {
                        Log.e(TAG, "Could not close the client socket", closeException);

                }
                return;
            }

            timestamp = System.currentTimeMillis();
            try {
                Thread.sleep(200);
            } catch (Exception e){
                Log.e(TAG, "Can't wait", e);
            }
            timestamp=System.currentTimeMillis()-timestamp;

            Log.d(TAG, "Difference = " + timestamp.toString());

            try {
                outStream = btSocket.getOutputStream();
                Log.d(TAG, "Opening  the stream");
            } catch (IOException e) {
                Log.e(TAG, "Fatal Error in output stream creation failed:" + e.getMessage());
            }
            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            // manageMyConnectedSocket(mmSocket);
            isrunned=true;
        }


        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            Log.d(TAG, "closing  the thread");
                if(isrunned) {
                    if (outStream != null) {                                                                //closing the stream
                        try {
                            outStream.close();
                            Log.d(TAG, "flushing  the stream");
                        } catch (IOException e) {
                            Log.e(TAG, "Could not close the stream", e);
                        }
                        outStream = null;
                    }

                    try {                                                                                   //closing the socket
                        btSocket.close();
                        Log.d(TAG, "Closing  the socket");
                    } catch (IOException e) {
                        Log.e(TAG, "Could not close the client socket", e);
                    }
                    isrunned = false;
                }
        }

        public void writeString(String message) {
            byte[] msgBuffer = message.getBytes();
            Log.d(TAG, "...Посылаем данные: " + message + "...");
            try {
                outStream.write(msgBuffer);
            } catch (IOException e) {
                String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            }

        }
        public void sendColor(int r, int g , int b){
            byte rr=(byte)r;
            byte gg=(byte)g;
            byte bb=(byte)b;
            byte[] msgBuffer = {66,rr,gg,bb};

            try {
                outStream.write(msgBuffer);
            } catch (IOException e) {
                String msg = "In onResume() and an exception occurred during write: " + e.getMessage();
            }

        }


    }




}


