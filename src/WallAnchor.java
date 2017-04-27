import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class WallAnchor {
    public static final int SIZE = 10;
    private int x, y;
    private int row, col;
    private Rectangle2D boundingRect;
    
    public WallAnchor(int x, int y, int row, int col) {
        this.x = x;
        this.y = y;
        this.row = row;
        this.col = col;
        boundingRect = new Rectangle2D(x, y, SIZE, SIZE);
    }
    
    public void render(GraphicsContext gc) {
        gc.setStroke(Color.HOTPINK);
        gc.setLineWidth(1);
        //gc.strokeRect(x - SIZE / 2, y - SIZE / 2, SIZE, SIZE);
        gc.strokeRect(x, y, SIZE, SIZE);
    }
    
    public Rectangle2D getBoundingRect() {
        return boundingRect;
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public int getRow() {
        return row;
    }
    
    public int getCol() {
        return col;
    }
}
