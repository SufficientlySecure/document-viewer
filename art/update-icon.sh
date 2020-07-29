#!/bin/bash

APP_DIR=../document-viewer
RES_DIR=src/main/res/
DIR=$APP_DIR/$RES_DIR/drawable
LDPI_DIR=$APP_DIR/$RES_DIR/drawable-ldpi
LDPI_DIR=$APP_DIR/$RES_DIR/drawable-ldpi
MDPI_DIR=$APP_DIR/$RES_DIR/drawable-mdpi
HDPI_DIR=$APP_DIR/$RES_DIR/drawable-hdpi
XDPI_DIR=$APP_DIR/$RES_DIR/drawable-xhdpi
XXDPI_DIR=$APP_DIR/$RES_DIR/drawable-xxhdpi
XXXDPI_DIR=$APP_DIR/$RES_DIR/drawable-xxxhdpi
PLAY_DIR=.


# Launcher Icon:
# -----------------------
# ldpi: 36x36
# mdpi: 48x48
# hdpi: 72x72
# xhdpi: 96x96
# xxhdpi: 144x144.
# xxxhdpi 192x192.
# google play: 512x512

NAME="application_icon"

inkscape -w 36  -h  36 -o "$LDPI_DIR/$NAME.png"   "$NAME.svg"
inkscape -w 48  -h  48 -o "$MDPI_DIR/$NAME.png"   "$NAME.svg"
inkscape -w 72  -h  72 -o "$HDPI_DIR/$NAME.png"   "$NAME.svg"
inkscape -w 96  -h  96 -o "$XDPI_DIR/$NAME.png"   "$NAME.svg"
inkscape -w 144 -h 144 -o "$XXDPI_DIR/$NAME.png"  "$NAME.svg"
inkscape -w 192 -h 192 -o "$XXXDPI_DIR/$NAME.png" "$NAME.svg"
inkscape -w 512 -h 512 -o "$DIR/$NAME.png"        "$NAME.svg"
# inkscape -w 512 -h 512 -o "$PLAY_DIR/$NAME.png"   "$NAME.svg"

NAME="application_icon_monochrome"
NAMEBLACK="application_icon_black"
NAMEWHITE="application_icon_white"

inkscape -w 36  -h  36 -o "$LDPI_DIR/$NAMEWHITE.png"   "$NAME.svg"
inkscape -w 48  -h  48 -o "$MDPI_DIR/$NAMEWHITE.png"   "$NAME.svg"
inkscape -w 72  -h  72 -o "$HDPI_DIR/$NAMEWHITE.png"   "$NAME.svg"
inkscape -w 96  -h  96 -o "$XDPI_DIR/$NAMEWHITE.png"   "$NAME.svg"
inkscape -w 144 -h 144 -o "$XXDPI_DIR/$NAMEWHITE.png"  "$NAME.svg"
inkscape -w 192 -h 192 -o "$XXXDPI_DIR/$NAMEWHITE.png" "$NAME.svg"
inkscape -w 512 -h 512 -o "$DIR/$NAMEWHITE.png"        "$NAME.svg"
# inkscape -w 512 -h 512 -o "$PLAY_DIR/$NAMEWHITE.png"   "$NAME.svg"

convert "$LDPI_DIR/$NAMEWHITE.png" -fuzz 99% -transparent white "$LDPI_DIR/$NAMEBLACK.png"
convert "$MDPI_DIR/$NAMEWHITE.png" -fuzz 99% -transparent white "$MDPI_DIR/$NAMEBLACK.png"
convert "$HDPI_DIR/$NAMEWHITE.png" -fuzz 99% -transparent white "$HDPI_DIR/$NAMEBLACK.png"
convert "$XDPI_DIR/$NAMEWHITE.png" -fuzz 99% -transparent white "$XDPI_DIR/$NAMEBLACK.png"
convert "$XXDPI_DIR/$NAMEWHITE.png" -fuzz 99% -transparent white "$XXDPI_DIR/$NAMEBLACK.png"
convert "$XXXDPI_DIR/$NAMEWHITE.png" -fuzz 99% -transparent white "$XXXDPI_DIR/$NAMEBLACK.png"
convert "$DIR/$NAMEWHITE.png" -fuzz 99% -transparent white "$DIR/$NAMEBLACK.png"
# convert "$PLAY_DIR/$NAMEWHITE.png" -fuzz 99% -transparent white "$PLAY_DIR/$NAMEBLACK.png"

convert "$LDPI_DIR/$NAMEBLACK.png" -negate "$LDPI_DIR/$NAMEWHITE.png"
convert "$MDPI_DIR/$NAMEBLACK.png" -negate "$MDPI_DIR/$NAMEWHITE.png"
convert "$HDPI_DIR/$NAMEBLACK.png" -negate "$HDPI_DIR/$NAMEWHITE.png"
convert "$XDPI_DIR/$NAMEBLACK.png" -negate "$XDPI_DIR/$NAMEWHITE.png"
convert "$XXDPI_DIR/$NAMEBLACK.png" -negate "$XXDPI_DIR/$NAMEWHITE.png"
convert "$XXXDPI_DIR/$NAMEBLACK.png" -negate "$XXXDPI_DIR/$NAMEWHITE.png"
convert "$DIR/$NAMEBLACK.png" -negate "$DIR/$NAMEWHITE.png"
# convert "$PLAY_DIR/$NAMEBLACK.png" -negate "$PLAY_DIR/$NAMEWHITE.png"
