package ttv;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;

public class Spielverwaltung implements NotifyCallback {
	
	private List<Spieler> spieler;
	
	private MeinSpieler meinSpieler;
	
	private ChordImpl chord;
	
	 public Spielverwaltung(boolean createChordNetwork) {
		 
		PropertiesLoader.loadPropertyFile();
		String protocol = URL.KNOWN_PROTOCOLS.get(URL.SOCKET_PROTOCOL);
		URL localURL = null;
		try {
			localURL = new URL(protocol + SchiffeVersenken.ADDRESS);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e );
		}
		
		chord = new ChordImpl();
		chord.setCallback(this);
		
		if(createChordNetwork) {
			try {
				chord.create(localURL);
			} catch (ServiceException e) {
				throw new RuntimeException ( " Could not create DHT !" , e ) ;
			}
		}
		//Join existing network.
		else {
			URL bootstrapURL = null;
			try {
				bootstrapURL = new URL(protocol + SchiffeVersenken.BOOTSTRPADDRESS);
			} catch (MalformedURLException e) {
				throw new RuntimeException ( e ) ;
			}
			try {
				chord.join(localURL, bootstrapURL);
			} catch (ServiceException e) {
				throw new RuntimeException ( " Could not join DHT ! " , e );
			}
		}
		
		
		
		 
		 meinSpieler = new MeinSpieler(SchiffeVersenken.ANZAHLINTERVALLE, chord.getID(), chord.getPredecessorID());
		 List<Integer> list = new ArrayList<Integer>();
		 
		 for(int i=0;i<SchiffeVersenken.ANZAHLSCHIFFE;i++) {
			 Random rand = new Random();
			 int zahl = rand.nextInt(99) + 0;
			 
			 while(list.contains(zahl)) {
				 zahl = rand.nextInt(99) + 0;
			 }
			 list.add(zahl);
		 }
		 
		 meinSpieler.setzeSchiffe(list);
		 
		 spieler = new ArrayList<Spieler>();
		 
		 
		 
		
	}
	
	

	@Override
	public void retrieved(ID target) {
		
		

		

	}

	@Override
	public void broadcast(ID source, ID target, Boolean hit) {
	


	}
	
	private boolean wurdeSchiffgetroffen(ID target) {
	
		if(target.isInInterval(meinSpieler.getPreviousPlayerID(), meinSpieler.getSpielerID())) {
			BigInteger id = target.toBigInteger();
			BigInteger myId = meinSpieler.getPreviousPlayerID().toBigInteger();
			BigInteger prevId = meinSpieler.getSpielerID().toBigInteger();
			int index = BigInteger.valueOf(100).divide(myId.subtract(prevId)).multiply(id.subtract(prevId)).intValue();
		}
		
		
		return false;
	}

}
