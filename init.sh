#!/bin/bash

git submodule update --init
cd document-viewer/jni/mupdf/mupdf
git submodule update --init
make generate
cd ..
# Causes malfunction at the moment
# patch -p0 < overrides/fonts.patch
patch -p0 < overrides/nightmode_slowcmyk.patch
