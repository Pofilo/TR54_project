package com.utbm.tr54.robot.thread;

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
import lejos.network.BroadcastReceiver;
import lejos.utility.Delay;

public class ServerThread extends Thread {

	private enum Direction {
		ONE, TWO
	};

	private boolean m_stop = false;

	private Dictionary<String, RobotData> m_roboInfos;
	private List<Pair<String, Direction>> m_accessList;

	public ServerThread() {
		m_roboInfos = new Hashtable<>();
		m_accessList = new ArrayList<>();

		try {
			BroadcastReceiver.getInstance().addListener(new BroadcastListener() {

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

						m_accessList.add(
								new Pair<String, ServerThread.Direction>(message.getAddress().getHostAddress(), dir));
						break;
					}
					}
				}
			});
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		while (!m_stop) {
			// Recoie régulierement des message de position et de vitesse
			// recoie une requete de passage d'intersection
			// construit une séquence de passage ordonner
			// *** la séquence contient les position, vitesse et orientation des
			// robots
			// transmet la séquence au clients
			Delay.msDelay(10);
		}
	}

	public void close() {
		m_stop = true;
	}
}
