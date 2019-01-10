package ttv;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.uniba.wiai.lspi.chord.com.Node;
import de.uniba.wiai.lspi.chord.data.ID;
import de.uniba.wiai.lspi.chord.data.URL;
import de.uniba.wiai.lspi.chord.service.NotifyCallback;
import de.uniba.wiai.lspi.chord.service.PropertiesLoader;
import de.uniba.wiai.lspi.chord.service.ServiceException;
import de.uniba.wiai.lspi.chord.service.impl.ChordImpl;
import de.uniba.wiai.lspi.util.logging.Logger;

public class Spielverwaltung implements NotifyCallback {
	
	private List<Spieler> spieler;
	
	private MeinSpieler meinSpieler;
	
	private ChordImpl chord;
	
	private Logger logger;
	
	 public Spielverwaltung(boolean createChordNetwork) {
		this.logger = Logger.getLogger(Spielverwaltung.class.getName());
		PropertiesLoader.loadPropertyFile();
		spieler = new ArrayList<Spieler>();
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
				System.out.println("Netzwerk erstellt");
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
				System.out.println("Netzwerk beigetretten");
			} catch (ServiceException e) {
				throw new RuntimeException ( " Could not join DHT ! " , e );
			}
		}
	}
	
	
	/**
	 * Wird aufgerufen wenn wir beschossen wurden
	 */
	@Override
	public void retrieved(ID target) {
		System.out.println("Auf mich wurde geschoﬂen");
		aktualisiertSpieler();
		System.out.println("Spieler wurden aktualisiert");
		boolean treffer = wurdeSchiffgetroffen(target);
		System.out.println("Wurde getroffen: "+ treffer);
		chord.broadcast(target, treffer);
		System.out.println("Broadcast erstellt");
		Spieler zielSpieler = waehleZiel();
		ID newTarget = waehleTarget(zielSpieler);
		chord.asyncRetrieve(newTarget);
	}
	

	/**
	 * Wird aufgerufen wenn man uns mitteilt das jemand getroffen wurde
	 */
	@Override
	public void broadcast(ID source, ID target, Boolean hit) {
		System.out.println("Auf jemanden wurde geschoﬂen");
		aktualisiertSpieler();
		fuegeSpielerhinzu(source);
		for (Spieler spieler2 : spieler) {
			aktualisiereSchiffe(target,hit,spieler2);
		}
	}

	private void aktualisiereSchiffe(ID target,boolean hit, Spieler spieler2) {
		if(target.isInInterval(spieler2.getPreviousPlayerID(), spieler2.getSpielerID())||
				target.equals(spieler2.getSpielerID())) {
			BigInteger idsRaum = BigInteger.valueOf(2).pow(160).subtract(BigInteger.valueOf(1));
			BigInteger id = target.toBigInteger();
			BigInteger prevId = spieler2.getPreviousPlayerID().toBigInteger();
			BigInteger myId = spieler2.getSpielerID().toBigInteger();
			
			BigInteger mod1 = myId.subtract(prevId).mod(idsRaum);
			BigInteger mod2 = id.subtract(prevId).mod(idsRaum);
			
			BigInteger intervall = mod1.divide(BigInteger.valueOf(100));
			
			int index = 0;
			while(intervall.compareTo(mod2) <= 0 && index < 99) {
				intervall = intervall.add(intervall);
				index++;
			}
			
			spieler2.feldaktualisieren(index, hit);
			System.out.println(spieler2+"\nTarget: "+target+"\nIndex: "+index+"\nGetroffen: "+hit);
			System.out.println("Hits: "+spieler2.getHits()+"Frei: "+spieler2.getVerfuegbareFelder().size());
		}
	}

	private void fuegeSpielerhinzu(ID source) {
		Spieler newSpieler = new Spieler(SchiffeVersenken.ANZAHLINTERVALLE, source, null);
		if(!spieler.contains(newSpieler)) {
			ID prevID = meinSpieler.getSpielerID();
			for (Spieler spieler2 : spieler) {
				if(spieler2.getSpielerID().isInInterval(prevID, newSpieler.getSpielerID())) {
					prevID = spieler2.getSpielerID();
				}
			}
			spieler.add(newSpieler);
			for (Spieler spieler2 : spieler) {
				prevID = spieler2.getPreviousPlayerID();
				for (Spieler spieler3 : spieler) {
					if (spieler3.getSpielerID().isInInterval(prevID, spieler2.getSpielerID())) {
						prevID = spieler3.getSpielerID();
					}
					spieler2.setPreviousPlayerID(prevID);
				}
			}
			
		}
	}

	/**
	 * Nehme eine zuf‰hliger Feld(index) aus den noch verf¸gbaren Felder des Ziel-Spielers
	 * rechne den index dann in die ID
	 * @param zielSpieler
	 * @return ID f¸r den gew‰hlten, freien chord.retrieve(targetID);Feld
	 */
	private ID waehleTarget(Spieler zielSpieler) {
		List<Integer> leererFelder = zielSpieler.getVerfuegbareFelder();
		Random rand = new Random();
		int zahl = rand.nextInt(leererFelder.size());
		int index = leererFelder.get(zahl);
		BigInteger idsRaum = BigInteger.valueOf(2).pow(160).subtract(BigInteger.valueOf(1));
		BigInteger spielerId = zielSpieler.getSpielerID().toBigInteger();
		BigInteger previousId = zielSpieler.getPreviousPlayerID().toBigInteger();
		index++;
		ID newTarget = ID.valueOf(spielerId.subtract(previousId).mod(idsRaum)
		.divide(BigInteger.valueOf(100))
		.multiply(BigInteger.valueOf(index)).add(previousId).mod(idsRaum));
		return newTarget;
	}
	
	/**
	 * Suche nach einem Spieler mit am meisten versenkten Schiffen
	 * @return
	 */
	private Spieler waehleZiel() {
		Spieler zielSpieler = spieler.get(0);
		for (Spieler spieler2 : spieler) {
			if(spieler2.getHits() > zielSpieler.getHits()) {
				zielSpieler = spieler2;
			}
		}
		return zielSpieler;
	}
	
	/**
	 * Rechne ID in Index um und schaue, ob da eine Schiff liegt, wenn ja wurde er getroffen
	 * und man liefert true
	 * @param target
	 * @return true wenn ein Schiff getroffen wurde
	 */
	private boolean wurdeSchiffgetroffen(ID target) {
		boolean getroffen = false;
		if(target.isInInterval(meinSpieler.getPreviousPlayerID(), meinSpieler.getSpielerID())||
				target.equals(meinSpieler.getSpielerID())) {
			BigInteger idsRaum = BigInteger.valueOf(2).pow(160).subtract(BigInteger.valueOf(1));
			BigInteger id = target.toBigInteger();
			BigInteger myId = meinSpieler.getSpielerID().toBigInteger();
			BigInteger prevId = meinSpieler.getPreviousPlayerID().toBigInteger();
			
			BigInteger mod1 = myId.subtract(prevId).mod(idsRaum);
			BigInteger mod2 = id.subtract(prevId).mod(idsRaum);
			
			BigInteger intervall = mod1.divide(BigInteger.valueOf(100));
			
			int index = 0;
			while(intervall.compareTo(mod2) <= 0 && index < 99) {
				intervall = intervall.add(intervall);
				index++;
			}
			/*
			 * Wenn IndexD = 1 ist, muss Index=0 gew‰hlt werden(erster Eintrag)
			 * Wenn IndexD = 1.1 ist, muss Index=1 gew‰hlt werden(zweiter Entrag)
			 * Deshalb die If-Anweisung
			 * Beispiel: IndexD=0.1 > Index=0 also w‰hle Index= 0
			 * Beispiel: IndexD=100(MAX) > Index=100 also w‰hle Index=99(siehe index--)
			 * IndexD=0 kann nicht vorkommen sonst w‰re es ein Spieler vor uns
			 * 
			 */
			System.out.println("Index: "+ index);
			getroffen = meinSpieler.angriff(index);
			System.out.println("Target: "+target+"\nIndex: "+index+"\nGetroffen: "+getroffen);
			System.out.println("Hits: "+meinSpieler.getHits()+"Frei: "+meinSpieler.getVerfuegbareFelder().size());
			
		};
		return getroffen;
	}

	private void aktualisiertSpieler() {
		List<Node> fingerTable = chord.getFingerTable();
		chord.sortFingerTable(fingerTable);
		Spieler newSpieler = null;
		for (int i = 0; i < fingerTable.size(); i++) {
			newSpieler = new Spieler(SchiffeVersenken.ANZAHLINTERVALLE, fingerTable.get(i).getNodeID(), null);
			if(spieler.contains(newSpieler)) {
				int index = spieler.indexOf(newSpieler);
				newSpieler = spieler.get(index);
				if(i == 0) {
					if(!(newSpieler.getPreviousPlayerID().isInInterval(meinSpieler.getSpielerID(), newSpieler.spielerID))) {
						newSpieler.setPreviousPlayerID(meinSpieler.getSpielerID());
					}
				} else {
					Node prevNode = fingerTable.get(i-1);
					if(!(newSpieler.getPreviousPlayerID().isInInterval(prevNode.getNodeID(), newSpieler.spielerID))) {
						newSpieler.setPreviousPlayerID(prevNode.getNodeID());
					}
				}
			}else {
				ID prevID = null;
				if(i == 0) {
					prevID = meinSpieler.getSpielerID();
					for (Spieler spieler2 : spieler) {
						if(spieler2.getSpielerID().isInInterval(prevID, newSpieler.getSpielerID())) {
							prevID = spieler2.getSpielerID();
						}
					}
					newSpieler.setPreviousPlayerID(prevID);
				}else {
					prevID = fingerTable.get(i-1).getNodeID();
					for (Spieler spieler2 : spieler) {
						if(spieler2.getSpielerID().isInInterval(prevID, newSpieler.getSpielerID())) {
							prevID = spieler2.getSpielerID();
						}
					}
					newSpieler.setPreviousPlayerID(prevID);
				}
				spieler.add(newSpieler);
			}
		}
		
	}


	public void erstelleSpiel() {
		meinSpieler = new MeinSpieler(SchiffeVersenken.ANZAHLINTERVALLE, chord.getID(), chord.getPredecessorID());
		 List<Integer> list = new ArrayList<Integer>();
		 
		 for(int i=0;i<SchiffeVersenken.ANZAHLSCHIFFE;i++) {
			 Random rand = new Random();
			 int zahl = rand.nextInt(SchiffeVersenken.ANZAHLINTERVALLE);
			 
			 while(list.contains(zahl)) {
				 zahl = rand.nextInt(SchiffeVersenken.ANZAHLINTERVALLE);
			 }
			 list.add(zahl);
		 }
		 meinSpieler.setzeSchiffe(list);
		 aktualisiertSpieler();
		 
		 System.out.println("Mein Spieler:"+meinSpieler);
		 for (Spieler spieler2 : spieler) {
			System.out.println(spieler2);
		}
		ID maxID = ID.valueOf(BigInteger.valueOf(2).pow(160).subtract(BigInteger.valueOf(1)));
		if(maxID.isInInterval(meinSpieler.previousPlayerID, meinSpieler.getSpielerID())) {
			System.out.println("Schieﬂe zuerst!");
			Spieler zielSpieler = waehleZiel();
			ID newTarget = waehleTarget(zielSpieler);
			System.out.println("Target: "+newTarget);
			chord.asyncRetrieve(newTarget);
			System.out.println("Geschoﬂen");
		}else {
			System.out.println("Schieﬂe NICHT!");
		}
	}
}
