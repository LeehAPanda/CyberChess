package main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;

import javax.swing.JPanel;

import piece.*;

public class GamePanel extends JPanel implements Runnable {
	public static final int WIDTH = 1100;
	public static final int HEIGHT = 800;
	final int FPS = 60;
	Thread gameThread;
	Board board = new Board();
	MouseHandler mouse = new MouseHandler();
	
	// PIECES
	public static ArrayList<Piece> pieces = new ArrayList<>();
	public static ArrayList<Piece> simPieces = new ArrayList<>();
	ArrayList<Piece> promoPieces = new ArrayList<>();
	Piece activeP, checkingP;
	public static Piece castlingP;
	
	// PIECE COLORS
	public static final int BLUE = 0;
	public static final int RED = 1;
	int currentColor = BLUE;
	
	// Booleans
	boolean canMove;
	boolean validSquare;
	boolean promotion;
	boolean gameOver;
	boolean stalemate;
	
	public GamePanel() {
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setBackground(Color.black);
		addMouseMotionListener(mouse);
		addMouseListener(mouse);
		
		setPieces();
		copyPieces(pieces, simPieces);
	}
	
	public void initGame() {
		gameThread = new Thread(this);
		gameThread.start();
	}
	
	public void setPieces() {
		// Blue team
		pieces.add(new Pawn(BLUE, 0, 6));
		pieces.add(new Pawn(BLUE, 1, 6));
		pieces.add(new Pawn(BLUE, 2, 6));
		pieces.add(new Pawn(BLUE, 3, 6));
		pieces.add(new Pawn(BLUE, 4, 6));
		pieces.add(new Pawn(BLUE, 5, 6));
		pieces.add(new Pawn(BLUE, 6, 6));
		pieces.add(new Pawn(BLUE, 7, 6));
		pieces.add(new Rook(BLUE, 0, 7));
		pieces.add(new Rook(BLUE, 7, 7));
		pieces.add(new Knight(BLUE, 1, 7));
		pieces.add(new Knight(BLUE, 6, 7));
		pieces.add(new Bishop(BLUE, 2, 7));
		pieces.add(new Bishop(BLUE, 5, 7));
		pieces.add(new Queen(BLUE, 3, 7));
		pieces.add(new King(BLUE, 4, 7));
		
		// Red team
		pieces.add(new Pawn(RED, 0, 1));
		pieces.add(new Pawn(RED, 1, 1));
		pieces.add(new Pawn(RED, 2, 1));
		pieces.add(new Pawn(RED, 3, 1));
		pieces.add(new Pawn(RED, 4, 1));
		pieces.add(new Pawn(RED, 5, 1));
		pieces.add(new Pawn(RED, 6, 1));
		pieces.add(new Pawn(RED, 7, 1));
		pieces.add(new Rook(RED, 0, 0));
		pieces.add(new Rook(RED, 7, 0));
		pieces.add(new Knight(RED, 1, 0));
		pieces.add(new Knight(RED, 6, 0));
		pieces.add(new Bishop(RED, 2, 0));
		pieces.add(new Bishop(RED, 5, 0));
		pieces.add(new Queen(RED, 3, 0));
		pieces.add(new King(RED, 4, 0));
	}
	
	private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
		target.clear();
		for (int i = 0; i < source.size(); i++) {
			target.add(source.get(i));
		}
	}
	
	@Override
	public void run() {
		double drawInterval = 1000000000 / FPS;
		double delta = 0;
		long lastTime = System.nanoTime();
		long currentTime;
		
		while (gameThread != null) {
			currentTime = System.nanoTime();
			
			delta += (currentTime - lastTime) / drawInterval;
			lastTime = currentTime;
			
			if (delta >= 1) {
				update();
				repaint();
				delta--;
			}
		}
	}
	
	private void update() {
		if (promotion) {
			promoting();
		} else if (gameOver == false && stalemate == false) {
			// Mouse button is pressed
			if (mouse.pressed) {
				if (activeP == null) {
					// If the activeP is null, check if you can pick up a piece
					for (Piece piece : simPieces) {
						// if mouse is on current color's piece pick it up as activeP
						if (piece.color == currentColor && 
								piece.col == mouse.x / Board.SQUARE_SIZE && 
								piece.row == mouse.y / Board.SQUARE_SIZE) {
							activeP = piece;
						}
					}
				} else {
					// If the player holds a piece, simulate the move
					simulate();
				}
			}
			
			// Mouse button is released
			if (mouse.pressed == false) {
				if (activeP != null) {
					if (validSquare) {
						// Move is confirmed, update the piece list in case a piece has been captured and removed during simulation
						copyPieces(simPieces, pieces);
						activeP.updatePos();
						if (castlingP != null)
							castlingP.updatePos();
						
						if(isKingInCheck() && isCheckmate()) {
							gameOver = true;
						} else if (isStalemate()) {
							stalemate = true;
						} else { // game still playing
							if (canPromote())
								promotion = true; 
							else
								changePlayer();
						}
					} else {
						// Move is not valid so reset everything
						copyPieces(pieces, simPieces);
						activeP.resetPos();
						activeP = null;
					}
				}
			}
		}
	}
	
	private void simulate() {
		canMove = false;
		validSquare = false;
		
		// Reset piece list in every loop
		// Basically for restoring removed piece during simulation
		copyPieces(pieces, simPieces);
		
		// Reset castling piece's position
		if (castlingP != null) {
			castlingP.col = castlingP.preCol;
			castlingP.x = castlingP.getX(castlingP.col);
			castlingP = null;
		}
		
		// If a piece is being held, update its position
		activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
		activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
		activeP.col = activeP.getCol(activeP.x);
		activeP.row = activeP.getRow(activeP.y);
		
		// Check if piece is hovering over a valid square
		if (activeP.canMove(activeP.col, activeP.row)) {
			canMove = true;
			
			// If hitting an enemy piece, remove it from the list
			if (activeP.hittingP != null) {
				simPieces.remove(activeP.hittingP.getIndex());
			}
			
			checkCastling();
			
			if (isIllegal(activeP) == false && opponentCanCaptureKing() == false) {
				validSquare = true;
			}
		}
	}
	
	private boolean isIllegal(Piece king) {
		if (king.type == Type.KING) {
			for (Piece piece : simPieces) {
				if (piece != king && piece.color != king.color && piece.canMove(king.col, king.row))
					return true;
			}
		}
		
		return false;
	}
	
	private boolean opponentCanCaptureKing() {
		Piece king = getKing(false);
		
		for (Piece piece : simPieces) {
			if (piece.color != king.color && piece.canMove(king.col, king.row))
				return true;
		}
		
		return false;
	}
	
	private boolean isKingInCheck() {
		Piece king = getKing(true);
		
		if (activeP.canMove(king.col, king.row)) {
			checkingP = activeP;
			return true;
		} else {
			checkingP = null;
		}
		
		return false;
	}
	
	private Piece getKing(boolean opponent) {
		Piece king = null;
		
		for (Piece piece : simPieces) {
			if (opponent) {
				if (piece.type == Type.KING && piece.color != currentColor)
					king = piece;
			} else {
				if (piece.type == Type.KING && piece.color == currentColor)
					king = piece;
			}
		}
		
		return king;
	}
	
	private boolean isCheckmate() {
		Piece king = getKing(true);
		
		if (kingCanMove(king))
			return false;
		else {
			// You still have a chance
			// Check if you can block attack with a piece
			
			// Check pos of checking piece and king in check
			int colDiff = Math.abs(checkingP.col - king.col);
			int rowDiff = Math.abs(checkingP.row - king.row);
			
			if (colDiff == 0) {
				// Checking piece is attacking vertically
				if (checkingP.row < king.row) {
					// Checking piece is above king
					for (int row = checkingP.row; row < king.row; row++) {
						for (Piece piece : simPieces) {
							if (piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row)) {
								return false;
							}
						}
					}
				}
				if (checkingP.row > king.row) {
					// Checking piece is below king
					for (int row = checkingP.row; row > king.row; row--) {
						for (Piece piece : simPieces) {
							if (piece != king && piece.color != currentColor && piece.canMove(checkingP.col, row)) {
								return false;
							}
						}
					}
				}
			} else if (rowDiff == 0) {
				// Checking piece is attacking horizontally
				if (checkingP.col < king.col) {
					// Checking piece is to the left
					for (int col = checkingP.col; col < king.col; col++) {
						for (Piece piece : simPieces) {
							if (piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row)) {
								return false;
							}
						}
					}
				}
				if (checkingP.col < king.col) {
					// Checking piece is to the right
					for (int col = checkingP.col; col > king.col; col--) {
						for (Piece piece : simPieces) {
							if (piece != king && piece.color != currentColor && piece.canMove(col, checkingP.row)) {
								return false;
							}
						}
					}
				}
			} else if (colDiff == rowDiff) {
				// Checking piece is attacking diagonally
				if (checkingP.row < king.row) {
					if (checkingP.col < king.col) {
						// Checking piece is upper left
						for (int col = checkingP.col, row = checkingP.row; col < king.col; col++, row++) {
							for (Piece piece : simPieces) {
								if (piece != king && piece.color != currentColor && piece.canMove(col, row))
									return true;
							}
						}
					}
					if (checkingP.col > king.col) {
						// Checking piece is upper right
						for (int col = checkingP.col, row = checkingP.row; col > king.col; col--, row++) {
							for (Piece piece : simPieces) {
								if (piece != king && piece.color != currentColor && piece.canMove(col, row))
									return true;
							}
						}
					}
				}
				if (checkingP.row > king.row) {
					if (checkingP.col < king.col) {
						// Checking piece is lower left
						for (int col = checkingP.col, row = checkingP.row; col < king.col; col++, row--) {
							for (Piece piece : simPieces) {
								if (piece != king && piece.color != currentColor && piece.canMove(col, row))
									return true;
							}
						}
					}
					if (checkingP.col > king.col) {
						// Checking piece is lower right
						for (int col = checkingP.col, row = checkingP.row; col > king.col; col--, row--) {
							for (Piece piece : simPieces) {
								if (piece != king && piece.color != currentColor && piece.canMove(col, row))
									return true;
							}
						}
					}
				}
			}
		}
		
		return true;
	}
	
	private boolean kingCanMove(Piece king) {
		// Simulate if there a square where the king can move to safety
		if (isValidMove(king, -1, -1)) {return true;}
		if (isValidMove(king, 0, -1)) {return true;}
		if (isValidMove(king, 1, -1)) {return true;}
		if (isValidMove(king, -1, 0)) {return true;}
		if (isValidMove(king, 1, 0)) {return true;}
		if (isValidMove(king, -1, 1)) {return true;}
		if (isValidMove(king, 0, 1)) {return true;}
		if (isValidMove(king, 1, 1)) {return true;}
		
		return false;
	}
	
	private boolean isValidMove(Piece king, int colPlus, int rowPlus) {
		boolean isValidMove = false;
		
		// Update king's pos for a second
		king.col += colPlus;
		king.row += rowPlus;
		
		if (king.canMove(king.col,  king.row)) {
			if (king.hittingP != null)
				simPieces.remove(king.hittingP.getIndex());
			if (isIllegal(king) == false)
				isValidMove = true;
		}
		
		king.resetPos();
		copyPieces(pieces, simPieces);
		
		return isValidMove;
	}
	
	private boolean isStalemate() {
		int count = 0;
		// count number of pieces
		for (Piece piece : simPieces)
			if (piece.color != currentColor)
				count++;
		
		if (count == 1)
			if (kingCanMove(getKing(true)) == false)
				return true;
		
		return false;
	}
	
	private void checkCastling() {
		if (castlingP != null) {
			if (castlingP.col == 0) {
				castlingP.col += 3;
			} else if (castlingP.col == 7) {
				castlingP.col -= 2;
			}
			castlingP.x = castlingP.getX(castlingP.col);
		}
	}
	
	private void changePlayer() {
		if (currentColor == BLUE) {
			currentColor = RED; 
			// Reset red's two stepped status
			for (Piece piece : pieces) {
				if (piece.color == RED)
					piece.twoStepped = false;
			}
		} else {
			currentColor = BLUE;
			// Reset blue's two stepped status
			for (Piece piece : pieces) {
				if (piece.color == BLUE)
					piece.twoStepped = false;
			}
		}
		
		activeP = null;
	}
	
	private boolean canPromote() {
		if (activeP.type == Type.PAWN) {
			if (currentColor == BLUE && activeP.row == 0 || currentColor == RED && activeP.row == 7) {
				promoPieces.clear();
				promoPieces.add(new Rook(currentColor, 9, 2));
				promoPieces.add(new Knight(currentColor, 9, 3));
				promoPieces.add(new Bishop(currentColor, 9, 4));
				promoPieces.add(new Queen(currentColor, 9, 5));
				return true;
			}
		}
		
		return false;
	}
	
	private void promoting() {
		if (mouse.pressed) {
			for (Piece piece : promoPieces) {
				if (piece.col == mouse.x / Board.SQUARE_SIZE && piece.row == mouse.y / Board.SQUARE_SIZE) {
					switch (piece.type) {
					case ROOK: simPieces.add(new Rook(currentColor, activeP.col, activeP.row)); break;
					case KNIGHT: simPieces.add(new Knight(currentColor, activeP.col, activeP.row)); break;
					case BISHOP: simPieces.add(new Bishop(currentColor, activeP.col, activeP.row)); break;
					case QUEEN: simPieces.add(new Queen(currentColor, activeP.col, activeP.row)); break;
					default: break;
					}
					
					simPieces.remove(activeP.getIndex());
					copyPieces(simPieces, pieces);
					activeP = null;
					promotion = false;
					changePlayer();
				}
			}
		}
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		Graphics2D g2 = (Graphics2D)g;
		
		// Board
		board.draw(g2);
		
		// Pieces
		for (Piece p : simPieces) {
			p.draw(g2);
		}
		
		if (activeP != null) {
			if (canMove) {
				if (isIllegal(activeP) || opponentCanCaptureKing()) {
					g2.setColor(Color.red);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
					g2.fillRect(activeP.col * Board.SQUARE_SIZE, activeP.row * Board.SQUARE_SIZE, 
							Board.SQUARE_SIZE, Board.SQUARE_SIZE);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				} else {
					g2.setColor(Color.white);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
					g2.fillRect(activeP.col * Board.SQUARE_SIZE, activeP.row * Board.SQUARE_SIZE, 
							Board.SQUARE_SIZE, Board.SQUARE_SIZE);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				}
			}
			
			// Draw active piece in the end so it won't be hidden by board or colored square
			activeP.draw(g2);
		}
		
		// Status messages
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g2.setFont(new Font("Book Antiqua", Font.PLAIN, 40));
		g2.setColor(Color.white);
		
		if (promotion) {
			g2.drawString("Promote to: ", 840, 150);
			for (Piece piece : promoPieces) {
				g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), 
						Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
			}
		} else {
			if (currentColor == BLUE) {
				g2.drawString("Blue's turn!", 840, 550);
				if (checkingP != null && checkingP.color == RED) {
					g2.setColor(Color.red);
					g2.drawString("The King", 840, 650);
					g2.drawString("is in check!", 840, 700);
				}
			} else {
				g2.drawString("Red's turn!", 840, 250);
				if (checkingP != null && checkingP.color == BLUE) {
					g2.setColor(Color.red);
					g2.drawString("The King", 840, 100);
					g2.drawString("is in check!", 840, 150);
				}
			}
		}
		
		if (gameOver) {
			String s = "";
			if (currentColor == BLUE)
				s = "Blue has won!";
			else
				s = "Red has won!";
			g2.setFont(new Font("Arial", Font.PLAIN, 90));
			g2.setColor(Color.green);
			g2.drawString(s, 200, 420);
		}
		
		if (stalemate) {
			g2.setFont(new Font("Arial", Font.PLAIN, 90));
			g2.setColor(Color.lightGray);
			g2.drawString("Stalemate!", 200, 420);
		}
	}
}
