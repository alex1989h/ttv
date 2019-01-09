package ttv;

import java.util.List;

import de.uniba.wiai.lspi.chord.data.ID;

public class MeinSpieler extends Spieler {

	public MeinSpieler(int intervalls, ID spielerID, ID previousPlayerID) {
		super(intervalls, spielerID, previousPlayerID);
		
	}
	
	public void setzeSchiffe(List<Integer> list) {
		
		for (int i = 0; i < list.size(); i++) {
			felder[list.get(i)]=1;
		}
	}
	
	public boolean angriff(int index) {
		if(felder[index] == 1) {
			return true;
		}else {
			return false;
		}
	}

}
