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
 * The Class ServerThread represent the server that organise the intersection
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

	/** Max time since last addition to the access list. */
	private final int deltaT = 5000;

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

	/**
	 * Instantiates a new server thread.
	 */
	public ServerThread() {
		m_roboInfos = new Hashtable<>();
		m_accessRequest = new ArrayList<>();
		m_accessList = new ArrayList<>();
		m_mutexAccessRequest = new Object();
		m_mutexAccessList = new Object();

		m_listenner = new BroadcastListener() {
			@Override
			public void onBroadcastReceived(DatagramPacket message) {
				byte[] messageData = message.getData();

				switch (messageData[0]) {
				case 0: // Client data
				{
					RobotData data = m_roboInfos.get(message.getAddress().getHostAddress());
					if (data == null) {
						data = new RobotData();
					}

					byte[] rawPosition = Arrays.copyOfRange(messageData, 1, 4);
					byte[] rawSpeed = Arrays.copyOfRange(messageData, 5, 8);

					data.position = ByteBuffer.wrap(rawPosition).getFloat();
					data.speed = ByteBuffer.wrap(rawSpeed).getFloat();

					m_roboInfos.put(message.getAddress().getHostAddress(), data);

					// When a robot informe us of his position will he was on the
					// access list,
					// we need to erase it from the access list when he passed
					// the intersection
					if (m_accessList.contains(message.getAddress().getHostAddress())) {
						synchronized (m_mutexAccessList) {
							if (((data.position > 25) && (data.position < 50))
									|| ((data.position > 75) && (data.position < 100))) {
								m_accessList.remove(message.getAddress().getHostAddress());
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

					Direction dir = (data.position > 50) ? Direction.ONE : Direction.TWO;

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
		Direction currentDirection = Direction.ONE;
		Stopwatch timeSinceLastAddition = new Stopwatch();

		while (!m_stop) {
			// If there was no access request, no need to continue
			synchronized (m_mutexAccessRequest) {
				if (m_accessRequest.isEmpty()) {
					continue;
				}
			}

			boolean conditionVerification = false;

			// The access sequence is empty
			synchronized (m_mutexAccessList) {
				if (m_accessList.isEmpty()) {
					conditionVerification = true;

				} else if (m_accessRequest.get(0).second != currentDirection
						&& timeSinceLastAddition.elapsed() >= deltaT) {
					// The last robot from the access list come from an other
					// direction and the time since an addition to the access
					// list
					// is superior to deltaTime
					conditionVerification = true;

				} else {// The direction of the last robot in the access
						// sequence is
						// the same has the direction of the current access
						// request
					conditionVerification = m_accessRequest.get(0).second == currentDirection;
				}
			}

			if (conditionVerification) {
				String robotId = m_accessRequest.get(0).first;
				currentDirection = m_accessRequest.get(0).second;

				synchronized (m_mutexAccessRequest) {
					m_accessRequest.remove(0);
				}

				synchronized (m_mutexAccessList) {
					m_accessList.add(robotId);
				}

				timeSinceLastAddition.reset();
			}

			if (!m_accessList.isEmpty()) {
				// 1 byte for the message header
				// 1 byte for the number of robot, then for each robot :
				// 4 byte for the identifiant (robot ip)
				// 4 byte for the position
				// 4 byte the the speed
				byte[] message = new byte[1 + 1 + m_accessList.size() * (4 + 4 + 4)];
				message[0] = 2; // message header
				message[1] = (byte) m_accessList.size(); // number of robot in
															// the access list

				for (int i = 0; i < m_accessList.size(); ++i) {
					String robotIdentifiant = m_accessList.get(i);
					// default address, should never be used
					byte[] ipAddress = new byte[] { 0, 0, 0, 0 };
					try {
						ipAddress = InetAddress.getByName(robotIdentifiant).getAddress();
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
					byte[] position = ByteBuffer.allocate(4).putFloat(m_roboInfos.get(robotIdentifiant).position)
							.array();
					byte[] speed = ByteBuffer.allocate(4).putFloat(m_roboInfos.get(robotIdentifiant).speed).array();

					message[2 + i * 12] = ipAddress[0];
					message[2 + i * 12 + 1] = ipAddress[1];
					message[2 + i * 12 + 2] = ipAddress[2];
					message[2 + i * 12 + 3] = ipAddress[3];

					message[2 + i * 12 + 4] = position[1];
					message[2 + i * 12 + 5] = position[2];
					message[2 + i * 12 + 6] = position[3];
					message[2 + i * 12 + 7] = position[4];

					message[2 + i * 12 + 8] = speed[1];
					message[2 + i * 12 + 9] = speed[2];
					message[2 + i * 12 + 10] = speed[3];
					message[2 + i * 12 + 11] = speed[4];
				}

				try {
					BroadcastManager.getInstance().broadcast(message);
				} catch (SocketException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			Delay.msDelay(10);
		}
	}

	/**
	 * Close the thread.
	 */
	public void close() {
		m_stop = true;

		try {
			BroadcastReceiver.getInstance().removeListener(m_listenner);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
}
