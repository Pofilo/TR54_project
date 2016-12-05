package com.utbm.tr54.robot.thread;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
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

public class ServerThread extends Thread {

	private enum Direction {
		NONE, ONE, TWO
	};

	/**
	 * Max time since last addition to the access list
	 */
	private final int deltaT = 5000;

	private boolean m_stop = false;

	private Dictionary<String, RobotData> m_roboInfos;
	private List<Pair<String, Direction>> m_accessRequest;
	private List<String> m_accessList;

	private Object m_mutexAccessRequest;
	private Object m_mutexAccessList;

	private BroadcastListener m_listenner;

	public ServerThread() {
		m_roboInfos = new Hashtable<>();
		m_accessRequest = new ArrayList<>();
		m_mutexAccessRequest = new Object();

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

					synchronized (m_mutexAccessList) {
						if (m_accessList.contains(message.getAddress().getHostAddress())) {
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

	public void run() {
		// Recoie régulierement des message de position et de vitesse
		// recoie une requete de passage d'intersection
		// construit une séquence de passage ordonner
		// *** la séquence contient les position, vitesse et orientation des
		// robots
		// transmet la séquence au clients

		Direction currentDirection = Direction.NONE;
		m_accessList = new ArrayList<>();
		Stopwatch timeSinceLastAddition = new Stopwatch();

		while (!m_stop) {
			synchronized (m_mutexAccessRequest) {
				if (m_accessRequest.isEmpty()) {
					continue;
				}
			}

			boolean conditionVerification = false;

			if (m_accessList.isEmpty()) {
				conditionVerification = true;
			} else if (m_accessRequest.get(0).second != currentDirection && timeSinceLastAddition.elapsed() >= deltaT) {
				conditionVerification = true;
			}else{
				conditionVerification = m_accessRequest.get(0).second == currentDirection;
			}

			if (conditionVerification) {
				String robotId = m_accessRequest.get(0).first;
				currentDirection = m_accessRequest.get(0).second;
				synchronized (m_mutexAccessRequest) {
					m_accessRequest.remove(0);
				}
				m_accessList.add(robotId);
				timeSinceLastAddition.reset();
			}

			if (!m_accessList.isEmpty()) {
				// 1 byte for the message header, then for each robot :
				// 4 byte for the identifiant (robot ip)
				// 4 byte for the position
				// 4 byte the the speed
				byte[] message = new byte[1 + m_accessList.size() * (4 + 4 + 4)];
				message[0] = 2;

				for (int i = 0; i < m_accessList.size(); ++i) {
					String robotIdentifiant = m_accessList.get(i);
					byte[] ipAddress = IPStringToByte(robotIdentifiant);
					byte[] position = ByteBuffer.allocate(4).putFloat(m_roboInfos.get(robotIdentifiant).position)
							.array();
					byte[] speed = ByteBuffer.allocate(4).putFloat(m_roboInfos.get(robotIdentifiant).speed).array();

					message[1 + i * 12] = ipAddress[0];
					message[1 + i * 12 + 1] = ipAddress[1];
					message[1 + i * 12 + 2] = ipAddress[2];
					message[1 + i * 12 + 3] = ipAddress[3];

					message[1 + i * 12 + 4] = position[1];
					message[1 + i * 12 + 5] = position[2];
					message[1 + i * 12 + 6] = position[3];
					message[1 + i * 12 + 7] = position[4];

					message[1 + i * 12 + 8] = speed[1];
					message[1 + i * 12 + 9] = speed[2];
					message[1 + i * 12 + 10] = speed[3];
					message[1 + i * 12 + 11] = speed[4];
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

	public void close() {
		m_stop = true;
		
		try {
			BroadcastReceiver.getInstance().removeListener(m_listenner);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	private byte[] IPStringToByte(String ip) {
		byte[] res = new byte[4];
		String[] ipPart = ip.split(".");
		for (int i = 0; i < 4; ++i) {
			byte part = Byte.parseByte(ipPart[i]);
			res[i] = part;
		}
		return res;
	}
}
