package com.utbm.tr54.robot.thread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import com.utbm.tr54.robot.RobotIA;

import lejos.hardware.Button;
import lejos.network.BroadcastListener;
import lejos.network.BroadcastManager;
import lejos.network.BroadcastReceiver;
import lejos.utility.Delay;

public class ClientThread extends Thread {

	private BroadcastListener m_listenner;
	private static final int PERIOD = 50;
	private boolean isRunning = true;
	private boolean canAdvance;
	private Object mutex = new Object();
	private RobotIA robotIA;
	
	private static ClientThread INSTANCE = null;
	
	public static ClientThread getInstance() {
		if(INSTANCE == null) {
			INSTANCE = new ClientThread();
		}
		return INSTANCE;
	}
	
	private ClientThread() {}
	
	public void init(RobotIA robotIA) {
		this.canAdvance = true;
		this.robotIA = robotIA;
		
		this.m_listenner = new BroadcastListener() {
			
			@Override
			public void onBroadcastReceived(DatagramPacket message) {
				byte[] messageData = message.getData();
				
				boolean inTheList = false;
				int indexInList = -1;
				
				if (messageData[0] == 2) {
					int nbRobots = messageData[1];
					for (int i = 0; i < nbRobots; i++) {
						byte[] rawIp = new byte[4];
						rawIp[0] = messageData[2 + i * 12];
						rawIp[1] = messageData[2 + i * 12 + 1];
						rawIp[2] = messageData[2 + i * 12 + 2];
						rawIp[3] = messageData[2 + i * 12 + 3];
						
						String receivedIp;
						try {
							receivedIp = InetAddress.getByAddress(rawIp).getHostAddress();
							if (BroadcastManager.getInstance().isSameAddress(receivedIp)) {
								inTheList = true;
								indexInList = i;
							}
						} catch (UnknownHostException | SocketException e) {
							e.printStackTrace();
						} 		
					}
					acquireCanAdvance(inTheList);
					if (inTheList) {
						// le robot est dans la liste
						switch (indexInList) {
						case 0: 
						{
							// The robot is the first of the list, he can advance							
							// LED in Green
							Button.LEDPattern(1);
							break;
						}
						case 1: 
						{
							// The robot is the second of the list, he musts wait
							// LED in Orange
							Button.LEDPattern(3);
							break;
						}
						default:
							// The robot is at least the third of the list, he musts wait too
							// LED in Red
							Button.LEDPattern(2);
							break;
						}
					} else {
						// the robot is not in the list, we switch off the light
						Button.LEDPattern(0);
					}
				}
			}
		};
		
		try {
			BroadcastReceiver.getInstance().addListener(m_listenner);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public void run() {
		while(isRunning) {
			sendClientInformation();
			Delay.msDelay(PERIOD);
		}
		try {
			BroadcastReceiver.getInstance().removeListener(m_listenner);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	public boolean isCanAdvance() {
		synchronized (this.mutex) {
			return this.canAdvance;
		}
	}
	
	private void acquireCanAdvance(boolean b) {
		synchronized (this.mutex) {
			this.canAdvance = b;
		}
	}
	
	private void sendClientInformation() {
		byte[] message = new byte[1 + 2 * 4];
		message[0] = 0; // message header
		
		byte[] position = ByteBuffer.allocate(4).putFloat(this.robotIA.getPosition()).array();
		byte[] speed = ByteBuffer.allocate(4).putFloat(this.robotIA.getSpeed()).array();
		
		message[1 + 0] = position[0];
		message[1 + 1] = position[1];
		message[1 + 2] = position[2];
		message[1 + 3] = position[3];

		message[1 + 4] = speed[0];
		message[1 + 5] = speed[1];
		message[1 + 6] = speed[2];
		message[1 + 7] = speed[3];
		
		try {
			BroadcastManager.getInstance().broadcast(message);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void sendAccessRequest() {
		byte[] message = new byte[1];
		message[0] = 1; // message header
	
		try {
			BroadcastManager.getInstance().broadcast(message);
		} catch (SocketException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setRunning(boolean b){
		this.isRunning = b;
	}
	

}
