All icons and icons templates are free to use according to the GPL license version 3 or older.
Sources not directly created by the author[1] are either in the public domain with no restrictions about the way they can be used or comply with GPL license.

[1] At the moment they are

	- The world globe, taken from wikipedia and used for OPDS icons
	   http://en.wikipedia.org/wiki/File:Blank_globe.svg
	- ENDOFLIST

See folder "External sources" for more information on the licences associated with each graphic element used.



Notes
====================

Sources are big to ensure no pixelation occur during creation. Page sizes in pixels are multiple of 8.
Sources folder contains png versions as previews to help finding the corresponding icon source (they share the same name).

To create a button: 
-------------------
Create a copy of icontemplate.svg (yeha, I know, I should have named that buttontemplate...)
Open to edit, add content (by importing svg graphics or by editing them on the spot). 
This will ensure the button outline will be the same for all buttons.

foldertemplate_button.svg can be used to add content to folder buttons
folder buttons use flat 2D folders to maximaze real estate.

To create a variant of the actionbar folder icon:
-------------------------------------------------
Actionbar folder icons are now 3d slanted and darker.

To create actionbar 3D folders, use foldertemplate_actionbar.svg (it is the same as actionbar_open.svg)
This will make sure the baseline and overall position of the folder remain unchanged in all versions.

slanted Folders are slanted with 15 degress or 20 degrees in the y direction, according to type.
foldertemplate.svg contains a bigger version.




Things to do:
==============
- Redo the remaining icons in order to have script-resizable SVG sources.
- Align button content to a common baseline (for different items)
- Quick zoom ---> add a ruler under the lens instead of the plus (+) symbol.
- Absolutely redo the triangles in outline view!!! They are too big and obtrusive.




Possible eyecandy enhancements
==============================

Reversing gradient of action bar background. Right now (vers. 1.5.3 dev 1447) the gradient is such as to present a grey-whitish discontinuity near the top black bar of the screen and a fading to black toward Ebookdroid area. In this way the first black row in filebrowse mode looks bigger then the ones below.
Reversing the gradient, or removing it could fix this asymmetry.

Outline view: 
reducing the size of the triangles, most notably their height, using an aspectratio = goldenration for both triangles.
Also, removing gradient coloring from the rows of the outline could greatly improve its appearance without noticeably hindering navigation (users will tap on text).
A fully black (or other uniform color) background should appear as more 'elegant'.

If possible, add a line under each first level section, once opened.

