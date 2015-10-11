import java.io.*;
import java.net.*;
import java.util.Random;

import com.sun.prism.paint.RadialGradient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.LinearGradientBuilder;
import javafx.scene.paint.RadialGradientBuilder;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class ConnectClient extends Application
implements ConnectConstants {
	private boolean myTurn = false;
	// Indicate the token for the player
	private char myToken = '.';
	// Indicate the token for the other player
	private char otherToken = '.';

	// Create and initialize cells
	Cell[][] cell = new Cell[ConnectConstants.TRACK_LENGTH][ConnectConstants.TRACKS];

	// Create and initialize a ntoification bar
	private Label lblStatus = new Label();

	// Indicate selected row and column by the current move
	private int rowSelected;
	private int columnSelected;

	// Input and output streams from/to server
	private DataInputStream fromServer;
	private DataOutputStream toServer;

	private boolean continueToPlay = true;
	private boolean waiting = true;

	//random value for dropping blocks
	Random random = new Random(ConnectConstants.RANDOM_SEED);

	// Host name or ip
	private String host = "localhost"; //Or through azure @ 104.208.38.138 shoutout Microsoft

	@Override // Override the start method in the Application class
	public void start(Stage primaryStage) {
		setUserAgentStylesheet(STYLESHEET_MODENA);

		GridPane pane = new GridPane();
		pane.setStyle("-fx-background-color: linear-gradient(to bottom right, yellow, gold)");
		for (int i = 0; i < ConnectConstants.TRACK_LENGTH; i++)
			for (int j = 0; j < ConnectConstants.TRACKS; j++){
				pane.add(cell[i][j] = new Cell(i, j), j, i);
				cell[i][j].setToken('.');
			}
		BorderPane borderPane = new BorderPane();
		borderPane.setCenter(pane);
		borderPane.setBottom(lblStatus);
		// Create a scene and place it in the stage
		Scene scene = new Scene(borderPane, 50 * ConnectConstants.TRACKS, 50 * ConnectConstants.TRACK_LENGTH + 10);
		primaryStage.setTitle("Connect Them"); // Set the stage title
		primaryStage.setScene(scene); // Place the scene in the stage
		primaryStage.show(); // Display the stage
		// Connect to the server
		connectToServer();
	}

	private void connectToServer() {
		try {
			// Create a socket to connect to the server
			Socket socket = new Socket(host, 8000);
			// Create an input stream to receive data from the server
			fromServer = new DataInputStream(socket.getInputStream());
			// Create an output stream to send data to the server
			toServer = new DataOutputStream(socket.getOutputStream());
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		// Control the game on a separate thread
		new Thread(() -> {
			try {
				// Get notification from the server
				int player = fromServer.readInt();
				// get player number
				if (player == PLAYER1) {
					myToken = 'a';
					otherToken = 'b';
					Platform.runLater(() -> {
						lblStatus.setText("Waiting for Player 2 to join.");
					});

					// Receive startup notification from the server
					fromServer.readInt(); // Whatever read is ignored
					// The other player has joined

					Platform.runLater(() ->
					lblStatus.setText("Player 2 has joined. You go first."));
					// It is your turn
					myTurn = true;
				}
				else if (player == PLAYER2) {
					myToken = 'b';
					otherToken = 'a';
					Platform.runLater(() -> {
						lblStatus.setText("Waiting for Player 1 to move.");
					});
				}
				// Continue to play
				while (continueToPlay) {
					if (player == PLAYER1) {
						waitForPlayerAction(); // Wait for player 1 to move
						sendMove(); // Send the move to the server
						receiveInfoFromServer(); // Receive info from the server
					}
					else if (player == PLAYER2) {
						receiveInfoFromServer(); // Receive info from the server
						waitForPlayerAction(); // Wait for player 2 to move
						sendMove(); // Send player 2's move to the server
					}

					//Removed because apparently being awesome is against TCP protocol
					//dropARando();
				}
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}).start();
	}

	private void dropARando() throws IOException {
		System.out.println("Dropping brick");
		//drop a block into a random column
		int col = random.nextInt(ConnectConstants.TRACKS);
		int row = 0;

		for (int i = 0; i < ConnectConstants.TRACK_LENGTH; i++) {
			row = i;
		}

		cell[row][col].token = 'c';

		//Doesn't like this
		cell[row][col].repaint();
		waiting = false;

	}

	/** Wait for the player to mark a cell */
	private void waitForPlayerAction() throws InterruptedException {
		while (waiting) {
			Thread.sleep(100);
		}
		waiting = true;
	}

	/** Send this player's move to the server */
	private void sendMove() throws IOException {
		toServer.writeInt(rowSelected);
		toServer.writeInt(columnSelected);
	}

	private void paintBlack(){
		for (int row = 0; row < ConnectConstants.TRACK_LENGTH; row++) {
			for (int col = 0; col < ConnectConstants.TRACKS; col++) {
				if (cell[row][col].token == '.') {
					cell[row][col].setToken('c');
				}
			}
		}
	}

	/** Receive info from the server */
	private void receiveInfoFromServer() throws IOException {
		// Receive game status
		int status = fromServer.readInt();
		if (status == PLAYER1_WON) {
			// Player 1 won, stop playing
			continueToPlay = false;
			if (myToken == 'a') {
				Platform.runLater(() -> {
					lblStatus.setText("I won!");
					lblStatus.setTextFill(Color.RED);
				});
			}
			else if (myToken == 'b') {
				Platform.runLater(() -> {
					lblStatus.setText("Player 1 has won!");
					lblStatus.setTextFill(Color.RED);
				});
				receiveMove();
			}

			//paintBlack();
		}
		else if (status == PLAYER2_WON) {
			// Player 2 won, stop playing
			continueToPlay = false;
			if (myToken == 'b') {
				Platform.runLater(() -> {
					lblStatus.setText("I won!");
					lblStatus.setTextFill(Color.BLUE);
				});
			}
			else if (myToken == 'a') {
				Platform.runLater(() -> {
					lblStatus.setText("Player 2 has won!");
					lblStatus.setTextFill(Color.BLUE);
				});
				receiveMove();
			}

			//paintBlack();
		}
		else if (status == DRAW) {
			// No winner, game is over
			continueToPlay = false;
			Platform.runLater(() ->
			lblStatus.setText("Game is over, no winner!"));
			if (myToken == 'b') {
				receiveMove();
			}
		}
		else {
			receiveMove();
			Platform.runLater(() -> lblStatus.setText("My turn"));
			myTurn = true; // It is my turn
		}
	}
	private void receiveMove() throws IOException {
		// Get the other player's move
		int row = fromServer.readInt();
		int column = fromServer.readInt();
		Platform.runLater(() -> cell[row][column].setToken(otherToken));
	}

	// An inner class for a cell
	public class Cell extends Pane {
		// Indicate the row and column of this cell in the board
		private int row;
		private int column;
		// Token used for this cell
		private char token = '.';
		public Cell(int row, int column) {
			this.row = row;
			this.column = column;
			this.setPrefSize(2000, 2000); // What happens without this?
			setStyle("-fx-border-color: black"); // Set cell's border
			this.setOnMouseClicked(e -> handleMouseClick());
		}
		public boolean isColFull(int col) {
			char token = cell[0][col].getToken();
			if (token != '.') {
				return true;
			}
			else return false;
		}
		/** Return token */
		public char getToken() {
			return token;
		}
		/** Set a new token */
		public void setToken(char c) {
			token = c;
			repaint();
		}
		protected void repaint() {
			Ellipse ellipse;

			if (token == 'a') {
				javafx.scene.paint.RadialGradient gradient1 = RadialGradientBuilder.create()
						.focusAngle(0)
						.focusDistance(.1)
						.centerX(80)
						.centerY(45)
						.radius(120)
						.proportional(false)
						.cycleMethod(CycleMethod.NO_CYCLE)
						.stops(new Stop(0, Color.RED), new Stop(1, Color.DARKRED))
						.build();
				ellipse = new Ellipse(this.getWidth() / 2,
						this.getHeight() / 2, this.getWidth() / 2 - 10,
						this.getHeight() / 2 - 10);
				ellipse.centerXProperty().bind(
						this.widthProperty().divide(2));
				ellipse.centerYProperty().bind(
						this.heightProperty().divide(2));
				ellipse.radiusXProperty().bind(
						this.widthProperty().divide(2).subtract(10));
				ellipse.radiusYProperty().bind(
						this.heightProperty().divide(2).subtract(10));
				ellipse.setStroke(Color.BLACK);
				//ellipse.setFill(Color.RED);
				ellipse.setFill(gradient1);

				this.getChildren().add(ellipse);

				ellipse = new Ellipse(this.getWidth() / 2,
						this.getHeight() / 2, this.getWidth() / 2 - 15,
						this.getHeight() / 2 - 15);
				ellipse.centerXProperty().bind(
						this.widthProperty().divide(2));
				ellipse.centerYProperty().bind(
						this.heightProperty().divide(2));
				ellipse.radiusXProperty().bind(
						this.widthProperty().divide(2).subtract(20));
				ellipse.radiusYProperty().bind(
						this.heightProperty().divide(2).subtract(20));
				ellipse.setStroke(Color.BLACK);
				//ellipse.setFill(Color.RED);
				ellipse.setFill(gradient1);

				this.getChildren().add(ellipse);
			}
			else if (token == 'b') {
				javafx.scene.paint.RadialGradient gradient1 = RadialGradientBuilder.create()
						.focusAngle(0)
						.focusDistance(.1)
						.centerX(80)
						.centerY(45)
						.radius(120)
						.proportional(false)
						.cycleMethod(CycleMethod.NO_CYCLE)
						.stops(new Stop(0, Color.BLUE), new Stop(1, Color.DARKBLUE))
						.build();

				ellipse = new Ellipse(this.getWidth() / 2,
						this.getHeight() / 2, this.getWidth() / 2 - 10,
						this.getHeight() / 2 - 10);
				ellipse.centerXProperty().bind(
						this.widthProperty().divide(2));
				ellipse.centerYProperty().bind(
						this.heightProperty().divide(2));
				ellipse.radiusXProperty().bind(
						this.widthProperty().divide(2).subtract(10));
				ellipse.radiusYProperty().bind(
						this.heightProperty().divide(2).subtract(10));
				ellipse.setStroke(Color.BLACK);
				//ellipse.setFill(Color.BLUE);
				ellipse.setFill(gradient1);


				this.getChildren().add(ellipse);

				ellipse = new Ellipse(this.getWidth() / 2,
						this.getHeight() / 2, this.getWidth() / 2 - 15,
						this.getHeight() / 2 - 15);
				ellipse.centerXProperty().bind(
						this.widthProperty().divide(2));
				ellipse.centerYProperty().bind(
						this.heightProperty().divide(2));
				ellipse.radiusXProperty().bind(
						this.widthProperty().divide(2).subtract(20));
				ellipse.radiusYProperty().bind(
						this.heightProperty().divide(2).subtract(20));
				ellipse.setStroke(Color.BLACK);
				//ellipse.setFill(Color.BLUE);
				ellipse.setFill(gradient1);

				this.getChildren().add(ellipse);
			}
			else if (token == 'c') {

				Rectangle rect = new Rectangle(this.getWidth(), this.getHeight(), this.getWidth()/2, this.getHeight()/2);
				rect.setStroke(Color.BLACK);
				rect.setFill(Color.BLACK);

				this.getChildren().add(rect);
			}
			else if (token == '.') {

				ellipse = new Ellipse(this.getWidth() / 2,
						this.getHeight() / 2, this.getWidth() / 2 - 10,
						this.getHeight() / 2 - 10);
				ellipse.centerXProperty().bind(
						this.widthProperty().divide(2));
				ellipse.centerYProperty().bind(
						this.heightProperty().divide(2));
				ellipse.radiusXProperty().bind(
						this.widthProperty().divide(2).subtract(10));
				ellipse.radiusYProperty().bind(
						this.heightProperty().divide(2).subtract(10));
				ellipse.setStroke(Color.BLACK);
				//ellipse.setFill(Color.rgb(255, 255, 204));
				javafx.scene.paint.RadialGradient gradient1 = RadialGradientBuilder.create()
						.focusAngle(0)
						.focusDistance(.1)
						.centerX(80)
						.centerY(45)
						.radius(120)
						.proportional(false)
						.cycleMethod(CycleMethod.NO_CYCLE)
						.stops(new Stop(0, Color.RED), new Stop(1, Color.BLACK))
						.build();

				ellipse.setFill(gradient1);
				this.getChildren().add(ellipse);
			}
		}

		/* Handle a mouse click event */
		private void handleMouseClick() {
			// If cell is not occupied and the player has the turn
			if (myTurn && !isColFull(column)) {
				myTurn = false;
				rowSelected = row;
				columnSelected = column;
				for (int i = 0; i < ConnectConstants.TRACK_LENGTH; i++) {
					if (cell[i][column].getToken() == '.') {
						rowSelected = i;
					}
				}
				cell[rowSelected][columnSelected].setToken(myToken); // Set the player's token in the cell
				lblStatus.setText("Waiting for the other player to move");
				waiting = false; // Just completed a successful move
			}
		}
	}

	public static void main (String[] args) {
		Application.launch(args);
	}
}