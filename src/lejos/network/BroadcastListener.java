package lejos.network;

import java.net.DatagramPacket;

/**
 * Broadcast listener interface
 * @author Alexandre Lombard
 */
public interface BroadcastListener {
	/**
	 * Triggered on broadcast received
	 * @param message the raw message
	 */
	public void onBroadcastReceived(final DatagramPacket message);
}
