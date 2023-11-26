package com.halil.ozel.bluetoothchatapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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

    // Button tanimlamalari
    Button listen, send, listDevices;

    // ListView tanimi
    ListView listView;

    // TextView tanimlamalari
    TextView messageBox, status;

    // EditText tanimi
    EditText writeMessage;

    // BluetoothAdapter
    BluetoothAdapter bluetoothAdapter;

    // BluetoothDevice array tanimi
    BluetoothDevice[] bluetoothDevices;

    // SendReceive degiskeni
    SendReceive sendReceive;

    // STATE_LISTENING sabit degeri
    static final int STATE_LISTENING = 1;

    // STATE_CONNECTING sabit degeri
    static final int STATE_CONNECTING = 2;

    // STATE_CONNECTED sabit degeri
    static final int STATE_CONNECTED = 3;

    // STATE_CONNECTION_FAILED sabit degeri
    static final int STATE_CONNECTION_FAILED = 4;

    // STATE_MESSAGE_RECEIVED sabit degeri
    static final int STATE_MESSAGE_RECEIVED = 5;

    // REQUEST_ENABLE_BLUETOOTH degeri tanımı
    int REQUEST_ENABLE_BLUETOOTH = 1;

    // App name degeri
    private static final String APP_NAME = "BluetoothChatApp";

    // uuid değeri değeri
    private static final UUID MY_UUID = UUID.fromString("318c6089-985c-4773-b7ca-4c6130e4209e");


    @SuppressLint("MissingPermission")
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
        if (!bluetoothAdapter.isEnabled()) {

            // bluetooth iznini kullanıcıdan istiyoruz.
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            // enableIntent işlemi
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
        }

        // implementListeners metodun çağrılması
        implementListeners();
    }


    // implementListeners adında bir function
    private void implementListeners() {
        // listelemeye tıklayınca neler olacak
        listDevices.setOnClickListener(v -> {

            // devices cihazların listelendiği
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                Set<BluetoothDevice> devices = bluetoothAdapter.getBondedDevices();

                // cihazların değeri kadar al ve diziye ata
                String[] strings = new String[devices.size()];

                // bluetoothDevices size değerini ata
                bluetoothDevices = new BluetoothDevice[devices.size()];

                // index değeri 0 olarak verdik.
                int index = 0;

                // cihazlarının boyutu 0 dan büyükse
                if (devices.size() > 0) {

                    // loop ile cihazları döndür.
                    for (BluetoothDevice device : devices) {

                        // bluetoothDevices index değerine device ata
                        bluetoothDevices[index] = device;

                        // isimleri index değerine koy
                        strings[index] = device.getName();

                        // index değerini arttır
                        index++;
                    }

                    // adapter nesnesi tanımlandı.
                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, strings);

                    // listview'e adapteri ata
                    listView.setAdapter(arrayAdapter);
                }


                // listen butonuna tıklanınca neler olacak
                listen.setOnClickListener(v3 -> {
                    // ServerClass oluşturma
                    ServerClass serverClass = new ServerClass();

                    // serverclass başlat
                    serverClass.start();
                });

                // listviewe tıklanınca neler olacak
                listView.setOnItemClickListener((parent, view, i, id) -> {

                    // ClientClass oluşturma
                    ClientClass clientClass = new ClientClass(bluetoothDevices[i]);

                    // clientClass başlat
                    clientClass.start();

                    // text değerini yaz
                    status.setText(R.string.connecting);
                });


                // send butonuna basınca neler olacak
                send.setOnClickListener(v1 -> {

                    // mesajın değerini al
                    String string = String.valueOf(writeMessage.getText());

                    // mesajı bytes halinde yolla
                    sendReceive.write(string.getBytes());
                });
            }
        });
    }

    // Handler değişkeni tanımlama
    Handler handler = new Handler(new Handler.Callback() {
        @SuppressLint("SetTextI18n")
        @Override
        public boolean handleMessage(Message msg) {

            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // Handler : UI Thread ile haberleşmeyi sağlayan bir sınıftır.
                switch (msg.what) {

                    // what : kullanıcı tanımlı mesaj kodudur.
                    // Bu mesajın neyle ilgili olduğuna kullanıcı karar verebilir.
                    // int tipinde tanımlanır.

                    // STATE_LISTENING değeriyse
                    case STATE_LISTENING:

                        // Listening texti yaz
                        status.setText(R.string.listening);
                        break;

                    // STATE_CONNECTING değeriyse
                    case STATE_CONNECTING:

                        // Connecting texti yaz
                        status.setText(R.string.connecting);
                        break;

                    // STATE_CONNECTED değeriyse
                    case STATE_CONNECTED:

                        // Connected texti yaz
                        status.setText(R.string.connected);
                        break;

                    // STATE_CONNECTION_FAILED değeriyse
                    case STATE_CONNECTION_FAILED:

                        // Connection Failed texti yaz
                        status.setText(R.string.failed);
                        break;

                    // STATE_MESSAGE_RECEIVED değeriyse
                    case STATE_MESSAGE_RECEIVED:

                        // readBuffer değişkeni mesaj objesini al
                        byte[] readBuffer = (byte[]) msg.obj;

                        // tempMessage değişkine değerler işleniyor
                        String tempMessage = new String(readBuffer, 0, msg.arg1);

                        // messageBox değerine mesajları yaz
                        messageBox.setText(tempMessage);

                        // işlemi bitir.
                        break;
                }
            }
            // değeri true döndür
            return true;
        }
    });


    // ServerClass sınıfında yapılacak işlemler
    private class ServerClass extends Thread {

        // BluetoothServerSocket nesnesi
        private BluetoothServerSocket serverSocket;

        // ServerClass yapıcı fonksiyonu
        @SuppressLint("MissingPermission")
        public ServerClass() {

            try {
                // serverSocket değerine uuid değerini kayıtla
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID);

                // IOException yakalama
            } catch (IOException e) {
                // hatayı bastır
                e.printStackTrace();
            }
        }

        // run fonksiyonunun işlemleri
        public void run() {
            // BluetoothSocket nesnesine null verildi.
            BluetoothSocket socket = null;

            // socket değeri null ise
            while (true) {

                try {
                    // Alınan mesajı istediğimiz değerleri verebiliriz.
                    Message message = Message.obtain();

                    // bağlantı oluştuğunda
                    message.what = STATE_CONNECTING;

                    // mesajı yollama işi
                    handler.sendMessage(message);

                    // socket işini kabul et
                    socket = serverSocket.accept();

                } catch (IOException e) {
                    e.printStackTrace();

                    // mesaj örneğini alma işi
                    Message message = Message.obtain();

                    // STATE_CONNECTION_FAILED olayı olunca
                    message.what = STATE_CONNECTION_FAILED;

                    // mesajı yollama
                    handler.sendMessage(message);
                }

                // socket değeri null değilse
                if (socket != null) {

                    // Alınan mesajı istediğimiz değerleri verebiliriz.
                    Message message = Message.obtain();

                    // bağlantı oluştuğunu anlama işi
                    message.what = STATE_CONNECTED;

                    // mesajı yollama işini handler ile yap
                    handler.sendMessage(message);

                    // sendReceive nesnesi tanımı
                    sendReceive = new SendReceive(socket);

                    // sendReceive işini başlat
                    sendReceive.start();

                    // işlemi kırk bırak
                    break;
                }
            }
        }
    }


    // ClientClass sınıfında yapılacak işlemler
    private class ClientClass extends Thread {

        // BluetoothDevice nesnesi
        private BluetoothDevice device;

        // BluetoothSocket nesnesi
        private BluetoothSocket socket;

        // ClientClass yapıcı sınıfı
        public ClientClass(BluetoothDevice device1) {

            // nesneye device değerine device1'i ata
            device = device1;

            try {
                // uuid değerine socket işine ata
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    socket = device.createRfcommSocketToServiceRecord(MY_UUID);
                }

                // hatayı yakala
            } catch (IOException e) {
                // hatayı yazdır
                e.printStackTrace();
            }
        }

        // run fonksiyonunda yapılacak işler
        public void run() {
            try {
                // socket'i connect et.
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    socket.connect();

                    // bir mesaj örneği alabilmek için bu metot kullanılır. Alınan mesaja istediğimiz değerler verilir.
                    Message message = Message.obtain();

                    // kullanıcı tanımlı mesaj kodudur. Int tipinde tanımlanır.
                    message.what = STATE_CONNECTED;

                    // mesaj kuyruğunun sonuna bir mesaj eklemeyi sağlar.
                    handler.sendMessage(message);

                    // sendReceive nesnesi işlemi
                    sendReceive = new SendReceive(socket);

                    // sendReceive başlat
                    sendReceive.start();
                }

            } catch (IOException e) {
                e.printStackTrace();

                // Alınan message istediğimiz değerler verilir.
                Message message = Message.obtain();

                // eğer fail olduysa değer
                message.what = STATE_CONNECTION_FAILED;

                // handler sendmessage işlemi
                handler.sendMessage(message);
            }
        }
    }

    // SendReceive sınıfı
    private class SendReceive extends Thread {
        // InputStream değişkeni
        private final InputStream inputStream;

        // OutputStream değişkeni
        private final OutputStream outputStream;

        // SendReceive constructor
        public SendReceive(BluetoothSocket socket) {

            // socket'i atadık.
            // BluetoothSocket değişkeni

            // tempInput null değeri
            InputStream tempInput = null;

            // tempOutput null değeri
            OutputStream tempOutput = null;

            try {
                // tempInput getInputStream metodu
                tempInput = socket.getInputStream();

                // tempOutput getOutputStream metodu
                tempOutput = socket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }

            // inputStream değerine tempInput değerini ata
            inputStream = tempInput;

            // outputStream değerine tempOutput değerini ata
            outputStream = tempOutput;
        }

        // run fonksiyonunda yapılacak işler
        public void run() {
            // buffer nesnesi oluşturma
            byte[] buffer = new byte[1024];

            // bytes değişkeni
            int bytes;

            // true değeri döndükçe
            while (true) {

                try {
                    // bytes değişkenine read buffer yap.
                    bytes = inputStream.read(buffer);

                    // parametreleri tutan bir mesaj oluşturmak için kullanılır.
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED, bytes, -1, buffer).sendToTarget();

                    // hatayı yakalama işlemi
                } catch (IOException e) {
                    // hatayı yazdır
                    e.printStackTrace();
                }
            }
        }

        // write işleminin yapıldığı function
        public void write(byte[] bytes) {
            try {
                // outputStream değerini yaz
                outputStream.write(bytes);
                // hata yakalama
            } catch (IOException e) {
                // hatayı yazdır
                e.printStackTrace();
            }
        }
    }
}
