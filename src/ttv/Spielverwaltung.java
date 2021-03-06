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

enum SpielerStatus {
	ICH, GEG, NEU, UNBEKANNT
}

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
			throw new RuntimeException(e);
		}

		chord = new ChordImpl();
		chord.setCallback(this);

		if (createChordNetwork) {
			// Erstelle einen Netzwerk
			try {
				chord.create(localURL);
				System.out.println("Netzwerk erstellt");
			} catch (ServiceException e) {
				throw new RuntimeException(" Could not create DHT !", e);
			}
		}
		// Trette einem Netzwerk bei.
		else {
			URL bootstrapURL = null;
			try {
				bootstrapURL = new URL(protocol + SchiffeVersenken.BOOTSTRPADDRESS);
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
			try {
				chord.join(localURL, bootstrapURL);
				System.out.println("Netzwerk beigetretten");
			} catch (ServiceException e) {
				throw new RuntimeException(" Could not join DHT ! ", e);
			}
		}
		/*
		 * Erstelle einen Kontroller f�r die LED
		 */
		URI uri = null;
		try {
			uri = new URI(SchiffeVersenken.COAPADDRESS);
			led = new CoAPLED(uri);
			new Thread(led).start();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Wird aufgerufen wenn wir beschossen wurden. Ablauf: 1. Zuerst werden die
	 * Spieler-Liste Aktualisiert, f�r den Fall, dass neue Nodes in der FingerTabler
	 * aufgetaucht sind. 2. Wird geschaut ob einer unsere Schiffe versenkt wurde. 3.
	 * Die Antwort aus 2. wirde an alle Nodes gesendet(Broadcast) 4. Ein Spieler aus
	 * unsere Liste, den wir beschie�en wollen, wird ausgesucht 5. Es wird eine
	 * genau ID, die der Spieler aus 4. verwaltet, ausgesucht und auf sie gescho�en
	 * (Retrieve).
	 */
	@Override
	public void retrieved(ID target) {
		aktualisiertSpieler();
		boolean treffer = wurdMeinSchiffVersenkt(target);
		setzteDieLED();
		chord.broadcast(target, treffer);
		Spieler zielSpieler = waehleZiel();
		if (zielSpieler != null) {
			ID newTarget = waehleTarget(zielSpieler);
			String targetString = newTarget.toString();
			targetString = targetString.substring(0, 2)+" ... "+targetString.substring(targetString.length()-3,targetString.length()-1);
			System.out.println("ANGRIFFSZIEL:    " + zielSpieler+" Target: "+targetString+", FEUER !!!");
			chord.asyncRetrieve(newTarget);
		} else {
			testeAufBesiegteGegener();
		}
	}

	/**
	 * Wird aufgerufen wenn man uns mitteilt das jemand getroffen wurde Ablauf: 1.
	 * Zuerst werden die Spieler-Liste Aktualisiert, f�r den Fall, dass neue Nodes
	 * in der FingerTabler aufgetaucht sind. 2. Der Absender des Broadcasts wird zu
	 * unsere Spieler-Liste hinzugef�gt, falls noch nicht vorhanden und es sich
	 * nicht um unseren Spieler handelt 3. Es wird durch unsere Spieler-Liste
	 * durchgegange und da der Feld des Spieler, der beschossen wurde,
	 * aktualiesiert.
	 */
	@Override
	public void broadcast(ID source, ID target, Boolean hit) {
		String output;
		String targetString = target.toString();
		targetString = targetString.substring(0, 2)+" ... "+targetString.substring(targetString.length()-3,targetString.length()-1);
		aktualisiertSpieler();
		SpielerStatus status = fuegeSpielerHinzu(source);
		for (Spieler spieler : spielerListe) {
			aktualisiereFeld(target, hit, spieler);
		}
		
		output = "BROADCAST: " + spielerString(source,status) + " Target: "+ targetString + ", Hit: " + hit;
		System.out.println(output);
		testeAufBesiegteGegener();
	}

	private String spielerString(ID source, SpielerStatus status) {
		Spieler newSpieler = new Spieler(SchiffeVersenken.ANZAHLINTERVALLE, source, null);
		if (newSpieler.equals(meinSpieler) && status == SpielerStatus.ICH) {
			return "("+status+") " + meinSpieler;
		}
		if (spielerListe.contains(newSpieler) && (status == SpielerStatus.GEG || status == SpielerStatus.NEU)) {
			int index = spielerListe.indexOf(newSpieler);
			newSpieler = spielerListe.get(index);
			return "("+status+") " + newSpieler;
		}
		return "("+SpielerStatus.UNBEKANNT+") "+newSpieler;
	}

	/**
	 * Es wird geschaut, ob die ID, auf die geschossen wurde, zwischen der ID des
	 * Spielers und der ID seines Vorg�nger liegt oder ob es sich um die ID des
	 * Spielers selbst handelt, wenn ja wird das Feld dieses Spielers aktualisiert.
	 * Dazu wirde die ID in ein Index des Feldes umgerechnet.
	 * 
	 * @param target
	 *            ID auf die gescho�en wurde
	 * @param hit
	 *            Zeigt, ob ein Schiff getroffen wurde oder nicht
	 * @param spieler
	 *            Augew�hler Spieler aus der Spieler-Liste
	 */
	private void aktualisiereFeld(ID target, boolean hit, Spieler spieler) {
		if (target.isInInterval(spieler.getPreviousPlayerID(), spieler.getSpielerID())
				|| target.equals(spieler.getSpielerID())) {
			int index = iDToIndex(target, spieler);
			spieler.feldaktualisieren(index, hit);
		}
	}

	/**
	 * Es wird geschaut, ob der Spieler (mit der source ID) NICHT in unsere
	 * Spieler-Liste ist und ob es sich NICHT um uns selber handelt (wir selber
	 * sollten nicht in der Spieler-Liste sein). Wenn ja (neuer Spieler), wirde
	 * dieser Spieler zu unseren Spieler-Liste hinzugef�gt und die vorgaenger IDs in
	 * jedem Spieler aktualisiert.
	 * 
	 * @param source
	 *            Die ID des Spielers
	 */
	private SpielerStatus fuegeSpielerHinzu(ID source) {
		Spieler newSpieler = new Spieler(SchiffeVersenken.ANZAHLINTERVALLE, source, null);
		if (newSpieler.equals(meinSpieler)) {
			return SpielerStatus.ICH;
		}
		if (!spielerListe.contains(newSpieler)) {
			/*
			 * Suche von dem neuen Spieler den Vorgaenger um seine ID als vorgaenger ID Im
			 * neune Spieler zu setzten
			 */
			ID prevID = meinSpieler.getSpielerID();
			for (Spieler spieler : spielerListe) {
				if (spieler.getSpielerID().isInInterval(prevID, newSpieler.getSpielerID())) {
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
			return SpielerStatus.NEU;
		} else {
			int index = spielerListe.indexOf(newSpieler);
			newSpieler = spielerListe.get(index);
			return SpielerStatus.GEG;
		}
	}

	/**
	 * Nehme eine zuf�hliger Feld(index) aus den noch verf�gbaren Felder des
	 * Ziel-Spielers, rechne diesen Index dann in die ID um und liefere sie zur�ck
	 * 
	 * @param zielSpieler
	 *            Auf ihn sollte gescho�en werden
	 * @return Die ID, die der ZielSpieler verwaltet, auf die geschossen werden
	 *         sollte
	 */
	private ID waehleTarget(Spieler zielSpieler) {
		List<Integer> leererFelder = zielSpieler.getVerfuegbareFelder();
		Random rand = new Random();
		int zahl = rand.nextInt(leererFelder.size());
		int index = leererFelder.get(zahl);
		ID id = indexToID(index, zielSpieler);
		return id;
	}

	/**
	 * Suche nach einem Spieler mit am meisten versenkten Schiffen. Von diesem
	 * Spieler suche denjenigen mit den wenigsten noch verfuegbaren Feldern
	 * 
	 * @return Spieler auf den gescho�en werden sollte
	 */
	private Spieler waehleZiel() {
		Spieler zielSpieler = null;
		/*
		 * Suche Spieler, der noch Schiffe hat und wenn es keine Gibt return null
		 */
		for (Spieler spieler : spielerListe) {
			if (spieler.getHits() < SchiffeVersenken.ANZAHLSCHIFFE) {
				zielSpieler = spieler;
				break;
			}
		}
		/*
		 * Es gib keine Spieler mit Schiffen
		 */
		if (zielSpieler != null) {
			return zielSpieler;
		}
		/*
		 * Kommentar zu der Methode (siehe oben)
		 */
		for (Spieler spieler : spielerListe) {
			if (spieler.getHits() >= SchiffeVersenken.ANZAHLSCHIFFE) {
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
	 * und der ID unseren Vorg�ngers liegt oder ob es sich um unsere ID handelt.
	 * Dann wird die ID in ein Index-Feld umgerechnet und geschaut ob ein Schiff in
	 * diesem Feld liegt, wenn ja wirde das Feld aktualisiert und true
	 * zur�ckgeliefert
	 * 
	 * @param target
	 *            Bescho�ene ID
	 * @return true wenn einer meiner Schiffe getroffen wurde
	 */
	private boolean wurdMeinSchiffVersenkt(ID target) {
		boolean getroffen = false;
		if (target.isInInterval(meinSpieler.getPreviousPlayerID(), meinSpieler.getSpielerID())
				|| target.equals(meinSpieler.getSpielerID())) {
			int index = iDToIndex(target, meinSpieler);
			getroffen = meinSpieler.angriff(index);
		}
		return getroffen;
	}

	/**
	 * Hollt sich die FingerTable und f�gt neu dazugekommene Spieler in unsere
	 * Spieler-Liste hinzu, dabei werden die vorg�nger IDs in den Spielern
	 * aktualiesiert
	 */
	private void aktualisiertSpieler() {
		List<Node> fingerTable = chord.getFingerTable();
		chord.sortFingerTable(fingerTable);
		Spieler newSpieler = null;
		for (int i = 0; i < fingerTable.size(); i++) {
			newSpieler = new Spieler(SchiffeVersenken.ANZAHLINTERVALLE, fingerTable.get(i).getNodeID(), null);
			if (spielerListe.contains(newSpieler)) {
				int index = spielerListe.indexOf(newSpieler);
				newSpieler = spielerListe.get(index);
				if (i == 0) {
					if (!(newSpieler.getPreviousPlayerID().isInInterval(meinSpieler.getSpielerID(),
							newSpieler.getSpielerID()))) {
						newSpieler.setPreviousPlayerID(meinSpieler.getSpielerID());
					}
				} else {
					Node prevNode = fingerTable.get(i - 1);
					if (!(newSpieler.getPreviousPlayerID().isInInterval(prevNode.getNodeID(),
							newSpieler.getSpielerID()))) {
						newSpieler.setPreviousPlayerID(prevNode.getNodeID());
					}
				}
			} else {
				ID prevID = null;
				if (i == 0) {
					prevID = meinSpieler.getSpielerID();
					for (Spieler spieler : spielerListe) {
						if (spieler.getSpielerID().isInInterval(prevID, newSpieler.getSpielerID())) {
							prevID = spieler.getSpielerID();
						}
					}
					newSpieler.setPreviousPlayerID(prevID);
				} else {
					prevID = fingerTable.get(i - 1).getNodeID();
					for (Spieler spieler : spielerListe) {
						if (spieler.getSpielerID().isInInterval(prevID, newSpieler.getSpielerID())) {
							prevID = spieler.getSpielerID();
						}
					}
					newSpieler.setPreviousPlayerID(prevID);
				}
				spielerListe.add(newSpieler);
			}
		}

		/*
		 * Aktualiesiere meine eigene vorgaenger ID
		 */
		ID prevID = meinSpieler.getPreviousPlayerID();
		for (Spieler spieler : spielerListe) {
			if (prevID == null) {
				prevID = spieler.getSpielerID();
			} else if (spieler.getSpielerID().isInInterval(prevID, meinSpieler.getSpielerID())) {
				prevID = spieler.getSpielerID();
			}
		}
		meinSpieler.setPreviousPlayerID(prevID);
	}

	/**
	 * Aktualisiert Spieler-Liste. Schaut, ob wir die gr��tm�glich ID verwalten,
	 * wenn ja d�rfen wir zuerst Schie�en
	 */
	public void erstelleSpiel() {
		aktualisiertSpieler();
		System.out.println("ICH: " + meinSpieler);
		for (Spieler spieler : spielerListe) {
			System.out.println("GEG: " + spieler);
		}
		ID maxID = ID.valueOf(BigInteger.valueOf(2).pow(160).subtract(BigInteger.valueOf(1)));
		if (meinSpieler.getPreviousPlayerID() != null
				&& maxID.isInInterval(meinSpieler.getPreviousPlayerID(), meinSpieler.getSpielerID())) {
			System.out.println("Schie�e zuerst!");
			Spieler zielSpieler = waehleZiel();
			if (zielSpieler != null) {
				ID newTarget = waehleTarget(zielSpieler);
				chord.asyncRetrieve(newTarget);
				System.out.println("Gescho�en");
			} else {
				System.out.println("Niemand da auf dem man schie�en kann");
			}
		} else {
			System.out.println("Schie�e NICHT!");
		}
	}

	/**
	 * Rechnet die ID in ein Index f�r den ausgewaelten Spieler
	 * 
	 * @param target
	 *            Die ID
	 * @param spieler
	 *            Ausgewaehler Spieler
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
		while (sum.compareTo(mod2) < 0) {
			sum = sum.add(intervall);
			index++;
			/**
			 * F�r den Fall, dass er �ber die maximale ID Raum f�r diesen Spieler geht
			 */
			if (index >= SchiffeVersenken.ANZAHLINTERVALLE) {
				index = 99;
				break;
			}
		}
		return index;
	}

	/**
	 * Rechnet den Index in eine ID um, f�r den augewaehlten Spieler
	 * 
	 * @param index
	 *            Index des Felds vom Spieler
	 * @param spieler
	 *            Augewaehler Spieler
	 * @return ID
	 */
	public ID indexToID(int index, Spieler spieler) {
		BigInteger idsRaum = BigInteger.valueOf(2).pow(160).subtract(BigInteger.valueOf(1));
		BigInteger spielerId = spieler.getSpielerID().toBigInteger();
		BigInteger previousId = spieler.getPreviousPlayerID().toBigInteger();

		BigInteger mod1 = spielerId.subtract(previousId).mod(idsRaum);

		BigInteger intervall = mod1.divide(BigInteger.valueOf(SchiffeVersenken.ANZAHLINTERVALLE));

		index++;
		BigInteger newTarget = intervall.multiply(BigInteger.valueOf(index)).add(previousId).mod(idsRaum);
		return ID.valueOf(newTarget);
	}

	/**
	 * Geht die Spieler-Liste durch und schaut, ob es besiegte Gegner gibt
	 * 
	 * @return true wenn min ein besiegter Gegner
	 */
	public boolean testeAufBesiegteGegener() {
		boolean gibtsBesiegte = false;
		boolean alleGegnerBesiegt = true;
		for (Spieler spieler : spielerListe) {
			if (spieler.getHits() >= SchiffeVersenken.ANZAHLSCHIFFE) {
				System.out.println("BESIEGT:   (GEG) " + spieler +" TOT !!!");
				gibtsBesiegte = true;
			} else {
				alleGegnerBesiegt = false;
			}
		}
		if (meinSpieler.getHits() >= SchiffeVersenken.ANZAHLSCHIFFE) {
			System.out.println("BESIEGT:   (ICH) " +meinSpieler +" TOT !!!");
		}
		if (alleGegnerBesiegt) {
			System.out.println(">>>>!!!!!!!!!!!!!!!!!!!!!!!! ALLE GEGNER BESIEGT !!!!!!!!!!!!!!!!!!!!!!!!<<<<");

		}
		return gibtsBesiegte;
	}

	/**
	 * Setzt die LED
	 */
	private void setzteDieLED() {
		int versenkt = meinSpieler.getHits();
		double prozentVersenkt = (100.0 / SchiffeVersenken.ANZAHLSCHIFFE) * versenkt;
		if (versenkt == 0) {
			led.setLEDStatus(Spielstatus.GRUEN);
		} else if (prozentVersenkt < 50) {
			led.setLEDStatus(Spielstatus.BLAU);
		} else if (prozentVersenkt >= 50 && versenkt < SchiffeVersenken.ANZAHLSCHIFFE) {
			led.setLEDStatus(Spielstatus.VIOLETT);
		} else {
			led.setLEDStatus(Spielstatus.ROT);
		}
	}

	/**
	 * Erstellt unseren eingenen Spieler mit der ID von Chord und unseren vorg�nger
	 * ID. Setzt auf die 100 Feldern 10 Schiffe auf zuf�hlige Positionen/Feldern.
	 */
	public void erstelleMeinenSpieler() {
		meinSpieler = new MeinSpieler(SchiffeVersenken.ANZAHLINTERVALLE, chord.getID(), chord.getPredecessorID());
		List<Integer> list = new ArrayList<Integer>();

		for (int i = 0; i < SchiffeVersenken.ANZAHLSCHIFFE; i++) {
			Random rand = new Random();
			int zahl = rand.nextInt(SchiffeVersenken.ANZAHLINTERVALLE);

			while (list.contains(zahl)) {
				zahl = rand.nextInt(SchiffeVersenken.ANZAHLINTERVALLE);
			}
			list.add(zahl);
		}
		meinSpieler.setzeSchiffe(list);
	}

}
