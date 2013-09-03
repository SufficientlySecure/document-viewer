# Document Viewer

Document Viewer is a fork of the last GPL version of EBookDroid (http://code.google.com/p/ebookdroid/).

We need your support to fix outstanding bugs, join development by forking the project!

Supports the following formats:
* PDF
* DjVu
* XPS (OpenXPS)
* Comic Books (cbz) (NO support for cbr (rar compressed))
* FictionBook (fb2, fb2.zip)

Additional fonts can be installed using the [Document Viewer Fontpack](https://github.com/dschuermann/document-viewer-fontpack).

General information about supported MIME types and available Intents can be found in the [Wiki](https://github.com/dschuermann/document-viewer/wiki).

## Build

1. ``cd document-viewer``
2. ``ndk-build``
3. ``ant debug``

## Font Pack

To save space in the main app, additional fonts can be installed with the fontpack: https://github.com/dschuermann/document-viewer-fontpack

# Licenses
Document Viewer is licensed under the GPLv3+.  
The file LICENSE includes the full license text.

## Details
Document Viewer is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Document Viewer is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Document Viewer.  If not, see <http://www.gnu.org/licenses/>.

## Java Libraries
* JCIFS  
  http://jcifs.samba.org/  
  LGPL v2.1

* Color Picker by Daniel Nilsson  
  http://code.google.com/p/color-picker-view/  
  Apache License v2

* VDT-XML by XimpleWare  
  http://vtd-xml.sourceforge.net/  
  GPLv2+


## C Libraries

* MuPDF - a lightweight PDF and XPS viewer   
  http://www.mupdf.com/  
  AGPLv3+

* djvu - a lightweight DJVU viewer based on DjVuLibre  
  http://djvu.sourceforge.net/  
  GPLv2

* cbx - a lightweight Comicbook viewer based on   
    - libPng  
    http://www.libpng.org/pub/png/libpng.html  
    libpng License (OSI Certified)
    - libjpeg  
    http://libjpeg.sourceforge.net/  
    libjpeg License
    - GIFLIB
    - ZLib   
