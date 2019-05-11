package com.halil.ozel.bluetoothchatapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    // Button tanımlamaları
     Button listen, send, listDevices;

     // listview tanımı
     ListView listView;

     // textview tanımlamaları
     TextView messageBox, status;

     // edittext tanımı
     EditText writeMessage;

     // bluetoothadapter
     BluetoothAdapter bluetoothAdapter;

     // BluetoothDevice array tanımı
     BluetoothDevice [] bluetoothDevices;


     // sendReceive değişkeni
     SendReceive sendReceive;

     // STATE_LISTENING sabit değeri
     static final int STATE_LISTENING = 1;

    // STATE_CONNECTING sabit değeri
     static final int STATE_CONNECTING = 2;

    // STATE_CONNECTED sabit değeri
     static final int STATE_CONNECTED = 3;

    // STATE_CONNECTION_FAILED sabit değeri
     static final int STATE_CONNECTION_FAILED = 4;

    // STATE_MESSAGE_RECEIVED sabit değeri
     static final int STATE_MESSAGE_RECEIVED = 5;

     // REQUEST_ENABLE_BLUETOOTH değeri tanımı ve değeri
     int REQUEST_ENABLE_BLUETOOTH = 1;

     // app name değeri
     private static final String APP_NAME = "BluetoothChatApp";

     // uuid değeri değeri
     private static final UUID MY_UUID = UUID.fromString("318c6089-985c-4773-b7ca-4c6130e4209e");



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // findViewById değerlerini gosterme işlemleri
        listen = findViewById(R.id.listen);
        send = findViewById(R.id.send);
        listDevices = findViewById(R.id.listDevices);
        listView = findViewById(R.id.listview);
        status = findViewById(R.id.status);
        messageBox = findViewById(R.id.msg);
        writeMessage = findViewById(R.id.writemsg);

        // adapter nesnesiyle bluetooth aygıtına erişim
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // bluetoothAdapter değeri açık değilse
        if (!bluetoothAdapter.isEnabled()){

            // bluetooth iznini kullanıcıdan istiyoruz.
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent,REQUEST_ENABLE_BLUETOOTH);
        }
        
        implementListeners();
    }



    // implementListeners adında bir function
    private void implementListeners() {



        // listelemeye tıklayınca neler olacak
        listDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // devices cihazların listelendiği
                Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();

                // cihazların değeri kadar al ve diziye ata
                String[] strings = new String[devices.size()];

                // bluetoothDevices size değerini ata
                bluetoothDevices =new BluetoothDevice[devices.size()];

                // index değeri 0 olarak verdik.
                int index=0;

                // cihazlarının boyutu 0 dan büyükse
                if (devices.size()>0){

                    // loop ile cihazları döndür.
                    for (BluetoothDevice device : devices){

                        // bluetoothDevices index değerine device ata
                        bluetoothDevices[index] = device;

                        // isimleri index değerine koy
                        strings[index] = device.getName();

                        // index değerini arttır
                        index++;
                    }

                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,strings);
                    listView.setAdapter(arrayAdapter);
                }

            }
        });


        // listen butonuna tıklanınca neler olacak
        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // ServerClass oluşturma
                ServerClass serverClass = new ServerClass();

                // serverclass başlat
                serverClass.start();
            }
        });


        // listviewe tıklanınca neler olacak
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long id) {

                // ClientClass oluşturma
                ClientClass clientClass = new ClientClass(bluetoothDevices[i]);

                // clientClass başlat
                clientClass.start();


                // text değerini yaz
                status.setText("Connecting");
            }
        });


        // send butonuna basınca neler olacak
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // mesajın değerini al
                String string = String.valueOf(writeMessage.getText());

                // mesajı bytes halinde yolla
                sendReceive.write(string.getBytes());
            }
        });

    }

    // Handler değişkeni tanımlama
    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {

            // Handler : UI Thread ile haberleşmeyi sağlayan bir sınıftır.
            switch (msg.what){

                // what : kullanıcı tanımlı mesaj kodudur.
                // Bu mesajın neyle ilgili olduğuna kullanıcı karar verebilir.
                // int tipinde tanımlanır.

                // STATE_LISTENING değeriyse
                case STATE_LISTENING:

                    // Listening texti yaz
                    status.setText("Listening");
                    break;

                // STATE_CONNECTING değeriyse
                case STATE_CONNECTING:

                    // Connecting texti yaz
                    status.setText("Connecting");
                    break;

                // STATE_CONNECTED değeriyse
                case STATE_CONNECTED:

                    // Connected texti yaz
                    status.setText("Connected");
                    break;

                // STATE_CONNECTION_FAILED değeriyse
                case STATE_CONNECTION_FAILED:

                    // Connection Failed texti yaz
                    status.setText("Connection Failed");
                    break;

                // STATE_MESSAGE_RECEIVED değeriyse
                case STATE_MESSAGE_RECEIVED:

                    // readBuffer değişkeni mesaj objesini al
                    byte[] readBuffer = (byte[]) msg.obj;

                    // tempMessage değişkine değerler işleniyor
                    String tempMessage = new String(readBuffer,0,msg.arg1);

                    // messageBox değerine mesajları yaz
                    messageBox.setText(tempMessage);

                    break;
            }

            return true;
        }
    });




    private class ServerClass extends Thread{

        private BluetoothServerSocket serverSocket;

        public ServerClass(){

            try{
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        public void run(){

            BluetoothSocket socket = null;

            while (socket == null){

                try{

                    Message message = Message.obtain();
                    message.what = STATE_CONNECTING;
                    handler.sendMessage(message);
                    socket = serverSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message = Message.obtain();
                    message.what = STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }

                if (socket!=null){

                    Message message = Message.obtain();
                    message.what = STATE_CONNECTED;
                    handler.sendMessage(message);


                    sendReceive = new SendReceive(socket);
                    sendReceive.start();


                    break;
                }

            }
        }
    }


    private class ClientClass extends Thread{

        private BluetoothDevice device;
        private BluetoothSocket socket;


        public ClientClass(BluetoothDevice device1){

            device = device1;

            try{
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run(){

            try {
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive = new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();

                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }


    }


    private class SendReceive extends Thread{

        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive(BluetoothSocket socket){

            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run(){

            byte [] buffer = new byte[1024];
            int bytes;

            while (true){

                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write (byte[] bytes){

            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }




}
