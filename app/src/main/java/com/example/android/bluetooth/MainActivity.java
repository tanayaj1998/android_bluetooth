package com.example.android.bluetooth;

import androidx.appcompat.app.AppCompatActivity;
import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import static java.lang.System.out;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public BluetoothAdapter btAdapter;
    public BluetoothDevice btDevice;
    public BluetoothSocket btSocket;
    public static final String SERVICE_ID = "00001101-0000-1000-8000-00805F9B34FB"; //SPP UUID
    public static final String SERVICE_ADDRESS = "00:21:13:02:2A:75"; // HC-05 BT ADDRESS

    TextView out;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        out = (TextView) findViewById(R.id.out);
        //Set Bluetooth Adapter
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        btDevice = btAdapter.getRemoteDevice(SERVICE_ADDRESS);


        if(btAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth not available", Toast.LENGTH_LONG).show();
        } else {
            if (!btAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, 3);
            } else {
                ConnectThread connectThread = new ConnectThread(btDevice);
                Toast.makeText(getApplicationContext(), "connecting...", Toast.LENGTH_LONG).show();
                connectThread.start();

            }
        }
    }

        // Connecting thread
        private class ConnectThread extends Thread {
            private final BluetoothSocket thisSocket;
            private final BluetoothDevice thisDevice;

            public ConnectThread(BluetoothDevice device) {
                BluetoothSocket tmp = null;
                thisDevice = device;

                try {
                    tmp = thisDevice.createRfcommSocketToServiceRecord(UUID.fromString(SERVICE_ID));
                } catch (IOException e) {
                    Log.e("TEST", "Can't connect to service");
                }
                thisSocket = tmp;
            }

            public void run() {
                // Cancel discovery because it otherwise slows down the connection.
                btAdapter.cancelDiscovery();

                try {
                    thisSocket.connect();
                    Log.d("TESTING", "Connected to socket");
                } catch (IOException connectException) {
                    try {
                        thisSocket.close();
                    } catch (IOException closeException) {
                        Log.e("TEST", "Can't close socket");
                    }
                    return;
                }
                connected(thisSocket);
                btSocket = thisSocket;

            }
            public void cancel() {
                try {
                    thisSocket.close();
                } catch (IOException e) {
                    Log.e("TEST", "Can't close socket");
                }
            }
        }
    private void connected(BluetoothSocket mmSocket) {
        Log.d(TAG, "connected: Starting.");

        // Start the thread to manage the connection and perform transmissions
        ConnectedThread connectedThread = new ConnectedThread(mmSocket);
        connectedThread.start();
    }
        //Connected Thread Recieving message
        private class ConnectedThread extends Thread {
            private final BluetoothSocket mmSocket;
            private final InputStream mmInStream;
            private byte[] mmBuffer;

            public ConnectedThread(BluetoothSocket socket) {
                mmSocket = socket;
                InputStream tmpIn = null;


                // Get the input and output streams; using temp objects because
                // member streams are final.
                try {
                    tmpIn = socket.getInputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating input stream", e);
                }


                mmInStream = tmpIn;

            }

            public void run() {
                mmBuffer = new byte[1024];
                int numBytes; // bytes returned from read()

                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    try {
                       // Read from the InputStream.
                         numBytes = mmInStream.read(mmBuffer);
                        // Send the obtained bytes to the UI activity.
                        final String incomingMessage = new String(mmBuffer, 0, numBytes);
                        Log.d(TAG, "InputStream: " + incomingMessage);

                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                out.setText(incomingMessage);
                            }
                        });
                    } catch (IOException e) {
                        Log.d(TAG, "Input stream was disconnected", e);
                        break;
                    }
                }
            }

            // Call this method from the main activity to shut down the connection.
            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "Could not close the connect socket", e);
                }
            }
        }
}








