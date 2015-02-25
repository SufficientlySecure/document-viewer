#!/bin/bash

git submodule update --init --recursive
cd document-viewer/jni/mupdf/mupdf
make generate
patch -p1 < ../overrides/fonts.patch
patch -p1 < ../overrides/nightmode_slowcmyk.patch
