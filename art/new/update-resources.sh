#!/bin/bash

TARGET_DIR=../../res/drawable
XDPI_DIR=../../res/drawable-xhdpi
HDPI_DIR=../../res/drawable-hdpi
LDPI_DIR=../../res/drawable-ldpi
MDPI_DIR=../../res/drawable-mdpi

XL_MDPI_DIR=../../res/drawable-xlarge-mdpi
XL_HDPI_DIR=../../res/drawable-xlarge-hdpi

echo "Remove old resources..."

rm -f $TARGET_DIR/*.png $TARGET_DIR/*.xml

echo "Copy XML resources..."

for i in `find . -depth -name "*.xml" -type f`
do
	NAME=`basename $i`
	cp $i "$TARGET_DIR/$NAME"
done

echo "Copy PNG resources..."

for i in `find . -maxdepth 1 -name "*.png" -type f`
do
	NAME=`basename $i`
	cp $i "$TARGET_DIR/$NAME"
done

for i in `find activities -depth -name "*.png" -type f`
do
	NAME=`basename $i`
	cp "$i" "$TARGET_DIR/$NAME"
done
for i in `find components -depth -name "*.png" -type f`
do
    NAME=`basename $i`
	cp "$i" "$TARGET_DIR/$NAME"
done

echo "Convert SVG actionbar and menu resources to PNG..."

for i in `find activities -depth -name "*.svg" -type f`
do
	NAME=`basename $i .svg`
	TYPE=`echo $NAME | awk '/.*_actionbar_.*/ { print "ACTIONBAR"; } /.*_menu_.*/ { print "MENU"; }'`

	echo "$TYPE $i"

	if [ "$TYPE" == "ACTIONBAR" ];
	then
		if [ "$HDPI_DIR/$NAME.png" -ot "$i" ];
		then
			inkscape -w 48 -h 48 -e "$HDPI_DIR/$NAME.png" $i
		fi
		if [ "$MDPI_DIR/$NAME.png" -ot "$i" ];
		then
			inkscape -w 36 -h 36 -e "$MDPI_DIR/$NAME.png" $i
		fi
		if [ "$LDPI_DIR/$NAME.png" -ot "$i" ];
		then
		inkscape -w 24 -h 24 -e "$LDPI_DIR/$NAME.png" $i
		fi

		cp "$MDPI_DIR/$NAME.png" "$TARGET_DIR/$NAME.png"

	elif [ "$TYPE" == "MENU" ];
	then
		if [ "$HDPI_DIR/$NAME.png" -ot "$i" ];
		then
			inkscape -w 48 -h 48 -e "$HDPI_DIR/$NAME.png" $i
		fi
		if [ "$MDPI_DIR/$NAME.png" -ot "$i" ];
		then
			inkscape -w 32 -h 32 -e "$MDPI_DIR/$NAME.png" $i
		fi
		if [ "$LDPI_DIR/$NAME.png" -ot "$i" ];
		then
			inkscape -w 24 -h 24 -e "$LDPI_DIR/$NAME.png" $i
		fi

		cp "$MDPI_DIR/$NAME.png" "$TARGET_DIR/$NAME.png"

	else
		if [ "$HDPI_DIR/$NAME.png" -ot "$i" ];
		then
			inkscape -w 48 -h 48 -e "$HDPI_DIR/$NAME.png" $i
		fi
		if [ "$MDPI_DIR/$NAME.png" -ot "$i" ];
		then
			inkscape -w 32 -h 32 -e "$MDPI_DIR/$NAME.png" $i
		fi
		if [ "$LDPI_DIR/$NAME.png" -ot "$i" ];
		then
			inkscape -w 24 -h 24 -e "$LDPI_DIR/$NAME.png" $i
		fi

		cp "$MDPI_DIR/$NAME.png" "$TARGET_DIR/$NAME.png"
	fi

done

