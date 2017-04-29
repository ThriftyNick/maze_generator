import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

public class MazeGraph {
    private HashMap<String, Vertex> verts;
    private boolean renderSolution;
    private LinkedList<Vertex> solutionPath;
    
    public MazeGraph() {        
        verts = new HashMap<String, Vertex>();
        renderSolution = false;
        solutionPath = new LinkedList<Vertex>();
    }
    
    public void render(GraphicsContext gc) {
        //render vertices
        if (verts != null && !verts.isEmpty()) {
            Set<String> ks = verts.keySet();
            for (String key : ks) {
                Vertex v = verts.get(key);
                v.render(gc);
            }
        }
        
        //render solution
        if (renderSolution) {
        	//draw edges between all vertices in solutionPath
        	gc.setStroke(Color.ORANGE);
        	//gc.setLineWidth(2);
        	Vertex start = solutionPath.getFirst();
        	for (Vertex end : solutionPath) {
        		gc.strokeLine(start.x, start.y, end.x, end.y);        		
        		start = end;
        	}
        }
    }

    public void addVert(double vertX, double vertY, int row, int col, PixelReader pReader) {
        //determine adjacencies
    	List<String> adjs = new ArrayList<String>();
    	if (col == -1) { //entrance
    		adjs.add(row + "_" + (col + 1));
    	}
    	else if (col == Game.GRID_SIZE * 2 - 3) { //exit
    		adjs.add(row + "_" + (col - 1));
    	}
    	else {
	        if (!pReader.getColor((int) vertX, (int) (vertY - Game.SPACING)).equals(Wall.UNSOLVED_COLOR)) {
	            adjs.add((row - 1) + "_" + col);
	        }
	        if (!pReader.getColor((int) vertX, (int) (vertY + Game.SPACING)).equals(Wall.UNSOLVED_COLOR)) {
	            adjs.add((row + 1) + "_" + col);
	        }
	        if (!pReader.getColor((int) (vertX - Game.SPACING), (int) vertY).equals(Wall.UNSOLVED_COLOR)) {
	            adjs.add(row + "_" + (col - 1));
	        }
	        if (!pReader.getColor((int) (vertX + Game.SPACING), (int) vertY).equals(Wall.UNSOLVED_COLOR)) {
	            adjs.add(row + "_" + (col + 1));
	        }
    	}
        
        Vertex theVertex = new Vertex(vertX, vertY, row, col, adjs);
        String key = theVertex.vNum;
        verts.put(key, theVertex);
    }
    
    private void addVert(Vertex v) {
        if (v != null) {
            String key = v.vNum;
            verts.put(key, v);
        }
    }
    
    public void connectGraph() {
        List<MazeGraph> subGraphs = getSubGraphs();             
        //We want one fully connected graph
        while (subGraphs.size() != 1) {
            //in each subgraph breach a wall
            
            for (MazeGraph mg : subGraphs) {
                Set<String> ks = mg.verts.keySet();
                for (String key : ks) {
                    Vertex vert = verts.get(key);
                    
                    //ensure point is betwixt anchor points (not inside of one)
                    //even number columns and rows
                    Point2D newVertLoc = null;
                    if (vert.row % 2 == 0 && vert.col % 2 == 0) {
                        newVertLoc = Game.seekNBreach(vert.x, vert.y);
                    }
                    
                    if (newVertLoc != null) {
                        //add new vertex to verts
                        double newVertX = newVertLoc.getX();
                        double newVertY = newVertLoc.getY();
                        int newVertRow = 0;
                        int newVertCol = 0;
                        //calculate row and col for new vert
                        if (newVertY < vert.y) { //above
                            newVertRow = vert.row - 1;
                            newVertCol = vert.col;
                        }
                        else if (newVertX > vert.x) {//right
                            newVertRow = vert.row;
                            newVertCol = vert.col + 1;
                        }
                        else if (newVertY > vert.y) {//below
                            newVertRow = vert.row + 1;
                            newVertCol = vert.col;
                        }
                        else if (newVertX < vert.x) {//left
                            newVertRow = vert.row;
                            newVertCol = vert.col - 1;
                        }
                        else {
                            throw new IllegalStateException("Unable to calculate newVert row/col");
                        }
                        addVert(newVertX, newVertY, newVertRow, newVertCol, Game.getPixelReader());
                        //introduce new vertex to its adjacencies
                        Vertex newVert = verts.get(newVertRow + "_" + newVertCol);
                        for (String neighborVert : newVert.adjacencies) {
                            Vertex neighbor = verts.get(neighborVert);
                            neighbor.addAdjacency(newVert);
                        }
                        break;
                    }
                }
            }
            
            subGraphs = getSubGraphs();
            //System.out.println(subGraphs.size());
        }
        
        
    }
    
    /**
     * 
     * @return List of fully connected subgraphs
     */
    private List<MazeGraph> getSubGraphs() {
        List<MazeGraph> subGraphs = new ArrayList<MazeGraph>();
        
        Set<String> ks = verts.keySet();
        Set<Vertex> members = new HashSet<Vertex>();
        for (String key : ks) {
            Vertex v = verts.get(key);
            if (!members.contains(v)) {
                MazeGraph subGraph = new MazeGraph();
                traverseGraph(v, members, subGraph);
                subGraphs.add(subGraph);
            }
        }
        
        return subGraphs;
    }
    
    /**
     * recursively traverses and builds subgraph
     * 
     */
    private void traverseGraph(Vertex localNode, Set<Vertex> mems, MazeGraph mg) {
        if (localNode == null) return;
        if (mems.contains(localNode)) return;
        mems.add(localNode);     
        mg.addVert(localNode);
            List<String> adjs = localNode.adjacencies;
            for (String adjacentNodeKey : adjs) {
                Vertex adjacentVert = verts.get(adjacentNodeKey);
                traverseGraph(adjacentVert, mems, mg);
            }
        
    }
    
    public Point2D getVertPos(int row, int col) {
    	String vKey = row + "_" + col;
    	Vertex v = verts.get(vKey);
    	if (v != null) {
    		return new Point2D(v.x, v.y);
    	}
    	
    	//throw new NoSuchElementException("Could not find vertex at row: " + row + " col: " + col);
    	return null;
    }
    
    private Vertex getStartVert() {
    	for (int row = 0; row < Game.GRID_SIZE * 2 - 3; row++) {
    		Vertex currentVert = verts.get(row + "_-1");
    		if (currentVert != null) {
    			return currentVert;
    		}
    	}
    	
    	throw new NoSuchElementException("Unable to find Start vertex");
    }
    
    private Vertex getExitVert() {
    	for (int row = 0; row < Game.GRID_SIZE * 2 - 3; row++) {
    		Vertex currentVert = verts.get(row + "_" + Game.EXIT_COLUMN);
    		if (currentVert != null) {
    			return currentVert;
    		}
    	}
    	
    	throw new NoSuchElementException("Unable to find Exit vertex.");
    }
    
    public void renderSolution(boolean b) {
    	renderSolution = b;    	
    }
    
    public void solveMaze() {
    	solutionPath = getSolutionPath();
    }
    
    private LinkedList<Vertex> getSolutionPath() {
    	LinkedList<Vertex> result = new LinkedList<Vertex>();
    	breadthFirstSearch();
    	Vertex current = getExitVert();    
    	
    	while (current.col != -1) {
    		result.addFirst(current);
    		current = current.bfsParent;
    	}
    	result.addFirst(current); //add start vertex
    	
		return result;
    }
    
    private void breadthFirstSearch() {
    	Queue<Vertex> theQueue = new LinkedList<Vertex>();
    	
    	//begin BFS with start vertex
    	Vertex startVert = getStartVert();
    	startVert.bfsIdentified = true;
    	theQueue.offer(startVert);
    	
    	while (!theQueue.isEmpty()) {
    		Vertex current = theQueue.remove();
    		
    		List<String> adjs = current.adjacencies;
            for (String adjacentNodeKey : adjs) {
                Vertex adjacentVert = verts.get(adjacentNodeKey);
                if (!adjacentVert.bfsIdentified) {
                	adjacentVert.bfsIdentified = true;
                	theQueue.offer(adjacentVert);
                	adjacentVert.bfsParent = current;
                }
            }
    	}
    }
    
    private class Vertex {
        private double x, y;
        private int row, col;
        private String vNum;
        private List<String> adjacencies;
        private Vertex bfsParent;
        private boolean bfsIdentified;
        
        public Vertex(double x, double y, int row, int col, List<String> adjs) {
            this.x = x;
            this.y = y;
            this.row = row;
            this.col = col;
            vNum = row + "_" + col; 
            adjacencies = adjs;
            bfsParent = null;
            bfsIdentified = false;
        }
        
        public void render(GraphicsContext gc) {
            gc.setFill(Color.CORAL);
            gc.fillOval(x-1, y-1, 2D, 2D);
        }
        
        public void addAdjacency(Vertex adjV) {
            adjacencies.add(adjV.vNum);
        }
        
    }
}
