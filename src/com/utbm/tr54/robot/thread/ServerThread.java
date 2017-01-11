package com.utbm.tr54.robot.thread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import com.utbm.tr54.robot.RobotData;
import com.utbm.tr54.utils.Pair;

import lejos.network.BroadcastListener;
import lejos.network.BroadcastManager;
import lejos.network.BroadcastReceiver;
import lejos.utility.Delay;
import lejos.utility.Stopwatch;

/**
 * The Class ServerThread represent the server that organize the intersection
 * passage order.
 */
public class ServerThread extends Thread {

	/**
	 * The Enum Direction represent a direction taken by a robot in the
	 * intersection.
	 */
	public enum Direction {
		/** The direction one. */
		ONE,
		/** The direction two. */
		TWO
	};

	/** The delay between each loop while the thread is running. */
	private static final int PERIOD = 30;
	
	/** Max time since last addition to the access list. */
	private final int deltaT = 700000;

	/** boolean controlling the execution of the loop. */
	private boolean m_stop = false;

	/** The dictionnary containing info about all the know robots. */
	private Dictionary<String, RobotData> m_roboInfos;

	/** The list of all access request. */
	private List<Pair<String, Direction>> m_accessRequest;

	/** The list of all robot who have access. */
	private List<String> m_accessList;

	/** The mutex controlling the access request list. */
	private Object m_mutexAccessRequest;

	/** The mutex controlling the access list. */
	private Object m_mutexAccessList;

	/** The listenner used to compute the messages. */
	private BroadcastListener m_listenner;

	/** The current direction authorized by the server */
	private Direction currentDirection;

	/** The time since an access request was accepted */
	private Stopwatch timeSinceLastAddition;

	/**
	 * Instantiates a new server thread.
	 */
	public ServerThread() {
		this.m_roboInfos = new Hashtable<>();
		this.m_accessRequest = new ArrayList<>();
		this.m_accessList = new ArrayList<>();
		this.m_mutexAccessRequest = new Object();
		this.m_mutexAccessList = new Object();

		this.m_listenner = new BroadcastListener() {
			@Override
			public void onBroadcastReceived(final DatagramPacket message) {
				byte[] messageData = message.getData();

				switch (messageData[0]) {
				case 0: // Client data
				{
					RobotData data = m_roboInfos.get(message.getAddress().getHostAddress());
					if (data == null) {
						data = new RobotData();
					}

					byte[] rawPosition = Arrays.copyOfRange(messageData, 1, 5);
					byte[] rawSpeed = Arrays.copyOfRange(messageData, 5, 9);

					data.position = ByteBuffer.wrap(rawPosition).getFloat();
					data.speed = ByteBuffer.wrap(rawSpeed).getFloat();

					m_roboInfos.put(message.getAddress().getHostAddress(), data);

					// When a robot inform us of his position will he was on
					// the access list, but have passed the intersection we need
					// to erase it from the access list when he cross the intersection
					synchronized (m_mutexAccessList) {
						if (m_accessList.contains(message.getAddress().getHostAddress())) {
							if (((data.position > 15) && (data.position < 50)) || ((data.position > 65))) {
								m_accessList.remove(message.getAddress().getHostAddress());
								proccessAccessList();
							}
						}
					}

					break;
				}
				case 1: // Passage request
				{
					RobotData data = m_roboInfos.get(message.getAddress().getHostAddress());
					if (data == null) {
						// We don't know this robot or his position, so we
						// can't let him pass
						return;
					}

					Direction dir = null;

					if ((data.position > -1) && (data.position < 50)) {
						dir = Direction.ONE;
					} else if ((data.position >= 50)) {
						dir = Direction.TWO;
					}

					if (dir == null) {
						return;
					}
					
					synchronized (m_mutexAccessList) {
						if(m_accessList.contains(message.getAddress().getHostAddress())){
							return;
						}
					}

					synchronized (m_mutexAccessRequest) {
						m_accessRequest.add(
								new Pair<String, ServerThread.Direction>(message.getAddress().getHostAddress(), dir));
					}
					break;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		this.currentDirection = Direction.ONE;
		this.timeSinceLastAddition = new Stopwatch();

		while (!m_stop) {
			processAccessRequest();
			proccessAccessList();

			Delay.msDelay(PERIOD);
		}
	}

	/**
	 * If there are access request waiting, we process them here, otherwise we
	 * return immediately
	 */
	private void processAccessRequest() {
		// If there was no access request, no need to continue
		synchronized (this.m_mutexAccessRequest) {
			if (this.m_accessRequest.isEmpty()) {
				return;
			}
		}

		List<Pair<String, Direction>> accessRequestCopy = new ArrayList<>();
		List<Pair<String, Direction>> acceptedAccessRequest = new ArrayList<>();

		// note : addAll is a shallow copy
		accessRequestCopy.addAll(this.m_accessRequest);

		// The access sequence is empty
		synchronized (this.m_mutexAccessList) {
			for (Pair<String, Direction> request : accessRequestCopy) {
				boolean conditionVerification = false;
				if (this.m_accessList.isEmpty()) {
					conditionVerification = true;
					this.currentDirection = request.second;
					System.out.println(request.first + " : A");

				} else if (request.second != this.currentDirection && this.timeSinceLastAddition.elapsed() >= deltaT) {
					// The last robot from the access list come from an other
					// direction and the time since an addition to the access
					// list is superior to deltaTime
					conditionVerification = true;
					System.out.println(request.first + " : T");

				} else {
					// The direction of the last robot in the access sequence is
					// the same has the direction of the current access request
					conditionVerification = request.second.equals(currentDirection);
					
					System.out.println(request.first + " : S " + this.currentDirection + " = " + request.second);
				}
				
				if (conditionVerification) {
					acceptedAccessRequest.add(request);
					this.timeSinceLastAddition.reset();
				}
			}
		}

		synchronized (this.m_mutexAccessList) {
			for (Pair<String, Direction> request : acceptedAccessRequest) {
				String robotId = request.first;
				if (!this.m_accessList.contains(robotId)) {
					this.m_accessList.add(robotId);
				}

			}
		}

		synchronized (this.m_mutexAccessRequest) {
			for (Pair<String, Direction> request : acceptedAccessRequest) {
				this.m_accessRequest.remove(request);

			}
		}
	}

	/**
	 * If there are robot in the access list, we broadcast them the entire
	 * access list
	 */
	private void proccessAccessList() {
		synchronized (this.m_mutexAccessList) {
			// 1 byte for the message header
			// 1 byte for the number of robot, then for each robot :
			// 4 byte for the identifier (ip address of the robot)
			// 4 byte for the position
			// 4 byte the the speed
			byte[] message = new byte[1 + 1 + this.m_accessList.size() * (4 + 4 + 4)];
			message[0] = 2; // message header
			message[1] = (byte) this.m_accessList.size(); // number of robot in the access list
			for (int i = 0; i < this.m_accessList.size(); ++i) {
				String robotIdentifiant = this.m_accessList.get(i);
				// default address, should never be used
				byte[] ipAddress = new byte[] { 0, 0, 0, 0 };
				try {
					ipAddress = InetAddress.getByName(robotIdentifiant).getAddress();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				byte[] position = ByteBuffer.allocate(4).putFloat(this.m_roboInfos.get(robotIdentifiant).position).array();
				byte[] speed = ByteBuffer.allocate(4).putFloat(this.m_roboInfos.get(robotIdentifiant).speed).array();

				message[2 + i * 12] = ipAddress[0];
				message[2 + i * 12 + 1] = ipAddress[1];
				message[2 + i * 12 + 2] = ipAddress[2];
				message[2 + i * 12 + 3] = ipAddress[3];

				message[2 + i * 12 + 4] = position[0];
				message[2 + i * 12 + 5] = position[1];
				message[2 + i * 12 + 6] = position[2];
				message[2 + i * 12 + 7] = position[3];

				message[2 + i * 12 + 8] = speed[0];
				message[2 + i * 12 + 9] = speed[1];
				message[2 + i * 12 + 10] = speed[2];
				message[2 + i * 12 + 11] = speed[3];
			}

			try {
				BroadcastManager.getInstance().broadcast(message);
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Close the thread.
	 */
	public void close() {
		this.m_stop = true;

		try {
			BroadcastReceiver.getInstance().removeListener(this.m_listenner);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
}
