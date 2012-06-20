#include <jni.h>

#include <android/log.h>

#include <nativebitmap.h>
#include <javahelpers.h>

#include <fitz.h>
#include <mupdf.h>
#include <muxps.h>

#define FORMAT_PDF 0
#define FORMAT_XPS 1

/* Debugging helper */

#define DEBUG(args...) \
	__android_log_print(ANDROID_LOG_DEBUG, "EBookDroid.MuPDF", args)

#define ERROR(args...) \
	__android_log_print(ANDROID_LOG_ERROR, "EBookDroid.MuPDF", args)

#define INFO(args...) \
	__android_log_print(ANDROID_LOG_INFO, "EBookDroid.MuPDF", args)

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
    fz_page *page;
    int number;
    fz_display_list* pageList;
};

#define RUNTIME_EXCEPTION "java/lang/RuntimeException"

extern fz_locks_context * jni_new_locks();
extern void jni_free_locks(fz_locks_context *locks);

void mupdf_throw_exception(JNIEnv *env, char *message)
{
    jthrowable new_exception = (*env)->FindClass(env, RUNTIME_EXCEPTION);
    if (new_exception == NULL)
	return;
    DEBUG("Exception '%s', Message: '%s'", RUNTIME_EXCEPTION, message);
    (*env)->ThrowNew(env, new_exception, message);
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
                mupdf_throw_exception(env, "Wrong password given");
                goto cleanup;
            }
        }
        else
        {
            mupdf_free_document(doc);
            mupdf_throw_exception(env, "Document needs a password!");
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

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfLinks_fillPageLinkTargetPoint(JNIEnv *env, jclass clazz, jlong linkhandle,
                                                                      jfloatArray pointArray)
{
    fz_link *link = (fz_link*) (long) linkhandle;

    if (!link || link->dest.kind != FZ_LINK_GOTO)
    {
        return JNI_FALSE;
    }

    jfloat *point = (*env)->GetPrimitiveArrayCritical(env, pointArray, 0);
    if (!point)
    {
        return JNI_FALSE;
    }

    point[0] = link->dest.ld.gotor.lt.x;
    point[1] = link->dest.ld.gotor.lt.y;

    (*env)->ReleasePrimitiveArrayCritical(env, pointArray, point, 0);

    return JNI_TRUE;
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
    renderpage_t *page;
    fz_device *dev;

    jclass cls;
    jfieldID fid;

    page = malloc(sizeof(renderpage_t));
    if (!page)
    {
        mupdf_throw_exception(env, "Out of Memory");
        return (jlong) (long) NULL;
    }
    page->page = NULL;
    page->pageList = NULL;

    fz_try(doc->ctx)
    {
        page->page = fz_load_page(doc->document, pageno - 1);
    }
    fz_catch(doc->ctx)
    {
        //fz_throw(ctx, "cannot load page tree: %s", filename);
        free(page);
        page = NULL;
        mupdf_throw_exception(env, "error loading page");
        goto cleanup;

    }

    fz_try(doc->ctx)
    {
        page->pageList = fz_new_display_list(doc->ctx);
        dev = fz_new_list_device(doc->ctx, page->pageList);
        fz_run_page(doc->document, page->page, dev, fz_identity, NULL);
    }
    fz_catch(doc->ctx)
    {
        fz_free_device(dev);
        fz_free_display_list(doc->ctx, page->pageList);
        fz_free_page(doc->document, page->page);
//	fz_throw(ctx, "cannot draw page %d",pageno);
        free(page);
        page = NULL;
        mupdf_throw_exception(env, "error loading page");
        goto cleanup;
    }
    fz_free_device(dev);

    cleanup:
    /* nothing yet */

    // DEBUG("PdfPage.nativeOpenPage(): return handle = %p", page);
    return (jlong) (long) page;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_free(JNIEnv *env, jclass clazz, jlong dochandle, jlong handle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page = (renderpage_t*) (long) handle;
    // DEBUG("PdfPage_free(%p)", page);

    if (page)
    {
        if (page->page)
            fz_free_page(doc->document, page->page);
        if (page->pageList)
            fz_free_display_list(doc->ctx, page->pageList);
        free(page);
    }
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

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_renderPage(JNIEnv *env, jobject this, jlong dochandle, jlong pagehandle,
                                                        jintArray viewboxarray, jfloatArray matrixarray,
                                                        jintArray bufferarray)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page = (renderpage_t*) (long) pagehandle;
    // DEBUG("PdfView(%p).renderPage(%p, %p)", this, doc, page);
    fz_matrix ctm;
    fz_bbox viewbox;
    fz_pixmap *pixmap;
    jfloat *matrix;
    jint *viewboxarr;
    jint *dimen;
    jint *buffer;
    int length, val;
    fz_device *dev = NULL;

    /* initialize parameter arrays for MuPDF */

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

    buffer = (*env)->GetPrimitiveArrayCritical(env, bufferarray, 0);

    fz_try(doc->ctx)
    {
	pixmap = fz_new_pixmap_with_data(doc->ctx, fz_device_bgr, viewbox.x1 - viewbox.x0, viewbox.y1 - viewbox.y0,
    	    (unsigned char*) buffer);

	fz_clear_pixmap_with_value(doc->ctx, pixmap, 0xff);

	dev = fz_new_draw_device(doc->ctx, pixmap);
	fz_run_display_list(page->pageList, dev, ctm, viewbox, NULL);
	fz_free_device(dev);

	fz_drop_pixmap(doc->ctx, pixmap);
    }
    fz_catch(doc->ctx)
    {
        DEBUG("Render failed");
    }
    
    (*env)->ReleasePrimitiveArrayCritical(env, bufferarray, buffer, 0);


}

/*JNI BITMAP API*/
JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfPage_renderPageBitmap(JNIEnv *env, jobject this, jlong dochandle,
                                                              jlong pagehandle, jintArray viewboxarray,
                                                              jfloatArray matrixarray, jobject bitmap)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page = (renderpage_t*) (long) pagehandle;
    // DEBUG("PdfView(%p).renderPageBitmap(%p, %p)", this, doc, page);
    fz_matrix ctm;
    fz_bbox viewbox;
    fz_pixmap *pixmap;
    jfloat *matrix;
    jint *viewboxarr;
    jint *dimen;
    int length, val;
    fz_device *dev = NULL;

    AndroidBitmapInfo info;
    void *pixels;

    int ret;

    if ((ret = NativeBitmap_getInfo(env, bitmap, &info)) < 0)
    {
        ERROR("NativeBitmap_getInfo() failed ! error=%d", ret);
        return JNI_FALSE;
    }

    // DEBUG("Checking format\n");
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
    {
        ERROR("Bitmap format is not RGBA_8888 !");
        return JNI_FALSE;
    }

    // DEBUG("locking pixels\n");
    if ((ret = NativeBitmap_lockPixels(env, bitmap, &pixels)) < 0)
    {
        ERROR("AndroidBitmap_lockPixels() failed ! error=%d", ret);
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

    fz_try(doc->ctx)
    {

	pixmap = fz_new_pixmap_with_data(doc->ctx, fz_device_rgb, viewbox.x1 - viewbox.x0, viewbox.y1 - viewbox.y0, pixels);

	fz_clear_pixmap_with_value(doc->ctx, pixmap, 0xff);

	dev = fz_new_draw_device(doc->ctx, pixmap);
	fz_run_display_list(page->pageList, dev, ctm, viewbox, NULL);
	fz_free_device(dev);

	fz_drop_pixmap(doc->ctx, pixmap);
    }
    fz_catch(doc->ctx)
    {
        DEBUG("Render failed");
    }

    NativeBitmap_unlockPixels(env, bitmap);

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

static int match(fz_text_page *page, const char *s, int n)
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
            if (tolower(c) != tolower(charat(page, n)))
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
    if (!ArrayListHelper_init(&alh, env) || !PageTextBoxHelper_init(&ptbh, env))
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

            n = match(pagetext, str, pos);
            if (n > 0)
            {
                DEBUG("MuPdfPage(%p).search(%p, %p): match found: %d, %d", thiz, doc, page, pos, n);
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
                    DEBUG("MuPdfPage(%p).search(%p, %p): found rectangle (%d, %d - %d, %d)", thiz, doc, page, coords[0], coords[1], coords[2], coords[3]);
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

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_mupdf_codec_MuPdfOutline_fillLinkTargetPoint(JNIEnv *env, jclass clazz, jlong outlinehandle,
                                                                      jfloatArray pointArray)
{
    fz_outline *outline = (fz_outline*) (long) outlinehandle;

    if (!outline || outline->dest.kind != FZ_LINK_GOTO)
    {
        return JNI_FALSE;
    }

    jfloat *point = (*env)->GetPrimitiveArrayCritical(env, pointArray, 0);
    if (!point)
    {
        return JNI_FALSE;
    }

    point[0] = outline->dest.ld.gotor.lt.x;
    point[1] = outline->dest.ld.gotor.lt.y;

    (*env)->ReleasePrimitiveArrayCritical(env, pointArray, point, 0);

    return JNI_TRUE;
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
