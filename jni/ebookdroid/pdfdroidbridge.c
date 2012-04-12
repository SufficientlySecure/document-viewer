#include <jni.h>

#include <android/log.h>

#include <nativebitmap.h>

#include <errno.h>

#include <fitz.h>
#include <mupdf.h>

/* Debugging helper */

#define DEBUG(args...) \
	__android_log_print(ANDROID_LOG_DEBUG, "EBookDroid.PDF", args)

#define ERROR(args...) \
	__android_log_print(ANDROID_LOG_ERROR, "EBookDroid.PDF", args)

#define INFO(args...) \
	__android_log_print(ANDROID_LOG_INFO, "EBookDroid.PDF", args)

typedef struct renderdocument_s renderdocument_t;
struct renderdocument_s
{
    fz_context *ctx;
    pdf_document *xref;
    fz_outline *outline;
};

typedef struct renderpage_s renderpage_t;
struct renderpage_s
{
    pdf_page *page;
    fz_display_list* pageList;
};

#define RUNTIME_EXCEPTION "java/lang/RuntimeException"

void throw_exception(JNIEnv *env, char *message)
{
    jthrowable new_exception = (*env)->FindClass(env, RUNTIME_EXCEPTION);
    if (new_exception == NULL)
    {
        return;
    }
    else
    {
        DEBUG("Exception '%s', Message: '%s'", RUNTIME_EXCEPTION, message);
    }
    (*env)->ThrowNew(env, new_exception, message);
}

static void pdf_free_document(renderdocument_t* doc)
{
    if (doc)
    {
        if (doc->outline)
            fz_free_outline(doc->ctx, doc->outline);
        doc->outline = NULL;

        if (doc->xref)
            pdf_close_document(doc->xref);
        doc->xref = NULL;

        fz_flush_warnings(doc->ctx);
        fz_free_context(doc->ctx);
        doc->ctx = NULL;

        free(doc);
        doc = NULL;
    }
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfDocument_open(JNIEnv *env, jclass clazz, jint storememory, jstring fname,
                                                      jstring pwd)
{
    fz_obj *obj;
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
        throw_exception(env, "Out of Memory");
        goto cleanup;
    }DEBUG("PdfDocument.nativeOpen(): storememory = %d", storememory);

//    doc->ctx = fz_new_context(&fz_alloc_default, 256<<20);
//    doc->ctx = fz_new_context(&fz_alloc_default, storememory);
    doc->ctx = fz_new_context(NULL, storememory);
    if (!doc->ctx)
    {
        free(doc);
        throw_exception(env, "Out of Memory");
        goto cleanup;
    }
    doc->xref = NULL;
    doc->outline = NULL;

//    fz_set_aa_level(fz_catch(ctx), alphabits);

    /*
     * Open PDF and load xref table
     */
    fz_try(doc->ctx)
    {
        doc->xref = pdf_open_document(doc->ctx, filename);
    }
    fz_catch(doc->ctx)
    {
        throw_exception(env, (char*) fz_caught(doc->ctx));
        pdf_free_document(doc);
//        throw_exception(env, "PDF file not found or corrupted");
        goto cleanup;
    }

    /*
     * Handle encrypted PDF files
     */

    if (pdf_needs_password(doc->xref))
    {
        if (strlen(password))
        {
            int ok = pdf_authenticate_password(doc->xref, password);
            if (!ok)
            {
                pdf_free_document(doc);
                throw_exception(env, "Wrong password given");
                goto cleanup;
            }
        }
        else
        {
            pdf_free_document(doc);
            throw_exception(env, "PDF needs a password!");
            goto cleanup;
        }
    }

    cleanup:

    (*env)->ReleaseStringUTFChars(env, fname, filename);
    (*env)->ReleaseStringUTFChars(env, pwd, password);

    // DEBUG("PdfDocument.nativeOpen(): return handle = %p", doc);
    return (jlong) (long) doc;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfDocument_free(JNIEnv *env, jclass clazz, jlong handle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) handle;
    pdf_free_document(doc);
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfDocument_getPageInfo(JNIEnv *env, jclass cls, jlong handle, jint pageNumber,
                                                             jobject cpi)
{
    renderdocument_t *doc = (renderdocument_t*) (long) handle;
    
    if(!doc)
	return -1;

    DEBUG("getPageInfo: %d", pageNumber);

    jclass clazz;
    jfieldID fid;

    fz_obj *pageobj = NULL;
    fz_obj *rotateobj = NULL;

    fz_obj *obj;
    fz_bbox bbox;
    fz_rect mediabox;

    int rotate = 0;

    pageobj = doc->xref->page_objs[pageNumber - 1];

    obj = fz_dict_gets(pageobj, "MediaBox");
    bbox = fz_round_rect(pdf_to_rect(doc->ctx, obj));
    if (fz_is_empty_rect(pdf_to_rect(doc->ctx, obj)))
    {
        fz_warn(doc->ctx, "cannot find page size for page %d", pageNumber + 1);
        bbox.x0 = 0;
        bbox.y0 = 0;
        bbox.x1 = 612;
        bbox.y1 = 792;
    }

    obj = fz_dict_gets(pageobj, "CropBox");
    if (fz_is_array(obj))
    {
        fz_bbox cropbox = fz_round_rect(pdf_to_rect(doc->ctx, obj));
        bbox = fz_intersect_bbox(bbox, cropbox);
    }

    mediabox.x0 = MIN(bbox.x0, bbox.x1);
    mediabox.y0 = MIN(bbox.y0, bbox.y1);
    mediabox.x1 = MAX(bbox.x0, bbox.x1);
    mediabox.y1 = MAX(bbox.y0, bbox.y1);

    if (mediabox.x1 - mediabox.x0 < 1 || mediabox.y1 - mediabox.y0 < 1)
    {
        fz_warn(doc->ctx, "invalid page size in page %d", pageNumber + 1);
        mediabox = fz_unit_rect;
    }

    // DEBUG( "Mediabox: %f %f %f %f", mediabox.x0, mediabox.y0, mediabox.x1, mediabox.y1);

    rotateobj = fz_dict_gets(pageobj, "Rotate");
    if (fz_is_int(rotateobj))
    {
        rotate = fz_to_int(rotateobj);
    }

    clazz = (*env)->GetObjectClass(env, cpi);
    if (0 == clazz)
    {
        return (-1);
    }

    fz_rect bounds = fz_transform_rect(fz_rotate(rotate), mediabox);

    int width = bounds.x1 - bounds.x0;
    int height = bounds.y1 - bounds.y0;

    fid = (*env)->GetFieldID(env, clazz, "width", "I");
    (*env)->SetIntField(env, cpi, fid, width);

    fid = (*env)->GetFieldID(env, clazz, "height", "I");
    (*env)->SetIntField(env, cpi, fid, height);

    fid = (*env)->GetFieldID(env, clazz, "dpi", "I");
    (*env)->SetIntField(env, cpi, fid, 0);

    fid = (*env)->GetFieldID(env, clazz, "rotation", "I");
    (*env)->SetIntField(env, cpi, fid, rotate);

    fid = (*env)->GetFieldID(env, clazz, "version", "I");
    (*env)->SetIntField(env, cpi, fid, 0);
    return 0;
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfLinks_getFirstPageLink(JNIEnv *env, jclass clazz, jlong handle,
                                                               jlong pagehandle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) handle;
    renderpage_t *page = (renderpage_t*) (long) pagehandle;

    return (jlong)(long)(page && page->page ? page->page->links : NULL);
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfLinks_getNextPageLink(JNIEnv *env, jclass clazz, jlong linkhandle)
{
    fz_link *link = (fz_link*) (long) linkhandle;

    return (jlong)(long)(link ? link->next : NULL);
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfLinks_getPageLinkType(JNIEnv *env, jclass clazz, jlong linkhandle)
{
    fz_link *link = (fz_link*) (long) linkhandle;

    return (jint)(link ? link->dest.kind : FZ_LINK_NONE);
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfLinks_getPageLinkUrl(JNIEnv *env, jclass clazz, jlong linkhandle)
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
Java_org_ebookdroid_droids_pdf_codec_PdfLinks_fillPageLinkSourceRect(JNIEnv *env, jclass clazz, jlong linkhandle,
                                                                     jfloatArray boundsArray)
{
    fz_link *link = (fz_link*) (long) linkhandle;

    if (!link || link->dest.kind != FZ_LINK_GOTO)
    {
        return 0;
    }

    jfloat *bounds = (*env)->GetPrimitiveArrayCritical(env, boundsArray, 0);
    if (!bounds)
    {
        return 0;
    }

    bounds[0] = link->rect.x0;
    bounds[1] = link->rect.y0;
    bounds[2] = link->rect.x1;
    bounds[3] = link->rect.y1;

    (*env)->ReleasePrimitiveArrayCritical(env, boundsArray, bounds, 0);

    return 1;
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfLinks_getPageLinkTargetPage(JNIEnv *env, jclass clazz, jlong linkhandle)
{
    fz_link *link = (fz_link*) (long) linkhandle;

    if (!link || link->dest.kind != FZ_LINK_GOTO)
    {
        return (jint)-1;
    }

    return (jint)link->dest.ld.gotor.page;
}

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfLinks_fillPageLinkTargetPoint(JNIEnv *env, jclass clazz, jlong linkhandle,
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

    point[0] = link->dest.ld.gotor.lt.x;
    point[1] = link->dest.ld.gotor.lt.y;

    (*env)->ReleasePrimitiveArrayCritical(env, pointArray, point, 0);

    return 1;
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfDocument_getPageCount(JNIEnv *env, jclass clazz, jlong handle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) handle;
    return (pdf_count_pages(doc->xref));
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfPage_open(JNIEnv *env, jclass clazz, jlong dochandle, jint pageno)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page;
    fz_obj *obj;
    fz_device *dev;

    jclass cls;
    jfieldID fid;

    page = malloc(sizeof(renderpage_t));
    if (!page)
    {
        throw_exception(env, "Out of Memory");
        return (jlong) (long) NULL;
    }
    page->page = NULL;
    page->pageList = NULL;

    fz_try(doc->ctx)
    {
        page->page = pdf_load_page(doc->xref, pageno - 1);
    }
    fz_catch(doc->ctx)
    {
        //fz_throw(ctx, "cannot load page tree: %s", filename);
        free(page);
        page = NULL;
        throw_exception(env, "error loading page");
        goto cleanup;

    }

    fz_try(doc->ctx)
    {
        page->pageList = fz_new_display_list(doc->ctx);
        dev = fz_new_list_device(doc->ctx, page->pageList);
        pdf_run_page(doc->xref, page->page, dev, fz_identity, NULL);
    }
    fz_catch(doc->ctx)
    {
        fz_free_device(dev);
        fz_free_display_list(doc->ctx, page->pageList);
        pdf_free_page(doc->ctx, page->page);
//	fz_throw(ctx, "cannot draw page %d",pageno);
        free(page);
        page = NULL;
        throw_exception(env, "error loading page");
        goto cleanup;
    }
    fz_free_device(dev);

    cleanup:
    /* nothing yet */

    // DEBUG("PdfPage.nativeOpenPage(): return handle = %p", page);
    return (jlong) (long) page;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfPage_free(JNIEnv *env, jclass clazz, jlong dochandle, jlong handle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page = (renderpage_t*) (long) handle;
    // DEBUG("PdfPage_free(%p)", page);

    if (page)
    {
        if (page->page)
            pdf_free_page(doc->ctx, page->page);
        if (page->pageList)
            fz_free_display_list(doc->ctx, page->pageList);
        free(page);
    }
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfPage_getBounds(JNIEnv *env, jclass clazz, jlong dochandle, jlong handle,
                                                       jfloatArray bounds)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page = (renderpage_t*) (long) handle;
    jfloat *bbox = (*env)->GetPrimitiveArrayCritical(env, bounds, 0);
    if (!bbox)
        return;
    fz_rect page_bounds = pdf_bound_page(doc->xref, page->page);
    // DEBUG("Bounds: %f %f %f %f", page_bounds.x0, page_bounds.y0, page_bounds.x1, page_bounds.y1);
    bbox[0] = page_bounds.x0;
    bbox[1] = page_bounds.y0;
    bbox[2] = page_bounds.x1;
    bbox[3] = page_bounds.y1;
    (*env)->ReleasePrimitiveArrayCritical(env, bounds, bbox, 0);
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfPage_renderPage(JNIEnv *env, jobject this, jlong dochandle, jlong pagehandle,
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

    ctm = fz_identity;

    matrix = (*env)->GetPrimitiveArrayCritical(env, matrixarray, 0);
    ctm.a = matrix[0];
    ctm.b = matrix[1];
    ctm.c = matrix[2];
    ctm.d = matrix[3];
    ctm.e = matrix[4];
    ctm.f = matrix[5];
    (*env)->ReleasePrimitiveArrayCritical(env, matrixarray, matrix, 0);
    // DEBUG( "Matrix: %f %f %f %f %f %f", ctm.a, ctm.b, ctm.c, ctm.d, ctm.e, ctm.f);

    viewboxarr = (*env)->GetPrimitiveArrayCritical(env, viewboxarray, 0);
    viewbox.x0 = viewboxarr[0];
    viewbox.y0 = viewboxarr[1];
    viewbox.x1 = viewboxarr[2];
    viewbox.y1 = viewboxarr[3];
    (*env)->ReleasePrimitiveArrayCritical(env, viewboxarray, viewboxarr, 0);
    // DEBUG( "Viewbox: %d %d %d %d", viewbox.x0, viewbox.y0, viewbox.x1, viewbox.y1);
    /* do the rendering */

    buffer = (*env)->GetPrimitiveArrayCritical(env, bufferarray, 0);

    pixmap = fz_new_pixmap_with_data(doc->ctx, fz_device_bgr, viewbox.x1 - viewbox.x0, viewbox.y1 - viewbox.y0,
        (unsigned char*) buffer);

    // DEBUG("doing the rendering...");

    fz_clear_pixmap_with_color(pixmap, 0xff);

    dev = fz_new_draw_device(doc->ctx, pixmap);
    fz_execute_display_list(page->pageList, dev, ctm, viewbox, NULL);
    fz_free_device(dev);

    (*env)->ReleasePrimitiveArrayCritical(env, bufferarray, buffer, 0);

    fz_drop_pixmap(doc->ctx, pixmap);

    // DEBUG("PdfView.renderPage() done");
}

/*JNI BITMAP API*/

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfPage_renderPageBitmap(JNIEnv *env, jobject this, jlong dochandle,
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
        ERROR("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return 0;
    }

    // DEBUG("Checking format\n");
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
    {
        ERROR("Bitmap format is not RGBA_8888 !");
        return 0;
    }

    // DEBUG("locking pixels\n");
    if ((ret = NativeBitmap_lockPixels(env, bitmap, &pixels)) < 0)
    {
        ERROR("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return 0;
    }

    ctm = fz_identity;

    matrix = (*env)->GetPrimitiveArrayCritical(env, matrixarray, 0);
    ctm.a = matrix[0];
    ctm.b = matrix[1];
    ctm.c = matrix[2];
    ctm.d = matrix[3];
    ctm.e = matrix[4];
    ctm.f = matrix[5];
    (*env)->ReleasePrimitiveArrayCritical(env, matrixarray, matrix, 0);
    // DEBUG( "Matrix: %f %f %f %f %f %f", ctm.a, ctm.b, ctm.c, ctm.d, ctm.e, ctm.f);

    viewboxarr = (*env)->GetPrimitiveArrayCritical(env, viewboxarray, 0);
    viewbox.x0 = viewboxarr[0];
    viewbox.y0 = viewboxarr[1];
    viewbox.x1 = viewboxarr[2];
    viewbox.y1 = viewboxarr[3];
    (*env)->ReleasePrimitiveArrayCritical(env, viewboxarray, viewboxarr, 0);
    // DEBUG( "Viewbox: %d %d %d %d", viewbox.x0, viewbox.y0, viewbox.x1, viewbox.y1);

    pixmap = fz_new_pixmap_with_data(doc->ctx, fz_device_rgb, viewbox.x1 - viewbox.x0, viewbox.y1 - viewbox.y0, pixels);

    // DEBUG("doing the rendering...");

    fz_clear_pixmap_with_color(pixmap, 0xff);

    dev = fz_new_draw_device(doc->ctx, pixmap);
    fz_execute_display_list(page->pageList, dev, ctm, viewbox, NULL);
    fz_free_device(dev);

    fz_drop_pixmap(doc->ctx, pixmap);

    // DEBUG("PdfView.renderPage() done");

    NativeBitmap_unlockPixels(env, bitmap);

    return 1;
}

//Outline
JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfOutline_open(JNIEnv *env, jclass clazz, jlong dochandle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    if (!doc->outline)
        doc->outline = pdf_load_outline(doc->xref);
//    DEBUG("PdfOutline.open(): return handle = %p", doc->outline);
    return (jlong) (long) doc->outline;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfOutline_free(JNIEnv *env, jclass clazz, jlong dochandle)
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
Java_org_ebookdroid_droids_pdf_codec_PdfOutline_getTitle(JNIEnv *env, jclass clazz, jlong outlinehandle)
{
    fz_outline *outline = (fz_outline*) (long) outlinehandle;
//	DEBUG("PdfOutline_getTitle(%p)",outline);
    if (outline)
        return (*env)->NewStringUTF(env, outline->title);
    return NULL;
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfOutline_getLink(JNIEnv *env, jclass clazz, jlong outlinehandle, jlong dochandle)
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
Java_org_ebookdroid_droids_pdf_codec_PdfOutline_fillLinkTargetPoint(JNIEnv *env, jclass clazz, jlong outlinehandle,
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

    point[0] = outline->dest.ld.gotor.lt.x;
    point[1] = outline->dest.ld.gotor.lt.y;

    (*env)->ReleasePrimitiveArrayCritical(env, pointArray, point, 0);

    return 1;
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfOutline_getNext(JNIEnv *env, jclass clazz, jlong outlinehandle)
{
    fz_outline *outline = (fz_outline*) (long) outlinehandle;
//	DEBUG("PdfOutline_getNext(%p)",outline);
    if (!outline)
        return 0;
    return (jlong) (long) outline->next;
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_droids_pdf_codec_PdfOutline_getChild(JNIEnv *env, jclass clazz, jlong outlinehandle)
{
    fz_outline *outline = (fz_outline*) (long) outlinehandle;
//	DEBUG("PdfOutline_getChild(%p)",outline);
    if (!outline)
        return 0;
    return (jlong) (long) outline->down;
}
