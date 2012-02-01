#include <jni.h>

#include <android/log.h>
#include <nativebitmap.h>

#include <errno.h>

#include <fitz.h>
#include <muxps.h>

/* Debugging helper */

#define DEBUG(args...) \
	__android_log_print(ANDROID_LOG_DEBUG, "EBookDroid.XPS", args)

#define ERROR(args...) \
	__android_log_print(ANDROID_LOG_ERROR, "EBookDroid.XPS", args)

#define INFO(args...) \
	__android_log_print(ANDROID_LOG_INFO, "EBookDroid.XPS", args)

typedef struct renderdocument_s renderdocument_t;
struct renderdocument_s
{
    fz_context* ctx;
    xps_document* xpsdoc;
    fz_outline* outline;
};

typedef struct renderpage_s renderpage_t;
struct renderpage_s
{
    xps_page* page;
    fz_display_list* pageList;
};

#define RUNTIME_EXCEPTION "java/lang/RuntimeException"

void xps_throw_exception(JNIEnv *env, char *message)
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

static void xps_free_document(renderdocument_t* doc)
{
    if (doc)
    {
        if (doc->outline)
            fz_free_outline(doc->ctx, doc->outline);
        doc->outline = NULL;

	if(doc->xpsdoc)
	    xps_close_document(doc->xpsdoc);
	doc->xpsdoc = NULL;
        
        fz_flush_warnings(doc->ctx);
        fz_free_context(doc->ctx);             
        doc->ctx = NULL;

        free(doc);
        doc = NULL;
    }
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsDocument_open(JNIEnv *env, jclass clazz, jint storememory, jstring fname)
{
    fz_obj *obj;
    renderdocument_t *doc;
    jboolean iscopy;
    jclass cls;
    jfieldID fid;
    char *filename;

    filename = (char*) (*env)->GetStringUTFChars(env, fname, &iscopy);

    doc = malloc(sizeof(renderdocument_t));
    if (!doc)
    {
        xps_throw_exception(env, "Out of Memory");
        goto cleanup;
    }
    DEBUG("XpsDocument.nativeOpen(): storememory = %d", storememory);
//    doc->ctx = fz_new_context(&fz_alloc_default, 256<<20);
//    doc->ctx = fz_new_context(&fz_alloc_default, storememory);
    doc->ctx = fz_new_context(NULL, storememory);
    if (!doc->ctx)
    {
	free(doc);
        xps_throw_exception(env, "Out of Memory");
        goto cleanup;
    }
    doc->xpsdoc = NULL;
    doc->outline = NULL;

    
    fz_try(doc->ctx)
    {
	doc->xpsdoc = xps_open_document(doc->ctx, filename);
    }
    fz_catch(doc->ctx)
    {
        xps_free_document(doc);
        xps_throw_exception(env, "XPS file not found or corrupted");
        goto cleanup;
    }

    cleanup:

    (*env)->ReleaseStringUTFChars(env, fname, filename);

    DEBUG("XpsDocument.nativeOpen(): return handle = %p", doc);
    return (jlong) (long) doc;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsDocument_free(JNIEnv *env, jclass clazz, jlong handle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) handle;

    xps_free_document(doc);
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsDocument_getPageInfo(JNIEnv *env, jclass cls, jlong handle, jint pageNumber,
                                                           jobject cpi)
{

    renderdocument_t *doc = (renderdocument_t*) (long) handle;

//	DEBUG("XpsDocument.getPageInfo = %p", doc);

    xps_page *page = NULL;

    jclass clazz;
    jfieldID fid;

    fz_try(doc->ctx)
    {
	page = xps_load_page(doc->xpsdoc, pageNumber - 1);
    }
    fz_catch(doc->ctx)
    {
        return (-1);
    }

    if (page)
    {
        clazz = (*env)->GetObjectClass(env, cpi);
        if (0 == clazz)
        {
            return (-1);
        }

        fid = (*env)->GetFieldID(env, clazz, "width", "I");
        (*env)->SetIntField(env, cpi, fid, page->width);

        fid = (*env)->GetFieldID(env, clazz, "height", "I");
        (*env)->SetIntField(env, cpi, fid, page->height);

        fid = (*env)->GetFieldID(env, clazz, "dpi", "I");
        (*env)->SetIntField(env, cpi, fid, 0);

        fid = (*env)->GetFieldID(env, clazz, "rotation", "I");
        (*env)->SetIntField(env, cpi, fid, 0);

        fid = (*env)->GetFieldID(env, clazz, "version", "I");
        (*env)->SetIntField(env, cpi, fid, 0);

        xps_free_page(doc->xpsdoc, page);
        return 0;
    }
    return (-1);
}

JNIEXPORT jint JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsDocument_getPageCount(JNIEnv *env, jclass clazz, jlong handle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) handle;
    return (xps_count_pages(doc->xpsdoc));
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsPage_open(JNIEnv *env, jclass clazz, jlong dochandle, jint pageno)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page;
    fz_obj *obj;
    jclass cls;
    jfieldID fid;

    page = malloc(sizeof(renderpage_t));
    if (!page)
    {
        xps_throw_exception(env, "Out of Memory");
        return (jlong) (long) NULL;
    }

    fz_try(doc->ctx)
    {
	page->page = xps_load_page(doc->xpsdoc, pageno - 1);
    }
    fz_catch(doc->ctx)
    {
	free(page);
        xps_throw_exception(env, "error loading page");
        goto cleanup;
    }

//New draw page
    page->pageList = fz_new_display_list(doc->ctx);
    fz_device *dev = fz_new_list_device(doc->ctx, page->pageList);
    xps_run_page(doc->xpsdoc, page->page, dev, fz_identity, NULL);
    fz_free_device(dev);
//

    cleanup:
    /* nothing yet */

    DEBUG("XpsPage.nativeOpenPage(): return handle = %p", page);
    return (jlong) (long) page;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsPage_free(JNIEnv *env, jclass clazz, jlong docHandle, jlong handle)
{

    renderdocument_t *doc = (renderdocument_t*) (long) docHandle;
    renderpage_t *page = (renderpage_t*) (long) handle;

    DEBUG("XpsPage_free(%p)", page);

    if (page)
    {
        if (page->page)
            xps_free_page(doc->xpsdoc, page->page);

        if (page->pageList)
            fz_free_display_list(doc->ctx, page->pageList);

        free(page);
    }
}


JNIEXPORT void JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsPage_getBounds(JNIEnv *env, jclass clazz, jlong dochandle, jlong handle, jfloatArray bounds)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page = (renderpage_t*) (long) handle;
    jfloat *bbox = (*env)->GetPrimitiveArrayCritical(env, bounds, 0);
    if (!bbox)
        return;
    fz_rect page_bounds = xps_bound_page(doc->xpsdoc, page->page);
    DEBUG("Bounds: %f %f %f %f", page_bounds.x0, page_bounds.y0, page_bounds.x1, page_bounds.y1);
    bbox[0] = page_bounds.x0;
    bbox[1] = page_bounds.y0;
    bbox[2] = page_bounds.x1;
    bbox[3] = page_bounds.y1;
    (*env)->ReleasePrimitiveArrayCritical(env, bounds, bbox, 0);
}


JNIEXPORT void JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsPage_renderPage(JNIEnv *env, jobject this, jlong dochandle, jlong pagehandle,
                                                      jintArray viewboxarray, jfloatArray matrixarray,
                                                      jintArray bufferarray)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page = (renderpage_t*) (long) pagehandle;
    DEBUG("XpsView(%p).renderPage(%p, %p)", this, doc, page);
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
    DEBUG("Matrix: %f %f %f %f %f %f", ctm.a, ctm.b, ctm.c, ctm.d, ctm.e, ctm.f);

    viewboxarr = (*env)->GetPrimitiveArrayCritical(env, viewboxarray, 0);
    viewbox.x0 = viewboxarr[0];
    viewbox.y0 = viewboxarr[1];
    viewbox.x1 = viewboxarr[2];
    viewbox.y1 = viewboxarr[3];

    (*env)->ReleasePrimitiveArrayCritical(env, viewboxarray, viewboxarr, 0);
    DEBUG("Viewbox: %d %d %d %d", viewbox.x0, viewbox.y0, viewbox.x1, viewbox.y1);
    /* do the rendering */

    buffer = (*env)->GetPrimitiveArrayCritical(env, bufferarray, 0);

//	pixmap = fz_new_pixmap_with_data(fz_device_bgr, viewbox.x0, viewbox.y0, viewbox.x1 - viewbox.x0, viewbox.y1 - viewbox.y0, (unsigned char*)buffer);
    pixmap = fz_new_pixmap_with_data(doc->ctx, fz_device_bgr, viewbox.x1 - viewbox.x0, viewbox.y1 - viewbox.y0,
        (unsigned char*) buffer);

    DEBUG("doing the rendering...");

    fz_clear_pixmap_with_color(pixmap, 0xff);

    dev = fz_new_draw_device(doc->ctx, pixmap);
    fz_execute_display_list(page->pageList, dev, ctm, viewbox, NULL);
    fz_free_device(dev);

    (*env)->ReleasePrimitiveArrayCritical(env, bufferarray, buffer, 0);

    fz_drop_pixmap(doc->ctx, pixmap);
    DEBUG("XpsView.renderPage() done");
}


/*JNI BITMAP API*/

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsContext_isNativeGraphicsAvailable(JNIEnv *env, jobject this)
{
    return NativePresent();
}

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsPage_renderPageBitmap(JNIEnv *env, jobject this, jlong dochandle,
                                                            jlong pagehandle, jintArray viewboxarray,
                                                            jfloatArray matrixarray, jobject bitmap)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
    renderpage_t *page = (renderpage_t*) (long) pagehandle;
    DEBUG("XpsView(%p).renderPageBitmap(%p, %p)", this, doc, page);
    fz_matrix ctm;
    fz_bbox viewbox;
    fz_pixmap *pixmap;
    jfloat *matrix;
    jint *viewboxarr;
    jint *dimen;
    jint *buffer;
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

    DEBUG("Checking format\n");
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
    {
        ERROR("Bitmap format is not RGBA_8888 !");
        return 0;
    }

    DEBUG("locking pixels\n");
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
    DEBUG("Matrix: %f %f %f %f %f %f", ctm.a, ctm.b, ctm.c, ctm.d, ctm.e, ctm.f);

    viewboxarr = (*env)->GetPrimitiveArrayCritical(env, viewboxarray, 0);
    viewbox.x0 = viewboxarr[0];
    viewbox.y0 = viewboxarr[1];
    viewbox.x1 = viewboxarr[2];
    viewbox.y1 = viewboxarr[3];

    (*env)->ReleasePrimitiveArrayCritical(env, viewboxarray, viewboxarr, 0);
    DEBUG("Viewbox: %d %d %d %d", viewbox.x0, viewbox.y0, viewbox.x1, viewbox.y1);

    /* do the rendering */

    pixmap = fz_new_pixmap_with_data(doc->ctx, fz_device_rgb, viewbox.x1 - viewbox.x0, viewbox.y1 - viewbox.y0, pixels);

    DEBUG("doing the rendering...");

    fz_clear_pixmap_with_color(pixmap, 0xff);
    DEBUG("doing the rendering...0");

    dev = fz_new_draw_device(doc->ctx, pixmap);
    DEBUG("doing the rendering...1");

    fz_execute_display_list(page->pageList, dev, ctm, viewbox, NULL);

    DEBUG("doing the rendering...2");

    fz_free_device(dev);
    DEBUG("doing the rendering...3");

    fz_drop_pixmap(doc->ctx, pixmap);

    DEBUG("XPSView.renderPageBitmap() done");

    NativeBitmap_unlockPixels(env, bitmap);

    return 1;
}


//Outline
JNIEXPORT jlong JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsOutline_open(JNIEnv *env, jclass clazz, jlong dochandle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;

    if (!doc->outline)
        doc->outline = xps_load_outline(doc->xpsdoc);

    DEBUG("XpsOutline.open(): return handle = %p", doc->outline);
    return (jlong) (long) doc->outline;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsOutline_free(JNIEnv *env, jclass clazz, jlong dochandle)
{
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;
//    DEBUG("XpsOutline_free(%p)", doc);
    if (doc)
    {
        if (doc->outline)
            fz_free_outline(doc->ctx, doc->outline);
        doc->outline = NULL;
    }
//    DEBUG("XpsOutline_free(%p)", doc);
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsOutline_getTitle(JNIEnv *env, jclass clazz, jlong outlinehandle)
{
    fz_outline *outline = (fz_outline*) (long) outlinehandle;
//    DEBUG("XpsOutline_getTitle(%p)",outline);
    if (outline)
        return (*env)->NewStringUTF(env, outline->title);
    return NULL;
}

JNIEXPORT jstring JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsOutline_getLink(JNIEnv *env, jclass clazz, jlong outlinehandle, jlong dochandle)
{
    fz_outline *outline = (fz_outline*) (long) outlinehandle;
    renderdocument_t *doc = (renderdocument_t*) (long) dochandle;

    // DEBUG("XpsOutline_getLink(%p)",outline);
    if (!outline)
        return NULL;

    char linkbuf[128];
    if (outline->dest.kind == FZ_LINK_URI)
    {
        snprintf(linkbuf, 128, "%s",  outline->dest.ld.uri.uri);
        // DEBUG("XpsOutline_getLink uri = %s",linkbuf);
    }
    else if (outline->dest.kind == FZ_LINK_GOTO)
    {
        snprintf(linkbuf, 127, "#%d", outline->dest.ld.gotor.page + 1);
        // DEBUG("XpsOutline_getLink goto = %s",linkbuf);
    }
    else
    {
        return NULL;
    }

    return (*env)->NewStringUTF(env, linkbuf);
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsOutline_getNext(JNIEnv *env, jclass clazz, jlong outlinehandle)
{
    fz_outline *outline = (fz_outline*) (long) outlinehandle;
//	DEBUG("XpsOutline_getNext(%p)",outline);
    if (!outline)
        return 0;
    return (jlong) (long) outline->next;
}

JNIEXPORT jlong JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsOutline_getChild(JNIEnv *env, jclass clazz, jlong outlinehandle)
{
    fz_outline *outline = (fz_outline*) (long) outlinehandle;
//	DEBUG("XpsOutline_getChild(%p)",outline);
    if (!outline)
        return 0;
    return (jlong) (long) outline->down;
}

