package ttv;

public class Transaction {
	private static int ID = 0;

	synchronized public static int getID() {
		return ID;
	}

	synchronized public static void setID(int iD) {
		ID = iD;
	}
}
