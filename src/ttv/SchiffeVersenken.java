package ttv;

import de.uniba.wiai.lspi.chord.service.Chord;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

public class SchiffeVersenken {
	
	public static int ANZAHLSCHIFFE = 10;
	
	public static int ANZAHLINTERVALLE = 100;
	
	public static String ADDRESS = ":// localhost :8080/";
	
	public static String BOOTSTRPADDRESS = ":// localhost :8080/";
	

	public static void main(String[] args) {
		Chord chord = new ChordImpl();
		NotifyCallback notifyCallback = new Spielverwaltung(false);
		
	}

}
