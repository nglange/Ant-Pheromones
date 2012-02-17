#!/bin/sh
# simple wrapper script to compile the program
# You can include parameters to the javac on the run line, eg
#    bin/compile.sh -Xlint:unchecked

# This first is the path to the location of all the parts of the project,
# eg, where the src/  classes/ bin/ dirs are located.
# CHANGE THIS after copy/move the project ==>
PROJECTDIR=/users/nglange/cscs530/workspace-AntPher-2012-02-06/AntPheromones4

# the parameters below only occassionally need to be changed
PACKAGENAME=AntPheromones
SOURCELOCATION=${PROJECTDIR}/src/${PACKAGENAME}

# add extra user libraries that should be included here
USERDEFINEDLIBS=

########################################################################
########################################################################
# PROBABLY NO CHANGES BELOW THIS

# the params below should be changed when versions of java
# and different libraries are upgraded so they point to the
# correct paths of java and libraries

# java path -- linux or mac
case $(uname) in
   Darwin)
      JAVADIR="/System/Library/Frameworks/JavaVM.framework/Versions/1.5.0/Home"
      ;;
   Linux)
      JAVADIR=/appl/jdk1.6.0_10
      ;;
   *)
      JAVADIR=/appl/jdk1.6.0_10
      ;;
esac 

if [ "$1" = "-h" ]
then
	echo " "
	echo "Compile the java sources for this project/package '${PACKAGENAME}'"
	echo "Sources in '${SOURCELOCATION}'"
	echo "Usage:   $0 [options]"
	echo "where options can be any javac options, e.g.:"
	echo "   $0 -Xlint:unchecked"
	echo " "
	exit
fi

if [ -n $1 ] 
then
	javaPars=$1
else
	javaPars=""
fi

CSCS530LIBDIR=/users/rlr/Courses/


# the actual compile command 
$JAVADIR/bin/javac $JAVAPARS -d ${PROJECTDIR}/bin/ -cp $USERDEFINEDLIBS:$PROJECTDIR/bin:$CSCS530LIBDIR/cscs530.jar   ${SOURCELOCATION}/*.java
