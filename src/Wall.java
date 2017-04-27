import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Wall {
    private WallAnchor p1, p2;
    private Rectangle2D boundingRect;
    private boolean isHalfWall;
    private boolean isBorderWall;
    private static Color currentColor;
    public static final Color UNSOLVED_COLOR = Color.ORANGERED;
    public static final Color SOLVED_COLOR = Color.GREENYELLOW;
    
    public Wall(WallAnchor p1, WallAnchor p2) {
        this.p1 = p1;
        this.p2 = p2;
        
        if (p2 == null) {
            //p2 = p1;
            isHalfWall = true;
            boundingRect = new Rectangle2D(p1.getX(), p1.getY(), WallAnchor.SIZE, WallAnchor.SIZE);
        }
        else {
            isHalfWall = false;
            boundingRect = setBoundingRect();            
        } 
        currentColor = UNSOLVED_COLOR;
        //System.out.println(currentColor);
    }
    
    private Rectangle2D setBoundingRect() {
        int rx, ry;
        int width, height;
        
        //set rx and width
        width = WallAnchor.SIZE * 2 + Game.SPACING;
        if (p1.getX() < p2.getX()) {
            rx = p1.getX();     
        }
        else if (p2.getX() < p1.getX()){
            rx = p2.getX();
        }
        else {
            //assert p1.getX() == p2.getX()
            rx = p1.getX();
            width = WallAnchor.SIZE;
        }
        
        //set ry and height
        height = WallAnchor.SIZE * 2 + Game.SPACING;
        if (p1.getY() < p2.getY()) {
            ry = p1.getY();
        }
        else if (p2.getY() < p1.getY()) {
            ry = p2.getY();
        }
        else {
            //assert p1.getY() == p2.getY()
            ry = p1.getY();
            height = WallAnchor.SIZE;
        }
        
        return new Rectangle2D(rx, ry, width, height); 
    }
    
    public Rectangle2D getBoundingRect() {
        return boundingRect;
    }
    
    public void render(GraphicsContext gc) {
        gc.setStroke(Color.BLACK);
        gc.setFill(currentColor);
        gc.setLineWidth(2);
        
        gc.fillRect(boundingRect.getMinX(), boundingRect.getMinY(), boundingRect.getWidth(), boundingRect.getHeight());
        //gc.strokeRect(boundingRect.getMinX(), boundingRect.getMinY(), boundingRect.getWidth(), boundingRect.getHeight());
        
    }
    
    public boolean isHalfWall() {
        return isHalfWall;
    }
    
    public boolean isBorderWall() {
        return isBorderWall;
    }
    
    public void markBorderWall() {
        isBorderWall = true;
    }
    
    public WallAnchor getP1() {
        return p1;
    }
    
    public WallAnchor getP2() {
        return p2;
    }
    
    public static void mazeSolvedColor() {
    	currentColor = SOLVED_COLOR;
    }
    
    public static void mazeUnsolvedColor() {
    	currentColor = UNSOLVED_COLOR;
    }
    
    public static Color getCurrentColor() {
    	return currentColor;
    }
    
    public boolean equals(Object other) {
        Wall otherWall = (Wall) other;
        if (this.isHalfWall  || otherWall.isHalfWall) {
            return super.equals(other);
        }
        else {
            return this.p1.equals(otherWall.p1) && this.p2.equals(otherWall.p2);
        }
    }
    
    public int hashCode() {
        if (p2 != null)
            return p1.hashCode() ^ p2.hashCode();
        return super.hashCode();
    }
    
    public String toString() {
        return "@" + hashCode();
    }
}
