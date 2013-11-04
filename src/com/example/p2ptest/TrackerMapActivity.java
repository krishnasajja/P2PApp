package com.example.p2ptest;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class TrackerMapActivity extends FragmentActivity {

	String[] receivedIPAddresses;
	GoogleMap MAP;
	List<String> peerIPAddresses;
	String IP_Address;
	String randomGrpNmbr;
	LocationListener locationListener;
	LocationManager locationManager;
	HashMap<String, PolylineOptions> peerLocationsMap;
	PolylineOptions lineOptions;
	LatLng ll;
	Marker marker;
	String title;
	private UpdateReceiver receiver;
	String msgReceived = null;

	@Override
	public void onBackPressed() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Are you sure you want to exit?")
				.setCancelable(false)
				.setPositiveButton("Yes",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								locationManager.removeUpdates(locationListener);
								//TODO update peers
								TrackerMapActivity.this.finish();
							}
						})
				.setNegativeButton("No", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		AlertDialog alert = builder.create();
		alert.show();

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		locationManager.removeUpdates(locationListener);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (receiver == null)
			receiver = new UpdateReceiver();
		IntentFilter intentFilter = new IntentFilter(
				SocketListeningService.BROADCAST_ACTION);
		registerReceiver(receiver, intentFilter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (receiver != null)
			unregisterReceiver(receiver);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.trackermap);
		Bundle extras = getIntent().getExtras();
		peerIPAddresses = new ArrayList<String>();
		if (extras != null) {
			String value = extras.getString("IPList");
			if(value == null){
				
				peerIPAddresses = Arrays.asList(extras.getString("IPListPeer").split(","));
				receivedIPAddresses = null;
			}else{
			receivedIPAddresses = value.split(",");
			}
			IP_Address = extras.getString("IP_Address");
		}
		if (peerLocationsMap == null) {
			peerLocationsMap = new HashMap<String, PolylineOptions>();
		}
		randomGrpNmbr = String.valueOf(new Random(System.currentTimeMillis())
		.nextInt());
		// sendRequests(receivedIPAddresses);
		new MapSendRequestsTask()
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		
		
		ProgressDialog mDialog = new ProgressDialog(this);
		mDialog.setMessage("Getting peer locations...");
		mDialog.setCancelable(false);
		mDialog.show();

		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		MAP = mapFragment.getMap();
		lineOptions = new PolylineOptions().width(15).color(Color.BLUE)
				.geodesic(true);
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		locationListener = new LocationListener() {
			public void onLocationChanged(Location loc) {
				// Toast.makeText(getApplicationContext(), "Updating location",
				// Toast.LENGTH_SHORT).show();
				// add new marker to Map and send new location to peers;TODO

				// 39.03769,-94.58515,4/28/2013 9:05:13 PM
				ll = new LatLng(loc.getLatitude(), loc.getLongitude());
				title = IP_Address;
				lineOptions.add(ll);
				marker = MAP.addMarker(new
				MarkerOptions().position(ll)
				 .title(title));

				/*
				 * marker.remove(); marker = MAP.addMarker(new
				 * MarkerOptions().position(ll).title(title)
				 * .icon(BitmapDescriptorFactory
				 * .fromResource(R.drawable.kidmap)));
				 */
				MAP.addPolyline(lineOptions);
				/*
				 * CameraPosition cameraP = new
				 * CameraPosition.Builder().target(ll) TODO
				 * .zoom(14).bearing(90) // Sets the orientation of the camera
				 * to // east .tilt(30) // Sets the tilt of the camera to 30
				 * degrees .build();
				 */
				// MAP.animateCamera(CameraUpdateFactory.newCameraPosition(cameraP));
				MAP.setMapType(GoogleMap.MAP_TYPE_NORMAL);
				// updatePeers("MAPUPDT "+IP_Address+" "+loc.getLatitude()+" "+
				// loc.getLongitude());
				new MapUpdatePeersTask().executeOnExecutor(
						AsyncTask.THREAD_POOL_EXECUTOR,
						"MAPUPDT " + IP_Address + " " + loc.getLatitude() + " "
								+ loc.getLongitude());
			}

			@Override
			public void onProviderDisabled(String arg0) {
			}

			@Override
			public void onProviderEnabled(String arg0) {
			}

			@Override
			public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			}
		};
		locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
		 1000*30*1, 0, locationListener);
		//locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000*60*2,
			//	0, locationListener);

		// getChildLocation();

		mDialog.dismiss();

	}

	public void setNormalView(View v) {
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		MAP = mapFragment.getMap();
		MAP.setMapType(GoogleMap.MAP_TYPE_NORMAL);
	}

	public void setHybridView(View v) {
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		MAP = mapFragment.getMap();
		MAP.setMapType(GoogleMap.MAP_TYPE_HYBRID);
	}

	private class UpdateReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.getAction().equals(
					SocketListeningService.BROADCAST_ACTION)) {
				Bundle extras = intent.getExtras();
				if (extras != null) {
					msgReceived = extras.getString("msgReceived");
				}
				if (msgReceived == null)
					return;

				if (msgReceived.contains("ACKREQ")) {
					// send group info, in group or smthg like that.
					StringBuffer result = new StringBuffer();
					int size = peerIPAddresses.size();
					for (int i = 0; i < size; i++) {
						if (i != 0)
							result.append(",");
						result.append(peerIPAddresses.get(i));
					}
					new MapSenderTask().executeOnExecutor(
							AsyncTask.THREAD_POOL_EXECUTOR, msgReceived
									.toString().split(" ")[1], "ACKACPTG "
									+ randomGrpNmbr + " " + result.toString()
									+ "," + IP_Address);
				} else if (msgReceived.contains("MAPUPDT")) {
					// line must be in different colour
					// own path - blue,TODO
					// "MAPUPDT "+IP_Address+" "+loc.getLatitude()+" "+
					// loc.getLongitude()
					// check for the hashmap and update map
					String ip = msgReceived.split(" ")[1];
					String lat = msgReceived.split(" ")[2];
					String lon = msgReceived.split(" ")[3];
					PolylineOptions lineOptions;
					LatLng ll;

					if (MAP == null) {
						SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
								.findFragmentById(R.id.map);
						MAP = mapFragment.getMap();
					}

					if (peerLocationsMap.containsKey(ip)) {
						lineOptions = peerLocationsMap.get(ip);
					} else {
						lineOptions = new PolylineOptions().width(15)
								.color(Color.BLACK).geodesic(true);
					}
					ll = new LatLng(Double.valueOf(lat), Double.valueOf(lon));
					// title = s[2];
					lineOptions.add(ll);
					MAP.addMarker(new MarkerOptions().position(ll).title(ip));
					MAP.addPolyline(lineOptions);
					CameraPosition cameraP = new CameraPosition.Builder()
							.target(ll).zoom(14).bearing(90) // Sets the
																// orientation
																// of the camera
																// to
																// east
							.tilt(30) // Sets the tilt of the camera to 30
										// degrees
							.build();
					MAP.animateCamera(CameraUpdateFactory
							.newCameraPosition(cameraP));
					peerLocationsMap.put(ip, lineOptions);

				} else if (msgReceived.contains("ACKACPT")) {

					/*
					 * runOnUiThread(new Runnable() { public void run() { //
					 * tv2.append("\n" + // msgReceived.split(" ")[1]);
					 * myStringArray.add(msgReceived.split( " ")[1]);
					 * adapter.notifyDataSetChanged(); //
					 * devicesList.setItemChecked(1, true); } });
					 */

				} else if (msgReceived.contains("ACCEPT_P2P")) {
					String senderip = msgReceived.split(" ")[1];
					peerIPAddresses.add(senderip);
					StringBuffer result = new StringBuffer();
					int size = peerIPAddresses.size();
					for (int i = 0; i < size; i++) {
						if (i != 0)
							result.append(",");
						result.append(peerIPAddresses.get(i));
					}
					Toast.makeText(TrackerMapActivity.this,
							senderip + " joined the group", Toast.LENGTH_SHORT)
							.show();
					// updatePeers("UPDATE "+
					// randomGrpNmbr+" "+result.toString()+","+IP_Address);
					new MapUpdatePeersTask().executeOnExecutor(
							AsyncTask.THREAD_POOL_EXECUTOR, "UPDATE "
									+ randomGrpNmbr + " " + result.toString()
									+ "," + IP_Address);
					// update the list and map also..
					// TODO update map
					// and send the updated list to all peers
					// send random number
				} else if (msgReceived.contains("UPDATE")) {
					// update grp num,
					randomGrpNmbr = msgReceived.split(" ")[1];
					Toast.makeText(TrackerMapActivity.this,
							"One more peer joined the group!",
							Toast.LENGTH_SHORT).show();
					peerIPAddresses = new ArrayList<String>();
					for (String ip : msgReceived.split(" ")[2].split(",")) {
						peerIPAddresses.add(ip);
					}
					// TODO update map
				}

			}
			// Do stuff - maybe update my view based on the changed DB contents
		}
	}

	public class MapSenderTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			try {
				InetAddress serverAddr = InetAddress.getByName(params[0]);
				Socket socket = new Socket(serverAddr, MainActivity.PORT);
				PrintWriter out = new PrintWriter(new OutputStreamWriter(
						socket.getOutputStream()));
				out.println(params[1]);
				out.flush();
				socket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

	}

	public class MapUpdatePeersTask extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			InetAddress serverAddr;
			Socket socket;
			PrintWriter out;
			for (String ip : peerIPAddresses) {
				try {
					if (ip.equals(IP_Address)) {
						continue;
					}
					serverAddr = InetAddress.getByName(ip);
					socket = new Socket(serverAddr, MainActivity.PORT);

					out = new PrintWriter(new OutputStreamWriter(
							socket.getOutputStream()));
					out.println(params[0]);
					out.flush();
					socket.close();
				} catch (Exception e) {
					e.printStackTrace();
					// request failed. delete from list. update peers.
					peerIPAddresses.remove(ip);
					StringBuffer result = new StringBuffer();
					int size = peerIPAddresses.size();
					for (int i = 0; i < size; i++) {
						if (i != 0)
							result.append(",");
						result.append(peerIPAddresses.get(i));
					}
					Toast.makeText(TrackerMapActivity.this,
							ip + " left the group", Toast.LENGTH_SHORT).show();
					// updatePeers("UPDATE "+
					// randomGrpNmbr+" "+result.toString()+","+IP_Address);
					new MapUpdatePeersTask().executeOnExecutor(
							AsyncTask.THREAD_POOL_EXECUTOR, "UPDATE "
									+ randomGrpNmbr + " " + result.toString()
									+ "," + IP_Address);
				}
			}
			return null;
		}

	}

	public class MapSendRequestsTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			InetAddress serverAddr;
			Socket socket;
			PrintWriter out;
			if(receivedIPAddresses == null) return null;

			for (String ip : receivedIPAddresses) {
				try {
					serverAddr = InetAddress.getByName(ip);
					socket = new Socket(serverAddr, MainActivity.PORT);

					out = new PrintWriter(new OutputStreamWriter(
							socket.getOutputStream()));
					out.println("REQUEST_P2P " + IP_Address);
					out.flush();
					socket.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return null;
		}

	}
}
