package net.ptidej.fitBitData;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity  extends AppCompatActivity {

    private NanoHTTPD nanoHTTPD;
    private SharedPreferences sharedpreferences;
    public static final String mypreference = "mypref";
    public static final String incomingMessage = "FitbitMessage";
    private WebSocketClient mWebSocketClient;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            // Start NanoHTTPD server
            nanoHTTPD = new SimpleNanoHTTPD();
            nanoHTTPD.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stop NanoHTTPD server
        if (nanoHTTPD != null) {
            nanoHTTPD.stop();
        }
    }

    private class SimpleNanoHTTPD extends NanoHTTPD {

        public SimpleNanoHTTPD() {
            super(8080);
        }

        @Override
        public Response serve(IHTTPSession session) {
            String uri = session.getUri();

            // Check if the request is received via the "/data" endpoint
            if ("/data".equals(uri)) {
                Method method = session.getMethod();
                if (Method.POST.equals(method)) {
                    try {
                        // Parse the body of the request as JSON
                        Map<String, String> files = new HashMap<>();
                        session.parseBody(files);
                        String postBody = files.get("postData");
                        System.out.println("Request Body: " + postBody);
                        //Send this to WIMP Webserver via web socket
                         connect2WIMPt(postBody);

                        // Create a JSON response
                        String jsonResponse = "{\"message\": \"Data received successfully\"}";
                        return newFixedLengthResponse(Response.Status.OK, "application/json", jsonResponse);
                    } catch (IOException | ResponseException e) {
                        e.printStackTrace();
                        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT, "Internal Server Error");
                    }
                } else {
                    return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, NanoHTTPD.MIME_PLAINTEXT, "Method Not Allowed");
                }
            } else {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not Found");
            }
        }
    }
    public void connect2WIMPt(String s){
        URI uri;
        String TAG="WimpApp";
        String websocketEndPointUrl;
        try {
            // websocketEndPointUrl="ws://192.168.2.39:8181"; // SocketServer's IP and port [home]
            websocketEndPointUrl="ws://192.168.191.114:8181"; // SocketServer's IP and port [Lab]
            uri = new URI(websocketEndPointUrl);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                Log.i(TAG, "WIMP connection is opened");
                mWebSocketClient.send("Client:" + Build.MANUFACTURER + " " + Build.MODEL);
                Log.i(TAG,"Client:  " + Build.MANUFACTURER + " " + Build.MODEL);
            }

            @Override
            public void onMessage(String message) {
                sharedpreferences = getSharedPreferences(mypreference, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putString(incomingMessage,message);
                editor.commit();

            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.i(TAG, "Closed, code= " + code+", reason="+reason+", remote="+remote);
            }

            @Override
            public void onError(Exception e) {

                Log.i(TAG, "Error " + e.getMessage());
            }
        };
        mWebSocketClient.connect();

    }
}