package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private String[] portNumbers = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;
    static int sequenceNumber = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }
        final EditText editText = (EditText) findViewById(R.id.editText1);

        findViewById(R.id.button4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            //recieving from client
            try {
                while (true) {
                    Socket socket = serverSocket.accept();
                    BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    final String msg = inputStream.readLine();
                    if (msg != null) {
                        publishProgress(msg);
                    }else{
                        Log.e("Exiting","Empty String");
                        break;
                    }

                    inputStream.close();
                    socket.close();
                }
            }catch (IOException e) {
                Log.e("Error", e.getMessage());
            }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();

            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append(strReceived + "\n");


            Uri.Builder builder = new Uri.Builder();
            builder.scheme("content").authority("edu.buffalo.cse.cse486586.groupmessenger1.provider");
            Uri uri = builder.build();


            ContentValues keyValueToInsert = new ContentValues();
            // inserting <"key", "value">
            keyValueToInsert.put("key", Integer.toString(sequenceNumber++));
            keyValueToInsert.put("value", strReceived);

            //Uri newUri =
            getContentResolver().insert(uri, keyValueToInsert);

        }
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String msgToSend = msgs[0];

                for(String port : portNumbers) {
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));
                    if(socket.isConnected()){
                        BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        outputStream.write(msgToSend);
                        outputStream.flush();
                    }
                }
            } catch (UnknownHostException e) {
                Log.e("Error", e.getMessage());
            } catch (IOException e) {
                Log.e("Error", e.getMessage());
            }

            return null;
        }
    }
}
