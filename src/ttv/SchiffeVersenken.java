package ttv;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Scanner;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;

public class SchiffeVersenken {
	
	public static int ANZAHLSCHIFFE = 10;
	
	public static int ANZAHLINTERVALLE = 100;
	
	public static String ADDRESS = "://localhost:8080/";
	
	public static String BOOTSTRPADDRESS = "://localhost:8080/";
	
	public static String COAPADDRESS = "coap://localhost:5683/led";

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		System.out.print("Gebe port ein: ");
		String eingabe = sc.next();
		ADDRESS = "://localhost:"+eingabe+"/";
	    System.out.print("Soll Netwerk erstellt werden (yes/no): ");
	    eingabe = sc.next();
	    boolean erstelle= true;
	    if(eingabe.equals("yes")) {
	    	erstelle = true;
	    }else if(eingabe.equals("no")) {
	    	erstelle = false;
	    }
	    
		NotifyCallback meinSpieler = new Spielverwaltung(erstelle);
	    System.out.println("Um das spiel zu starten gebe etwas ein");
	    eingabe = sc.next();
		((Spielverwaltung)meinSpieler).erstelleSpiel();
		sc.close();
	}
	
	
	/**
	 * Nur um die LED zu testen
	 */
	public static void testled() {
		Scanner sc = new Scanner(System.in);
		System.out.print("Gebe port ein: ");
		String eingabe = sc.next();
		
		URI uri = null;
		CoAPLED led = null;
		try {
			uri = new URI("coap://localhost:5683/led");
			led = new CoAPLED(uri);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		while(!eingabe.equals("s")) {
			 eingabe = sc.next();
			if(eingabe.equals("g")) {
				led.setLED(Spielstatus.GRUEN);
			}else if(eingabe.equals("b")) {
				led.setLED(Spielstatus.BLAU);
			}else if(eingabe.equals("v")) {
				led.setLED(Spielstatus.VIOLETT);
			}else if(eingabe.equals("r")){
				led.setLED(Spielstatus.ROT);
			}
		}
		sc.close();
	}

}
