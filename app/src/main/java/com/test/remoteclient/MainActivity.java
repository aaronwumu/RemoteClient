
package com.test.remoteclient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

public class MainActivity extends Activity {
    private static final String TAG             = "client";
    
    private static final int PORT               = 9000;
    private static final int REQUEST_CODE_FILE  = 0;
    
    private static final int SHOW_CONNECT       = 0; 
    private static final int SHOW_DONE          = 1;
    
    private Socket mTcpSocket = null;
    private BufferedOutputStream mOutStream = null;//for byte type stream
    private BufferedInputStream mInStream = null;//for byte type stream
    private boolean mTcpConnect = false;
    private boolean mSendingData = false;
    
    private EditText mEditIP;
    private TextView mTextViewFilePath;
    
    private Button mButtonConnect;
    private Button mButtonTransfer;
    private Button mButtonSelectFile;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mEditIP = (EditText)findViewById(R.id.editTextIP);
        mTextViewFilePath = (TextView)findViewById(R.id.textViewFilepath);
        
        mButtonConnect = (Button)findViewById(R.id.buttonConnect);
        mButtonSelectFile = (Button)findViewById(R.id.buttonFileSelect);
        mButtonTransfer = (Button)findViewById(R.id.buttonTransfer);
        mButtonTransfer.setEnabled(false);

        mEditIP.setText(getLocalIpAddress());
        
        mButtonConnect.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                new Thread(new ConnectThread()).start();
            }
        });
        
        mButtonSelectFile.setOnClickListener(new OnClickListener(){
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, FileExplorer.class);
                startActivityForResult(intent, REQUEST_CODE_FILE);
            }
        });
        
        mButtonTransfer.setOnClickListener(new OnClickListener(){
            public void onClick(View v){
                if(!mSendingData)
                    new Thread(new SendThread()).start();
            }
        });

        Gson gs = new Gson();
    }

    public String getLocalIpAddress() {
        try {
            Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces();
            while(en.hasMoreElements()){
                NetworkInterface intf = en.nextElement();
                Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();
                while(enumIpAddr.hasMoreElements()){
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()
                        && !inetAddress.isAnyLocalAddress()
                        && !inetAddress.isLinkLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }
        return null;
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Log.d(TAG, "onActivityResult requestCode:" + requestCode + " resultCode:" + resultCode + " intent :" + intent);
        
        if (resultCode != RESULT_OK) {
            return;
        }
        
        switch (requestCode) {
            case REQUEST_CODE_FILE:
                if (intent != null) {
                    String path = intent.getStringExtra("file_path");
                    Log.d(TAG, "select file:" + path);
                    
                    mTextViewFilePath.setText(path);
                }
                break;
                
            default:
                super.onActivityResult(requestCode, resultCode, intent);
                break;
        }
    }

    /*
     * addr:server ip
     * port server port
     */
    public int TcpConnect(String addr, int port){
        try{
            mTcpSocket = new Socket(InetAddress.getByName(addr), port);
            if( null == mTcpSocket ){
                Log.v(TAG, "socket connect fail");
                return 0;
            }
            
            mOutStream = new BufferedOutputStream(mTcpSocket.getOutputStream());
            if( null == mOutStream ){
                Log.v(TAG, "output stream fail");
                return 0;
            }

            //in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            mInStream = new BufferedInputStream(mTcpSocket.getInputStream());
            if( null == mInStream ){
                Log.v(TAG, "input stream fail");
                return 0;
            }
            
            mTcpConnect = true;
        }catch(Exception e){
            Log.v(TAG, "error in socket_interface connect to server: " + e);
            return 0;
        }
        return 1;
    }

    private void sendFileName(OutputStream out, String fileName){
        byte len[] = new byte[4];
        
        len[0] = (byte)((fileName.length()>>24)&0xff);
        len[1] = (byte)((fileName.length()>>16)&0xff);
        len[2] = (byte)((fileName.length()>>8)&0xff);
        len[3] = (byte)(fileName.length()&0xff);
        
        Log.d(TAG, "send file name len:" + len.length + " name:" + fileName);
        
        try {         
            out.write(len, 0, len.length);

            out.write(fileName.getBytes(), 0, fileName.length());
            //write to stream
            out.flush();
        } catch (IOException e) {
            Log.d(TAG, "send file name " + e.toString());
        }
    }
    
    private void sendFileSize(OutputStream out, long size){
        byte len[] = new byte[8];
        
        len[0] = (byte)((size>>56)&0xff);
        len[1] = (byte)((size>>48)&0xff);
        len[2] = (byte)((size>>40)&0xff);
        len[3] = (byte)((size>>32)&0xff);
        
        len[4] = (byte)((size>>24)&0xff);
        len[5] = (byte)((size>>16)&0xff);
        len[6] = (byte)((size>>8)&0xff);
        len[7] = (byte)(size&0xff);
        
        Log.d(TAG, "send file size: " + size + " size len:" + len.length);
        try {
            out.write(len, 0, len.length);
            //write to stream
            out.flush();
        } catch (IOException e) {
            Log.d(TAG, "send file size " + e.toString());
        }
    }
    
    public boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
                Log.d(TAG, "send data len: " + len);
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            Log.d(TAG, "send data:" + e.toString());
            return false;
        }
        return true;
    }

    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {    
            switch (msg.what) {
            case SHOW_CONNECT:
                String str = "";
                if( 1 == msg.arg1){
                    str = "connect success";
                    mButtonTransfer.setEnabled(true);
                }
                else{
                    str = "connect fail";
                    mButtonTransfer.setEnabled(false);
                }
                
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
                break;
                
            case SHOW_DONE:
                String strDone = (msg.arg1 == 1)?"success":"fail";
                Toast.makeText(MainActivity.this, "transfer " + strDone, Toast.LENGTH_SHORT).show();
                break;
            } 
            super.handleMessage(msg); 
        }
    };
    
    class ConnectThread implements Runnable{
        public void run(){
            int ret = TcpConnect(mEditIP.getText().toString(), PORT);
            Message msg = Message.obtain();
            msg.what = SHOW_CONNECT;
            msg.arg1 = ret;
            mHandler.sendMessage(msg);
        }
    }
    
    class SendThread implements Runnable{
        public void run(){
            try {
                Log.d(TAG, "Client socket connected: " + mTcpSocket.isConnected());
                mSendingData = true;
                
                OutputStream stream = mTcpSocket.getOutputStream();
                
                String filePath = mTextViewFilePath.getText().toString();
                Log.d(TAG, "filePath:" + filePath);
                
                String filename = "";
                int index = filePath.lastIndexOf('/') + 1;
                if (index > 0) {
                    filename = filePath.substring(index);
                } else {
                    filename = filePath;
                }
                
                sendFileName(stream, filename);
                
                File f = new File(filePath);  
                long size = f.length();
                sendFileSize(stream, size);

                InputStream is = new FileInputStream(filePath);
                boolean ret = copyFile(is, stream);
                Log.d(TAG, "Client: Data written done");
                
                
                Message msg = Message.obtain();
                msg.what = SHOW_DONE;
                msg.arg1 = ret?1:0;
                mHandler.sendMessage(msg);
                
                //Toast.makeText(MainActivity.this, "file transfer done!", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            } finally {
                if (mTcpSocket != null) {
                    if (mTcpSocket.isConnected()) {
                        try {
                            mTcpSocket.close();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }
            
            mSendingData = false;
        }
    }

}
