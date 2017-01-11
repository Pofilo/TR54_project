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

/**
 * The Class ClientThread.
 */
public class ClientThread extends Thread {

	/** The listenner used to compute the messages. */
	private BroadcastListener m_listenner;
	
	/** The delay between each loop while the thread is running. */
	private static final int PERIOD = 50;
	
	/** The boolean defining whether the thread is active. */
	private boolean isRunning = true;
	
	/** The boolean defining if the robot robot is authorized to advance. */
	private boolean canAdvance;
	
	/** The mutex used to synchronize the canAdvance variable. */
	private Object mutex = new Object();
	
	/** The robot IA. */
	private RobotIA robotIA;
	
	/** The instance of ClientThread as the class is a singleton. */
	private static ClientThread INSTANCE = null;
	
	/**
	 * Gets the single instance of ClientThread.
	 *
	 * @return single instance of ClientThread
	 */
	public static ClientThread getInstance() {
		if(INSTANCE == null) {
			INSTANCE = new ClientThread();
		}
		return INSTANCE;
	}
	
	/**
	 * Instantiates a new client thread.
	 */
	private ClientThread() {}
	
	/**
	 * Inits the listener.
	 *
	 * @param robotIA the robot IA
	 */
	public void init(final RobotIA robotIA) {
		this.canAdvance = true;
		this.robotIA = robotIA;
		
		// we create a listener where we want to receive the list of robots authorized to advance
		this.m_listenner = new BroadcastListener() {
			
			@Override
			public void onBroadcastReceived(final DatagramPacket message) {
				byte[] messageData = message.getData();
				
				boolean inTheList = false;
				int indexInList = -1;
				
				// we check if the header corresponds to the message we want to receive
				if (messageData[0] == 2) {
					int nbRobots = messageData[1];
					// we extract the ip
					for (int i = 0; i < nbRobots; i++) {
						byte[] rawIp = new byte[4];
						rawIp[0] = messageData[2 + i * 12];
						rawIp[1] = messageData[2 + i * 12 + 1];
						rawIp[2] = messageData[2 + i * 12 + 2];
						rawIp[3] = messageData[2 + i * 12 + 3];
						
						String receivedIp;
						try {
							receivedIp = InetAddress.getByAddress(rawIp).getHostAddress();
							// we check is the ip is us
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
						// the robot is in the list, we check his position
						switch (indexInList) {
						case 0: 
						{
							// The robot is the first of the list						
							// LED in Green
							Button.LEDPattern(1);
							break;
						}
						case 1: 
						{
							// The robot is the second of the list
							// LED in Orange
							Button.LEDPattern(3);
							break;
						}
						default:
							// The robot is at least the third of the list
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
		
		// we add the listener
		try {
			BroadcastReceiver.getInstance().addListener(this.m_listenner);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		while(this.isRunning) {
			sendClientInformation();
			Delay.msDelay(PERIOD);
		}
		// we order the ClientThread to stop, so we remove the listener
		try {
			BroadcastReceiver.getInstance().removeListener(this.m_listenner);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Checks if the robot can advance.
	 *
	 * @return true, if the robot can advance
	 */
	public boolean isCanAdvance() {
		synchronized (this.mutex) {
			return this.canAdvance;
		}
	}
	
	/**
	 * Acquire the variable canAdvance.
	 *
	 * @param b corresponding if the robot can advance or not
	 */
	private void acquireCanAdvance(final boolean b) {
		synchronized (this.mutex) {
			this.canAdvance = b;
		}
	}
	
	/**
	 * Send client information (position and speed)
	 */
	public void sendClientInformation() {
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
	
	/**
	 * Send access request.
	 */
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
	
	/**
	 * Set isRunning corresponding to the client thread
	 *
	 * @param b the new running
	 */
	public void setRunning(final boolean b){
		this.isRunning = b;
	}
}
