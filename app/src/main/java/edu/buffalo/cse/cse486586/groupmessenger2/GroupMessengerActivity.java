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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.PriorityQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */

class Message implements Serializable {
    //msg by client
    String msg;
    //process_id = client
    int client;
    //message id by client
    int messageId;
    //sequence no. proposed/finalized
    int sequenceNo;
    //client that suggested the sequence no
    int client_sequence;
    //sequenceNo.client_sequence
    float final_seq;
    /*
    newmsg = -1
    undeliverable = 0
    deliverable = +1
     */
    int status;
    /*deliverable/undeliverable
    boolean status;*/

    //phase1 : send message/request proposal numbers
    public Message(int client, int messageId, String msg, int status) {
        this.client = client;
        this.msg = msg;
        this.messageId = messageId;
        this.status = status;
    }

    //phase 2 : send proposal for mid
    public Message(int messageId, int sequenceNo, int client_sequence) {
        this.messageId = messageId;
        this.sequenceNo = sequenceNo;
        this.client_sequence = client_sequence;
    }
    public Message(int sequenceNo, String msg, int client, int messageId, int client_sequence, int status){
        this.messageId = messageId;
        this.sequenceNo = sequenceNo;
        this.client_sequence = client_sequence;
        this.client = client;
        this.status = status;
        this.msg = msg;
    }
    public Message(int sequenceNo, String msg, int client, int messageId, int client_sequence, int status, float final_seq){
        this.messageId = messageId;
        this.sequenceNo = sequenceNo;
        this.client_sequence = client_sequence;
        this.client = client;
        this.status = status;
        this.msg = msg;
        this.final_seq = final_seq;
    }
}

class messageComparator implements Comparator<Message>
{
    // Overriding compare()method of Comparator
    // for increasing order of sequence
    @Override
    public int compare(Message m1, Message m2) {
        if (m1.final_seq > m2.final_seq)
            return 1;
        else if (m1.final_seq < m2.final_seq)
            return -1;
        return 0;
    }
}


public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    private String[] portNumbers = {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;
    static int sequenceNumber = 0;
    int process_id;
    int counter = 0, proposal = 0;
    PriorityQueue <Message> queue = new PriorityQueue<Message>(10, new messageComparator());

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


        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        process_id = Integer.parseInt(portStr);
        Log.d("port", portStr);
        Log.d("line1",tel.getLine1Number());
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */


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
            Message msg, msg_final, temp;
            ObjectInputStream inputStream;
            ObjectOutputStream outputStream;
            //recieving from client
            //try {
            while (true)
            {
               float fin_seq =0;
                try
                {
                    Socket socket = serverSocket.accept();
                    inputStream = new ObjectInputStream(socket.getInputStream());
                    outputStream = new ObjectOutputStream(socket.getOutputStream());
                    //BufferedReader inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    //BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    //final String msg = inputStream.readLine();
                    msg = (Message) inputStream.readObject();
                    Log.d("mid", "" + msg.messageId);
                    Log.d("msg", msg.msg);
                    Log.d("client", "" + msg.client);

                    //stage 2 : recieved new message <pid, counter, msg, -1>
                    //if (msg.status == -1) {
                        //increase proposal number;
                        proposal += 1;
                        //send proposal to client <mid, proposal>
                        Message prop = new Message(msg.messageId, proposal, process_id);
                        Log.d("prop:", ""+prop.sequenceNo);
                        Log.d("proposal", ""+prop.toString());
                        //outputStream = new ObjectOutputStream(socket.getOutputStream());
                        outputStream.writeObject(prop);
                        outputStream.flush();
                        //put message in hold-back queue
                        //Message temp;
                        fin_seq = Integer.MAX_VALUE + (float) process_id/10000 ;
                        Log.d("fin_server", ""+fin_seq);
                        temp = new Message(Integer.MAX_VALUE, msg.msg, msg.client, msg.messageId, process_id, 0, fin_seq);
                        queue.add(temp);
                        Log.d("message in queue", ""+temp.sequenceNo+" "+temp.msg+" "+temp.client+" "+temp.messageId+" "+ process_id);
                   // }

                    /* receive agreement,
                    increment sequence number to max possible,
                    find message in queue, remove it and add message with deliverable and agreed sequence number.
                     */
                    inputStream = new ObjectInputStream(socket.getInputStream());
                    msg_final = (Message)inputStream.readObject();
                    Log.d("agreement 10", msg_final.msg);
                    //update local proposal number.
                    proposal = Math.max(proposal, msg_final.sequenceNo);
                    //remove temp from queue
                    queue.remove(temp);
                    //add deliverable with final sequence number to queue
                    queue.add(msg_final);

                    while(queue.peek() != null)// && queue.peek().status == 1)
                    {
                        temp = queue.poll();
                        publishProgress(temp.msg);
                    }


                    inputStream.close();
                    socket.close();
                }catch(IOException e){
                    Log.e("Error", e.getMessage());
                }catch(ClassNotFoundException e){
                    Log.e("Error", e.getMessage());
                }
            }
            //return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[0].trim();

            TextView localTextView = (TextView) findViewById(R.id.textView1);
            localTextView.append(strReceived + "\n");


            Uri.Builder builder = new Uri.Builder();
            builder.scheme("content").authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
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
            ObjectOutputStream outputStream;
            ObjectInputStream inputStream;
            counter++;
            Message message = new Message(process_id, counter, msgs[0], -1);
            Socket sockets[] = new Socket[5];
            Message recProposals[] = new Message[5];
            //String msgToSend = msgs[0];
            int i =0;
            for (String port : portNumbers)
            {
                try {
                    sockets[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(port));
                    if (sockets[i].isConnected()) {
                        //stage 1: multicast new message <pid, counter, msg, -1>
                        outputStream = new ObjectOutputStream(sockets[i].getOutputStream());
                        outputStream.writeObject(message);
                        outputStream.flush();
                        //receive proposals
                        inputStream = new ObjectInputStream(sockets[i].getInputStream());
                        recProposals[i] = (Message)inputStream.readObject();
                        Log.d("recvMsg", ""+recProposals[i].sequenceNo+" "+recProposals[i].client_sequence);
                        i++;
                    }
                } catch (UnknownHostException e) {
                    Log.e("Error", e.getMessage());
                    e.printStackTrace();
                } catch (IOException e) {
                    Log.e("Error", e.getMessage());
                    e.printStackTrace();
                }catch (ClassNotFoundException e) {
                    Log.e("Error", e.getMessage());
                    e.printStackTrace();
                }
            }
            //stage 3: receive proposals, choose proposal, send agreement
            //Once all proposal have been received, choose highest sequence no, with highest proposer
            int highest = -1;
            int proposer = -1;
            for(i =0; i < 5; i++)
            {
                try{
                    if(recProposals[i].sequenceNo >= highest){
                        highest = recProposals[i].sequenceNo;
                    }
                    if(highest == recProposals[i].sequenceNo){
                        proposer = Math.max(proposer, recProposals[i].client_sequence);
                    }
                    Log.d("highest", ""+highest+" "+proposer);
                } catch (NullPointerException e){
                    Log.e("Error", e.getMessage());
                }
            }
            //create agreement
            //  public Message(int sequenceNo, String msg, int client, int messageId, int client_sequence, int status)
            float fin_seq = highest + (float)proposer/10000;
            Log.d("final proposal", ""+fin_seq);
            Message agreement = new Message(highest, message.msg, process_id, counter, proposer, 1 , fin_seq);
            Log.d("msg", " "+agreement.sequenceNo +" "+ agreement.msg+" "+ agreement.client + " "+ agreement.messageId + " "+ agreement.client_sequence + " "+ agreement.status +" "+ agreement.final_seq);
            //send to all clients
            i =0;
            for (String port : portNumbers) {
                try {
                    outputStream = new ObjectOutputStream(sockets[i].getOutputStream());
                    outputStream.writeObject(agreement);
                    outputStream.flush();
                    i++;
                } catch (IOException e) {
                    Log.e("Error", e.getMessage());
                    e.printStackTrace();
                }
            }
            Log.d("send", "done sending");

            return null;
        }
    }



}
