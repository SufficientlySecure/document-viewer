#include "fitz-internal.h"
#include "mupdf-internal.h"

#include <sys/stat.h>

char font_DroidSansMono[] = "/system/fonts/DroidSansMono.ttf";
char font_DroidSans[] = "/system/fonts/DroidSans.ttf";
char font_DroidSansBold[] = "/system/fonts/DroidSans-Bold.ttf";
char font_DroidSerifRegular[] = "/system/fonts/DroidSerif-Regular.ttf";
char font_DroidSerifBold[] = "/system/fonts/DroidSerif-Bold.ttf";
char font_DroidSerifItalic[] = "/system/fonts/DroidSerif-Italic.ttf";
char font_DroidSerifBoldItalic[] = "/system/fonts/DroidSerif-BoldItalic.ttf";
char font_StandardSym[] = "/sdcard/.org.ebookdroid/StandardSymL.cff";
char font_Dingbats[] = "/sdcard/.org.ebookdroid/Dingbats.cff";
char font_DroidSansFallback[] = "/system/fonts/DroidSansFallback.ttf";

//TODO: load from config file
char* ext_font_Courier = "/sdcard/.org.ebookdroid/fonts/FreeMono.ttf";
char* ext_font_CourierBold = "/sdcard/.org.ebookdroid/fonts/FreeMonoBold.ttf";
char* ext_font_CourierOblique = "/sdcard/.org.ebookdroid/fonts/FreeMonoOblique.ttf";
char* ext_font_CourierBoldOblique = "/sdcard/.org.ebookdroid/fonts/FreeMonoBoldOblique.ttf";
char* ext_font_Helvetica = "/sdcard/.org.ebookdroid/fonts/FreeSans.ttf";
char* ext_font_HelveticaBold = "/sdcard/.org.ebookdroid/fonts/FreeSansBold.ttf";
char* ext_font_HelveticaOblique = "/sdcard/.org.ebookdroid/fonts/FreeSansOblique.ttf";
char* ext_font_HelveticaBoldOblique = "/sdcard/.org.ebookdroid/fonts/FreeSans.ttf";
char* ext_font_TimesRoman = "/sdcard/.org.ebookdroid/fonts/FreeSerif.ttf";
char* ext_font_TimesBold = "/sdcard/.org.ebookdroid/fonts/FreeSerifBold.ttf";
char* ext_font_TimesItalic = "/sdcard/.org.ebookdroid/fonts/FreeSerifItalic.ttf";
char* ext_font_TimesBoldItalic = "/sdcard/.org.ebookdroid/fonts/FreeSerifBoldItalic.ttf";
char* ext_font_Symbol = "/sdcard/.org.ebookdroid/fonts/StandardSymL.cff";
char* ext_font_ZapfDingbats = "/sdcard/.org.ebookdroid/fonts/Dingbats.cff";

int file_exists(char * filename)
{
    struct stat info;
    int i = stat(filename, &info);
    /* File found */
    if ( i == 0 )
	return 1;
    return 0;
}

char* get_ext_font(char* font, char* def)
{
    if(font && file_exists(font))
	return font;
    return def;
}

unsigned char *
pdf_lookup_builtin_font(char *name, unsigned int *len)
{
	if (!strcmp("Courier", name)) {
		*len = 0;
		return (unsigned char*) get_ext_font(ext_font_Courier, font_DroidSansMono);
	}
	if (!strcmp("Courier-Bold", name)) {
		*len = 0;
		return (unsigned char*) get_ext_font(ext_font_CourierBold, font_DroidSansMono);
	}
	if (!strcmp("Courier-Oblique", name)) {
		*len = 0;
		return (unsigned char*) get_ext_font(ext_font_CourierOblique, font_DroidSansMono);
	}
	if (!strcmp("Courier-BoldOblique", name)) {
		*len = 0;
		return (unsigned char*) get_ext_font(ext_font_CourierBoldOblique, font_DroidSansMono);
	}
	if (!strcmp("Helvetica", name)) {
		*len = 0;
		return (unsigned char*) get_ext_font(ext_font_Helvetica, font_DroidSans);
	}
	if (!strcmp("Helvetica-Bold", name)) {
		*len = 0;
		return (unsigned char*) get_ext_font(ext_font_HelveticaBold, font_DroidSansBold);
	}
	if (!strcmp("Helvetica-Oblique", name)) {
		*len = 0;
		return (unsigned char*) get_ext_font(ext_font_HelveticaOblique, font_DroidSans);
	}
	if (!strcmp("Helvetica-BoldOblique", name)) {
		*len = 0;
		return (unsigned char*) get_ext_font(ext_font_HelveticaBoldOblique, font_DroidSansBold);
	}
	if (!strcmp("Times-Roman", name)) {
		*len = 0;
		return (unsigned char*) get_ext_font(ext_font_TimesRoman, font_DroidSerifRegular);
	}
	if (!strcmp("Times-Bold", name)) {
		*len = 0;
		return (unsigned char*) get_ext_font(ext_font_TimesBold, font_DroidSerifBold);
	}
	if (!strcmp("Times-Italic", name)) {
		*len = 0;
		return (unsigned char*) get_ext_font(ext_font_TimesItalic, font_DroidSerifItalic);
	}
	if (!strcmp("Times-BoldItalic", name)) {
		*len = 0;
		return (unsigned char*) get_ext_font(ext_font_TimesBoldItalic, font_DroidSerifBoldItalic);
	}
	if (!strcmp("Symbol", name)) {
		*len = 0;
		return (unsigned char*) get_ext_font(ext_font_Symbol, font_StandardSym);
	}
	if (!strcmp("ZapfDingbats", name)) {
		*len = 0;
		return (unsigned char*) get_ext_font(ext_font_ZapfDingbats, font_Dingbats);
	}
	*len = 0;
	return NULL;
}

unsigned char *
pdf_lookup_substitute_font(int mono, int serif, int bold, int italic, unsigned int *len)
{
	if (mono) {
		*len = 0;
		return (unsigned char*) font_DroidSansMono;
	} else {
		*len = 0;
		return (unsigned char*) font_DroidSans;
	}

}

unsigned char *
pdf_lookup_substitute_cjk_font(int ros, int serif, unsigned int *len)
{
	*len = 0;
	return (unsigned char*) font_DroidSansFallback;
}
