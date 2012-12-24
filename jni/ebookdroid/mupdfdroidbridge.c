#include <jni.h>

#include <android/log.h>

#include <javahelpers.h>

#include <fitz.h>
#include <mupdf.h>
#include <muxps.h>

#include <ebookdroid.h>

#define FORMAT_PDF 0
#define FORMAT_XPS 1

/* Debugging helper */

#define LCTX "EBookDroid.MuPDF"

#define DEBUG(args...) \
    __android_log_print(ANDROID_LOG_DEBUG, LCTX, args)

#define ERROR(args...) \
    __android_log_print(ANDROID_LOG_ERROR, LCTX, args)

#define INFO(args...) \
    __android_log_print(ANDROID_LOG_INFO, LCTX, args)

typedef struct renderdocument_s renderdocument_t;
struct renderdocument_s
{
    fz_context *ctx;
    fz_document *document;
    fz_outline *outline;
    unsigned char format; // save current document format.
};

typedef struct renderpage_s renderpage_t;
struct renderpage_s
{
    fz_context *ctx;
    fz_page *page;
    int number;
    fz_display_list* pageList;
};

#define RUNTIME_EXCEPTION "java/lang/RuntimeException"
#define PASSWORD_REQUIRED_EXCEPTION "org/ebookdroid/droids/mupdf/codec/exceptions/MuPdfPasswordRequiredException"
#define WRONG_PASSWORD_EXCEPTION "org/ebookdroid/droids/mupdf/codec/exceptions/MuPdfWrongPasswordEnteredException"

extern fz_locks_context * jni_new_locks();
extern void jni_free_locks(fz_locks_context *locks);

extern char ext_font_Courier[1024]; //= "/sdcard/.org.ebookdroid/fonts/FreeMono.ttf";
extern char ext_font_CourierBold[1024]; // = "/sdcard/.org.ebookdroid/fonts/FreeMonoBold.ttf";
extern char ext_font_CourierOblique[1024]; // = "/sdcard/.org.ebookdroid/fonts/FreeMonoOblique.ttf";
extern char ext_font_CourierBoldOblique[1024]; // = "/sdcard/.org.ebookdroid/fonts/FreeMonoBoldOblique.ttf";
extern char ext_font_Helvetica[1024]; // = "/sdcard/.org.ebookdroid/fonts/FreeSans.ttf";
extern char ext_font_HelveticaBold[1024]; // = "/sdcard/.org.ebookdroid/fonts/FreeSansBold.ttf";
extern char ext_font_HelveticaOblique[1024]; // = "/sdcard/.org.ebookdroid/fonts/FreeSansOblique.ttf";
extern char ext_font_HelveticaBoldOblique[1024]; // = "/sdcard/.org.ebookdroid/fonts/FreeSans.ttf";
extern char ext_font_TimesRoman[1024]; // = "/sdcard/.org.ebookdroid/fonts/FreeSerif.ttf";
extern char ext_font_TimesBold[1024]; // = "/sdcard/.org.ebookdroid/fonts/FreeSerifBold.ttf";
extern char ext_font_TimesItalic[1024]; // = "/sdcard/.org.ebookdroid/fonts/FreeSerifItalic.ttf";
extern char ext_font_TimesBoldItalic[1024]; // = "/sdcard/.org.ebookdroid/fonts/FreeSerifBoldItalic.ttf";
extern char ext_font_Symbol[1024]; // = "/sdcard/.org.ebookdroid/fonts/StandardSymL.cff";
extern char ext_font_ZapfDingbats[1024]; // = "/sdcard/.org.ebookdroid/fonts/Dingbats.cff";

void mupdf_throw_exception_ex(JNIEnv *env, const char* exception, char *message)
{
    jthrowable new_exception = (*env)->FindClass(env, exception);
    if (new_exception == NULL)
    {
        DEBUG("Exception class not found: '%s'", exception);
        return;
    }
    DEBUG("Exception '%s', Message: '%s'", exception, message);
    (*env)->ThrowNew(env, new_exception, message);
}

void mupdf_throw_exception(JNIEnv *env, char *message)
{
    mupdf_throw_exception_ex(env, RUNTIME_EXCEPTION, message);
}

static void mupdf_free_document(renderdocument_t* doc)
{
    if (!doc)
    {
        return;
    }

    fz_locks_context *locks = doc->ctx->locks;

    if (doc->outline)
    {
        fz_free_outline(doc->ctx, doc->outline);
    }
    doc->outline = NULL;

    if (doc->document)
    {
        fz_close_document(doc->document);
    }
    doc->document = NULL;

    fz_flush_warnings(doc->ctx);
    fz_free_context(doc->ctx);
    doc->ctx = NULL;

    jni_free_locks(locks);

    free(doc);
    doc = NULL;
}

void setFontFileName(char* ext_Font, const char* fontFileName)
{
    if (fontFileName && fontFileName[0])
    {
        strcpy(ext_Font, fontFileName);
    }
    else
    {
        ext_Font[0] = 0;
    }
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfContext_setMonoFonts(JNIEnv *env, jclass clazz, jstring regular,
                                                                 jstring italic, jstring bold, jstring boldItalic)
{
    jboolean iscopy;
    const char* regularFFN = GetStringUTFChars(env, regular, &iscopy);
    const char* italicFFN = GetStringUTFChars(env, italic, &iscopy);
    const char* boldFFN = GetStringUTFChars(env, bold, &iscopy);
    const char* boldItalicFFN = GetStringUTFChars(env, boldItalic, &iscopy);

    setFontFileName(ext_font_Courier, regularFFN);
    setFontFileName(ext_font_CourierOblique, italicFFN);
    setFontFileName(ext_font_CourierBold, boldFFN);
    setFontFileName(ext_font_CourierBoldOblique, boldItalicFFN);

    ReleaseStringUTFChars(env, regular, regularFFN);
    ReleaseStringUTFChars(env, italic, italicFFN);
    ReleaseStringUTFChars(env, bold, boldFFN);
    ReleaseStringUTFChars(env, boldItalic, boldItalicFFN);
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfContext_setSansFonts(JNIEnv *env, jclass clazz, jstring regular,
                                                                 jstring italic, jstring bold, jstring boldItalic)
{
    jboolean iscopy;
    const char* regularFFN = GetStringUTFChars(env, regular, &iscopy);
    const char* italicFFN = GetStringUTFChars(env, italic, &iscopy);
    const char* boldFFN = GetStringUTFChars(env, bold, &iscopy);
    const char* boldItalicFFN = GetStringUTFChars(env, boldItalic, &iscopy);

    setFontFileName(ext_font_Helvetica, regularFFN);
    setFontFileName(ext_font_HelveticaOblique, italicFFN);
    setFontFileName(ext_font_HelveticaBold, boldFFN);
    setFontFileName(ext_font_HelveticaBoldOblique, boldItalicFFN);

    ReleaseStringUTFChars(env, regular, regularFFN);
    ReleaseStringUTFChars(env, italic, italicFFN);
    ReleaseStringUTFChars(env, bold, boldFFN);
    ReleaseStringUTFChars(env, boldItalic, boldItalicFFN);
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfContext_setSerifFonts(JNIEnv *env, jclass clazz, jstring regular,
                                                                 jstring italic, jstring bold, jstring boldItalic)
{
    jboolean iscopy;
    const char* regularFFN = GetStringUTFChars(env, regular, &iscopy);
    const char* italicFFN = GetStringUTFChars(env, italic, &iscopy);
    const char* boldFFN = GetStringUTFChars(env, bold, &iscopy);
    const char* boldItalicFFN = GetStringUTFChars(env, boldItalic, &iscopy);

    setFontFileName(ext_font_TimesRoman, regularFFN);
    setFontFileName(ext_font_TimesItalic, italicFFN);
    setFontFileName(ext_font_TimesBold, boldFFN);
    setFontFileName(ext_font_TimesBoldItalic, boldItalicFFN);

    ReleaseStringUTFChars(env, regular, regularFFN);
    ReleaseStringUTFChars(env, italic, italicFFN);
    ReleaseStringUTFChars(env, bold, boldFFN);
    ReleaseStringUTFChars(env, boldItalic, boldItalicFFN);
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfContext_setSymbolFont(JNIEnv *env, jclass clazz, jstring regular)
{
    jboolean iscopy;
    const char* regularFFN = GetStringUTFChars(env, regular, &iscopy);

    setFontFileName(ext_font_Symbol, regularFFN);

    ReleaseStringUTFChars(env, regular, regularFFN);
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfContext_setDingbatFont(JNIEnv *env, jclass clazz, jstring regular)
{
    jboolean iscopy;
    const char* regularFFN = GetStringUTFChars(env, regular, &iscopy);

    setFontFileName(ext_font_ZapfDingbats, regularFFN);

    ReleaseStringUTFChars(env, regular, regularFFN);
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_open(JNIEnv *env, jclass clazz, jint storememory, jint format, jstring fname,
                                                      jstring pwd)
{
    renderdocument_t *doc;
    jboolean iscopy;
    jclass cls;
    jfieldID fid;
    char *filename;
    char *password;

    filename = (char*) (*env)->GetStringUTFChars(env, fname, &iscopy);
    password = (char*) (*env)->GetStringUTFChars(env, pwd, &iscopy);

    doc = malloc(sizeof(renderdocument_t));
    if (!doc)
    {
        mupdf_throw_exception(env, "Out of Memory");
        goto cleanup;
    }
    DEBUG("MuPdfDocument.nativeOpen(): storememory = %d", storememory);

    fz_locks_context *locks = jni_new_locks();
    if (!locks)
    {
        DEBUG("MuPdfDocument.nativeOpen(): no locks available");
    }
    doc->ctx = fz_new_context(NULL, locks, storememory);
    if (!doc->ctx)
    {
        free(doc);
        mupdf_throw_exception(env, "Out of Memory");
        goto cleanup;
    }
    doc->document = NULL;
    doc->outline = NULL;

//    fz_set_aa_level(fz_catch(ctx), alphabits);
    doc->format = format;
    fz_try(doc->ctx)
    {
    if(format == FORMAT_XPS)
        doc->document = (fz_document*) xps_open_document(doc->ctx, filename);
    else // FORMAT_PDF
        doc->document = (fz_document*) pdf_open_document(doc->ctx, filename);

//        doc->document = fz_open_document(doc->ctx, filename);
    }
    fz_catch(doc->ctx)
    {
        mupdf_throw_exception(env, (char*) fz_caught(doc->ctx));
        mupdf_free_document(doc);
//        mupdf_throw_exception(env, "PDF file not found or corrupted");
        goto cleanup;
    }

    /*
     * Handle encrypted PDF files
     */

    if (fz_needs_password(doc->document))
    {
        if (strlen(password))
        {
            int ok = fz_authenticate_password(doc->document, password);
            if (!ok)
            {
                mupdf_free_document(doc);
                mupdf_throw_exception_ex(env, WRONG_PASSWORD_EXCEPTION, "Wrong password given");
                goto cleanup;
            }
        }
        else
        {
            mupdf_free_document(doc);
            mupdf_throw_exception_ex(env, PASSWORD_REQUIRED_EXCEPTION, "Document needs a password!");
            goto cleanup;
        }
    }

    cleanup:

    (*env)->ReleaseStringUTFChars(env, fname, filename);
    (*env)->ReleaseStringUTFChars(env, pwd, password);

    // DEBUG("MuPdfDocument.nativeOpen(): return handle = %p", doc);
    return (jlong) (long) doc;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_free(JNIEnv *env, jclass clazz, jlong handle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) handle;
    mupdf_free_document(doc);
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_getPageInfo(JNIEnv *env, jclass cls, jlong handle, jint pageNumber,
                                                             jobject cpi)
{
    renderdocument_t *doc = (renderdocument_t*) (long) handle;

    //TODO: Review this. Possible broken

    fz_page *page = NULL;
    fz_rect bounds;

    jclass clazz;
    jfieldID fid;

    fz_try(doc->ctx)
    {
    page = fz_load_page(doc->document, pageNumber - 1);
    bounds = fz_bound_page(doc->document, page);
    }
    fz_catch(doc->ctx)
    {
        return -1;
    }

    if (page)
    {
        clazz = (*env)->GetObjectClass(env, cpi);
        if (0 == clazz)
        {
            return (-1);
        }

        fid = (*env)->GetFieldID(env, clazz, "width", "I");
        (*env)->SetIntField(env, cpi, fid, bounds.x1-bounds.x0);

        fid = (*env)->GetFieldID(env, clazz, "height", "I");
        (*env)->SetIntField(env, cpi, fid, bounds.y1-bounds.y0);

        fid = (*env)->GetFieldID(env, clazz, "dpi", "I");
        (*env)->SetIntField(env, cpi, fid, 0);

        fid = (*env)->GetFieldID(env, clazz, "rotation", "I");
        (*env)->SetIntField(env, cpi, fid, 0);

        fid = (*env)->GetFieldID(env, clazz, "version", "I");
        (*env)->SetIntField(env, cpi, fid, 0);

        fz_free_page(doc->document, page);
        return 0;
    }
    return -1;
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getFirstPageLink(JNIEnv *env, jclass clazz, jlong handle,
                                                               jlong pagehandle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) handle;
    renderpage_t *page = (renderpage_t*) (long) pagehandle;
    return (jlong)(long)((page && doc)?fz_load_links(doc->document, page->page):NULL);
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getNextPageLink(JNIEnv *env, jclass clazz, jlong linkhandle)
{
    fz_link *link = (fz_link*) (long) linkhandle;
    return (jlong)(long)(link ? link->next : NULL);
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getPageLinkType(JNIEnv *env, jclass clazz, jlong linkhandle)
{
    fz_link *link = (fz_link*) (long) linkhandle;
    return (jint)(link ? link->dest.kind : FZ_LINK_NONE);
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getPageLinkUrl(JNIEnv *env, jclass clazz, jlong linkhandle)
{
    fz_link *link = (fz_link*) (long) linkhandle;

    if (!link || link->dest.kind != FZ_LINK_URI)
    {
        return NULL;
    }

    char linkbuf[1024];
    snprintf(linkbuf, 1023, "%s", link->dest.ld.uri.uri);

    return (*env)->NewStringUTF(env, linkbuf);
}

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_fillPageLinkSourceRect(JNIEnv *env, jclass clazz, jlong linkhandle,
                                                                     jfloatArray boundsArray)
{
    fz_link *link = (fz_link*) (long) linkhandle;

    if (!link || link->dest.kind != FZ_LINK_GOTO)
    {
        return JNI_FALSE;
    }

    jfloat *bounds = (*env)->GetPrimitiveArrayCritical(env, boundsArray, 0);
    if (!bounds)
    {
        return JNI_FALSE;
    }

    bounds[0] = link->rect.x0;
    bounds[1] = link->rect.y0;
    bounds[2] = link->rect.x1;
    bounds[3] = link->rect.y1;

    (*env)->ReleasePrimitiveArrayCritical(env, boundsArray, bounds, 0);

    return JNI_TRUE;
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_getPageLinkTargetPage(JNIEnv *env, jclass clazz, jlong linkhandle)
{
    fz_link *link = (fz_link*) (long) linkhandle;

    if (!link || link->dest.kind != FZ_LINK_GOTO)
    {
        return (jint)-1;
    }

    return (jint)link->dest.ld.gotor.page;
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_fillPageLinkTargetPoint(JNIEnv *env, jclass clazz, jlong linkhandle,
                                                                      jfloatArray pointArray)
{
    fz_link *link = (fz_link*) (long) linkhandle;

    if (!link || link->dest.kind != FZ_LINK_GOTO)
    {
        return 0;
    }

    jfloat *point = (*env)->GetPrimitiveArrayCritical(env, pointArray, 0);
    if (!point)
    {
        return 0;
    }

//    DEBUG("MuPdfLinks_fillPageLinkTargetPoint(): %d %x (%f, %f) - (%f, %f)",
//          link->dest.ld.gotor.page,
//          link->dest.ld.gotor.flags,
//          link->dest.ld.gotor.lt.x, link->dest.ld.gotor.lt.y,
//          link->dest.ld.gotor.rb.x, link->dest.ld.gotor.rb.y);

    point[0] = link->dest.ld.gotor.lt.x;
    point[1] = link->dest.ld.gotor.lt.y;

    (*env)->ReleasePrimitiveArrayCritical(env, pointArray, point, 0);

    return link->dest.ld.gotor.flags;
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfDocument_getPageCount(JNIEnv *env, jclass clazz, jlong handle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) handle;
    return (fz_count_pages(doc->document));
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_open(JNIEnv *env, jclass clazz, jlong dochandle, jint pageno)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page = NULL;
    fz_device *dev = NULL;

//    DEBUG("MuPdfPage_open(%p, %d): start", doc, pageno);

    fz_context* ctx = fz_clone_context(doc->ctx);
    if (!ctx)
    {
        mupdf_throw_exception(env, "Context cloning failed");
        return (jlong) (long) NULL;
    }

    page = fz_malloc_no_throw(ctx, sizeof(renderpage_t));
//    DEBUG("MuPdfPage_open(%p, %d): page=%p", doc, pageno, page);

    if (!page)
    {
        mupdf_throw_exception(env, "Out of Memory");
        return (jlong) (long) NULL;
    }

    page->ctx = ctx;
    page->page = NULL;
    page->pageList = NULL;

    fz_try(ctx)
    {
        page->pageList = fz_new_display_list(ctx);
        dev = fz_new_list_device(ctx, page->pageList);
        page->page = fz_load_page(doc->document, pageno - 1);
        fz_run_page(doc->document, page->page, dev, fz_identity, NULL);
    }
    fz_always(ctx)
    {
        fz_free_device(dev);
    }
    fz_catch(ctx)
    {
        fz_free_device(dev);
        fz_free_display_list(ctx, page->pageList);
        fz_free_page(doc->document, page->page);

        fz_free(ctx, page);
        fz_free_context(ctx);

        page = NULL;
        ctx = NULL;
        mupdf_throw_exception(env, "error loading page");
    }

//    DEBUG("MuPdfPage_open(%p, %d): finish: %p", doc, pageno, page);

    return (jlong) (long) page;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_free(JNIEnv *env, jclass clazz, jlong dochandle, jlong handle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page = (renderpage_t*) (long) handle;
//    DEBUG("MuPdfPage_free(%p): start", page);

    if (!page || !page->ctx)
    {
        DEBUG("No page to free");
        return;
    }

    fz_context *ctx = page->ctx;

    if (page->pageList)
    {
        fz_free_display_list(ctx, page->pageList);
    }

    if (page->page)
    {
        fz_free_page(doc->document, page->page);
    }

    fz_free(ctx, page);
    fz_free_context(ctx);
    page = NULL;
    ctx = NULL;

//    DEBUG("MuPdfPage_free(%p): finish", page);
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_getBounds(JNIEnv *env, jclass clazz, jlong dochandle, jlong handle,
                                                       jfloatArray bounds)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page = (renderpage_t*) (long) handle;
    jfloat *bbox = (*env)->GetPrimitiveArrayCritical(env, bounds, 0);
    if (!bbox)
        return;
    fz_rect page_bounds = fz_bound_page(doc->document, page->page);
    // DEBUG("Bounds: %f %f %f %f", page_bounds.x0, page_bounds.y0, page_bounds.x1, page_bounds.y1);
    bbox[0] = page_bounds.x0;
    bbox[1] = page_bounds.y0;
    bbox[2] = page_bounds.x1;
    bbox[3] = page_bounds.y1;
    (*env)->ReleasePrimitiveArrayCritical(env, bounds, bbox, 0);
}


JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_renderPageDirect(JNIEnv *env, jobject this, jlong dochandle,
                                                              jlong pagehandle, jintArray viewboxarray,
                                                              jfloatArray matrixarray, jobject byteBuffer, jint nightmode, jint slowcmyk)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page = (renderpage_t*) (long) pagehandle;

//    DEBUG("MuPdfPage_renderPageBitmap(%p, %p): start", doc, page);

    fz_matrix ctm;
    fz_bbox viewbox;
    fz_pixmap *pixmap;
    jfloat *matrix;
    jint *viewboxarr;
    jint *dimen;
    int length, val;
    fz_device *dev = NULL;

    void *pixels;

    int ret;

    pixels = (*env)->GetDirectBufferAddress(env, byteBuffer);
    if (!pixels) {
        ERROR("GetDirectBufferAddress failed!");
        return JNI_FALSE;
    }

    matrix = (*env)->GetPrimitiveArrayCritical(env, matrixarray, 0);
    ctm = fz_identity;
    ctm.a = matrix[0];
    ctm.b = matrix[1];
    ctm.c = matrix[2];
    ctm.d = matrix[3];
    ctm.e = matrix[4];
    ctm.f = matrix[5];
    (*env)->ReleasePrimitiveArrayCritical(env, matrixarray, matrix, 0);

    viewboxarr = (*env)->GetPrimitiveArrayCritical(env, viewboxarray, 0);
    viewbox.x0 = viewboxarr[0];
    viewbox.y0 = viewboxarr[1];
    viewbox.x1 = viewboxarr[2];
    viewbox.y1 = viewboxarr[3];
    (*env)->ReleasePrimitiveArrayCritical(env, viewboxarray, viewboxarr, 0);

    fz_context* ctx = page->ctx;
    if (!ctx)
    {
        ERROR("No page context");
        return JNI_FALSE;
    }

    //add check for night mode and set global variable accordingly
    ctx->ebookdroid_nightmode = nightmode;

    //add check for slowcmyk mode and set global variable accordingly
    ctx->ebookdroid_slowcmyk = slowcmyk;

    fz_try(ctx)
    {
         pixmap = fz_new_pixmap_with_data(ctx, fz_device_rgb, viewbox.x1 - viewbox.x0, viewbox.y1 - viewbox.y0, pixels);

         fz_clear_pixmap_with_value(ctx, pixmap, 0xff);

         dev = fz_new_draw_device(ctx, pixmap);

         fz_run_display_list(page->pageList, dev, ctm, viewbox, NULL);
    }
    fz_always(ctx)
    {
       fz_free_device(dev);
       fz_drop_pixmap(ctx, pixmap);
    }
    fz_catch(ctx)
    {
        DEBUG("Render failed");
    }
    return JNI_TRUE;
}

static fz_text_char textcharat(fz_text_page *page, int idx)
{
    static fz_text_char emptychar = { { 0, 0, 0, 0 }, ' ' };
    fz_text_block *block;
    fz_text_line *line;
    fz_text_span *span;
    int ofs = 0;
    for (block = page->blocks; block < page->blocks + page->len; block++)
    {
        for (line = block->lines; line < block->lines + block->len; line++)
        {
            for (span = line->spans; span < line->spans + line->len; span++)
            {
                if (idx < ofs + span->len)
                {
                    return span->text[idx - ofs];
                }
                if (span + 1 == line->spans + line->len)
                {
                    if (idx == ofs + span->len)
                    {
                        return emptychar;
                    }
                    ofs++;
                }
                ofs += span->len;
            }
        }
    }
    return emptychar;
}

static int charat(fz_text_page *page, int idx)
{
    return textcharat(page, idx).c;
}

static fz_bbox bboxcharat(fz_text_page *page, int idx)
{
    return fz_round_rect(textcharat(page, idx).bbox);
}

static int textlen(fz_text_page *page)
{
    fz_text_block *block;
    fz_text_line *line;
    fz_text_span *span;
    int len = 0;
    for (block = page->blocks; block < page->blocks + page->len; block++)
    {
        for (line = block->lines; line < block->lines + block->len; line++)
        {
            for (span = line->spans; span < line->spans + line->len; span++)
            {
                len += span->len;
            }
            len++;
        }
    }
    return len;
}

static int match(CharacterHelper* ch, fz_text_page *page, const char *s, int n)
{
    int orig = n;
    int c;

    while (*s)
    {
        s += fz_chartorune(&c, (char *) s);
        if (c == ' ' && charat(page, n) == ' ')
        {
            while (charat(page, n) == ' ')
            {
                n++;
            }
        }
        else
        {
            if (c != CharacterHelper_toLowerCase(ch, charat(page, n)))
            {
                return 0;
            }
            n++;
        }
    }
    return n - orig;
}

JNIEXPORT jobjectArray JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_search(JNIEnv * env, jobject thiz, jlong dochandle, jlong pagehandle,
                                                        jstring text)
{

    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page = (renderpage_t*) (long) pagehandle;
    // DEBUG("MuPdfPage(%p).search(%p, %p)", thiz, doc, page);

    if (!doc || !page)
    {
        return NULL;
    }

    const char *str = (*env)->GetStringUTFChars(env, text, NULL);
    if (str == NULL)
    {
        return NULL;
    }

    ArrayListHelper alh;
    PageTextBoxHelper ptbh;
    CharacterHelper ch;

    if (!ArrayListHelper_init(&alh, env) || !PageTextBoxHelper_init(&ptbh, env)|| !CharacterHelper_init(&ch, env))
    {
        DEBUG("search(): JNI helper initialization failed", pagehandle);
        return NULL;
    }
    jobject arrayList = ArrayListHelper_create(&alh);
    // DEBUG("MuPdfPage(%p).search(%p, %p): array: %p", thiz, doc, page, arrayList);
    if (!arrayList)
    {
        return NULL;
    }

    fz_bbox *hit_bbox = NULL;

    fz_text_sheet *sheet = NULL;
    fz_text_page *pagetext = NULL;
    fz_device *dev = NULL;
    int pos;
    int len;
    int i, n;
    int hit_count = 0;

    fz_try(doc->ctx)
    {
        fz_rect rect;

        // DEBUG("MuPdfPage(%p).search(%p, %p): load page text", thiz, doc, page);

        rect = fz_bound_page(doc->document, page->page);
        sheet = fz_new_text_sheet(doc->ctx);
        pagetext = fz_new_text_page(doc->ctx, rect);
        dev = fz_new_text_device(doc->ctx, sheet, pagetext);
        fz_run_page(doc->document, page->page, dev, fz_identity, NULL);

        // DEBUG("MuPdfPage(%p).search(%p, %p): free text device", thiz, doc, page);

        fz_free_device(dev);
        dev = NULL;

        len = textlen(pagetext);

        // DEBUG("MuPdfPage(%p).search(%p, %p): text length: %d", thiz, doc, page, len);

        for (pos = 0; pos < len; pos++)
        {
            fz_bbox rr = fz_empty_bbox;
            // DEBUG("MuPdfPage(%p).search(%p, %p): match %d", thiz, doc, page, pos);

            n = match(&ch, pagetext, str, pos);
            if (n > 0)
            {
//                DEBUG("MuPdfPage(%p).search(%p, %p): match found: %d, %d", thiz, doc, page, pos, n);
                for (i = 0; i < n; i++)
                {
                    rr = fz_union_bbox(rr, bboxcharat(pagetext, pos + i));
                }

                if (!fz_is_empty_bbox(rr))
                {
                    int coords[4];
                    coords[0] = (rr.x0);
                    coords[1] = (rr.y0);
                    coords[2] = (rr.x1);
                    coords[3] = (rr.y1);
//                    DEBUG("MuPdfPage(%p).search(%p, %p): found rectangle (%d, %d - %d, %d)", thiz, doc, page, coords[0], coords[1], coords[2], coords[3]);
                    jobject ptb = PageTextBoxHelper_create(&ptbh);
                    if (ptb)
                    {
                        // DEBUG("MuPdfPage(%p).search(%p, %p): rect %p", thiz, doc, page, ptb);
                        PageTextBoxHelper_setRect(&ptbh, ptb, coords);
                        // PageTextBoxHelper_setText(&ptbh, ptb, txt);
                        // DEBUG("MuPdfPage(%p).search(%p, %p): add rect %p to array %p", thiz, doc, page, ptb, arrayList);
                        ArrayListHelper_add(&alh, arrayList, ptb);
                    }
                }
            }
        }
    } fz_always(doc->ctx)
    {
        // DEBUG("MuPdfPage(%p).search(%p, %p): free resources", thiz, doc, page);
        if (pagetext)
        {
            fz_free_text_page(doc->ctx, pagetext);
        }
        if (sheet)
        {
            fz_free_text_sheet(doc->ctx, sheet);
        }
        if (dev)
        {
            fz_free_device(dev);
        }
    }fz_catch(doc->ctx)
    {
        jclass cls;
        (*env)->ReleaseStringUTFChars(env, text, str);
        cls = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
        if (cls != NULL)
        {
            (*env)->ThrowNew(env, cls, "Out of memory in MuPDFCore_searchPage");
        }
        (*env)->DeleteLocalRef(env, cls);
        return NULL;
    }

    (*env)->ReleaseStringUTFChars(env, text, str);

    return arrayList;
}


//Outline
JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_open(JNIEnv *env, jclass clazz, jlong dochandle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    if (!doc->outline)
        doc->outline = fz_load_outline(doc->document);
//    DEBUG("PdfOutline.open(): return handle = %p", doc->outline);
    return (jlong) (long) doc->outline;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_free(JNIEnv *env, jclass clazz, jlong dochandle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
//    DEBUG("PdfOutline_free(%p)", doc);
    if (doc)
    {
        if (doc->outline)
            fz_free_outline(doc->ctx, doc->outline);
        doc->outline = NULL;
    }
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getTitle(JNIEnv *env, jclass clazz, jlong outlinehandle)
{
    fz_outline *outline = (fz_outline*) (long) outlinehandle;
//	DEBUG("PdfOutline_getTitle(%p)",outline);
    if (outline)
        return (*env)->NewStringUTF(env, outline->title);
    return NULL;
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getLink(JNIEnv *env, jclass clazz, jlong outlinehandle, jlong dochandle)
{
    fz_outline *outline = (fz_outline*) (long) outlinehandle;
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;

    // DEBUG("PdfOutline_getLink(%p)",outline);
    if (!outline)
        return NULL;

    char linkbuf[128];
    if (outline->dest.kind == FZ_LINK_URI)
    {
        snprintf(linkbuf, 128, "%s", outline->dest.ld.uri.uri);
        // DEBUG("PdfOutline_getLink uri = %s",linkbuf);
    }
    else if (outline->dest.kind == FZ_LINK_GOTO)
    {
        snprintf(linkbuf, 127, "#%d", outline->dest.ld.gotor.page + 1);
        // DEBUG("PdfOutline_getLink goto = %s",linkbuf);
    }
    else
    {
        return NULL;
    }

    return (*env)->NewStringUTF(env, linkbuf);
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_fillLinkTargetPoint(JNIEnv *env, jclass clazz, jlong outlinehandle,
                                                                      jfloatArray pointArray)
{
    fz_outline *outline = (fz_outline*) (long) outlinehandle;

    if (!outline || outline->dest.kind != FZ_LINK_GOTO)
    {
        return 0;
    }

    jfloat *point = (*env)->GetPrimitiveArrayCritical(env, pointArray, 0);
    if (!point)
    {
        return 0;
    }

//    DEBUG("MuPdfOutline_fillLinkTargetPoint(): %d %x (%f, %f) - (%f, %f)",
//          outline->dest.ld.gotor.page,
//          outline->dest.ld.gotor.flags,
//          outline->dest.ld.gotor.lt.x, outline->dest.ld.gotor.lt.y,
//          outline->dest.ld.gotor.rb.x, outline->dest.ld.gotor.rb.y);

    point[0] = outline->dest.ld.gotor.lt.x;
    point[1] = outline->dest.ld.gotor.lt.y;

    (*env)->ReleasePrimitiveArrayCritical(env, pointArray, point, 0);

    return outline->dest.ld.gotor.flags;
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getNext(JNIEnv *env, jclass clazz, jlong outlinehandle)
{
    fz_outline *outline = (fz_outline*) (long) outlinehandle;
//	DEBUG("MuPdfOutline_getNext(%p)",outline);
    return (jlong)(long)(outline?outline->next:NULL);
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_getChild(JNIEnv *env, jclass clazz, jlong outlinehandle)
{
    fz_outline *outline = (fz_outline*) (long) outlinehandle;
//	DEBUG("MuPdfOutline_getChild(%p)",outline);
    return (jlong)(long)(outline?outline->down:NULL);
}

