#!/bin/bash

if [ "x$CLASSDIR" == "x" ]; then CLASSDIR=/tmp/java; fi

if [ ! -e $CLASSDIR ]; then echo "mkdir $CLASSDIR since it does not exist"; mkdir -p $CLASSDIR; fi

export CLASSPATH=$CLASSDIR:$CLASSPATH

# use ``which'' to defeat aliases.

JAVAC="`which javac` -g -d $CLASSDIR"
RMIC="`which rmic` -d $CLASSDIR"

echo CLASSDIR: $CLASSDIR
echo CLASSPATH: $CLASSPATH
echo JAVAC: $JAVAC
echo RMIC: $RMIC

$JAVAC belief_nets/Global.java

pushd remote_data; $JAVAC *.java; popd
pushd general; $JAVAC *.java; popd
pushd numerical; $JAVAC *.java; popd

pushd distributions
$JAVAC Abstract* Distribution.java ConditionalDistribution.java MixGaussians.java Gaussian.java Mixture.java LocationScaleDensity.java Translatable.java SupportNotWellDefinedException.java SplineDensity.java
popd

pushd approximation; $JAVAC *.java; popd
pushd regression; $JAVAC *.java; popd

$JAVAC `find belief_nets/ -name \*.java` `find distributions/  -name \*.java`

pushd apps; $JAVAC *.java; popd

pushd addons/dbn; $JAVAC *.java; popd

pushd render; $JAVAC *.java; popd

# FOR NOW, test IS NOT PACKAGED WITH THE OTHER STUFF. !!!
# pushd test; $JAVAC *.java; popd

$RMIC riso.belief_nets.BeliefNetwork
$RMIC riso.belief_nets.TemporalBeliefNetwork
$RMIC riso.belief_nets.BeliefNetworkContext
$RMIC riso.belief_nets.Variable     

$RMIC riso.remote_data.RemoteObservableImpl
$RMIC riso.remote_data.RemoteObserverImpl

$RMIC riso.render.PlotPanel
