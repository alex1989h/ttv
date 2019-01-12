package ttv;

import java.net.URI;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;

/**
 * Status von unserem Spieler.
 * Grün: Spieler bereit, alle Schiffe unversehrt
 * Blau: mindestens eines, aber weniger als 50 % der Schiffe wurden versenkt
 * Violett: mehr als 50%, aber nicht alle der Schiffe wurden versenkt
 * Rot: alle Schiffe wurden versenkt
 */
enum Spielstatus{GRUEN, BLAU, VIOLETT, ROT}

public class CoAPLED implements Runnable{
	
	private CoapClient coapClient;
	private Spielstatus status;
	
	/**
	 * Constructor.
	 * @param uri
	 */
	public CoAPLED(URI uri) {
		coapClient = new CoapClient(uri);
		status = Spielstatus.GRUEN;
	}
	
	/**
	 * Setze die LED.
	 * @param status
	 */
	public void setLED(Spielstatus status) {
		switch (status) {
		case GRUEN:
			coapClient.put("1", MediaTypeRegistry.TEXT_PLAIN);
			coapClient.put("g", MediaTypeRegistry.TEXT_PLAIN);
			break;
		case BLAU:
			coapClient.put("1", MediaTypeRegistry.TEXT_PLAIN);
			coapClient.put("b", MediaTypeRegistry.TEXT_PLAIN);
			break;
		case VIOLETT:
			coapClient.put("1", MediaTypeRegistry.TEXT_PLAIN);
			coapClient.put("b", MediaTypeRegistry.TEXT_PLAIN);
			coapClient.put("r", MediaTypeRegistry.TEXT_PLAIN);
//			coapClient.put("1", MediaTypeRegistry.TEXT_PLAIN);
//			coapClient.put("violet", MediaTypeRegistry.TEXT_PLAIN);//TODO:Rausfinden wie der Paylod für violette Farbe heißt
			break;
		case ROT:
			coapClient.put("1", MediaTypeRegistry.TEXT_PLAIN);
			coapClient.put("r", MediaTypeRegistry.TEXT_PLAIN);
			break;
		default:
			System.out.println("ERROR: Unbekannter Spielstatus");
			break;
		}
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			setLED(status);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				e.printStackTrace();
			}
		}
		
	}

	public void setLEDStatus(Spielstatus status) {
		this.status = status;
	}
	
	

}
