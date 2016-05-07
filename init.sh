#!/bin/bash

git submodule update --init --recursive --force

cd document-viewer/jni/mupdf/mupdf
make generate
#patch -p1 < ../overrides/fonts.patch
patch -p1 < ../overrides/nightmode_slowcmyk.patch

cd ../../djvu/djvulibre
patch -p1 < ../overrides/remove_print_save_api.patch
patch -p1 < ../overrides/compatibility.patch
