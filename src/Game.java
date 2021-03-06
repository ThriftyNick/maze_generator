import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

/*TODO:
 * Finish code optimization
 * 
 * add sound effect for win state
 * 
 * outline walls
 * 
 * Update on site
 * 
 * Make available on github
 */

/*
 * winSound.wav by maxmakessounds
 * https://www.freesound.org/people/maxmakessounds/sounds/353546/
 * 
 * startSound.wav by CosmicEmbers
 * https://www.freesound.org/people/CosmicEmbers/sounds/387351/
 * 
 * Creative Commons Attribution License
 * https://creativecommons.org/licenses/by/3.0/
 * 
 */

public class Game extends Application {
    public static final int CANVAS_WIDTH = 512;
    public static final int CANVAS_HEIGHT = 512;
    public static final int GRID_SIZE = 20;
    public static final int SPACING = 10;
    public static final int EXIT_COLUMN = GRID_SIZE * 2 - 3;
    private static final double WALL_DENSITY = 0.55;
    private static Canvas canvas;
    private static final AudioClip MAZE_BEGIN_SOUND = new AudioClip(Game.class.getResource("startSound.wav").toString());
    private static final AudioClip MAZE_COMPLETE_SOUND = new AudioClip(Game.class.getResource("winSound.wav").toString());
    private boolean rendered, congratulated;
    private WallAnchor[][] anchorPoints;
    private static List<Wall> walls;
    
    /*Location info for the entrance and exit
     * 0:entrance yPos, 1:entrance row, 2:exit yPos, 3:exit row
     * */
    private double[] doorwayInfo;
    
    private Player player;
    private MazeGraph mazeGraph;
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        //initialization
        primaryStage.setTitle("ThriftyNick's Maze Generator");
        Pane root = new Pane();
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        scene.getStylesheets().add("/customStyle.css");
        
        canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        root.getChildren().add(canvas);                      
        
        Button showSolution = new Button("Show Solution");
        Scene snapScene = new Scene(showSolution);
        snapScene.snapshot(null);       
        showSolution.setTranslateX((CANVAS_WIDTH / 2) - showSolution.getWidth() / 2);
        showSolution.setTranslateY(GRID_SIZE * 2 * SPACING + 50); 
        root.getChildren().add(showSolution);
        showSolution.setOnAction((ActionEvent e) -> {
        	if (showSolution.getText().contains("Show")) {
        		mazeGraph.renderSolution(true);
        		showSolution.setText("Hide Solution");  
        	}
        	else {
        		mazeGraph.renderSolution(false);
        		showSolution.setText("Show Solution");
        	}
        });
        
        
        Button reset = new Button("New Maze");
        snapScene = new Scene(reset);
        snapScene.snapshot(null);
        reset.setTranslateX((CANVAS_WIDTH / 2) - reset.getWidth() / 2);
        reset.setTranslateY(15);
        root.getChildren().add(reset);
        reset.setOnAction((ActionEvent e) -> {         		
        	generateMaze();
        	rendered = false; 
        	showSolution.setText("Show Solution"); 
        });        
        
        GraphicsContext gc = canvas.getGraphicsContext2D();
        
        player = new Player();
        
        ArrayList<String> input = new ArrayList<String>();
        
        scene.setOnKeyPressed(
                new EventHandler<KeyEvent>() {
                    @Override
                    public void handle(KeyEvent e) {
                        String code = e.getCode().toString();
                        if (!input.contains(code))
                            input.add(code);
                    }     
                });
        
        scene.setOnKeyReleased(
                new EventHandler<KeyEvent>() {
                    @Override
                    public void handle(KeyEvent e) {
                        String code = e.getCode().toString();
                        input.remove(code);
                    }                  
                });
        
        anchorPoints = new WallAnchor[GRID_SIZE][GRID_SIZE];
        generateMaze();
        rendered = false;
        congratulated = false;
        
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1);
        
        TimeValue lastNanoTime = new TimeValue(System.nanoTime());
        //main game loop
        new AnimationTimer() {
            public void handle(long currentNanoTime) {                    
                /*//calculate time since last update
                double elapsedTime = (currentNanoTime - lastNanoTime.value) / 1000000000.0;
                lastNanoTime.value = currentNanoTime;*/
                     
                /*GAME LOGIC*/               
                //Player Controls
                int destRow = -2;
                int destCol = -2;
                if (input.contains("LEFT") || input.contains("A")) {
                	destRow = player.getRow();
                	destCol = player.getCol() - 1;
                	input.remove("LEFT");
                	input.remove("A");                               
                }
                if (input.contains("RIGHT") || input.contains("D")) {
                	destRow = player.getRow();
                	destCol = player.getCol() + 1;
                	input.remove("RIGHT");
                	input.remove("D");
                }
                if (input.contains("UP") || input.contains("W")) {
                	destRow = player.getRow() - 1;
                	destCol = player.getCol();
                	input.remove("UP");
                	input.remove("W");              
                }
                if (input.contains("DOWN") || input.contains("S")) {
                	destRow = player.getRow() + 1;
                	destCol = player.getCol();
                	input.remove("DOWN");
                	input.remove("S");
                }
                
                if (destRow != -2 && destCol != -2) { //if new move
	                Point2D destinationPos = mazeGraph.getVertPos(destRow, destCol);
	            	if (destinationPos != null) {
	            		player.setPosition(destinationPos.getX(), destinationPos.getY());
	            		player.setGraphLoc(destRow, destCol);
	            	}
                }
                
                //check for win
                if (player.getCol() == EXIT_COLUMN && !congratulated) {
                	congratulate();
                	congratulated = true;
                }
                /*END GAME LOGIC*/
                
                /*RENDERING*/
                gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);                
                
                //render anchor points               
                /*for (int row = 0; row < GRID_SIZE; row++) {
                	for (int col = 0; col < GRID_SIZE; col++) {
                		anchorPoints[row][col].render(gc);
                	}
                }*/
                
                //render walls
                for (Wall w : walls) {
                    w.render(gc);
                }                                
                                                                
                //render graph
                if (rendered) {
                    mazeGraph.render(gc);
                }
                
                //initMazeGraph depends on pixel data so is not called until
                //walls have been rendered
                if (!rendered) {
                    initMazeGraph();
                    outlineWalls();
                    rendered = true;
                    congratulated = false;
                }
                
                //render player
                if (player != null) {
                    player.render(gc);
                }
                /*END RENDERING*/
                              
            }
            
        }.start();
        
        primaryStage.show();
     
    }
    
    private void generateMaze() {
        initWallAnchors(GRID_SIZE);        
        setWalls();    
    }
    
    private void initWallAnchors(int size) {
        int startX = CANVAS_WIDTH / 2 - (((WallAnchor.SIZE + SPACING) * GRID_SIZE) - SPACING) / 2;
        int startY = 50;
        int xPos = startX;
        int yPos = startY;
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                anchorPoints[row][col] = new WallAnchor(xPos, yPos, row, col);
                xPos += SPACING * 2;
            }
            xPos = startX;
            yPos += SPACING * 2;
        }
        
    }

    private void setWalls() {
        walls = new ArrayList<Wall>();                 
        
        //top & bottom walls
        for (int col = 0; col < GRID_SIZE - 1; col++) {
            //top wall
        	Wall w = new Wall(anchorPoints[0][col], anchorPoints[0][col + 1]);
            w.markBorderWall();
            walls.add(w);            
            
            //bottom wall
            w = new Wall(anchorPoints[GRID_SIZE - 1][col], anchorPoints[GRID_SIZE - 1][col + 1]);
            w.markBorderWall();
            walls.add(w);                       
        }
        
        //left & right walls
        for (int row = 0; row < GRID_SIZE - 1; row++) {
        	//left wall
            Wall w = new Wall(anchorPoints[row][0], anchorPoints[row + 1][0]);
            w.markBorderWall();
            walls.add(w);            
            
            //right wall
            w = new Wall(anchorPoints[row][GRID_SIZE - 1], anchorPoints[row + 1][GRID_SIZE - 1]);
            w.markBorderWall();
            walls.add(w);
        }
        
        //two possible configurations for entry/exit locations
        int entryLoc = 0;
        int exitLoc = 0;
        int configuration = flipCoin();
        int loc1 = (int) (Math.random() * (GRID_SIZE - 1) / 2);
        int loc2 = (int) (Math.random() * (GRID_SIZE - 1) / 2 + GRID_SIZE / 2);
        if (configuration == 0) {
            entryLoc = loc1;
            exitLoc = loc2;
        }
        else {
            entryLoc = loc2;
            exitLoc = loc1;
        }
        
        if (entryLoc == 0) entryLoc = 1;
        if (entryLoc == GRID_SIZE - 1) entryLoc = GRID_SIZE - 2;
        if (exitLoc == 0) exitLoc = 1;
        if (exitLoc == GRID_SIZE - 1) exitLoc = GRID_SIZE - 2;
        
        //initialize doorwayInfo which will contain location information about the entrance and exit
        doorwayInfo = new double[4];
        
        //create Entry point
        Wall doorway = null;
        for (Wall w : walls) {
            if (w.getP1().getRow() == entryLoc && w.getP1().getCol() == 0) {
                doorway = w;
                break;
            }
        }
        if (doorway != null) {
            breachWall(doorway);
            doorwayInfo[0] = doorway.getP1().getY() + SPACING + (SPACING / 2); //set entrance vertex yPos
            doorwayInfo[1] = doorway.getP1().getRow() * 2; //set entrance vertex row            
        }
       
        //create Exit point
        doorway = null;
        for (Wall w : walls) {
            if (w.getP1().getRow() == exitLoc && w.getP1().getCol() == GRID_SIZE - 1) {
                doorway = w;
                break;
            }
        }
        if (doorway != null) {
            breachWall(doorway);
        	doorwayInfo[2] = doorway.getP1().getY() + SPACING + (SPACING / 2); //set exit vertex yPos
        	doorwayInfo[3] = doorway.getP1().getRow() * 2; //set exit vertex row        	        	        	
        }
        
        //place inner walls
        Set<Wall> placedWalls = new HashSet<Wall>();
        //Takes two anchor points for one full wall position
        double totalAvailablePositions = 2 * ((GRID_SIZE - 2) * (GRID_SIZE - 2));
        double percentComplete = 0.0;
        while (percentComplete < WALL_DENSITY) {
        	//horizontal wall
        	Wall w = null;
        	do {
                int row = (int) (Math.random() * (GRID_SIZE - 2) + 1);
                int col = (int) (Math.random() * (GRID_SIZE - 1));
                w = new Wall(anchorPoints[row][col], anchorPoints[row][col + 1]);
            } while (placedWalls.contains(w));
            placedWalls.add(w);
            walls.add(w);
            
            //vertical wall
            w = null;
            do {
                int row = (int) (Math.random() * (GRID_SIZE - 1));
                int col = (int) (Math.random() * (GRID_SIZE - 2) + 1);
                w = new Wall(anchorPoints[row][col], anchorPoints[row + 1][col]);
            } while (placedWalls.contains(w));
            placedWalls.add(w);
            walls.add(w);
            percentComplete = placedWalls.size() / totalAvailablePositions;
        }
        
    }
    
    /**
     * <pre>: walls have been rendered
     * <post>: mazeGraph contains vertices of all traversable positions.  
     * 	mazeGraph is fully connected.
     * 	player is positioned at the start position.
     */
    private void initMazeGraph() {
        mazeGraph = new MazeGraph();        
        PixelReader reader = getPixelReader();
        
        double startX = anchorPoints[0][0].getX() + WallAnchor.SIZE / 2;
        double startY = anchorPoints[0][0].getY() + WallAnchor.SIZE / 2;
        startX += SPACING;
        startY += SPACING;
        double xPos = startX;
        double yPos = startY;
        
        //add entrance vertex
        mazeGraph.addVert(xPos - SPACING, doorwayInfo[0], (int) doorwayInfo[1], -1, null);
        //place player at entrance vertex
        Point2D entryPos = mazeGraph.getVertPos((int) doorwayInfo[1], -1);
        player.setPosition(entryPos.getX(), entryPos.getY());
        player.setGraphLoc((int) doorwayInfo[1], -1);
        
        //add inner vertices
        for (int row = 0; row < GRID_SIZE + GRID_SIZE - 1 - 2; row++) {
            for (int col = 0; col < GRID_SIZE + GRID_SIZE - 1 - 2; col++) {
            	if (!reader.getColor((int) xPos, (int) yPos).equals(Wall.UNSOLVED_COLOR)) {                
            		mazeGraph.addVert(xPos, yPos, row, col, reader);
                }                
                xPos += SPACING;
            }
            xPos = startX;
            yPos += SPACING;            
        }
        
        //add exit vertex
        mazeGraph.addVert(startX + (SPACING * GRID_SIZE * 2) - (SPACING * 3), doorwayInfo[2], (int) doorwayInfo[3], EXIT_COLUMN, null);
        
        //eliminate closed off areas (disconnected subgraphs) and make one fully
        //connected graph
        mazeGraph.connectGraph();
        
        //compute shortest solution path        
        mazeGraph.solveMaze(); 
        
        MAZE_BEGIN_SOUND.play();
    } 
    
    private void outlineWalls() {    	
    	for (int row = 0; row < EXIT_COLUMN; row++) {
    		for (int col = -1; col <= EXIT_COLUMN; col++) {
    			Point2D pt = mazeGraph.getVertPos(row, col);
    			if (pt != null) {
    				Wall[] surroundingWalls = detectSurroundingWalls(pt);
    				for (int direction = 0; direction < 4; direction++) {
    					if (surroundingWalls[direction] != null) {
    						Wall w = surroundingWalls[direction];
    						Line l = null;
    						double lineSpacing = SPACING / 2.0;
    						switch (direction) {
    							case 0: l = new Line(pt.getX() - lineSpacing, pt.getY() - lineSpacing, pt.getX() + lineSpacing, pt.getY() - lineSpacing);
    									break;
    							case 1:	l = new Line(pt.getX() + lineSpacing, pt.getY() - lineSpacing, pt.getX() + lineSpacing, pt.getY() + lineSpacing);
    									break;
    							case 2: l = new Line(pt.getX() - lineSpacing, pt.getY() + lineSpacing, pt.getX() + lineSpacing, pt.getY() + lineSpacing);
    									break;
    							case 3:	l = new Line(pt.getX() - lineSpacing, pt.getY() - lineSpacing, pt.getX() - lineSpacing, pt.getY() + lineSpacing);
    									break;
    						}
    					
    						w.addOutlineEdge(l);    						    						    					
    					}
    				}    				
    			}
    		}
    	}
    }
    
    public static Wall[] detectSurroundingWalls(Point2D loc) {
    	Wall[] surroundingWalls = {null, null, null, null};
    	Point2D above = new Point2D(loc.getX(), loc.getY() - SPACING);
        Point2D right = new Point2D(loc.getX() + SPACING, loc.getY());
        Point2D below = new Point2D(loc.getX(), loc.getY() + SPACING);
        Point2D left = new Point2D(loc.getX() - SPACING, loc.getY());
        for (Wall w : walls) {
            Rectangle2D wallRect = w.getBoundingRect();
            if (wallRect.contains(above)) {
            	surroundingWalls[0] = w;            	
            }
            else if (wallRect.contains(right)) {
            	surroundingWalls[1] = w;            	
            }
            else if (wallRect.contains(below)) {
            	surroundingWalls[2] = w;            	
            }
            else if (wallRect.contains(left)) {
            	surroundingWalls[3] = w;            	
            }
        }
    	return surroundingWalls;
    }
    
    /**
     * Knocks down a wall
     * @param wall the wall to breach
     * @return Center point of wall which was breached
     */
    public static Point2D breachWall(Wall wall) {
    	Rectangle2D r = wall.getBoundingRect();
    	double centerX = r.getMaxX() - r.getWidth() / 2.0;
    	double centerY = r.getMaxY() - r.getHeight() / 2.0;
    	Point2D centerPoint = new Point2D(centerX, centerY);
        walls.remove(wall);
        walls.add(new Wall(wall.getP1(), null));
        walls.add(new Wall(wall.getP2(), null));
        return centerPoint;
    }

    public static PixelReader getPixelReader() {
        WritableImage img = canvas.snapshot(null, null);
        return img.getPixelReader();
    }

    public static int flipCoin() {
        double coin = Math.random();
        if (coin < 0.51) {
            return 0;
        }
        return 1;
    }
    
    /**
     * Called when exit has been reached
     */
    private void congratulate() {    	
    	/*
    	 * Winning Message:
    	 * change walls color
    	 * play sound effect
    	 * flash next maze button
    	 */
    	Wall.setWallColor(1);
    	MAZE_COMPLETE_SOUND.play();
    }

    private static class TimeValue {
        private float value;
        
        public TimeValue(float v) {
            value = v;
        }
    }

}
