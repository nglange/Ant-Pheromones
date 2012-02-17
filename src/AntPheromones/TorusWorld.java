package  AntPheromones;

/**
TorusWorld

Extends GridWorld, a world of discrete cells,
so that moveObject now moves things around a torus.
Also provides x/y-norm methods to return torus-normalized
values for raw x,y values.

*/

import java.awt.Point;
import java.util.ArrayList;

public class TorusWorld extends GridWorld {

	public TorusWorld ( int sizeX, int sizeY, Model aModel ) {
		super( sizeX, sizeY, aModel );
	}


	/**
	 * moveObject
	 * @param ObjectInGrid
	 * @param int dX
	 * @param int dY
	 * @return boolean
	 * try to move object to the requested dx,dy .
	 * note we must check to be sure the new location is "legal", i.e., cell is empty 
	 * If move is ok:  move in world, and tell object its new x,y, and return true
	 * if not ok, return false
	 */
	public boolean moveObject ( ObjectInGrid obj, int dX, int dY ) {
		int currentX = obj.getX();	// where it is
		int currentY = obj.getY();

		int newX = xnorm( obj.getX() + dX );	// get location it wants to move to
		int newY = ynorm( obj.getY() + dY );

		// see if new cell is empty.
		if ( getObjectAt( newX, newY ) != null ) {
			return false;
		}

		// its ok to move, so tell the world and the object about the move
		putObjectAt( currentX, currentY, null );	// old cell empty now
		putObjectAt( newX, newY, obj );			   	// obj in new cell now
		obj.setX( newX );							// tell object its new location
		obj.setY( newY );
		return true;
	}

	/**
	 * getOpenNeighborLocations
	 * @param int x
	 * @param int y 
	 * @return a ArrayList of Points, one point for each unoccupied cell in the 
	 * Moore neighborhood around the given x,y location.
	 * Notes: includes point for x,y if its open.
	 *       returns torus normalized x,y values in the Points.
	 */

	public ArrayList<Point> getOpenNeighborLocations ( int x, int y ) {
		ArrayList<Point> ptList = new ArrayList<Point>();
		
		// figure out the range of cells to search
		int minX = x - 1;
		int maxX = x + 1;
		int minY = y - 1;
		int maxY = y + 1;

		// search area for open cells
		for ( int ty = minY; ty <= maxY; ++ty ) {
			for ( int tx = minX; tx <= maxX; ++tx ) {
				int txnorm = xnorm( tx );				// normalize for torus
				int tynorm = ynorm( ty );
				if ( getObjectAt( txnorm, tynorm ) == null ) { // its open
					Point p = new Point( txnorm, tynorm );
					ptList.add( p );
				}
			}
		}

		return ptList;
	}

	/**
	 * xnorm
	 * return torus-normalized values for raw x value.
	 * @param x
	 * @return int 
	 */
	public int xnorm ( int x ) { 
		if ( x > xSize -1 || x < 0 ) {
			while ( x < 0 ) x += xSize;
			return x % xSize; 
		}
		return x;
	}
	/**
	 * ynorm
	 * return torus-normalized values for raw y value.
	 * @param y
	 * @return int 
	 */
	public int ynorm ( int y ) { 
		if ( y > ySize -1 || y < 0 ) {
			while ( y < 0 ) y += ySize;
			return y % ySize; 
		}
		return y;
	}
}

