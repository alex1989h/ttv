package ttv;

import java.util.ArrayList;
import java.util.List;

import de.uniba.wiai.lspi.chord.data.ID;

public class Spieler {

	private int id;

	private static int ids = 0;

	protected int[] felder;

	private ID spielerID;

	private ID previousPlayerID;

	public Spieler(int intervalls, ID spielerID, ID previousPlayerID) {
		id = ids;
		ids++;
		this.felder = new int[intervalls];

		this.spielerID = spielerID;
		this.previousPlayerID = previousPlayerID;

		for (int i = 0; i < felder.length; i++) {
			felder[i] = 0;
		}
	}

	public int feldaktualisieren(int index, boolean hit) {
		if (hit) {
			felder[index] = 1;
		} else {
			felder[index] = -1;
		}

		return 0;
	}

	public int getHits() {
		int counter = 0;
		for (int i = 0; i < felder.length; i++) {
			if (felder[i] == 1) {
				counter++;
			}
		}
		return counter;
	}

	public List<Integer> getVerfuegbareFelder() {
		List<Integer> verfuegbareFelder = new ArrayList<Integer>();
		for (int i = 0; i < felder.length; i++) {
			if (felder[i] == 0) {
				verfuegbareFelder.add(i);
			}
		}

		return verfuegbareFelder;
	}

	public int[] getFelder() {
		return felder;
	}

	public void setFelder(int[] felder) {
		this.felder = felder;
	}

	public ID getSpielerID() {
		return spielerID;
	}

	public void setSpielerID(ID spielerID) {
		this.spielerID = spielerID;
	}

	public ID getPreviousPlayerID() {
		return previousPlayerID;
	}

	public void setPreviousPlayerID(ID previousPlayerID) {
		this.previousPlayerID = previousPlayerID;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((spielerID == null) ? 0 : spielerID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		// if (getClass() != obj.getClass())// Wegen der Vererbung
		// return false;
		Spieler other = (Spieler) obj;
		if (spielerID == null) {
			if (other.spielerID != null)
				return false;
		} else if (!spielerID.equals(other.spielerID))
			return false;
		return true;
	}

	@Override
	public String toString() {
		String myID = spielerID != null ? spielerID.toString() : null;
		String prevID = previousPlayerID != null ? previousPlayerID.toString() : null;
		myID = myID != null ? myID.substring(0, 2) + " ... " + myID.substring(myID.length() - 3,myID.length() - 1) : null;
		prevID = prevID != null ? prevID.substring(0, 2) + " ... " + prevID.substring(prevID.length() - 3, prevID.length() - 1) : null;
		return "[Spieler " + id + ", ID: " + myID + ", PrevID: " + prevID + ", Hits:" + String.format("%3d", getHits())
				+ ", Frei:" + String.format("%3d", getVerfuegbareFelder().size()) + "]";
	}
}
