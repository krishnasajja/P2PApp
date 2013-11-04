package com.example.p2ptest;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	public String IP_Address = null;
	public static final int PORT = 8888;
	public TextView tv2;
	public ListView devicesList;
	ArrayAdapter<String> adapter;
	ArrayList<String> myStringArray = new ArrayList<String>();
	ScanTask st;
	private UpdateReceiver receiver;
	String msgReceived = null;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		TextView textView = (TextView) findViewById(R.id.textView1);

		devicesList = (ListView) findViewById(R.id.deviceslist);
		myStringArray.add("192.168.1.1");
		myStringArray.add("192.168.1.2");

		adapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_list_item_multiple_choice,
				myStringArray);
		devicesList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		devicesList.setItemsCanFocus(false);
		devicesList.setAdapter(adapter);

		WifiManager wim = (WifiManager) getSystemService(WIFI_SERVICE);
		if (wim != null) {
			IP_Address = Formatter.formatIpAddress(wim.getConnectionInfo()
					.getIpAddress());
			textView.append("\n" + IP_Address);
			
		}
		//TODO
		startService(new Intent(this, SocketListeningService.class));
		
		Button selection = (Button) findViewById(R.id.button1);
		selection.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				String selected = "";
				int cntChoice = devicesList.getCount();
				if(st!=null){
					System.out.println("Cancelling Scan task");
				st.cancel(true);
				}
				
				SparseBooleanArray sparseBooleanArray = devicesList
						.getCheckedItemPositions();
				for (int i = 0; i < cntChoice; i++) {
					if (sparseBooleanArray.get(i)) {
						selected += devicesList.getItemAtPosition(i).toString()
								+ ",";
					}
				}
				selected = selected.substring(0, selected.length() - 1);
				Toast.makeText(MainActivity.this, selected, Toast.LENGTH_SHORT)
						.show();

				Intent intent = new Intent(MainActivity.this,
						TrackerMapActivity.class);
				intent.putExtra("IPList", selected);
				intent.putExtra("IP_Address", IP_Address);
				//finish();
				MainActivity.this.startActivity(intent);
			}
		});

	}
	
	@Override
	protected void onResume() {
	    super.onResume();
	    if (receiver == null) receiver = new UpdateReceiver();
	    IntentFilter intentFilter = new IntentFilter(SocketListeningService.BROADCAST_ACTION);
	    registerReceiver(receiver, intentFilter);
	    // register service TODO
	}
	
	@Override
	protected void onPause() {
	    super.onPause();
	    if (receiver != null) unregisterReceiver(receiver);
	   
	    // deregister service TODO
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(isMyServiceRunning()){
			stopService(new Intent(MainActivity.this,SocketListeningService.class));
		}
		
	}
	private boolean isMyServiceRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (SocketListeningService.class.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void startScan(View v) {

		String[] ipArray = IP_Address.split("\\.");
		String ipScan = ipArray[0] + "." + ipArray[1] + "." + ipArray[2] + ".";
		st = new ScanTask();
		st.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ipScan);
	}
	
	private class UpdateReceiver extends BroadcastReceiver {
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	
	        if (intent.getAction().equals(SocketListeningService.BROADCAST_ACTION)) {
	        	Bundle extras = intent.getExtras();
	    		if (extras != null) {
	    			msgReceived = extras.getString("msgReceived");
	    		}
	        	if(msgReceived == null) return;
	        	
	        	if (msgReceived.contains("ACKREQ")) {
					//if(msgReceived.split(" ")[1].equals(IP_Address))
					//	return;
					new SenderTask().executeOnExecutor(
							AsyncTask.THREAD_POOL_EXECUTOR, msgReceived
									.toString().split(" ")[1], "ACKACPT "
									+ IP_Address);
				} else if (msgReceived.contains("ACKACPT")) {
					runOnUiThread(new Runnable() {
						public void run() {
							myStringArray.add(msgReceived.split(
									" ")[1]);
							adapter.notifyDataSetChanged();
						}
					});

				} else if (msgReceived.contains("ACKACPTG")) {
					runOnUiThread(new Runnable() {
						public void run() {
							Boolean alreadyExists = false;
							//TODO check if grp exists
							/*Boolean alreadyExists = false;
							String grpId = msgReceived.split(
									" ")[1];
							for(String s: myStringArray){
								if(s.contains(grpId)){
									alreadyExists = true;
								}
							}*/
							if(!alreadyExists){
								String s = "Group:" + msgReceived.split(" ")[2];
								System.out.println("s: "+s);
								System.out.println("1: "+ msgReceived.split(" ")[1]);
								System.out.println("2: "+ msgReceived.split(" ")[2]);
							myStringArray.add(s);//TODO changed from 2 to 1
							adapter.notifyDataSetChanged();
							}
						}
					});

				}else if (msgReceived.contains("REQUEST_P2P")) {
					// Pop up asking user to choose
					MainActivity.this.runOnUiThread(new Runnable() {
						  public void run() {
							  if(!isFinishing()){
							  new AlertDialog.Builder(MainActivity.this)
								.setTitle("Request for group connection")
								.setMessage("Do you want to join the group?")
								.setPositiveButton("Yes",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												if(st!=null)
												st.cancel(true);
												// send accept mdg
												new SenderTask()
														.executeOnExecutor(
																AsyncTask.THREAD_POOL_EXECUTOR,
																msgReceived
																		.toString()
																		.split(" ")[1],
																"ACCEPT_P2P "
																		+ IP_Address);
												// go to map activity
												Intent intent = new Intent(
														MainActivity.this,
														TrackerMapActivity.class);
												intent.putExtra("IPListPeer",
														msgReceived
																.split(" ")[1]);
												intent.putExtra("IP_Address", IP_Address);
												
												//MainActivity.this.finish();
												dialog.dismiss();
												MainActivity.this
														.startActivity(intent);
																									
											}
										})
								.setNegativeButton("No",
										new DialogInterface.OnClickListener() {
											public void onClick(
													DialogInterface dialog,
													int which) {
												// do nothing
												dialog.dismiss();
											}
										}).show();
							  }
						  }
						});
					
					
				}
			}
	        //Do stuff - maybe update my view based on the changed DB contents
	    }
	}
	

	public class ScanTask extends AsyncTask<String, Void, Void> {
		Socket socket;
		@Override
		protected Void doInBackground(String... params) {
			String partIP = params[0];
			
			PrintWriter out;
			String ip = null;
			for (int i = 140; i < 150 && !isCancelled(); i++) {
				try {
					ip = partIP + String.valueOf(i);
					//InetAddress serverAddr = InetAddress.getByName(ip);
					socket = new Socket();
					socket.setSoTimeout(2000);
					socket.connect(new InetSocketAddress(ip, PORT), 200);
					//socket = new Socket(serverAddr, PORT);

					out = new PrintWriter(new OutputStreamWriter(
							socket.getOutputStream()));
					out.println("ACKREQ " + IP_Address);
					out.flush();
					socket.close();
				} catch (Exception e) {
					System.out.println("IP address " + ip + " not found.");
				}finally{
					if(socket!= null){
						try {
							socket.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			return null;
		}
		@Override
		protected void onCancelled(){
			if(socket!= null){
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	public class SenderTask extends AsyncTask<String, Void, Void> {
		Socket socket;
		@Override
		protected Void doInBackground(String... params) {
			try {
				InetAddress serverAddr = InetAddress.getByName(params[0]);
				socket = new Socket(serverAddr, PORT);

				PrintWriter out = new PrintWriter(new OutputStreamWriter(
						socket.getOutputStream()));
				out.println(params[1]);
				out.flush();
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}finally{
				if(socket!=null){
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			return null;
		}

	}

}
