import java.io.*;
import java.net.*;
import java.util.Date;

public class azureServer
implements Runnable, ConnectConstants {
	private int sessionNo = 1;

	public static void main (String[] args) {
		azureServer server = new azureServer();
		Thread newThread = new Thread(server);
		newThread.run();
	}

	@Override 
	public void run() {
		// Create a scene and place it in the stage
		try {
			ServerSocket serverSocket = new ServerSocket(8000);
			System.out.println(new Date() + ": Server started at socket 8000");

			while (true) {
				System.out.println(new Date() +
						": Wait for players to join session " + sessionNo);
				// Connect to player 1
				Socket player1 = serverSocket.accept();
				System.out.println(new Date() + ": Player 1 joined session "
						+ sessionNo);
				System.out.println("Player 1's IP address" +
						player1.getInetAddress().getHostAddress());
				// Notify that the player is Player 1
				new DataOutputStream(
						player1.getOutputStream()).writeInt(PLAYER1);

				// Connect to player 2
				Socket player2 = serverSocket.accept();
				System.out.println(new Date() +
						": Player 2 joined session " + sessionNo);
				System.out.println("Player 2's IP address" +
						player2.getInetAddress().getHostAddress());
				// Notify that the player is Player 2
				new DataOutputStream(
						player2.getOutputStream()).writeInt(PLAYER2);

				System.out.println(new Date() + ": Start a thread for session " + sessionNo++);

				// Launch a new thread
				new Thread(new HandleASession(player1, player2)).start();
			}
		}
		catch(IOException ex) {
			ex.printStackTrace();
		}
	}

	class HandleASession implements Runnable, ConnectConstants {
		private Socket player1;
		private Socket player2;
		// Create and initialize cells
		private char[][] cell = new char[ConnectConstants.TRACK_LENGTH][ConnectConstants.TRACKS];
		private DataInputStream fromPlayer1;
		private DataOutputStream toPlayer1;
		private DataInputStream fromPlayer2;
		private DataOutputStream toPlayer2;

		// Continue to play
		private boolean continueToPlay = true;
		/** Construct a thread */
		public HandleASession(Socket player1, Socket player2) {
			this.player1 = player1;
			this.player2 = player2;
			// Initialize cells
			for (int i = 0; i < ConnectConstants.TRACK_LENGTH; i++)
				for (int j = 0; j < ConnectConstants.TRACKS; j++)
					cell[i][j] = '.';
		}
		//game logic
		public void run() {
			try {
				// Create data input and output streams
				DataInputStream fromPlayer1 = new DataInputStream(
						player1.getInputStream());
				DataOutputStream toPlayer1 = new DataOutputStream(
						player1.getOutputStream());
				DataInputStream fromPlayer2 = new DataInputStream(
						player2.getInputStream());
				DataOutputStream toPlayer2 = new DataOutputStream(
						player2.getOutputStream());

				// Write anything to notify player 1 to start
				// This is just to let player 1 know to start
				toPlayer1.writeInt(1);

				//game loop
				while (true) {
					// Receive a move from player 1
					int row = fromPlayer1.readInt();
					int column = fromPlayer1.readInt();
					cell[row][column] = 'a';
					// Check if Player 1 wins
					if (isWon('a')) {
						toPlayer1.writeInt(PLAYER1_WON);
						toPlayer2.writeInt(PLAYER1_WON);
						sendMove(toPlayer2, row, column);
						break; // Break the loop
					}
					else if (isFull()) {
						// Check if all cells are filled
						toPlayer1.writeInt(DRAW);
						toPlayer2.writeInt(DRAW);
						sendMove(toPlayer2, row, column); 
						break;
					}
					else {
						// Notify player 2 to take the turn
						toPlayer2.writeInt(CONTINUE);
						// Send player 1's selected row and column to player 2
						sendMove(toPlayer2, row, column);
					}
					// Receive a move from Player 2
					row = fromPlayer2.readInt();
					column = fromPlayer2.readInt();
					cell[row][column] = 'b';
					// Check if Player 2 wins
					if (isWon('b')) {
						toPlayer1.writeInt(PLAYER2_WON);
						toPlayer2.writeInt(PLAYER2_WON);
						sendMove(toPlayer1, row, column);
						break;
					}
					else {
						// Notify player 1 to take the turn
						toPlayer1.writeInt(CONTINUE);
						// Send player 2's selected row and column to player 1
						sendMove(toPlayer1, row, column);
					}
				}
			}
			catch(IOException ex) {
				ex.printStackTrace();
			}
		}
		/** Send the move to other player */
		private void sendMove(DataOutputStream out, int row, int column)
				throws IOException {
			//send row
			out.writeInt(row);
			//send column
			out.writeInt(column);
		}

		/** Determine if the cells are all occupied */
		private boolean isFull() {
			for (int i = 0; i < ConnectConstants.TRACK_LENGTH; i++)
				for (int j = 0; j < ConnectConstants.TRACKS; j++)
					if (cell[i][j] == '.')
						return false; // At least one cell is not filled
			return true;
		}
		/** Determine if the player with the specified token wins */
		private boolean isWon(char token) {
			if (winVertical(token) || winHorizontal(token) || winDiaDown(token) || winDiaUp(token)) {
				return true;
			}
			else {
				return false;
			}
		}

		private boolean winDiaUp(char token) {
			int connected = 0;

			for (int row = 0; row < ConnectConstants.TRACK_LENGTH; row++) {
				for (int col = 0; col < ConnectConstants.TRACKS; col++) {
					if (cell[row][col] == token) {
						try {
							connected++;
							int  curRow = row + 1;
							int curCol = col + 1;
							while (connected < ConnectConstants.CONNECT_TO_WIN) {
								if (cell[curRow][curCol] == token) {
									connected++;
									curRow++;
									curCol++;
								}
								else {
									connected = 0;
									break;
								}
							}
							if (connected == ConnectConstants.CONNECT_TO_WIN) {
								return true;
							}
							else return false;
						}
						catch (Exception ex) {
						}
					}
				}
				connected = 0;
			}
			return false;
		}
		private boolean winDiaDown(char token) {
			int connected = 0;

			for (int row = 0; row < ConnectConstants.TRACK_LENGTH; row++) {
				for (int col = 0; col < ConnectConstants.TRACKS; col++) {
					if (cell[row][col] == token) {
						try {
							connected++;
							int  curRow = row - 1;
							int curCol = col + 1;
							while (connected < ConnectConstants.CONNECT_TO_WIN) {
								if (cell[curRow][curCol] == token) {
									connected++;
									curRow--;
									curCol++;
								}
								else {
									connected = 0;
									break;
								}
							}
							if (connected == ConnectConstants.CONNECT_TO_WIN) {
								return true;
							}
							else return false;
						}
						catch (Exception ex) {
						}
					}
				}
				connected = 0;
			}

			return false;
		}
		private boolean winHorizontal(char token) {

			int connected = 0;

			for (int row = 0; row < ConnectConstants.TRACK_LENGTH; row++) {
				for (int col = 0; col < ConnectConstants.TRACKS; col++) {
					if (cell[row][col] == token) {
						connected++;
						if (connected == ConnectConstants.CONNECT_TO_WIN) {
							return true;
						}
					}
					else {
						connected = 0;
					}
				}
				connected = 0;
			}

			return false;
		}
		private boolean winVertical(char token) {
			int connected = 0;

			for (int col = 0; col < ConnectConstants.TRACKS; col++) {
				for (int row = 0; row < ConnectConstants.TRACK_LENGTH; row++) {
					if (cell[row][col] == token) {
						connected++;
						if (connected == ConnectConstants.CONNECT_TO_WIN) {
							return true;
						}
					}
					else {
						connected = 0;
					}
				}
				connected = 0;
			}

			return false;
		}
	}
}