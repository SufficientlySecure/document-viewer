#!/bin/bash

TARGET_DIR=../../res/drawable

echo "Remove old resources..."

rm -f $TARGET_DIR/*

echo "Copy XML resources..."

for i in `find . -depth -name "*.xml" -type f`
do
    NAME=`basename $i`
	cp $i $TARGET_DIR/$NAME
done

echo "Copy PNG resources..."

for i in `find . -depth -name "*.png" -type f`
do
    NAME=`basename $i`
	cp $i $TARGET_DIR/$NAME
done
