#!/bin/bash

if [ "x$TIMESTAMP" == "x" ]; then
    echo "Hey, you forgot to specify TIMESTAMP."
    exit 1
fi

set -x
pushd ..
STUFFTOBUILD=`echo riso/{addons,approximation,apps,belief_nets,distributions,general,numerical,regression,remote_data,render}`

echo STUFFTOBUILD: $STUFFTOBUILD

l="riso/*.* riso/LICENSE `find $STUFFTOBUILD -name RCS -prune -o -type f -print`"
tar cvzf /tmp/riso-$TIMESTAMP.src.tar.gz $l

popd
set +x
