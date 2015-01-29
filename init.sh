#!/bin/bash

git submodule update --init
cd document-viewer/jni/mupdf/mupdf
git submodule update --init
make generate
