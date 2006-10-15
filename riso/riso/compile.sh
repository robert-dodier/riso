#!/bin/bash

if [ "x$CLASSDIR" == "x" ]; then CLASSDIR=/tmp/java; fi

if [ ! -e $CLASSDIR ]; then echo "mkdir $CLASSDIR since it does not exist"; mkdir -p $CLASSDIR; fi

export CLASSPATH="/home/robert/by-others/grappa/grappa-1.2/archives/grappa1_2.jar:/usr/local/JavaDeps-1.1"

export CLASSPATH="$CLASSDIR:$CLASSPATH"

# use ``which'' to defeat aliases.

JAVAC="`which javac` -g -d $CLASSDIR -source 1.4"
RMIC="`which rmic` -d $CLASSDIR"

echo CLASSDIR: $CLASSDIR
echo CLASSPATH: $CLASSPATH
echo JAVAC: $JAVAC
echo RMIC: $RMIC

set -x
pushd ..
STUFFTOBUILD=`echo riso/{addons,approximation,apps,belief_nets,distributions,general,numerical,regression,remote_data,render}`

echo STUFFTOBUILD: $STUFFTOBUILD

java smr.JavaDeps.JavaDeps -v -o riso/tmp-riso.deps -d $CLASSDIR `find $STUFFTOBUILD -name \*.java`

cat << EOF > riso/tmp-riso.makefile
JAVACOMPILE=$JAVAC

default: all
include riso/tmp-riso.deps
all: \$(CLASSES)
EOF

make -f riso/tmp-riso.makefile
popd

$RMIC riso.belief_nets.BeliefNetwork
$RMIC riso.belief_nets.TemporalBeliefNetwork
$RMIC riso.belief_nets.BeliefNetworkContext
$RMIC riso.belief_nets.Variable     

$RMIC riso.remote_data.RemoteObservableImpl
$RMIC riso.remote_data.RemoteObserverImpl

$RMIC riso.render.PlotPanel
set +x
