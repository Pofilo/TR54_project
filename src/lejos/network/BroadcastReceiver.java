package lejos.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class used to receive the broadcast
 * @author Alexandre Lombard
 *
 */
public class BroadcastReceiver implements AutoCloseable {
	
	private static BroadcastReceiver instance = null;
	
	/**
	 * Gets an instance of the broadcast receiver 
	 * @return the broadcast receiver
	 * @throws SocketException
	 */
	public static BroadcastReceiver getInstance() throws SocketException {
		if(instance == null) {
			instance = new BroadcastReceiver();
		}
		
		return instance;
	}
	
	private DatagramSocket socket;
	private List<BroadcastListener> listeners = new ArrayList<>();
	
	private BroadcastReceiverRunnable runnable;
	
	private BroadcastReceiver() throws SocketException {
		this.socket = new DatagramSocket(8888);
		this.runnable = new BroadcastReceiverRunnable(this);
		
		new Thread(this.runnable).start();
	}
	
	/**
	 * Close the broadcast receiver
	 */
	public void close() {
		this.runnable.stop();
		this.socket.close();
	}
	
	/**
	 * Add a listener to broadcast messages
	 * @param listener the listener to add
	 */
	public void addListener(BroadcastListener listener) {
		this.listeners.add(listener);
	}
	
	/**
	 * Remove a broadcast listener
	 * @param listener the listener to remove
	 */
	public void removeListener(BroadcastListener listener) {
		this.listeners.remove(listener);
	}
	
	/**
	 * Fire the broadcast received event
	 * @param message the raw message received
	 */
	protected void fireBroadcastReceived(byte[] message) {
		for(BroadcastListener listener : this.listeners) {
			listener.onBroadcastReceived(message);
		}
	}
	
	/**
	 * Gets the datagram socket
	 * @return the datagram socket
	 */
	protected DatagramSocket getSocket() {
		return this.socket;
	}
	
	private static class BroadcastReceiverRunnable implements Runnable {

		private boolean stop = false;
		private byte[] buffer = new byte[1024];
		
		private BroadcastReceiver broadcastReceiver;
		
		private BroadcastReceiverRunnable(BroadcastReceiver broadcastReceiver) {
			this.broadcastReceiver = broadcastReceiver;
		}
		
		@Override
		public void run() {
			while(!this.stop) {
				final DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				try {
					this.broadcastReceiver.getSocket().receive(packet);
					if(!BroadcastManager.getInstance().IsSameAddress(packet.getAddress().getHostAddress())){
						this.broadcastReceiver.fireBroadcastReceived(packet.getData());
					}					
				} catch (IOException e) {
					//
				}
			}
		}
		
		public void stop() {
			this.stop = true;
		}
	}
}
