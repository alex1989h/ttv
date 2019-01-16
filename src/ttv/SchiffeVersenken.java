package ttv;

import java.util.Scanner;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;

public class SchiffeVersenken {

	public static int ANZAHLSCHIFFE = 10;

	public static int ANZAHLINTERVALLE = 100;

	public static String ADDRESS = "://localhost:8080/";

	public static String BOOTSTRPADDRESS = "://localhost:8080/";

	public static String COAPADDRESS = "coap://localhost/led";
	
	public static String AUSGABE = "no";

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		String eingabe = null;
		if (args.length < 1) {
			System.out.print("Gebe Addresse ein (lokal/ich): ");
			eingabe = sc.next();
			ADDRESS = "://" + eingabe;
		} else {
			ADDRESS = "://" + args[0];
		}
		ADDRESS = ADDRESS.charAt(ADDRESS.length()-1) != '/'?ADDRESS+"/":ADDRESS;
		System.out.println("Local: " + ADDRESS);
		if (args.length < 2) {
			System.out.print("Gebe Addresse ein (bootstrap): ");
			eingabe = sc.next();
			BOOTSTRPADDRESS = "://"+ eingabe;
		} else {
			BOOTSTRPADDRESS = "://" + args[1];
		}
		BOOTSTRPADDRESS = BOOTSTRPADDRESS.charAt(BOOTSTRPADDRESS.length()-1) != '/'?BOOTSTRPADDRESS+"/":BOOTSTRPADDRESS;
		System.out.println("Bootstrap: " + BOOTSTRPADDRESS);
		if (args.length < 3) {
			System.out.print("Gebe Addresse ein (CoAP): ");
			eingabe = sc.next();
			COAPADDRESS = "coap://"+ eingabe;
		} else {
			COAPADDRESS = "coap://" + args[2];
		}
		System.out.println("CoAp-Address: " + COAPADDRESS);
		System.out.print("Soll Netwerk erstellt werden (yes/no): ");
		eingabe = sc.next();
		
		boolean erstelle = true;
		if (eingabe.equals("yes")) {
			erstelle = true;
		} else if (eingabe.equals("no")) {
			erstelle = false;
		}
		
		System.out.print("Soll nur wenn jemand besiegt wurde ne Ausgabe erfolgen (yes/no): ");
		SchiffeVersenken.AUSGABE = sc.next();
		

		NotifyCallback meinSpieler = new Spielverwaltung(erstelle);
		System.out.println("Um das spiel zu starten gebe etwas ein");
		((Spielverwaltung) meinSpieler).erstelleMeinenSpieler();
		eingabe = sc.next();
		((Spielverwaltung) meinSpieler).erstelleSpiel();
		sc.close();
	}
}
