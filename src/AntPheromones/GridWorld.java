package  AntPheromones;

/**
* GridWorld
* Extend the Repast Object2DGrid class to implement
* a world of discrete cells with these movement rules:
* * An object can't move into an occupied cell
* * Bounded world (can't move off the edges)
* This provides these methods:
* a) moveObject 
*    check the "physics" and moves (or not) an object 
* b) addToRandomLocationInWorld -- a version
*    that works best for worlds with density < 0.8 or so.
* c) getOpenNeighborLocations ( int x, int y )
*    return an arraylist of points for open cells around x,y
*
* Note: This assumes the grid contains objects that implement ObjectsInGrid,  
* objects that implement set/get-X/Y and draw() methods.
*
* This implements only one constructor, given  x,y-size of the world
* and the Model using the world/
*
 */

import java.awt.Point;
import java.util.ArrayList;

import uchicago.src.sim.space.*;

public class GridWorld extends Object2DGrid {

	/** the Model that is using this world. */
	public Model theModel;

	public GridWorld(int sizeX, int sizeY, Model aModel) {
		super(sizeX, sizeY);
		theModel = aModel;
	}

	/**
	 * placeAtRandomLocationInWorld
	 * 
	 * @param ObjectInGrid object to be placed in world
	 * @return boolean placed or not place the object in a randomly selected
	 *         empty location tell the object to set its x,y accordingly
	 * 
	 *         NB: if it can't find a spot after X*Y trials, it gives up and
	 *         returns (added=) false. Works best for densities < 0.8 or so.
	 */

	public boolean placeAtRandomLocation(ObjectInGrid obj) {
		int x = 0, y = 0; // candidate x,y locations
		int maxTrials = xSize * ySize; // only try this many times
		int numTrials = 0; // haven't tried to look at any cells yet
		Boolean foundOpenCell = false; // not found empty cell (so not added)

		// find a randomly selected empty location, or give up if takes too long
		while (!foundOpenCell && numTrials < maxTrials) {
			x = Model.getUniformIntFromTo(0, xSize - 1);
			y = Model.getUniformIntFromTo(0, ySize - 1);
			if ( getObjectAt(x, y) == null ) // found empty cell!
				foundOpenCell = true;
			else
				// still need to look
				++numTrials; //  increment number of times we tried
		}

		if ( foundOpenCell ) { // if true, we can:
			putObjectAt( x, y, obj ); // put obj in world at that location
			obj.setX( x ); // tell the object where we put it
			obj.setY( y );
		}

		return foundOpenCell;
	}

	/**
	 * moveObject
	 * 
	 * @param ObjectInGrid
	 * @param int dX
	 * @param int dY
	 * @return boolean moved or not try to move object to the requested dx,dy .
	 *         note we must check to be sure the new location is "legal": - cell
	 *         is not off the edge of the world - cell is empty If move is ok:
	 *         move in world, tell object its new x,y, and return true if not
	 *         ok, return false
	 */
	public boolean moveObject(ObjectInGrid obj, int dX, int dY) {
		int currentX = obj.getX(); // where it is
		int currentY = obj.getY();

		int newX = obj.getX() + dX; // get location it wants to move to
		int newY = obj.getY() + dY;

		// first check to be sure new location is in the world!
		if ( newX < 0 || newY < 0 || newX >= xSize || newY >= ySize ) {
			return false;
		}

		// see if new cell is empty.
		if ( getObjectAt(newX, newY) != null ) {
			return false;
		}

		// its ok to move, so tell the world and the object about the move
		putObjectAt(currentX, currentY, null); // old cell empty now
		putObjectAt(newX, newY, obj); // obj in new cell now
		obj.setX(newX); // tell object its new location
		obj.setY(newY);
		return true;
	}


	/**
	 * moveObjectTo
	 * 
	 * @param ObjectInGrid
	 * @param int newX
	 * @param int newY
	 * @return boolean moved or not try to move object to the requested newX,newY .
	 *         note we must check to be sure the new location is "legal": - cell
	 *         is not off the edge of the world - cell is empty If move is ok:
	 *         move in world, tell object its new x,y, and return true if not
	 *         ok, return false
	 */
	public boolean moveObjectTo( ObjectInGrid obj, int newX, int newY) {
		
		// first check to be sure new location is in the world!
		if ( newX < 0 || newY < 0 || newX >= xSize || newY >= ySize ) {
			return false;
		}

		// see if new cell is empty.
		if ( getObjectAt(newX, newY) != null ) {
			return false;
		}

		// its ok to move, so tell the world and the object about the move
		putObjectAt( obj.getX(), obj.getY(), null); // old cell empty now
		putObjectAt(newX, newY, obj); // obj in new cell now
		obj.setX(newX); // tell object its new location
		obj.setY(newY);
		return true;
	}
	
	/**
	 * getOpenNeighborLocations
	 * 
	 * @param x
	 * @param y
	 * @return ArrayList<Point> returns a ArrayList of Points, one point for
	 *         each unoccupied cell in the Moore neighborhood around the given
	 *         x,y location. NB: includes a point for x,y itself if it is open!
	 */
	public ArrayList<Point> getOpenNeighborLocations(int x, int y) {
		ArrayList<Point> ptList = new ArrayList<Point>();

		// figure out the range of cells to search
		int minX = Math.max(0, x - 1);
		int maxX = Math.min(x + 1, xSize - 1);
		int minY = Math.max(0, y - 1);
		int maxY = Math.min(y + 1, ySize - 1);

		// search area for open cells
		for (int ty = minY; ty <= maxY; ++ty) {
			for (int tx = minX; tx <= maxX; ++tx) {
				if (getObjectAt(tx, ty) == null) { // its open
					Point p = new Point(tx, ty);
					ptList.add(p);
				}
			}
		}

		return ptList;
	}

	/**
	 * testGetOpenNeighborLocations
	 * 
	 * @param x
	 * @param y
	 *            for testing...call from Model buildModel, step, etc.
	 */
	public void testGetOpenNeighborLocations(int x, int y) {

		System.out.printf("--- Test:  open pts around %d,%d:", x, y);

		ArrayList<Point> nborPts = getOpenNeighborLocations(x, y);

		for (Point p : nborPts) {
			int px = (int) p.getX();
			int py = (int) p.getY();
			System.out.printf(" %d,%d", px, py);
		}
		System.out.printf("\n");

	}

}
