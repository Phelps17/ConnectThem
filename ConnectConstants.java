import java.awt.Color;

public interface ConnectConstants {
	public static int PLAYER1 = 1; // Indicate player 1
	public static int PLAYER2 = 2; // Indicate player 2
	public static int COMPUTER_PLAYER = 8; // Indicate player 3
	public static int PLAYER1_WON = 1; // Indicate player 1 won
	public static int PLAYER2_WON = 2; // Indicate player 2 won
	public static int DRAW = 3; // Indicate a draw
	public static int CONTINUE = 4; // Indicate to continue

	//PREFERENCE VALUES
	public static final int PLAYERS_PLAYING = 2; //can be 2 to 4
	public final static int TRACK_LENGTH = 12; //can be any int
	public final static int TRACKS = 12; //can be any int
	public final static int CONNECT_TO_WIN = 5;
	public static final Color[] PLAYER_COLORS = {Color.RED, Color.BLUE, Color.YELLOW, Color.GREEN};

	//OPERATING CONFIG VALUES
	public static final char[] PLAYER_IDS = { 'a', 'b', 'c', 'd'};
	public final static int VALID = 1;
	public final static int INVALID = 0;
	public final static char EMPTY_SLOT = '.';
	public final static int STATUS_PLAYON = 0;
	public final static int STATUS_SOMEONE_WON = 2;
	public final static int STATUS_BOARD_FULL = 3;
	public final static int RANDOM_SEED = 2151996;
}