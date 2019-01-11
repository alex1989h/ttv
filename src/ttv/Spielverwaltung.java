package ttv;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
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

public class Spielverwaltung implements NotifyCallback {
	
	private List<Spieler> spielerListe;
	
	private MeinSpieler meinSpieler;
	
	private ChordImpl chord;
	
	private CoAPLED led;
	
	 public Spielverwaltung(boolean createChordNetwork) {
		PropertiesLoader.loadPropertyFile();
		spielerListe = new ArrayList<Spieler>();
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
			// Erstelle einen Netzwerk
			try {
				chord.create(localURL);
				System.out.println("Netzwerk erstellt");
			} catch (ServiceException e) {
				throw new RuntimeException ( " Could not create DHT !" , e ) ;
			}
		}
		//Trette einem Netzwerk bei.
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
		/*
		 * Erstelle einen Kontroller für die LED
		 */
		URI uri = null;
		try {
			uri = new URI(SchiffeVersenken.COAPADDRESS);
			led = new CoAPLED(uri);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Wird aufgerufen wenn wir beschossen wurden.
	 * Ablauf:
	 * 1. Zuerst werden die Spieler-Liste Aktualisiert, für den Fall, 
	 * dass neue Nodes in der FingerTabler aufgetaucht sind.
	 * 2. Wird geschaut ob einer unsere Schiffe versenkt wurde.
	 * 3. Die Antwort aus 2. wirde an alle Nodes gesendet(Broadcast)
	 * 4. Ein Spieler aus unsere Liste, den wir beschießen wollen, wird ausgesucht
	 * 5. Es wird eine genau ID, die der Spieler aus 4. verwaltet, ausgesucht 
	 * und auf sie geschoßen (Retrieve).
	 */
	@Override
	public void retrieved(ID target) {
		System.out.println("==== RETRIEVED (Auf mich wurde geschoßen) ====");
		aktualisiertSpieler();
		boolean treffer = wurdMeinSchiffVersenkt(target);
		System.out.println("Wurde ICH getroffen: "+ treffer);
		setzteDieLED();
		chord.broadcast(target, treffer);
		Spieler zielSpieler = waehleZiel();
		if(zielSpieler != null) {
			System.out.println("Mein Angriffziel: "+zielSpieler);
			ID newTarget = waehleTarget(zielSpieler);
			if(newTarget.isInInterval(zielSpieler.getPreviousPlayerID(), zielSpieler.getSpielerID())) {
				System.out.println("Die Ziel-ID ist im richtigen Bereich vom Spieler");
			}else {
				System.out.println("ACHTUNG: Die Ziel-ID ist nicht imrichtigen Bereich vom Spieler");
			}
			chord.asyncRetrieve(newTarget);
		} else {
			System.out.println("!!! ALLE SPIELER BESIEGT !!!");
		}
		System.out.println("==== RETRIEVED END ====");
	}
	

	/**
	 * Wird aufgerufen wenn man uns mitteilt das jemand getroffen wurde
	 * Ablauf:
	 * 1. Zuerst werden die Spieler-Liste Aktualisiert, für den Fall, 
	 * dass neue Nodes in der FingerTabler aufgetaucht sind.
	 * 2. Der Absender des Broadcasts wird zu unsere Spieler-Liste hinzugefügt,
	 * falls noch nicht vorhanden und es sich nicht um unseren Spieler handelt
	 * 3. Es wird durch unsere Spieler-Liste durchgegange und da der Feld des Spieler,
	 * der beschossen wurde, aktualiesiert.
	 */
	@Override
	public void broadcast(ID source, ID target, Boolean hit) {
		System.out.println("==== BROADCAST (Auf jemanden wurde geschoßen) ====");
		System.out.println("Source: "+source+" Target: "+target+" Hit: "+hit);
		aktualisiertSpieler();
		fuegeSpielerHinzu(source);
		for (Spieler spieler : spielerListe) {
			aktualisiereFeld(target,hit,spieler);
		}
		testeAufBesiegteGegener();
		System.out.println("==== BROADCAST END ====");
	}

	/**
	 * Es wird geschaut, ob die ID, auf die geschossen wurde, zwischen der ID des
	 * Spielers und der ID seines Vorgänger liegt oder ob es sich um die ID des Spielers
	 * selbst handelt, wenn ja wird das Feld dieses Spielers aktualisiert.
	 * Dazu wirde die ID in ein Index des Feldes umgerechnet.
	 * @param target ID auf die geschoßen wurde
	 * @param hit Zeigt, ob ein Schiff getroffen wurde oder nicht
	 * @param spieler Augewähler Spieler aus der Spieler-Liste
	 */
	private void aktualisiereFeld(ID target,boolean hit, Spieler spieler) {
		if(target.isInInterval(spieler.getPreviousPlayerID(), spieler.getSpielerID())||
				target.equals(spieler.getSpielerID())) {
			int index = iDToIndex(target, spieler);
			spieler.feldaktualisieren(index, hit);
			System.out.println("=== Aktualisiere Feld ===");
			System.out.println(spieler);
			System.out.println("Index: "+index+" Getroffen: "+hit+" Target-ID: "+target);
			System.out.println("=== Aktualisiere Feld END ===");
		}
	}

	/**
	 * Es wird geschaut, ob der Spieler (mit der source ID) NICHT in unsere 
	 * Spieler-Liste ist und ob es sich NICHT um uns selber handelt 
	 * (wir selber sollten nicht in der Spieler-Liste sein).
	 * Wenn ja (neuer Spieler), wirde dieser Spieler zu unseren Spieler-Liste hinzugefügt
	 * und die vorgaenger IDs in jedem Spieler aktualisiert.
	 * @param source Die ID des Spielers
	 */
	private void fuegeSpielerHinzu(ID source) {
		Spieler newSpieler = new Spieler(SchiffeVersenken.ANZAHLINTERVALLE, source, null);
		if(newSpieler.equals(meinSpieler)){
			System.out.println("Source (ICH):"+meinSpieler);
			return;
		}
		if(!spielerListe.contains(newSpieler)) {
			/*
			 * Suche von dem neuen Spieler den Vorgaenger um seine ID als vorgaenger ID
			 * Im neune Spieler zu setzten
			 */
			ID prevID = meinSpieler.getSpielerID();
			for (Spieler spieler : spielerListe) {
				if(spieler.getSpielerID().isInInterval(prevID, newSpieler.getSpielerID())) {
					prevID = spieler.getSpielerID();
				}
			}
			newSpieler.setPreviousPlayerID(prevID);
			spielerListe.add(newSpieler);
			/*
			 * Aktualisier in vorgaenger IDs in den Spieler aus der Liste
			 */
			for (Spieler spieler : spielerListe) {
				prevID = spieler.getPreviousPlayerID();
				for (Spieler spieler2 : spielerListe) {
					if (spieler2.getSpielerID().isInInterval(prevID, spieler.getSpielerID())) {
						prevID = spieler2.getSpielerID();
					}
				}
				spieler.setPreviousPlayerID(prevID);
			}
			/*
			 * Aktualiesiere meine eigene vorgaenger ID
			 */
			prevID = meinSpieler.getPreviousPlayerID();
			for (Spieler spieler : spielerListe) {
				if (spieler.getSpielerID().isInInterval(prevID, meinSpieler.getSpielerID())) {
					prevID = spieler.getSpielerID();
				}
			}
			meinSpieler.setPreviousPlayerID(prevID);
			
			System.out.println("Source (NEU):"+newSpieler);
			System.out.println("=== Spieler in der Liste ===");
			for (Spieler spieler : spielerListe) {
				System.out.println(spieler);
			}
			System.out.println("=== Spieler in der Liste END ===");
		}else {
			int index = spielerListe.indexOf(newSpieler);
			newSpieler = spielerListe.get(index);
			System.out.println("Source (GEG):"+newSpieler);
		}
	}

	/**
	 * Nehme eine zufähliger Feld(index) aus den noch verfügbaren Felder des Ziel-Spielers,
	 * rechne diesen Index dann in die ID um und liefere sie zurück
	 * @param zielSpieler Auf ihn sollte geschoßen werden
	 * @return Die ID, die der ZielSpieler verwaltet, auf die geschossen werden sollte
	 */
	private ID waehleTarget(Spieler zielSpieler) {
		List<Integer> leererFelder = zielSpieler.getVerfuegbareFelder();
		Random rand = new Random();
		int zahl = rand.nextInt(leererFelder.size());
		int index = leererFelder.get(zahl);
		ID id = indexToID(index, zielSpieler);
		System.out.println("Ziel-Index: "+index+" Ziel-ID: "+id);
		return id;
	}
	
	/**
	 * Suche nach einem Spieler mit am meisten versenkten Schiffen.
	 * Von diesem Spieler suche denjenigen mit den wenigsten noch verfuegbaren Feldern
	 * @return Spieler auf den geschoßen werden sollte
	 */
	private Spieler waehleZiel() {
		Spieler zielSpieler = null;
		/*
		 * Suche Spieler, der noch Schiffe hat und wenn es keine Gibt return null
		 */
		for (Spieler spieler : spielerListe) {
			if(spieler.getHits() < SchiffeVersenken.ANZAHLSCHIFFE) {
				zielSpieler = spieler;
				break;
			}
			return zielSpieler;
		}
		/*
		 * Kommentar zu der Methode (siehe oben) 
		 */
		for (Spieler spieler : spielerListe) {
			if(spieler.getHits() >= SchiffeVersenken.ANZAHLSCHIFFE) {
				continue;
			}
			if (spieler.getHits() > zielSpieler.getHits()) {
				zielSpieler = spieler;
			} else if (spieler.getHits() == zielSpieler.getHits()
					&& spieler.getVerfuegbareFelder().size() < zielSpieler.getVerfuegbareFelder().size()) {
				zielSpieler = spieler;
			}
		}
		return zielSpieler;
	}
	
	/**
	 * Es wird geschaut, ob die ID, auf die geschossen wurde, zwischen unseren ID
	 * und der ID unseren Vorgängers liegt oder ob es sich um unsere ID handelt.
	 * Dann wird die ID in ein Index-Feld umgerechnet und geschaut ob ein Schiff in diesem
	 * Feld liegt, wenn ja wirde das Feld aktualisiert und true zurückgeliefert
	 * @param target Beschoßene ID
	 * @return true wenn einer meiner Schiffe getroffen wurde
	 */
	private boolean wurdMeinSchiffVersenkt(ID target) {
		boolean getroffen = false;
		if(target.isInInterval(meinSpieler.getPreviousPlayerID(), meinSpieler.getSpielerID())||
				target.equals(meinSpieler.getSpielerID())) {
			int index = iDToIndex(target, meinSpieler);
			getroffen = meinSpieler.angriff(index);
			System.out.println("ICH: " + meinSpieler);
			System.out.println("ICH: Index: " + index + " Getroffen: " + getroffen + " Target-ID: " + target);
		}
		return getroffen;
	}

	/**
	 * Hollt sich die FingerTable und fügt neu dazugekommene Spieler in unsere Spieler-Liste
	 * hinzu, dabei werden die vorgänger IDs in den Spielern aktualiesiert
	 */
	private void aktualisiertSpieler() {
		List<Node> fingerTable = chord.getFingerTable();
		chord.sortFingerTable(fingerTable);
		Spieler newSpieler = null;
		for (int i = 0; i < fingerTable.size(); i++) {
			newSpieler = new Spieler(SchiffeVersenken.ANZAHLINTERVALLE, fingerTable.get(i).getNodeID(), null);
			if(spielerListe.contains(newSpieler)) {
				int index = spielerListe.indexOf(newSpieler);
				newSpieler = spielerListe.get(index);
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
					for (Spieler spieler : spielerListe) {
						if(spieler.getSpielerID().isInInterval(prevID, newSpieler.getSpielerID())) {
							prevID = spieler.getSpielerID();
						}
					}
					newSpieler.setPreviousPlayerID(prevID);
				}else {
					prevID = fingerTable.get(i-1).getNodeID();
					for (Spieler spieler : spielerListe) {
						if(spieler.getSpielerID().isInInterval(prevID, newSpieler.getSpielerID())) {
							prevID = spieler.getSpielerID();
						}
					}
					newSpieler.setPreviousPlayerID(prevID);
				}
				spielerListe.add(newSpieler);
			}
		}
		
	}
	
	/**
	 * Erstellt unseren eingenen Spieler mit der ID von Chord und unseren vorgänger ID.
	 * Setzt auf die 100 Feldern 10 Schiffe auf zufählige Positionen/Feldern.
	 * Aktualisiert Spieler-Liste.
	 * Schaut, ob wir die größtmöglich ID verwalten, wenn ja dürfen wir zuerst Schießen
	 */
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
		 
		 System.out.println("ICH: "+meinSpieler);
		 for (Spieler spieler : spielerListe) {
			System.out.println("GEG: "+spieler);
		}
		ID maxID = ID.valueOf(BigInteger.valueOf(2).pow(160).subtract(BigInteger.valueOf(1)));
		if(maxID.isInInterval(meinSpieler.previousPlayerID, meinSpieler.getSpielerID())) {
			System.out.println("Schieße zuerst!");
			Spieler zielSpieler = waehleZiel();
			ID newTarget = waehleTarget(zielSpieler);
			chord.asyncRetrieve(newTarget);
			System.out.println("Geschoßen");
		}else {
			System.out.println("Schieße NICHT!");
		}
	}
	
	/**
	 * Rechnet die ID in ein Index für den ausgewaelten Spieler
	 * @param target Die ID
	 * @param spieler Ausgewaehler Spieler
	 * @return Index
	 */
	public int iDToIndex(ID target, Spieler spieler) {
		BigInteger idsRaum = BigInteger.valueOf(2).pow(160).subtract(BigInteger.valueOf(1));
		BigInteger id = target.toBigInteger();
		BigInteger myId = spieler.getSpielerID().toBigInteger();
		BigInteger prevId = spieler.getPreviousPlayerID().toBigInteger();
		
		BigInteger mod1 = myId.subtract(prevId).mod(idsRaum);
		BigInteger mod2 = id.subtract(prevId).mod(idsRaum);
		
		BigInteger intervall = mod1.divide(BigInteger.valueOf(SchiffeVersenken.ANZAHLINTERVALLE));
		BigInteger sum = intervall;
		int index = 0;
		while(sum.compareTo(mod2) < 0) {
			sum = sum.add(intervall);
			index++;
			/**
			 * Für den Fall, dass er über die maximale ID Raum für diesen Spieler geht
			 */
			if(index >= SchiffeVersenken.ANZAHLINTERVALLE) {
				index = 99;
				break;
			}
		}
		return index;
	}
	
	/**
	 * Rechnet den Index in eine ID um, für den augewaehlten Spieler
	 * @param index Index des Felds vom Spieler
	 * @param spieler Augewaehler Spieler
	 * @return ID
	 */
	public ID indexToID(int index, Spieler spieler) {
		BigInteger idsRaum = BigInteger.valueOf(2).pow(160).subtract(BigInteger.valueOf(1));
		BigInteger spielerId = spieler.getSpielerID().toBigInteger();
		BigInteger previousId = spieler.getPreviousPlayerID().toBigInteger();
		
		BigInteger mod1 = spielerId.subtract(previousId).mod(idsRaum);
		
		BigInteger intervall = mod1.divide(BigInteger.valueOf(SchiffeVersenken.ANZAHLINTERVALLE));
		
		index++;
		BigInteger newTarget = intervall.multiply(BigInteger.valueOf(index))
				.add(previousId).mod(idsRaum);
		return ID.valueOf(newTarget);
	}
	
	/**
	 * Geht die Spieler-Liste durch und schaut, ob es besiegte Gegner gibt
	 * @return true wenn min ein besiegter Gegner
	 */
	public boolean testeAufBesiegteGegener() {
		boolean gibtsBesiegte = false;
		for (Spieler spieler : spielerListe) {
			if(spieler.getHits() >= SchiffeVersenken.ANZAHLSCHIFFE) {
				System.out.println("!!!!!!!!!!!!!!!!! SPIELER WURDE BESIEGT !!!!!!!!!!!!!!!!!");
				System.out.println("BESIEGT: "+spieler);
				gibtsBesiegte=true;
			}
		}
		return gibtsBesiegte;
	}
	
	private void setzteDieLED() {
		int versenkt = meinSpieler.getHits();
		double prozentVersenkt = (100.0/SchiffeVersenken.ANZAHLSCHIFFE)*versenkt;
		if(versenkt == 0) {
			led.setLED(Spielstatus.GRUEN);
		}else if(prozentVersenkt < 50) {
			led.setLED(Spielstatus.BLAU);
		}else if(prozentVersenkt >= 50 && versenkt < SchiffeVersenken.ANZAHLSCHIFFE) {
			led.setLED(Spielstatus.VIOLETT);
		}else {
			led.setLED(Spielstatus.ROT);
		}
	}
	
	
}
