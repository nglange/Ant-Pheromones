package AntPheromones;


/**
  BatchModel is a non-gui extension of base Model
**/

import uchicago.src.sim.engine.BaseController;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimEvent;

public class BatchModel extends Model {

	////////////////////////////////////////////////////////////////////
	// main entry point
	public static void main( String[] args ) {

		BatchModel model = new BatchModel();

		// set the type of model class, this is necessary
		// so the parameters object knows whether or not
		// to do GUI related updates of panels, etc when a
		// parameter is changed
		model.setModelType("BatchModel");

		model.setCommandLineArgs(args);

		PlainController control = new PlainController();
		model.setController(control);
		control.setExitOnExit(true);
		control.setModel(model);
		model.addSimEventListener(control);
		if ( model.getRDebug() > 0 )
			System.out.printf("\n==> BatchModel main...about to startSimulation...\n");
		control.startSimulation();
	}

	// setup() -- BatchModel just does what the super class does.
	public void setup() {
		super.setup();
	}

	// begin()
	// ask the super class to do its building, then build a schedule.
	public void begin() {
		// set schedule to null so buildModel knows not to 
		// record changes ( changes are recorded if 
		// schedule != null ).  in buildSchedule() the 
		// schedule is allocated before the actual schedule is created.
		schedule = null;
		buildModel();     // the base Model class does this
		buildSchedule();
	}

	////////////////////////////////////////////////////////////////
	// buildSchedule
	// 
	// This may need to be changed, depending on what you want to
	// happen in a batch run (vs a GUI run).
	
	public void buildSchedule() {

		schedule = new Schedule(1);

		// schedule the current BatchModel's step() function
		// to execute every time step starting with time  step 0
		schedule.scheduleActionBeginning(0, this, "step");

		// schedule the current BatchModel's processEndOfRun() 
		// function to execute at the end of the Batch Run.
		// You need to specify the time to schedule it (instead 
		// of doing scheduleActionAtEnd() or it will just run forever
		schedule.scheduleActionAt(getStopT(), this, "processEndOfRun");
	}

	// processEndOfRun
	// we need this to tell it to stop running!
	public void processEndOfRun ( ) {
		super.processEndOfRun();
		this.fireEndSim();
	}
}

/////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////
// Why this class below?
//
// the reason we did that is because the repast "BatchController" had methods
// in it that started GUI stuff.  this caused problems when we ssh'd into
// another machine and run a job--when we tried to disconnect, the ssh
// session would stay hung until the job was finished because the job needed
// the X11-forwarding to be open to run.

class PlainController extends BaseController {
	private boolean exitonexit;

	public PlainController() {
		super();
		exitonexit = false;
	}

	public void startSimulation() {
		startSim();
	}

	public void stopSimulation() {
		stopSim();
	}
	
	public void exitSim(){ exitSim(); }

	public void pauseSimulation() {
		pauseSim();
	}

	public boolean isBatch() {
		return true;
	}

	protected void onTickCountUpdate() {}

	// this might not be necessary
	public void setExitOnExit(boolean in_Exitonexit) {
		exitonexit = in_Exitonexit;
	}

	public void simEventPerformed(SimEvent evt) {
		if(evt.getId() == SimEvent.STOP_EVENT) {
			stopSimulation();
		}
		else if(evt.getId() == SimEvent.END_EVENT) {
			if(exitonexit) {
				System.exit(0);
			}
		}
		else if(evt.getId() == SimEvent.PAUSE_EVENT) {
			pauseSimulation();
		}
	}

	// function added because it is required for repast 2.2
	public long getRunCount() {
		return 0;
	}

	// function added because it is required for repast 2.2
	public boolean isGUI() {
		return false;
	}
}
