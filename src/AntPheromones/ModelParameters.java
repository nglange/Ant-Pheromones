package AntPheromones;

/**
 *  ModelParameters
 * 
 *  Adds commonly used features to the basic RepastJ SimModelImpl class
 *  * standard parameters (seed, stopT, etc)
 *  * easy way to add parameters so they can be set a run time or read from files
 *  * report file opening, writing, etc
 *  * some basic RNG distributions
 */

import java.io.*;
import java.util.*;
import java.util.regex.*;  // for MatchResult

import java.lang.reflect.*;
import java.net.UnknownHostException;

import uchicago.src.sim.engine.*;

import uchicago.src.sim.util.*;

//for the xml parsing of input files
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import java.util.TreeMap;

public class ModelParameters extends SimModelImpl {

	/**
	 *  setup
	// this should be called *last* in the Model setup() that
	// extends this class.
	 */
	public void setup() {
		changesVector = new Vector<ChangeObj>();
		setupParametersMap();
		// this might be kind of kludgy?
		// only process command line arguments if it is the first run
		// if it is the first run then schedule is null,
		// if not then schedule is initialized (and is set to null
		// on the next line)
		if( schedule == null ) 
			processCommandLinePars( commandLineArgs );
		schedule = null;

		if ( rDebug > 0 )
			System.out.printf( "<--- ModelParameters setup() done.\n" );
	}

	public void begin() {
		// this must be declared in the class that 'extends' this one
	}

	/**
	 * buildModelStart
	 * this should be called first by the buildModel in the extending class.
	 * 
	 */
	public void buildModelStart() {
		if ( getSeed() == 1234567 || getSeed() == 0) {
			long s = System.currentTimeMillis();
			setSeed( s );
			if ( rDebug > 1 )
				System.out.printf( "\nseed was 1234567 or 0, now ==> s=%d\n", s );
		}
		if( rDebug > 1 )
			System.out.printf( "\nabout to setSeed(%d)\n", getSeed() );
		resetRNGenerators();
	}


	// buildSchedule
	// the extending classes must fill this in
	public void buildSchedule() {
		//if ( rDebug > 0 )
		//	System.err.printf( "-> ModelParameters buildSchedule...\n" );
		schedule = new Schedule();
	}



	public String[] getInitParam() {
		// this must be declared in the class that 'extends' this one
		return null;
	}

	// Generic parameters
	protected String		initialParametersFileName = "";
 	protected String		initialAgentsFileName = "";
	protected String		reportFileName = "report";
	protected String		outputDirName =  "./";
	protected int			reportFrequency = 1;
	protected int			runNumber = 0;
	protected int			stopT = 100;
	protected int			rDebug = 0;
	protected int			saveRunEndState = 0;
	protected long    		seed = 1234567;
	protected PrintWriter		reportFile, plaintextReportFile;
	protected PrintWriter		changesFile;

	// other utilities
	protected String[] commandLineArgs;
	protected String modelType = "Model";
	public    String		hostname = "unknown";

	//for input file 
	protected boolean 	STRICT_FILE_FORMAT = true;
	protected Vector<ChangeObj> 		changesVector;

	// variables for processing run-time changes that are
	// read in from the input file
	protected int 		numberOfChanges = 0;
	protected int 		nextChangeToDo = 0;
	protected int[] 	changeSteps = new int[64];
	protected int[] 	changeIDs = new int[64];
  	protected ArrayList<ArrayList<String>> changeSpecs = new ArrayList<ArrayList<String>>(16);

	// required by SimModelImpl
	protected BasicAction	stepMethods;
	protected Schedule	schedule = null;

	// setupParametersMap
	// this implements the mapping from aliases to long names,
	// for the 'base' parameters common to all models.
	// For parameters for a particular model, add lines
	// to addToParametersMap().
	protected   TreeMap<String,String> parametersMap;

	public void setupParametersMap () {
		DMSG( 1, "setupParametersMap()" );

		parametersMap = null;
		parametersMap = new TreeMap<String,String>();
		// generic model parameters
		parametersMap.put( "D", "rDebug" );
		parametersMap.put( "S", "seed" );
		parametersMap.put( "iPFN", "initialParametersFileName" );
		parametersMap.put( "iAFN", "initialAgentsFileName" );
		parametersMap.put( "rFN", "reportFileName" );
		parametersMap.put( "T", "stopT" );
		parametersMap.put( "sRES", "saveRunEndState" );
		parametersMap.put( "oDN", "outputDirName" );
		parametersMap.put( "rF", "reportFrequency" );
		parametersMap.put( "rN", "runNumber" );

		addModelSpecificParameters();
	}

	/**
	 *  addModelSpecificParameters
	 *  a subclass should override this to add model specific parameters.
	 */ 
	public void addModelSpecificParameters() {
	}

	public void printParametersMap () {

		ArrayList<String> parameterNames = new ArrayList<String>( parametersMap.values() );
		ArrayList<String> parameterAliases = new ArrayList<String>( parametersMap.keySet() );

		for( int i = 0; i < parameterAliases.size(); i++ ) {
			Method getmethod = null;
			String parAlias = (String)  parameterAliases.get(i);
			String parName = (String) parametersMap.get( parAlias );

			getmethod = findGetMethodFor( parName );

			if( getmethod != null ) {
				try {
					Object returnVal = getmethod.invoke( this, new Object[] {} );
					String s =  parName + " (" + parAlias + ") = " + returnVal;
					System.out.printf( "%s\n", s );
				} catch( Exception e ) { e.printStackTrace(); }
			}
			else {
				System.err.printf ( "COULD NOT FIND SET METHOD FOR:  %s\n", 
					parameterNames.get( i ) );
				System.err.printf ( "Is the entry in the parametersMap for this correct?" );
			}
		}

	}

	/**
	 * print parameters and alias mapping in format suitable for gsdrone
	 */
	public void printForGSDrone () {

		ArrayList<String> parameterNames = new ArrayList<String>( parametersMap.values() );
		ArrayList<String> parameterAliases = new ArrayList<String>( parametersMap.keySet() );
		for( int i = 0; i < parameterAliases.size(); i++ ) {
			Method getmethod = null;
			String parAlias = (String)  parameterAliases.get(i);
			String parName = (String) parametersMap.get( parAlias );
			getmethod = findGetMethodFor( parName );
			if( getmethod != null ) {
				try {
					Object returnVal = getmethod.invoke( this, new Object[] {} );
					String s = "<value param=\"" + parName + "\" value=\"" + returnVal + "\" />";
					System.out.printf( "%s\n", s );
				} catch( Exception e ) { e.printStackTrace(); }
			}
			else {
				System.err.printf ( "COULD NOT FIND SET METHOD FOR:  %s\n", 
					parameterNames.get( i ) );
				System.err.printf ( "Is the entry in the parametersMap for this correct?" );
			}
		}

		System.out.printf( "\n" );


		for( int i = 0; i < parameterAliases.size(); i++ ) {
			Method getmethod = null;
			String parAlias = (String)  parameterAliases.get(i);
			String parName = (String) parametersMap.get( parAlias );
			getmethod = findGetMethodFor( parName );
			if( getmethod != null ) {
				try {
					// Object returnVal = getmethod.invoke( this, new Object[] {} );
					getmethod.invoke( this, new Object[] {} );
					String s = "<abbrev param=\"" + parName + "\" abbrev=\"" + parAlias + "\" />";
					System.out.printf( "%s\n", s );
				} catch( Exception e ) { e.printStackTrace(); }
			}
			else {
				System.err.printf ( "COULD NOT FIND SET METHOD FOR:  %s\n", 
					parameterNames.get( i ) );
				System.err.printf ( "Is the entry in the parametersMap for this correct?" );
			}
		}
	}
	
	////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////


	// generic setters/getters

	public String[] getCommandLineArgs () { return commandLineArgs; }
	public void setCommandLineArgs ( String[] arguments ) {
		// for( int i = 0; i < arguments.length; i++ ) {
	   	//	System.out.println( "setCommandLineArgs: " + arguments[i] );
		// }
		commandLineArgs = arguments;
	}

	public String getModelType() { return modelType; }
	public void setModelType( String s ) {
		modelType = s;
	}

	public String getInitialParametersFileName () { 
		return initialParametersFileName; }
	public void setInitialParametersFileName ( String s ) {
		initialParametersFileName = s;
	}

	public String getInitialAgentsFileName () { 
		return initialAgentsFileName; }
	public void setInitialAgentsFileName ( String s ) {
		initialAgentsFileName = s;
	}

	public String getReportFileName () { return reportFileName; }
	public void setReportFileName ( String s ) {
		reportFileName = s;
	}

	public String getOutputDirName () { return outputDirName; }
	public void setOutputDirName ( String s ) {
		outputDirName = s;
	}

	public int getReportFrequency () { return reportFrequency; }
	public void setReportFrequency ( int i ) {
		reportFrequency = i;
	}

	public int getRunNumber () { return runNumber; }
	public void setRunNumber ( int i ) {
		runNumber = i;
	}

	public int getStopT () { return stopT; }
	public void setStopT ( int i ) {
		stopT = i;
	}

	public int getSaveRunEndState () { return saveRunEndState; }
	public void setSaveRunEndState ( int i ) {
		saveRunEndState = i;
	}

	public int getRDebug () { return rDebug; }
	public void setRDebug ( int i ) {
		if( rDebug == i ) {
			// System.out.printf( "setRDebug called, but value unchanged, returning" );
			return;
		}
		// System.out.println( "setRDebug ( " + i + " ) called" );
		rDebug = i;
		if( modelType.equals( "GUIModel" ) ) {
			updateAllProbePanels();
		}
		if( modelType.equals( "GUIModel" ) && schedule != null )
			writeChangeToReportFile ( "rDebug", String.valueOf( i ) );
	}

	public long getSeed () { return seed; }
	public void setSeed ( long i ) {
		if( rDebug > 0 )
			System.out.println( "setSeed ( " + i + " ) called" );
		seed = i;

		resetRNGenerators();

		if( modelType.equals( "GUIModel" ) ) {
			updateAllProbePanels();
		}
		if( modelType.equals( "GUIModel" ) && schedule != null)
			writeChangeToReportFile ( "seed", String.valueOf( i ) );
	}

	public void resetRNGenerators ( ) {
		if ( rDebug > 0 )
			System.out.printf( "\nresetRNGenerators with %d\n", getSeed() );

		// this is required because once you change the seed you invalidate
		// any previously created distributions
		uchicago.src.sim.util.Random.setSeed( seed );
		uchicago.src.sim.util.Random.createUniform();
		uchicago.src.sim.util.Random.createNormal( 0.0, 1.0 );
	}

	// NOTE: these are class methods!
	
	/**
	 * @param low
	 * @param high
	 * @return int drawn from uniform random [low,high]
	 */
	static public int getUniformIntFromTo ( int low, int high ) {
		int randNum = uchicago.src.sim.util.Random.uniform.nextIntFromTo( low, high );
		// System.out.println( "getUniformIntFromTo:  " + randNum );
		return randNum;
	}

	/**
	 * @param mean
	 * @param sd
	 * @return double drawn from normal (mean, sd)
	 * cf http://acs.lbl.gov/~hoschek/colt/api/cern/jet/random/Normal.html
	 */
	static public double getNormalDouble ( double mean, double sd ) {
		double randNum =  uchicago.src.sim.util.Random.normal.nextDouble ( mean, sd );
		// System.out.println( "getNormalDouble:  " + randNum );
		return randNum;
	}
	/**
	 * @param low
	 * @param high
	 * @return double drawn from uniform random [low,high)
	 */
	static public double getUniformDoubleFromTo( double low, double high ) {
		double randNum = uchicago.src.sim.util.Random.uniform.nextDoubleFromTo( low, high );
		// System.out.println( "getUniformDoubleFromTo:  " + randNum );
		return randNum;
	}

	/**
	 * @param mean
	 * @param sd
	 * @return double in [0,1] drawn from normal (mean, sd)
	 * loop until a number between 0 and 1 is generated,
	 * if mean and sd are set correctly the loop will rarely happen
	 */
	static public double getNormalDoubleProb ( double mean, double sd ) {
		if ( mean < 0 || mean > 1 ) {
			System.err.printf ( "\ngetNormalDoubleProb: Invalid value set for normal distribution mean\n\n" );
			return -1;
		}
		double d = uchicago.src.sim.util.Random.normal.nextDouble ( mean, sd );
		while ( d < 0 || d > 1 )
			d = uchicago.src.sim.util.Random.normal.nextDouble ( mean, sd );

		// System.out.println( "getNormalDoubleProb:  " + d );

		return d;
	}

	public void setRngSeed ( long i ) {
		System.out.println( "setRngSeed ( " + i + " ) called" );
		setSeed( i );
	}

	public PrintWriter getReportFile () { return reportFile; }
	public PrintWriter getPlaintextReportFile () { return plaintextReportFile; }
	public Schedule getSchedule() { return schedule; }

	// rePast needs this (i guess...)
	public String getName() { return "ModelParameters"; }

	// some generic utilities
	public void updateAllProbePanels() {
		DMSG ( 2, "updateAllProbePanels()" );
		ProbeUtilities.updateProbePanels();

		// kludge...
		// need this in case updateAllProbePanels gets called
		// before the probe panel is created (if it is called
		// before, then a RuntimeException occurs)
		// did have if(schedule != null), but that means panels
		// do not update at all during time=0, so people get confused.
		try {
			ProbeUtilities.updateModelProbePanel();
		}
		catch (RuntimeException e) {
			// ignore exception
			DMSG( 3, "RuntimeException when updating model probe panel, ignoring ..." );
		}
	}

	// captialize first character of s
	protected String capitalize( String s ) {
		char c = s.charAt( 0 );
		char upper = Character.toUpperCase( c );
		return upper + s.substring( 1, s.length() );
	}


	// REPORT FILE PROCESSING ------------------------------
	//
	
	/**
	 * startReportFile
	 * opens two report files
	 * one XML report file and one plaintext report file
	 * call writeLineToReportFile to write to XML report file
	 * and writeLineToPlaintextReportFile to write to plaintext file
	 * 
	 */

	public PrintWriter startReportFile ( )  {
		if ( rDebug > 0 )
			System.out.println( "startReportFile called!" );
		reportFile = null;
		plaintextReportFile = null;
		String fullFileName = reportFileName + String.format( ".%02d", runNumber );
		String xmlFullFileName = reportFileName + ".xml" 
						+ String.format( ".%02d", runNumber );

		// BufferedReader inFile = IOUtils.openFileToRead(initialParametersFileName);

		reportFile = IOUtils.openFileToWrite( outputDirName, xmlFullFileName, "r" );
		plaintextReportFile = IOUtils.openFileToWrite( outputDirName, fullFileName, "r" );

		// the first line you have to write is the XML version line
		// DO NOT WRITE THIS LINE USING writeLineToReportFile()!
		reportFile.println( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );

		writeLineToReportFile( "<reportfile>" );
		writeLineToPlaintextReportFile( "# begin reportfile" );
		
		try {
			hostname = java.net.InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			System.err.printf("\n\nError trying to fetch hostname!\n\n");
			e.printStackTrace();
		}
		hostname = "# hostname: " + hostname;
		writeLineToPlaintextReportFile( hostname );

		//  write the initial parameters to the report file
		writeParametersToReportFile();

		writeHeaderCommentsToReportFile();  // the user must define this!

		return reportFile;
	}

	/**
	 * write line to xml report file (started by startReportFile)
	 * @param line
	 */
	public void writeLineToReportFile ( String line ) {
		if ( reportFile == null ) {
			DMSG( 3, "report file not opened yet" );
			// click the initialize button to open it! 
			// returning w/o writing to report file ...");
			return;
		}
		else {
			reportFile.println( line );
		}
	}

	/**
	 * write line to plain text report file (started by startReportFile)
	 * @param line
	 */
	public void writeLineToPlaintextReportFile ( String line ) {
		if ( plaintextReportFile == null ) {
			DMSG( 3, "report file not opened yet" );
			// click the initialize button to open it! 
			// returning w/o writing to report file ...");
			return;
		}
		else {
			plaintextReportFile.println( line );
		}
	}

	public void writeChangeToReportFile( String varname, String value ) {
		DMSG( 1, "writeChangeToReportFile(): write change to report file: " 
			+ varname + " changed to " + value );

		writeLineToReportFile( "<change>" );
		writeLineToReportFile( "\t<" + varname + ">" + value 
									+ "</" + varname + ">" );
		String s = String.format(  "\t<time>%.0f</time>", getTickCount() );
		writeLineToReportFile( s );
		writeLineToReportFile( "</change>" );

		writeLineToPlaintextReportFile( "# change:  " + varname + "=" + value );
	}

	public void endReportFile ( ) {
		writeLineToReportFile( "</reportfile>" );
		writeLineToPlaintextReportFile( hostname );  // has # in front already
		writeLineToPlaintextReportFile( "# end report file" );
		IOUtils.closePWFile( reportFile );
		IOUtils.closePWFile( plaintextReportFile );
	}


	// this iterates through the values stored in the parametersMap, 
	// calls the getter on each parameter, and outputs the 
	// parameter and its value to the report file.
	// this is called right before the model run starts (after all 
	// initial parameters are changed!) so 
	// the initial parameters are in the report file.
	public void writeParametersToReportFile() {
		DMSG( 1, "writeParametersToReportFile()" );

		writeLineToReportFile( "<parameters>" );
		writeLineToPlaintextReportFile( "# begin parameters" );

		ArrayList<String> parameterNames = new ArrayList<String>( parametersMap.values() );
		for( int i = 0; i < parameterNames.size(); i++ ) {
			Method getmethod = null;
			getmethod = findGetMethodFor( (String) parameterNames.get( i ) );

			if( getmethod != null ) {
				try {
					Object returnVal = getmethod.invoke( this, new Object[] {} );

					writeLineToReportFile( "\t<" + parameterNames.get(i) + ">"
								+ returnVal 
								+ "</" + parameterNames.get(i) + ">" );

					writeLineToPlaintextReportFile( parameterNames.get(i)
									+ "="
									+ returnVal );

				} catch( Exception e ) { e.printStackTrace(); }
			}
			else {
				System.err.printf ( "COULD NOT FIND SET METHOD FOR:  %s\n", 
					parameterNames.get( i ) );
				System.err.printf ( "Is the entry in the parametersMap for this correct?" );
			}
		}
		writeLineToReportFile( "</parameters>" );
		writeLineToPlaintextReportFile( "# end parameters" );
	}


	///////////////////////////////////////////////////////////////////////////////////////
	//
	// Generic report file processing  ------------------------------
	//
	// These are similar to those above, but these require the user
	// to specify a particular "basename" for the files, and they
	// require/allow the user to separately open/writeTo/close the xml and plain text files.
	//
	// 
	// 
	//
	// void writeParametersToReportFile( PrintWriter rfile )
	// void writeParametersToPlainTextReportFile( PrintWriter rfile ) 
	//
	// void writeLineToReportFile ( String line, PrintWriter rfile )
	//
	//  void endReportFile ( PrintWriter rfile ) 
	//  void endPlainTextReportFile ( PrintWriter rfile ) 
	//

	/**
	 * PrintWriter startReportFile ( String baseName )  -- an xml formated report file
	 * @param baseName
	 * @return
	 */

	public PrintWriter startReportFile ( String baseName ) {
		if ( rDebug > 0 )
			System.err.printf( "startReportFile called for baseName='%s'\n", baseName );
		PrintWriter rFile = null;
		String xmlFullFileName = baseName + ".xml" + String.format( ".%02d", runNumber );

		rFile = IOUtils.openFileToWrite( outputDirName, xmlFullFileName, "r" );

		// the first line you have to write is the XML version line
		// DO NOT WRITE THIS LINE USING writeLineToReportFile(...)!
		rFile.println( "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" );

		writeLineToReportFile( "<reportfile>", rFile );
		//  write the initial parameters to the report file
		writeParametersToReportFile( rFile );

		return rFile;
	}

	/**
	 * PrintWriter startPlainTextReportFile ( String baseName ) -- plain text report file
	 * @param baseName
	 * @return
	 */
	public PrintWriter startPlainTextReportFile ( String baseName ) {
		if ( rDebug > 0 )
			System.err.printf( "startPlainTextReportFile called for baseName='%s'\n", baseName );
		PrintWriter rFile = null;
		String fullFileName = baseName + String.format( ".%02d", runNumber );

		rFile = IOUtils.openFileToWrite( outputDirName, fullFileName, "r" );

		writeLineToReportFile( "# begin reportfile", rFile );

		//  write the initial parameters to the report file
		writeParametersToPlainTextReportFile( rFile );

		return rFile;
	}

	/**
	 * this just writes whatever is sent to it, and then a newline!
	 * @param line
	 * @param rFile
	 */
	public void writeLineToReportFile ( String line, PrintWriter rFile ) {
		if ( rFile == null ) {
			System.err.printf( "\nERROR - A user-defined report file not opened yet!\n" );
			return;
		}
		else {
			rFile.println( line );
		}
	}
	
	/**
	 * this just writes whatever is sent to it (no newline added)
	 * @param line
	 * @param rFile
	 */
	public void writeBufferToReportFile ( String line, PrintWriter rFile ) {
		if ( rFile == null ) {
			System.err.printf( "\nERROR - A user-defined report file not opened yet!\n" );
			return;
		}
		else {
			rFile.printf( line );
		}
	}


	/**
	 * close file
	 * @param rFile
	 */
	public void endReportFile ( PrintWriter rFile ) {
		writeLineToReportFile( "</reportfile>", rFile );
		IOUtils.closePWFile( rFile );
	}

	/**
	 * close file
	 * @param rFile
	 */
	public void endPlainTextReportFile ( PrintWriter rFile ) {
		writeLineToReportFile( "# end report file", rFile );
		IOUtils.closePWFile( rFile );
	}

	

	/**
	 * 	these iterate through the values stored in the parametersMap, 
	 * calls the getter on each parameter, and outputs the 
	 * parameter and its value to the report file.
	 * this is called right before the model run starts (after all 
	 * initial parameters are changed!) so 
	 * the initial parameters are in the report file.
	 * @param rFile
	 */
	public void writeParametersToReportFile( PrintWriter rFile ) {
		DMSG( 1, "writeParametersToReportFile( rFile )" );
		writeLineToReportFile( "<parameters>", rFile );
		ArrayList<String> parameterNames = new ArrayList<String>( parametersMap.values() );
		for( int i = 0; i < parameterNames.size(); i++ ) {
			Method getmethod = null;
			getmethod = findGetMethodFor( (String) parameterNames.get( i ) );
			if( getmethod != null ) {
				try {
					Object returnVal = getmethod.invoke( this, new Object[] {} );
					writeLineToReportFile( "\t<" + parameterNames.get(i) + ">"
								+ returnVal 
								+ "</" + parameterNames.get(i) + ">", rFile );
				} catch( Exception e ) { e.printStackTrace(); }
			}
			else {
				System.err.printf ( "COULD NOT FIND SET METHOD FOR:  %s\n", 
					parameterNames.get( i ) );
				System.err.printf ( "Is the entry in the parametersMap for this correct?" );
			}
		}
		writeLineToReportFile( "</parameters>", rFile );
	}

	/**
	 * 	these iterate through the values stored in the parametersMap, 
	 * calls the getter on each parameter, and outputs the 
	 * parameter and its value to the report file.
	 * this is called right before the model run starts (after all 
	 * initial parameters are changed!) so 
	 * the initial parameters are in the report file.
	 * @param rFile
	 */
	public void writeParametersToPlainTextReportFile( PrintWriter rFile ) {
		DMSG( 1, "writeParametersToPlainTextReportFile( rFile )" );
		writeLineToReportFile( "# begin parameters", rFile );
		ArrayList<String> parameterNames = new ArrayList<String>( parametersMap.values() );
		for( int i = 0; i < parameterNames.size(); i++ ) {
			Method getmethod = null;
			getmethod = findGetMethodFor( (String) parameterNames.get( i ) );
			if( getmethod != null ) {
				try {
					Object returnVal = getmethod.invoke( this, new Object[] {} );
					writeLineToReportFile( parameterNames.get(i) + "=" + returnVal, rFile );
				} catch( Exception e ) { e.printStackTrace(); }
			}
			else {
				System.err.printf ( "COULD NOT FIND SET METHOD FOR:  %s\n", 
					parameterNames.get( i ) );
				System.err.printf ( "Is the entry in the parametersMap for this correct?" );
			}
		}
		writeLineToReportFile( "# end parameters", rFile );
	}

	// ------>   End of Report File Processing    <------------------------------



	//////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////
	//
	// ------>   Input Parameter Processing    <------------------------------
	//
	//////////////////////////////////////////////////////////////////////////
	//////////////////////////////////////////////////////////////////////////
	// parseParametersFile
	//
	public void parseParametersFile() {
		// a klunky way to see if the parameters file exists
		try {
			BufferedReader inFile = 
				IOUtils.openFileToRead(initialParametersFileName );
			IOUtils.closeBRFile( inFile );
		}
		catch(Exception e) { // not an error, just not there!
			if ( rDebug > 0 )
				System.err.printf( "  -- no initialParametersFileName '%s' to parse.\n", 
						   initialParametersFileName );
			return;
		}

		try {
			//setup the input file
			DocumentBuilderFactory myDBF = DocumentBuilderFactory.newInstance();
			DocumentBuilder myDB = myDBF.newDocumentBuilder();
			Document myDocument = myDB.parse(initialParametersFileName);

			if ( rDebug > 0 )
				System.out.println("Parsing parameter file: "+initialParametersFileName);

			NodeList tmpList = myDocument.getElementsByTagName("parameters");
			Element tmpElement = (Element)tmpList.item(0);
			NodeList parameterList = tmpElement.getElementsByTagName("*");

			for(int i = 0; i < parameterList.getLength(); i++) {
				if ( parameterList.item(i).getChildNodes().item(0) == null)
					continue;
				DMSG( 1, "name:  " + parameterList.item(i).getNodeName()
							+ "  value:  "
							+ parameterList.item(i).getChildNodes().item(0).getNodeValue() );
				set( parameterList.item(i).getNodeName(), 
					parameterList.item(i).getChildNodes().item(0).getNodeValue() );
			}

			// process changes
			NodeList parameterChangeList = myDocument.getElementsByTagName( "change" );
			processChangeList( parameterChangeList );

			DMSG( 1, "Done parsing file:  " + initialParametersFileName );
		}
		catch(Exception e) {
			System.out.println("Exception when parsing parameters file:  "
						+ initialParametersFileName);
			System.out.println("Is the file in the correct format?");
			e.printStackTrace();
		}
	}


	/////////////////////////////////////////////////////////////////////////
	// processCommandLinePars
	// storeParameter
	//
	//
	public void processCommandLinePars ( String[] args ) {
		int r;
		if( args.length > 0 
			&& ( args[0].equals( "--help" ) || args[0].equals( "-h" ) ) ) {
			printProjectHelp();
		}

		for ( int i = 0; i < args.length; ++i ) {
			r = storeParameter( args[i] );
			if( r != 0 )  {
				System.out.println( "Error processing cmdLine par:  " + args[i] );
			}
		}
	}

	// storeParameter
	// format:  parname=value
	// parse out parname, and find method for setParname
	// if not found, return -1
	// otherwise set the value and return 0.
	// to set the value, we have to get the setMethod, and its par type.
	// then convert the string value to the appropriate object, and
	// use invoke to do the setting!

	public int storeParameter ( String line ) {
		int r = 0;
		String pname, pvalue;
		StringTokenizer st = new StringTokenizer( line, "=;," );
		Method setm = null;

		if ( ( pname = st.nextToken() ) == null ) {
			System.err.printf("\n** storeParameter -- couldn't find pname on '%s'.\n",
						  line );
			return -1;
		}
		if ( ( pvalue = st.nextToken() ) == null ) {
			System.err.printf("\n** storeParameter -- couldn't find value on '%s'.\n",
						  line );
			return -1;
		}
		pname = pname.trim();
		pvalue = pvalue.trim();

		pname = aliasToParameterName ( pname );

		// if this is a scheduledChange, create the change
		// and insert it into the changesVector
		if( pname.equals ( "sC" ) ) {
			String changetime = pvalue;
			String changepname, changepvalue;

			if ( ( changepname = st.nextToken() ) == null ) {
				System.out.println( "\n** storeParameter -- couldn't find "
					+ "scheduleChange pname on:  " + line );
				return -1;
			}

			if ( ( changepvalue = st.nextToken() ) == null ) {
				System.out.println( "\n** storeParameter -- couldn't find "
					+ "scheduleChange pvalue on:  " + line );
				return -1;
			}

			changepname = changepname.trim();
			changepvalue = changepvalue.trim();

			changepname = aliasToParameterName ( changepname );

			ChangeObj newChange = new ChangeObj( Integer.parseInt( changetime ),
						changepname, changepvalue );

			DMSG ( 1, "scheduledChange from command line created:  "
				+ "  Time:  " + changetime + "  pname:  "
				+ changepname + "  pvalue:  " + changepvalue );
			changesVector.add ( newChange );

			return 0;
		}

		setm = findSetMethodFor( pname );
		String ptype = getParTypeOfSetMethod( setm );

		try {
			setm.invoke( this, new Object[] { valToObject( ptype, pvalue ) } );
		} catch ( Exception  e ) {
			System.err.printf( "\n storeParameter: '%s'='%s' invoke exception!\n", 
						   pname, pvalue );

			System.err.printf( "  --> %s\n", e.toString() );
			e.printStackTrace();
			return -1;
		}

		if( pname.equals ( "initialParametersFileName" ) ) {
			DMSG( 1, "Processing initial parameters file:  " + pvalue );
			parseParametersFile();
		}

		return r;
	}

	// returns the long parameter name if the parameter passed in is
	// an alias.  if it is not an alias, the name sent to it is returned.
	public String aliasToParameterName ( String alias ) {
		// check to see if "alias" is an alias in the parametersMap
		// if it is then "alias" is a valid alias, so set "alias" to the 
		// actual parameter name that is in the map
		if( parametersMap.containsKey( alias ) ) {
			DMSG( 1, "Converting alias " + alias + " to " + parametersMap.get( alias ) );
			alias = (String) parametersMap.get( alias );
		}

		return alias;
	}

	// getParTypeOfSetMethod
	// get type of setPar method parameter
	@SuppressWarnings("all")
	public String getParTypeOfSetMethod ( Method m ) {
		Class[] parTypes = m.getParameterTypes();
		String s = parTypes[0].getName();
		return s;
	}

	// findGetMethodFor
	// find get<ParName> method for specified parameter name
	@SuppressWarnings("all") 
	protected Method findGetMethodFor( String varname ) {
		String methodname = new String( "get" + capitalize( varname ) );
		Class c = getClass();
		Method[] methods = c.getMethods();
		Method getmethod = null;

		for ( int j = 0; j < methods.length; j++ ) {
			if ( methods[j].getName().equals( methodname ) ) {
				getmethod = methods[j];
				break;
			}
		}
		if ( getmethod == null ) {
			System.err.printf( "\n** findGetMethodFor -- couldn't find '%s'\n",
						   methodname );
			return getmethod;
		}

		return getmethod;
	}

	// findSetMethodFor
	// find set<ParName> method for specified parameter name 
	@SuppressWarnings("all")
	public Method findSetMethodFor ( String pname ) {
		Class c = this.getClass();
		Method[] methods = c.getMethods();
		int nf = methods.length;
		String setmethodname = "set" + capitalize( pname );
		String mname;
		Method method = null;
		for ( int i = 0; i < nf; ++i ) {
			mname = methods[i].getName();
			if ( mname.equals( setmethodname ) ) {
				method = methods[i];
				break;
			}
		}
		if ( method == null ) {
			System.err.printf( "\n** findSetMethodFor -- couldn't fine '%s'\n",
						   setmethodname );
			return method;
		}
		return method;
	}

	// valToObject
	// return value stored in object of appropriate type
	private Object valToObject( String type, String val ) {
		if ( type.equals( "int" ) ) {
		  return Integer.valueOf( val );
		} else if ( type.equals( "double" ) ) {
		  return Double.valueOf( val );
		} else if ( type.equals( "float" ) ) {
		  return Float.valueOf( val );
		} else if ( type.equals( "long" ) ) {
		  return Long.valueOf(val);
		} else if ( type.equals( "boolean" ) ) {
		  return Boolean.valueOf(val);
		} else if ( type.equals( "java.lang.String" ) ) {
		  return val;
		} else {
		  throw new IllegalArgumentException( "illegal type" );
		}
	}

	public String skipCommentLines ( BufferedReader inFile ) {
		String line;
		while ( ( line = IOUtils.readBRLine ( inFile ) ) != null ) {
			if ( line.charAt(0) != '#' )
				break;
		}
		return line;
	}

	/////////////////////////////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////////////////
	// applyAnyStoredChanges
	// look through all of the changes, if any have time of this time 
	// step execute the change
	public void applyAnyStoredChanges () {
		if ( rDebug > 0 ) {
			System.out.println( "applyAnyStoredChanges called at time step: " 
						   + getTickCount() );
		}

		for( int i = 0; i < changesVector.size(); i++ ) {
			ChangeObj tmpObj = (ChangeObj) changesVector.get( i );
			if( tmpObj.time == getTickCount() ) {
				if ( rDebug > 0 ) {
					System.out.println( "applyAnyStoredChanges():  Changing " 
								   + tmpObj.varname + " to " +tmpObj.value );
				}
				set( tmpObj.varname, tmpObj.value );
			}
		}
	}

	///////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////
	// utility methods for accessing parts of model

	@SuppressWarnings("all")
	public void setObjectParameter( Object inObject, String varname, String value ) {
		String methodname = new String( "set" + capitalize( varname ) );
		Class c = inObject.getClass();
		Method[] methods = c.getMethods();
		Method setmethod = null;

		for ( int j = 0; j < methods.length; j++ ) {
			if ( methods[j].getName().equals( methodname ) ) {
				setmethod = methods[j];
				break;
			}
		}

		if(setmethod != null) {
			try {
				Class[] parameterTypes = setmethod.getParameterTypes();
				if( parameterTypes[0].getName().equals( "int" ) ) {
					DMSG( 3, "int parameter type" );
					setmethod.invoke( inObject, new Object[] { Integer.valueOf( value ) } );
				}
				else if( parameterTypes[0].getName().equals( "long" ) ) {
					DMSG( 3, "long parameter type" );
					setmethod.invoke( inObject, new Object[] { Long.valueOf( value ) } );
				}
				else if( parameterTypes[0].getName().equals( "double" ) ) {
					DMSG( 3, "double parameter type" );
					setmethod.invoke( inObject, new Object[] { Double.valueOf( value ) } );
				}
				else if( parameterTypes[0].getName().equals( "float" ) ) {
					DMSG( 3, "float parameter type" );
					setmethod.invoke( inObject, new Object[] { Float.valueOf( value ) } );
				}
				else if( parameterTypes[0].getName().equals( "boolean" ) ) {
					DMSG( 3, "boolean parameter type" );
					setmethod.invoke( inObject, new Object[] { Boolean.valueOf( value ) } );
				}
				else if( parameterTypes[0].getName().equals( "java.lang.String" ) ) {
					DMSG( 3, "String parameter type" );
					setmethod.invoke( inObject, new Object[] { value } );
				}
				else {
					System.out.println( "COULD NOT DETERMINE PARAMETER TYPE" );
				}
				DMSG( 1, "setObjectParameter():  " + varname + " changed to " + value );
			} catch( Exception e ) { e.printStackTrace(); }
		}
		else {
			System.out.println( "COULD NOT FIND SET METHOD FOR:  " + varname );
		}
	}

	private void processChange(Element c) {

		
		DMSG(3, "Processing A Change");

		NodeList tmpList = c.getElementsByTagName("*");

		ChangeObj newChange = new ChangeObj(0,"","");

		for(int i = 0; i < tmpList.getLength(); i++)  {
			Element tmpElement = (Element)tmpList.item(i);
			// System.out.println("tmpElement.getTagName(): " + tmpElement.getTagName());
			if(tmpElement.getTagName().equals("time")) {
				newChange.time = Integer.parseInt(tmpElement.getChildNodes().item(0).getNodeValue());
			}
			else {
				newChange.varname = tmpElement.getTagName();
				newChange.value = tmpElement.getChildNodes().item(0).getNodeValue();
			}
		}

		changesVector.add(newChange);

		DMSG(3,"Done processing a Change");
	}

	private void processChangeList( NodeList c ) {

		DMSG(3, "Processing " + c.getLength() + " changes ..." );
		for( int i = 0; i < c.getLength(); i++ )
			processChange( (Element) c.item( i ) );

		for( int i = 0; i < changesVector.size(); i++ ) {
			ChangeObj tmpObj = (ChangeObj) changesVector.get( i );
			DMSG( 3, "Time:  " + tmpObj.time + "  VarName:  " 
				+ tmpObj.varname + "  Value:  " + tmpObj.value );
		}
	}

	@SuppressWarnings("all")
	private void set( String varname, String value ) {

		// first convert varname to the alias, if it is an alias
		varname = aliasToParameterName ( varname );

		Method setmethod = findSetMethodFor ( varname );

		if( setmethod != null ) {
			try {
				Class[] parameterTypes = setmethod.getParameterTypes();
				if ( parameterTypes[0].getName().equals( "int" ) ) {
					DMSG( 3, "int parameter type" );
					setmethod.invoke( this, new Object[] { Integer.valueOf( value ) } );
				}
				else if ( parameterTypes[0].getName().equals( "long" ) ) {
					DMSG( 3, "long parameter type" );
					setmethod.invoke( this, new Object[] { Long.valueOf( value ) } );
				}
				else if( parameterTypes[0].getName().equals( "float" ) ) {
					DMSG( 3, "float parameter type" );
					setmethod.invoke( this, new Object[] { Float.valueOf( value ) } );
				}
				else if( parameterTypes[0].getName().equals( "boolean" ) ) {
					DMSG( 3, "boolean parameter type" );
					setmethod.invoke( this, new Object[] { Boolean.valueOf( value ) } );
				}
				else if ( parameterTypes[0].getName().equals( "double" ) ) {
					DMSG( 3, "double parameter type" );
					setmethod.invoke( this, new Object[] { Double.valueOf( value ) } );
				}
				else if ( parameterTypes[0].getName().equals( "java.lang.String" ) ) {
					DMSG( 3, "String parameter type" );
					setmethod.invoke( this, new Object[] { value } );
				}
				else {
					System.out.println( "COULD NOT DETERMINE PARAMETER TYPE" );
				}
				DMSG( 1, "set():  " + varname + " changed to " + value );
			} catch( Exception e ) { e.printStackTrace(); }
		}
		else {
			System.out.println( "COULD NOT FIND SET METHOD FOR:  " + varname );
			System.out.println( "Is the parameter name correct?" );
		}
	}

	// loadChangeParameters
	// we expect to see
	//   @changeParameters
	//   step=<timeStep>
	//   parName=parValue
	//   ...
	//   @endChangeParameters
	//   <timeStep> is time step changes are to occur.
	//   store in changeSteps[numberOfChanges]
	//   store number of parameters to change in changeIDs[numberOfChanges]
	//   increment 	 numberOfChanges
	// Return 0 if ok, 1 if not.  next line will be after @endChangeParameters
	public int loadChangeParameters( BufferedReader inFile ) {
		ArrayList<String> lines = new ArrayList<String>(16);
		String line, ends = "@endChangeParameters";
		int step = 0, numPars = 0, done = 0;
		if ( rDebug > 0 )
			System.out.printf( "\n\n*** loadChangeParameters \n\n" );

		// first get the step= line, and the time and ID values
		line = skipCommentLines( inFile );
		if ( rDebug > 0 )
			System.out.printf("0: %s\n", line );

		/* was
		r = Format.sscanf( line, "step=%i", p.add(iV) );
		step = iV.intValue();
		*/
		Scanner scanner = new Scanner( line );
		scanner.findInLine( "step=(\\d+)");
		MatchResult result = scanner.match();
		try { step = Integer.parseInt( result.group() ); }
		catch(NumberFormatException e) { }

		// get lines into a bunch of strings, add to list of these sets of lines.
		while ( done == 0 ) {
			line = skipCommentLines( inFile );
			if ( line.equals( ends ) )
				done = 1;
			else {
				// *** It would be nice to check these here...
				lines.add( line );
				++numPars;
			}
		}
		changeSpecs.add( lines );

		if ( numPars == 0 ) {  // oops!
			System.err.printf( "\n*** loadChangeParameters found 0 changes! Last line='%s'\n",
						   line );
			return -1;
		}

		// store time and id in next place in arrays.
		changeSteps[numberOfChanges] = step;
		changeIDs[numberOfChanges] = 0 - numPars;
		++numberOfChanges;
		
		for ( int c = 0; c < numberOfChanges; ++c ) {
			if ( changeIDs[c] >= 0 )
				continue;
			lines = (ArrayList<String>) changeSpecs.get(c);
			System.out.printf( "Change %d at t=%d, ID=%d:\n", 
						 c, changeSteps[c], changeIDs[c] );
			for ( int i = 0; i < numPars; ++i ) {
				System.out.printf("%d: %s\n", i+1, (String)lines.get(i) );
			}
		}

   		return 0;
	}

	public void DMSG(int debugLevel, String debugStr) {
		if(rDebug >= debugLevel) {
			System.out.println("debug:\t" + debugStr);
		}
	}

	////////////////////////////////////////////////////////////////////
	// printProjectHelp
	// this could be filled in with some help to get from running with -help parameter
	//
	public void printProjectHelp() {
		// this is declared in the class that 'extends' this one
	}

	////////////////////////////////////////////////////////////////////
	// writeHeaderCommentsToReportFile
	// include comments to be written just after the list of parameter 
	// values and just before the step-by-step data lines.

	public void writeHeaderCommentsToReportFile () {
		// this is declared in the class that 'extends' this one
	}

}

////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////
// auxilliary classes for processing changes
//
//

class ChangeObj {
	public ChangeObj() {}
	public ChangeObj(int in_time, String in_varname, String in_value) {
		time = in_time;
		varname = in_varname;
		value = in_value;
	}
	public int time;
	public String varname;
	public String value;
}

class ACChangeObj {
	public ACChangeObj() {}
	public ACChangeObj(int in_time, int in_id, String in_varname, String in_value) {
		time = in_time;
		id = in_id;
		varname = in_varname;
		value = in_value;
	}
	public int time;
	public int id;
	public String varname;
	public String value;
}

////////////////////////////////////////////////////////////////////////////////////
////////////////////////////////////////////////////////////////////////////////////
// auxilliary class for file opening/closing
// and string processing
//

class IOUtils {

	public static String readBRLine ( BufferedReader file ) {
		String s;
		try {
			s = file.readLine();
		} catch  ( IOException e ) {
			//System.out.println( "closeBRFile error!" );
			s = null;
		}
		return s;
	}

	public  static BufferedReader openFileToRead ( String filename ) {
		BufferedReader in;
		try {
			in = new BufferedReader( new FileReader(filename));
		} catch ( IOException e ) {
			// no file, etc
			// System.out.println( "openFileToRead error on filename="+filename );
			in = null;
		}
		//System.err.printf("openFileToRead: '%s'\n", filename );
		return in;
	}

	public static PrintWriter openFileToWrite ( String dir, String filename, String how ) {
		PrintWriter out;
		try {
			File f = new File( dir, filename );
			out = new PrintWriter( new FileWriter(f) );
		} catch ( IOException e ) {
			// no file, etc
			//System.out.println( "openFileToWrite error on dir/filename="
			//					+ dir + "/" + filename );
			out = null;
		}
		//System.err.printf("openFileToWrite: '%s'\n", filename );
		return out;
	}

	public  static int closeBRFile (  BufferedReader file ) {
		int r = 0;
		try {
			file.close();
		} catch  ( IOException e ) {
			//System.out.println( "closeBRFile error!" );
			r = -1;
		}
		return r;
	}

	public static int closePWFile ( PrintWriter file ) {
		int r = 0;
   		file.close();
		return r;
	}

	//////////////////////////////////////////////////////////////


	public static int tokenToInt( String token ) {
		int i;
		token = token.trim();
		try {
			i = Integer.parseInt( token );
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException(" tokenToInt error, token="+token ); 
		}
		return i;
	}

	public static double tokenToDouble( String token ) {
		double d;
		token = token.trim();
		try {
			d = Double.parseDouble( token );
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException(" tokenToDouble error, token="+token ); 
		}
		return d;
	}


}
