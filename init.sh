#!/bin/bash

git submodule update --init
cd document-viewer/jni/mupdf/mupdf
git submodule update --init
make generate
patch -p1 < ../overrides/fonts.patch
patch -p1 < ../overrides/nightmode_slowcmyk.patch
