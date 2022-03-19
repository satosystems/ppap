#!/bin/sh

MODS=""
for v in `jdeps --list-deps --ignore-missing-deps ppap-$1.jar`
do
  if [ "$MODS" = "" ]
  then
    MODS=$v
  else
    MODS="$MODS,$v"
  fi
done
jlink --compress=2 --module-path /usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home/jmods --add-modules "$MODS" --output jre
unset MODS
