#!/bin/sh
# simple wrapper script to do batch runs ...e.g.,
#     /users/rlr/RePast/Demos-4/heatBugs2/bin/batchrun.sh nB=100 T=400
# this is primarily intended to use be used as the programName in
# drone control files.  See
#    /users/rlr/RePast/Demos-4/heatBugs2/Readme.txt
#

# This first is the path to the location of all the parts of the project,
# eg, where the src/  classes/ bin/ dirs are located.
# CHANGE THIS after copy/move the project ==>
PROJECTDIR=/users/nglange/cscs530/workspace-AntPher-2012-02-06/AntPheromones4









PACKAGENAME=AntPheromones


# the parameters below only occassionally need to be changed
BATCHMODELNAME=BatchModel

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
      JAVADIR=/appl64/jdk1.6.0_11
      ;;
   *)
      JAVADIR=/appl/jdk1.6.0_10
      ;;
esac 


# library paths
CSCS530LIBDIR=/users/rlr/Courses/

# the actual run command 
$JAVADIR/bin/java -Djava.awt.headless=true -cp $USERDEFINEDLIBS:$PROJECTDIR/bin:$CSCS530LIBDIR/cscs530.jar  $PACKAGENAME.$BATCHMODELNAME $*


#
# Old stuff -- loading libs separately
#
#REPASTLIBDIR=/appl/repast/repast3.1/RepastJ
#CSCSLIBDIR=/appl/java/CSCS
#VISADLIBDIR=/appl/java/visad2.0
#COMMONSDIR=/appl/java/commons-collections-2.1
#JUNGDIR=/appl/java/jung1.1.1
#
# the actual run command
#$JAVADIR/bin/java -Djava.awt.headless=true -cp $USERDEFINEDLIBS:$PROJECTDIR/classes:$REPASTLIBDIR/repast.jar:$REPASTLIBDIR/lib/colt.jar:$REPASTLIBDIR/lib/jgl3.1.0.jar:$REPASTLIBDIR/lib/plot.jar:$REPASTLIBDIR/lib/trove.jar:$CSCSLIBDIR/graph3d.jar:$VISADLIBDIR/visad.jar:$COMMONSDIR/commons-collections.jar:$JUNGDIR/jung-1.1.1.jar $PACKAGENAME.$BATCHMODELNAME $*
