#!/bin/bash

git submodule update --init --recursive --force
git submodule foreach --recursive git clean -dfx

cd document-viewer/jni/mupdf/mupdf
make generate
#patch -p1 < ../overrides/fonts.patch
patch -p1 < ../overrides/nightmode_slowcmyk.patch
patch -p1 < ../overrides/cve-2017-14685.patch
patch -p1 < ../overrides/cve-2017-14686.patch
patch -p1 < ../overrides/cve-2017-14687.patch
patch -p1 < ../overrides/cve-2017-15587.patch

cd ../../djvu/djvulibre
patch -p1 < ../overrides/remove_print_save_api.patch
patch -p1 < ../overrides/compatibility.patch
