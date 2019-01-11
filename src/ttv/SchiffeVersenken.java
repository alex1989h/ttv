package ttv;

import java.util.Scanner;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;

public class SchiffeVersenken {
	
	public static int ANZAHLSCHIFFE = 10;
	
	public static int ANZAHLINTERVALLE = 100;
	
	public static String ADDRESS = "://localhost:8080/";
	
	public static String BOOTSTRPADDRESS = "://localhost:8080/";
	
	public static String COAPADDRESS = "coap://localhost/led";

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
	    ((Spielverwaltung)meinSpieler).erstelleMeinenSpieler();
	    eingabe = sc.next();
		((Spielverwaltung)meinSpieler).erstelleSpiel();
		sc.close();
	}
}
