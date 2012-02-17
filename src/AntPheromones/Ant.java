package AntPheromones;

/**
Ant.java 

A simple Ant object that can move in a GridWorld (a Repast Object2DGrid).
Note it implements the Repast Drawable interface, so that
it can be displayed via the Repast Object2DDisplay gui object,
and a ObjectInGrid, so it can be placed and move in that world.

*/

import java.awt.Color;
import java.awt.Point;
import java.util.Vector;
import java.util.ArrayList;
import java.awt.BasicStroke;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import uchicago.src.sim.space.Diffuse2D;
import uchicago.src.sim.gui.ColorMap;


public class Ant implements ObjectInGrid, Drawable {
	// "class" variables -- one value for all instances 
    public  static int          	nextId = 0; // to give each an id
	public  static TorusWorld    	world;  	// where the agents live
	public  static Model		   	model;      // the model "in charge"
	public  static Diffuse2D		pSpace;	    // where the pheromone is stored
	public  static GUIModel		    guiModel = null;   // the gui model "in charge"
    // we'll use this to draw a border around the bugs' cells (the f means float)
    public  static BasicStroke      bugEdgeStroke = new BasicStroke( 1.0f );
	// randomMoveMethod -- how to pick that random cell to move to
	//  0 -> pick uniform random from list
	//  1 -> biased way to do it!
	public static int randomMoveMethod = 0;
	public static double maxDistanceToCenter;

	// we use this to have Ant shades indicated their probRandMove
	public static ColorMap		 probRandMoveColorMap;
	public static final int      colorMapSize = 64;
	public static final double   colorMapMax =  colorMapSize - 1.0;
	
	// instance variables  
	public int 	   		id;			// unique id number for each ant instance
	public int 			x, y;		// cache the ant's x,y location
	public double		weight;		// ant's weight
	public int			age;		// ant's age in days
	public boolean		live;		// is it live or dead
	public  double		probRandMove; // probability it'll  move randomly
	public  double		probDieCenter; // probability it'll die at center
	public Color		myColor;    // color of this agent

	// an Ant constructor
	// note it assigns ID values in sequence as ant's are created.
	public Ant ( ) {
		id = nextId++;
		x = 0;		y = 0;
		weight = 0.0; age = 0;  
		live = true;
		probRandMove = 0.0;
		setInitialColor();
	}

	public Ant ( double wt ) {  // required weight parameters
		id = nextId++;
		x = 0;		y = 0;
		age = 0;
		weight = wt; 
		live = true; 
		probRandMove = 0.0;
		setInitialColor();
	}

	public void setInitialColor () {  // set agents initial color
		myColor = Color.blue;
	}


	////////////////////////////////////////////////////////////////////////////
	// setters and getters
	//
	public int getId() {  return id; }
	public int getX() { return x; }
	public void setX( int i ) { x = i; }
	public int getY() { return y; }
	public void setY( int i ) { y = i; }

	public double getWeight() { return weight; }
	public void setWeight( double w ) { weight = w; }
	public int getAge() { return age; }
	public void setAge( int a ) { age = a; }
	public boolean getLive() { return live; }
	public void setLive( boolean l ) { live = l; }


	public double getProbRandMove() { return probRandMove; }
	// Note: setProbRandMove also sets the color!
	public void setProbRandMove( double d ) { 
		if ( d < 0.0 || d > 1.0 ) 
			System.err.printf("\nsetProbRandMove(%.3f): out of [0,1]!\n", d );
		else {
			probRandMove = d; 
			if ( guiModel != null ) {
				setBugColorFromPRM();
			}
		}
	}
	public double getProbDieCenter() {
		return probDieCenter;
	}

	public void setProbDieCenter(double probDieCenter) {
		this.probDieCenter = probDieCenter;
	}

	/**
	// setBugColorFromPRM - set color from from probRandMove
	// Note we map from [0,0.5] to full range of colors
	// anything over 0.5 is the same color - black!
	 */
	public void setBugColorFromPRM () {
	   	int i =  (int) Math.round( 2.0 * probRandMove * colorMapMax );
		i = (int) Math.min( i, colorMapMax );
	   	myColor = probRandMoveColorMap.getColor( i );
		if ( model.getRDebug() > 2 )
			System.out.printf( "setBugColorFromPRM: probRandMove %.3f -> i %d.\n",
							   probRandMove, i );
	}
	
	/**
	// setupBugDrawing
	// set the guiModel address, which we can test to see if in GUI mode
	// also set up a color map to map bug's probRandMove values to greenness
	// NOTE: more color at low end of map!
	 * 
	 * @param m
	 */
	public static void setupBugDrawing ( GUIModel m ) {
		guiModel = m;
		probRandMoveColorMap = new ColorMap ();
		for ( int i = 0; i < colorMapSize; i++ ) {
			// this one makes degress of blueness
			//probRandMoveColorMap.mapColor ( (int) colorMapMax - i, 
			// 0.0, 0.0, i / colorMapMax );

			// this one makes degrees of greenness
			probRandMoveColorMap.mapColor ( (int) colorMapMax - i, 
											0.0, i / colorMapMax, 0.0 );
		}
	}
	
	
	// note these are class (static) methods, to set class (static) variables
	public static void setWorld( TorusWorld w ) {	world = w; }
	public static void setModel( Model m ) { model = m; }
	public static void resetNextId() { nextId = 0; }  // call when we reset the model
	public static void setPSpace( Diffuse2D space ) {
		pSpace = space;
	}
	/**
	 * @return the maxDistanceToCenter
	 */
	public static double getMaxDistanceToCenter() {
		return maxDistanceToCenter;
	}

	/**
	 * @param maxDistanceToCenter the maxDistanceToCenter to set
	 */
	public static void setMaxDistanceToCenter(double maxDistanceToCenter) {
		Ant.maxDistanceToCenter = maxDistanceToCenter;
	}

	public static void setGUIModel( GUIModel m ) { guiModel = m; }
	public static void setRandomMoveMethod  ( int r ) { 
		randomMoveMethod = r;
	}


	// getDistanceToSource
	// just ask the model how far i am from the source.
	public double getDistanceToSource ( ) {
		return model.calcDistanceToSource( this );
	}

	// return the number of neighbors the bug has, at distance d
	@SuppressWarnings("unchecked")
	public int getNumberOfNeighbors( int d ) {
		Vector<Object> nbors = (Vector<Object>)world.getMooreNeighbors( x, y, d, d, false );
		return nbors.size();
	}

	/**
	// step
	// Top level definition of what the ant can do each time its activiated.
	// Currently it does is
	// - with probRandMove, move to random open adjacent cell
	// - check one randomly selected open neighbor cell, and
	//   if its got more pheromone than where the ant is now, move there
	// - otherwise call makeRandomMove() method.
	*/
	public boolean step () {
		int neighborhoodRadius = 1; 			 // how far do i look.
		boolean moved = false;  // not moved this step so far

		if ( model.getRDebug() > 0 ) 
			System.err.printf( "   --Ant-step() for id=%d at x,y=%d,%d.\n",
						   id, x, y );

		amIStillAlive();
		
		if ( !live )  // if it died
			return live;    	 // return its live value (false!)
		
		// see if we move randomly...
		if ( probRandMove > Model.getUniformDoubleFromTo( 0.0, 1.0 ) ) {
			Point pt = findRandomOpenNeighborCell ( );
            if ( pt != null ) { 
                moved = world.moveObjectTo( this, (int) pt.getX(), (int) pt.getY() );
				if ( moved && model.getRDebug() > 1 ) 
					System.out.printf("     -- moved to random cell %d,%d.\n",x,y);
			}
		}

		else {  
			moved = tryMoveToMorePheromone( neighborhoodRadius );
		}

		if ( !moved )
			moved = makeRandomMove();

		if ( model.getRDebug() > 1 ) 
			System.err.printf("      Ant.step() done. moved = %b.\n", moved );

		return live;  // should be true!
	}
	
	/**
	 * amIStillAlive
	// calculate bugs chance of dying, based on probDieCenter
	// and distance to source:
	//          pdc * ( 1 - d/D )
	// where D = maxDistanceToCenter, and d = this bugs distance to source!
	// NOTE: maxDistanceToCenter is really distance from 0,0 to source!
	 * 
	 * if died, set live field false.
	 * @return live value
	 */
	public boolean amIStillAlive ( )  {
		double dtc = model.calcDistanceToSource( this );
		double probDie = probDieCenter * ( 1.0 - ( dtc / maxDistanceToCenter ));
		
		if ( probDie > Model.getUniformDoubleFromTo( 0.0, 1.0 ) ) {
			live = false;
		}
		return live;
	}
	
	public boolean tryMoveToMorePheromone ( int radius ) {
		boolean moved = false;
		// try to move to cell with more pheromone
		Point pt =  findMostPheromoneOpenNeighborCell ( radius );
		if ( pt != null ) {  // we got one!
			int newX = (int) pt.getX();
			int newY = (int) pt.getY();
			if ( pSpace.getValueAt( x, y ) < pSpace.getValueAt( newX, newY ) ) {
				moved = world.moveObjectTo( this, newX, newY );
				if ( moved &&  model.getRDebug() > 1 )
					System.out.printf("     -- moved to better cell at %d,%d.\n",
									  x, y );
			}
		}	
		return moved;
	}
	
	/**
	// findRandomOpenNeighborCell
	// pick random open Moore neighbor cell and return its 
	// coordinates in a Point,
	// Return null if no open cell found.
	// 
	// NOTE how it picks depends on randomMoveMethod:
	// 0 - pick at random from open neighbors
	// 1 - pick the first found of the  open neighbors (biased!)
	*/
	public Point findRandomOpenNeighborCell () {
		Point 		openP = null;
		ArrayList<Point> openPts = world.getOpenNeighborLocations( x, y );

		// now pick a random open point, if any to pick from
		if ( openPts.size() > 0 ) {
			if ( randomMoveMethod == 0 ) {
				openP = openPts.get( Model.getUniformIntFromTo( 0, openPts.size()-1 ) );
			}
			else   // randomMoveMethod = 1 is a biased way to do it!
				openP = openPts.get( 0 );
		}

		return openP;
	}
	

	/**
	// findMostPheromoneOpenNeighborCell
	// look at open neighbor cells (within d), return Point with coordinates
	// of cell with most pheromone.
	// Return null if no open cell found.
	// NB: This assumes world is a TorusWorld, so we normalize x,y values.
	// NB: pick from ties at random
	*/
	
	public Point findMostPheromoneOpenNeighborCell ( int d ) {
		ArrayList<Point> openPts = new ArrayList<Point>();  // list of open points
		int minx = x - d;
		int maxx = x + d;
		int miny = y - d;
		int maxy = y + d;

		// look at neighbor cells, get a list of those with the most Pher.
		// (the list could be just 1 cell of course.)
		double mostP = -1;  // most P seen so far;  anything is better than -1!
		for ( int tx = minx; tx <= maxx; ++tx ) {
			int txnorm = world.xnorm( tx );
			for ( int ty = miny; ty <= maxy; ++ty ) {
				int tynorm = world.ynorm( ty );
				if ( world.getObjectAt( txnorm, tynorm ) == null ) { // its open 
					double p = pSpace.getValueAt( txnorm, tynorm );
					if ( p >= mostP ) { 		// best or better than best so far
						if ( p > mostP ) { 		// new best!
							openPts.clear();  	// get rid of any previous best
							mostP = p;          // set to new best value
						}
						openPts.add( new Point( tx, ty ) );   // add to list
					}
				}
			}
		}

		// now pick a random open best point, if any to pick from
		int    numOpenPts = openPts.size();
		Point  openP = null;  				// the one we return
		if ( numOpenPts == 1 )				// only one to pick!
			openP = openPts.get( 0 );
		else if ( numOpenPts > 1 )      	// pick one at random
			openP = openPts.get( Model.getUniformIntFromTo( 0, numOpenPts-1 ) );

		if ( model.getRDebug() > 2 ) {
			if ( openP == null ) 
				System.out.printf( "     -> no open neighbor with more pheromone.\n" );
			else
				System.out.printf( "     -> new best@%.0f,%.0f (ph=%.3f vs here=%.3f)\n",
		   			   openP.getX(), openP.getY(), mostP, pSpace.getValueAt( x, y ) );
		}

		return openP;
	}	
	
	/**
	// makeRandomMove
	// - get random dx,dy values in -1,0,1 as possible move to make
	// - asks the world to move it by the selected amounts (dx,dy)
	// - checks for error conditions and print appropriate messages.
	//
	// NB: Only tries 1K times! 
	// For Demo, make the ant with ID=0 move to the left (dx=1).
	*/
	public boolean makeRandomMove () {
		boolean moved = false;  // not moved this step so far

		// get a random amount to move into dx,dy, but not to own cell!
		int dx = 0, dy = 0, nmTrials = 0, maxTrials = 1024;
		while ( dx == 0 && dy == 0 && nmTrials < maxTrials ) {
			dx = Model.getUniformIntFromTo( -1, 1 );  // dx = { -1,0,1 } 
			dy = Model.getUniformIntFromTo( -1, 1 );  // dy = { -1,0,1 }
			++nmTrials;
		}

		if ( model.getRDebug() > 0 ) 
			System.err.printf( "   - try to move dx,dy = %d,%d\n", dx, dy );

		moved = world.moveObject( this, dx, dy );

		if ( !moved ) {
		    if ( model.getRDebug() > 0 )
				System.out.printf( "   *** obj tried to move offworld or to occupied cell.\n" );
		}
		else { // moved ok!
			moved = true;
		}

		return moved;
	}

	/**
	// incrementAge
	// add given amount to age field
	// return new age value.
	*/
	public int incrementAge ( int incAge ) {
		age = age + incAge;   		
		return age;
	}

	/**
	// printSelf
	// print ant fields to System.out.
	//
	*/
	public void printSelf ( ) {
		System.out.printf( " - Ant %2d (x,y=%d,%d; live=%b) age %2d, wt %5.2f, prm %.2f, prdc %.2f\n",
						   id, x, y, live, age, weight, probRandMove, probDieCenter );
	}

	/**
	// draw 
	// we implement Drawable interface, so we need this method
	// so that the ant can draw itself when requested  (by the GUI display).
	*/
    public void draw( SimGraphics g ) {
	   	g.drawFastRoundRect( myColor );
        g.drawRectBorder( bugEdgeStroke, Color.yellow );
    }


}
