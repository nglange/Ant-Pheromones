package AntPheromones;


import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import uchicago.src.sim.analysis.OpenSequenceGraph;
import uchicago.src.sim.analysis.Plot;
import uchicago.src.sim.analysis.Sequence;
import uchicago.src.sim.engine.AbstractGUIController;
import uchicago.src.sim.engine.Controller;
import uchicago.src.sim.gui.ColorMap;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.gui.Value2DDisplay;


public class GUIModel extends Model {

    private Object2DDisplay  worldDisplay;	// 2D Object lattice -> display (Repast)
	private Object2DDisplay  foodDisplay;  // 2D object lattics -> display (Repast) FOOD!
    private DisplaySurface	 dsurf;		    // display surface (RePast)
	public Value2DDisplay   pSpaceDisplay; // 2D Value lattice  -> display (Repast)

    public  OpenSequenceGraph		graph;
    public  OpenSequenceGraph		graphNbors;
    
    public  Plot  					avgDistGraph;
    public  double 					avgDistGraphRange = 200;
    public  double					avgDistGraphMaxX = 200;

	// colormap and scaling variables
	public static ColorMap		 pherColorMap;
	public static final int      colorMapSize = 64;
	public static final double   colorMapMax =  colorMapSize - 1.0;


	/////////////////////////////////////////////////////////////////////
	// setup
	//
	// this runs automatically when the model starts
	// and when you click the reload button, to "tear down" any 
	// existing display objects, and get ready to initialize 
	// them at the start of the next 'run'.
	//
	public void setup() {
	   	//System.out.printf( "==> GUIModel setup() called...\n" );

		super.setup();  // the super class does conceptual-model setup

		// NOTE: you may want to set these next two to 'true'
		// if you are on a windows machine.  that would tell repast
		// to by default send System.out and .err output to
		// a special repast output window.
		AbstractGUIController.CONSOLE_ERR = false;
		AbstractGUIController.CONSOLE_OUT = false;
		AbstractGUIController.UPDATE_PROBES = true;

		if ( dsurf != null ) dsurf.dispose();
		if ( graph != null )  graph.dispose();
		if ( graphNbors != null )  graphNbors.dispose();
		graph = null;
		dsurf = null;
		graphNbors = null;

		if ( avgDistGraph != null )
            avgDistGraph.dispose();
        avgDistGraph = null;
        
		// tell the Ant class we are in GUI mode.
		Ant.setupBugDrawing( this );
		Food.setupFoodDrawing( this );

		// init, setup and turn on the modelMinipulator stuff (in custom actions)
		modelManipulator.init();
		// one custom action to just print the bug list
		modelManipulator.addButton( "Print Bugs", 
			new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					System.out.printf( "printBugs...\n" );
					printBugs();
				}
			} 
	    );
		// another action:  reset all bugs probRandMove field 
		// using the parameters that define the distribution to use
		modelManipulator.addButton( "Reset Bug probRandMove", 
			new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					resetBugProbRandMove();
				}
			} 
	    );
		
		if ( rDebug > 0 )
			System.out.printf( "<== GUIModel setup() done.\n" );
	}

	/////////////////////////////////////////////////////////////////////
	// begin
	//
	// this runs when you click the "initialize" button
	// (the button with the single arrow that goes around in a circle)
	//
	public void begin()	{
		DMSG(1, "==> enter GUIModel-begin()" );
		buildModel();     			// the base model does this
		buildDisplay();
		buildSchedule();
		DMSG(1, "<== leave GUIModel-begin() done." );
	}

	/////////////////////////////////////////////////////////////////////
	// buildDisplay
	//
	// builds the display and display related things
	//
	public void buildDisplay() {
		if ( rDebug > 0 )
			System.out.printf( "==> GUIModel buildDisplay...\n" );

		// create the object we see as the 2D "world" on the screen 
		dsurf = new DisplaySurface( this, "Agent Display" );
		registerDisplaySurface( "Main Display", dsurf );

		// create the ColorMap for displayihg the amount of pheromone
		// as a degree of red-ness in the cells.
		pherColorMap = new ColorMap ();
		for (int i = 0; i < colorMapSize; i++) {
            // we specify the position i, and a fraction of each of RGB shade
			pherColorMap.mapColor ( i, i /  colorMapMax, 0, 0 );
		}

		// we are going to display bug color based on probRandMove
		setBugColorBasedOnProbRandMove();

		// enable the custom action(s)
		modelManipulator.setEnabled( true );
		
		// create mapper object, from 2D GridWorld to the display surface
		worldDisplay = new Object2DDisplay( world );
		
		// let's try this with food
		foodDisplay = new Object2DDisplay( world );

		// speed up display of ants -- just display them!
        worldDisplay.setObjectList( antList );

		foodDisplay.setObjectList( foodList );

		// create the link between the pSpace and the dsurf.
		// we have to tell it how to scale values to fit the colorMap.
		// We want the largest possible state (pheromone) value
		// to map into the colorMapMax-th entry in the colorMap array.
		// The Value2DDisplay does this mapping with the parameters m,c:
		//   int color index = (state / m) + c
		// so we want m = truncate( maxValue / maxColorIndex ) 
		pSpaceDisplay = new Value2DDisplay( pSpace, pherColorMap );
		int m = (int) (maxPher / colorMapMax);
		if ( rDebug > 1 )
			System.out.printf( "  -> pSpaceDisplay scaling m = %d.\n", m );
		pSpaceDisplay.setDisplayMapping( m, 0 );

		// add the pSpace to the surface first, so the bugs write over it.
        dsurf.addDisplayable( pSpaceDisplay, "Pheromone");
		// now add the display of agents
        dsurf.addDisplayableProbeable( worldDisplay, "Agents");
		dsurf.addDisplayableProbeable( foodDisplay, "Food");
        addSimEventListener( dsurf );  // link to the other parts of the repast gui

		dsurf.display();

		// set up sequence for pop size, etc, and graph to plot on
		class SeqAntPopSize implements Sequence {
			public double getSValue() {
				return getAntPopSize();
			}
		}
		class SeqAntPopAvgX implements Sequence {
			public double getSValue() {
				return getAntPopAvgX();
			}
		}
		class SeqAntPopAvgDistSource implements Sequence {
			public double getSValue() {
				return getAntAvgDistanceFromSource() * 10;
			}
		}
		class SeqDeathsPerStep implements Sequence {
			public double getSValue() {
				return deathsPerStep * 10;
			}
		}

		graph = new OpenSequenceGraph( "Population Stats", this );
		graph.setXRange( 0, 200 );
		graph.setYRange( 0, 60 );
		graph.setYIncrement( 20 );
		graph.setAxisTitles( "time", "Pop Size" );
		graph.addSequence("Ant Pop Size", new SeqAntPopSize(), Color.BLACK );
		graph.addSequence("Ant Avg X", new SeqAntPopAvgX(), Color.BLUE );
		graph.addSequence("Ant Avg Dist Source * 10", 
						  new SeqAntPopAvgDistSource(), Color.GREEN );
		graph.addSequence("Deaths/step*10", new SeqDeathsPerStep(), Color.ORANGE );
		graph.display();

		// a second graph!
		// lets create a graph for average bug number of neighbors at 1,2 cells away
		class AverageBugNbor1Count implements Sequence {
			public double getSValue() {
				return averageBugNbor1Count;
			}
		}
		class AverageBugNbor2Count implements Sequence {
			public double getSValue() {
				return averageBugNbor2Count;
			}
		}
		class AverageProbDieCenter implements Sequence {
			public double getSValue() {
				return avgProbDieCenter * 100;
			}
		}
		class AverageProbRandomMove implements Sequence {
			public double getSValue() {
				return avgProbRandomMove * 100;
			}
		}
		graphNbors = new OpenSequenceGraph( "More Stats", this );
		graphNbors.setXRange( 0, 200 );
		graphNbors.setYRange( 0, 10 );
		graphNbors.setYIncrement( 10 );
		graphNbors.setAxisTitles( "time", "Avg #Nbors" );
		graphNbors.addSequence("Avg. Bug 1-Nbor Count", new AverageBugNbor1Count());
		graphNbors.addSequence("Avg. Bug 2-Nbor Count", new AverageBugNbor2Count());
		graphNbors.addSequence("Avg. probDieCenter *100", new AverageProbDieCenter());
		graphNbors.addSequence("Avg. probRandomMove *100", new AverageProbRandomMove());

		graphNbors.display();
		
		createAvgDistGraph();
		
		if ( rDebug > 0 )
			System.out.printf( "<== GUIModel buildDisplay done.\n" );
	}
	
	/**
	 * create Plot graph to show how to use use a for-loop to
	 * add a number of lines determined at run time.
	 */
	public void createAvgDistGraph () {
        avgDistGraph = new Plot("Avg Ant Distance to Points", this);
        avgDistGraph.setXRange( 0, avgDistGraphMaxX );
        avgDistGraph.setXIncrement( 200 ); // doesnt seem to work!
        avgDistGraph.setYRange( 0, sizeY );
        avgDistGraph.setAxisTitles("Time", "Avg Ant Distance To Point");

        for ( int p = 0; p < keyPoints.size(); ++p ) {
        	Point pt = keyPoints.get(p);
            String axisTitle = String.format( "keyPoint %d (%.0f,%.0f)", 
              p, pt.getX(), pt.getY() );
            avgDistGraph.addLegend( p, axisTitle );
        }
        avgDistGraph.display();
	}


	/**
	// setBugColorBasedOnProbRandMove
	*/
	public void setBugColorBasedOnProbRandMove() {
		for ( Ant bug : antList )
			bug.setBugColorFromPRM();
	}
	
	////////////////////////////////////////////////////////////////
	// buildSchedule
	//
	// This builds the entire schedule, i.e., 
	//  - the base model step (calls stepReport)
	//  - display steps.

	public void buildSchedule() {

		if ( rDebug > 0 )
			System.out.printf( "==> GUIModel buildSchedule...\n" );

		// schedule the current GUIModel's step() function
		// to execute every time step starting with time step 0
		schedule.scheduleActionBeginning( 0, this, "step" );

		// schedule the current GUIModel's processEndOfRun() 
		// function to execute at the end of the run
		schedule.scheduleActionAtEnd( this, "processEndOfRun" );
	}


	///////////////////////////////////////////////////////////////////////////////
	// step
	//
	// executed each step of the model.
	// Ask the super-class to do its step() method,
	// and then this does display related activities.
	//
	public void step() {

		super.step();			// the model does whatever it does

		// add things after this for all displays (graphs, etc)
		dsurf.updateDisplay();
		graph.step();
		graphNbors.step();
		
		// plot avgDist for the keyPoints
		if ( getTickCount() > avgDistGraphMaxX ) {  //advance the window if needed
			double min = avgDistGraphMaxX;
			avgDistGraphMaxX += avgDistGraphRange; 
	        avgDistGraph.setXRange( min, avgDistGraphMaxX );
		}
		for ( int p = 0; p < keyPoints.size(); ++p ) {
			Point pt = keyPoints.get(p);
		   double d = calcAvgAntPopDistanceTo( (int) pt.getX(), (int) pt.getY() );
		   avgDistGraph.plotPoint( getTickCount(), d ,  p );
		}
		avgDistGraph.updateGraph();

	}

	// processEndOfRun
	// called once, at end of run.
	public void processEndOfRun ( ) {
		if ( rDebug > 0 )  
			System.out.printf("\n\n===== GUIModel processEndOfRun =====\n\n" );
		applyAnyStoredChanges();
		endReportFile();
		this.fireStopSim();
	}


/////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////
//   ****  NO NEEd TO CHANGE THE REST OF THIS  *****

	////////////////////////////////////////////////////////////////////
	// main entry point
	public static void main( String[] args ) {

		uchicago.src.sim.engine.SimInit init =
			new uchicago.src.sim.engine.SimInit();
		GUIModel model = new GUIModel();

		//System.out.printf("==> GUIMOdel main...\n" );

		// set the type of model class, this is necessary
		// so the parameters object knows whether or not
		// to do GUI related updates of panels,etc when a
		// parameter is changed
		model.setModelType("GUIModel");

        // Do this to set the Update Probes option to true in the
        // Repast Actions panel
        Controller.UPDATE_PROBES = true;

		model.setCommandLineArgs( args );
		init.loadModel( model, null, false ); // does setup()

		// this new function calls ProbeUtilities.updateProbePanels() and 
		// ProbeUtilities.updateModelProbePanel()
		model.updateAllProbePanels();

	}

}
