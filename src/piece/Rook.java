package piece;

import main.GamePanel;
import main.Type;

public class Rook extends Piece {

	public Rook(int color, int col, int row) {
		super(color, col, row);
		
		type = Type.ROOK;
		
		if (color == GamePanel.BLUE) {
			image = getImage("/b-rook");
		} else {
			image = getImage("/r-rook");
		}
	}

	public boolean canMove(int targetCol, int targetRow) {
		if (isWithinBoard(targetCol, targetRow) &&
				isSameSquare(targetCol, targetRow) == false) {
			// Rook can move as long as its col or row stays the same since it can move infinitely across one column or row
			// And it's not the same square it started on
			if (targetCol == preCol || targetRow == preRow) {
				if (isValidSquare(targetCol, targetRow) && 
						pieceIsOnStraightLine(targetCol, targetRow) == false) {
					return true;
				}
			}
		}
		
		return false;
	}
}
