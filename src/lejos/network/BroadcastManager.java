package lejos.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Singleton class used to send broadcast messages
 * 
 * @author Alexandre Lombard
 */
public class BroadcastManager implements AutoCloseable {

	private static BroadcastManager instance = null;

	/**
	 * Gets an instance of the broadcast manager
	 * 
	 * @return the broadcast manager
	 * @throws SocketException
	 */
	public static BroadcastManager getInstance() throws SocketException {
		if (instance == null) {
			instance = new BroadcastManager();
		}

		return instance;
	}

	private DatagramSocket socket;
	private List<String> m_interfacesAddress = null;

	@SuppressWarnings("rawtypes")
	private BroadcastManager() throws SocketException {
		this.socket = new DatagramSocket();

		// Retreive every interface address
		m_interfacesAddress = new ArrayList<>();

		Enumeration e;
		try {
			e = NetworkInterface.getNetworkInterfaces();
			while (e.hasMoreElements()) {
				NetworkInterface n = (NetworkInterface) e.nextElement();
				Enumeration ee = n.getInetAddresses();
				while (ee.hasMoreElements()) {
					InetAddress i = (InetAddress) ee.nextElement();
					m_interfacesAddress.add(i.getHostAddress());
				}
			}
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
	}

	/**
	 * Close the broadcast manager
	 */
	public void close() {
		this.socket.close();
	}

	/**
	 * Broadcast a raw message
	 * 
	 * @param message
	 *            the message
	 * @throws IOException
	 *             thrown if unable to send the packet
	 */
	public void broadcast(byte[] message) throws IOException {
		try {
			final DatagramPacket datagramPacket = new DatagramPacket(message, message.length,
					InetAddress.getByName("255.255.255.255"), 8888);

			this.socket.send(datagramPacket);
		} catch (UnknownHostException e) {
			//
		}
	}
	
	/**
	 * Verify if a given address is equals to one of our own ip address
	 * @param address a string representing an ip address
	 * @return true if the given address is the same as one of our own address
	 */
	public boolean isSameAddress(String address) {
		boolean result = false;
		for (String knowAddress : m_interfacesAddress) {
			result = knowAddress.compareTo(address) == 0;
			if(result){
				break;
			}
		}
		return result;
	}
}
