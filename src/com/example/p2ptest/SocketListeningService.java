package com.example.p2ptest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public class SocketListeningService extends Service {
	public static final String BROADCAST_ACTION = "com.example.p2ptest.messagereceived";
	public static final int PORT = 8888;
	Intent intent;
	ServerSocket serverSocket;
	Socket socket;
	StringBuilder msgReceived;
	SomeThread thread;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		thread = new SomeThread(this);
		thread.start();
	}

	@Override
	public void onDestroy() {
		if (thread != null) {
			thread.interrupt();
			thread = null;
		}

		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public class SomeThread extends Thread {
		Context context;

		public SomeThread(Context ctx) {
			context = ctx;
		}

		public void run() {
			try {
				serverSocket = new ServerSocket(PORT);
				serverSocket.setReuseAddress(true);
				while (true) {
					socket = serverSocket.accept();
					
					BufferedReader in = new BufferedReader(
							new InputStreamReader(socket.getInputStream()));
					String line = null;
					msgReceived = new StringBuilder();
					while ((line = in.readLine()) != null) {
						msgReceived.append(line);
					}
					if (msgReceived.toString().length() > 0) {
						intent = new Intent(BROADCAST_ACTION);
						intent.putExtra("msgReceived", msgReceived.toString());
						sendBroadcast(intent);
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (serverSocket != null) {
					try {
						serverSocket.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (socket != null) {
					try {
						socket.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}

		}
	}

}