package lejos.network;

/**
 * Broadcast listener interface
 * @author Alexandre Lombard
 */
public interface BroadcastListener {
	/**
	 * Triggered on broadcast received
	 * @param message the raw message
	 */
	public void onBroadcastReceived(byte[] message);
}
