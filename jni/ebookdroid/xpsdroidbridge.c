#include <jni.h>

#include <android/log.h>

#include <errno.h>

#include <fitz.h>
#include <muxps.h>

/* Debugging helper */

#define DEBUG(args...) \
	__android_log_print(ANDROID_LOG_DEBUG, "XPSDroid", args)

#define ERROR(args...) \
	__android_log_print(ANDROID_LOG_ERROR, "XPSDroid", args)

#define INFO(args...) \
	__android_log_print(ANDROID_LOG_INFO, "XPSDroid", args)


typedef struct renderdocument_s renderdocument_t;
struct renderdocument_s
{
	xps_context *ctx;
	fz_glyph_cache *drawcache;
};

typedef struct renderpage_s renderpage_t;
struct renderpage_s
{
	xps_page *page;
//New draw page
	fz_display_list *pageList;
//
};

#define RUNTIME_EXCEPTION "java/lang/RuntimeException"

void xps_throw_exception(JNIEnv *env, char *message)
{
	jthrowable new_exception = (*env)->FindClass(env, RUNTIME_EXCEPTION);
	if(new_exception == NULL) {
		return;
	} else {
		DEBUG("Exception '%s', Message: '%s'", RUNTIME_EXCEPTION, message);
	}
	(*env)->ThrowNew(env, new_exception, message);
}


JNIEXPORT jlong JNICALL
	Java_org_ebookdroid_xpsdroid_codec_XpsDocument_open
	(JNIEnv *env, jclass clazz,
			jint fitzmemory, jstring fname)
{
	fz_error error;
	fz_obj *obj;
	renderdocument_t *doc;
	jboolean iscopy;
	jclass cls;
	jfieldID fid;
	char *filename;

	filename = (char*)(*env)->GetStringUTFChars(env, fname, &iscopy);

	doc = fz_malloc(sizeof(renderdocument_t));
	if(!doc) 
	{
		xps_throw_exception(env, "Out of Memory");
		goto cleanup;
	}

	/* initialize renderer */

	doc->drawcache = fz_new_glyph_cache();
	if (!doc->drawcache) 
	{
		xps_throw_exception(env, "Cannot create new renderer");
		goto cleanup;
	}

	error = xps_open_file(&(doc->ctx), filename);
	if (error || (!doc->ctx)) 
	{
		xps_throw_exception(env, "XPS file not found or corrupted");
		goto cleanup;
	}

cleanup:

	(*env)->ReleaseStringUTFChars(env, fname, filename);

	DEBUG("XpsDocument.nativeOpen(): return handle = %p", doc);
	return (jlong) (long)doc;
}

JNIEXPORT void JNICALL
	Java_org_ebookdroid_xpsdroid_codec_XpsDocument_free
	(JNIEnv *env, jclass clazz, jlong handle)
{
	renderdocument_t *doc = (renderdocument_t*) (long)handle;

	if(doc) 
	{			
		if (doc->drawcache) 
		    fz_free_glyph_cache(doc->drawcache);
		doc->drawcache = NULL;
		
		if(doc->ctx)
		    xps_free_context(doc->ctx);		
		doc->ctx = NULL;
		
		fz_free(doc);
		doc = NULL;
	}
}


JNIEXPORT jint JNICALL
	Java_org_ebookdroid_xpsdroid_codec_XpsDocument_getPageInfo(JNIEnv *env,
                                    jclass cls,
                                    jlong handle,
                                    jint pageNumber,
                                    jobject cpi)
{

	renderdocument_t *doc = (renderdocument_t*) (long)handle;

	DEBUG("XpsDocument.getPageInfo = %p", doc);

	xps_page *page = NULL;
	
	jclass clazz;
	jfieldID fid;


	fz_error* error = xps_load_page(&page, doc->ctx, pageNumber - 1);


	if(!error && page) 
	{
	    clazz = (*env)->GetObjectClass(env,cpi);
	    if (0 == clazz)
	    {
		return(-1);
	    }
	    
	    fid = (*env)->GetFieldID(env,clazz,"width","I");
	    (*env)->SetIntField(env,cpi,fid,page->width);
	    
	    fid = (*env)->GetFieldID(env,clazz,"height","I");
	    (*env)->SetIntField(env,cpi,fid,page->height);

	    fid = (*env)->GetFieldID(env,clazz,"dpi","I");
	    (*env)->SetIntField(env,cpi,fid,0);

	    fid = (*env)->GetFieldID(env,clazz,"rotation","I");
	    (*env)->SetIntField(env,cpi,fid,0);

	    fid = (*env)->GetFieldID(env,clazz,"version","I");
	    (*env)->SetIntField(env,cpi,fid,0);

    	    xps_free_page(doc->ctx, page);
    	    return 0;
	}
	return(-1);
}




static void
xps_run_page(xps_context *ctx, xps_page *page, fz_device *dev, fz_matrix ctm)
{
	ctx->dev = dev;
	xps_parse_fixed_page(ctx, ctm, page);
	ctx->dev = NULL;
}


JNIEXPORT jint JNICALL
	Java_org_ebookdroid_xpsdroid_codec_XpsDocument_getPageCount
	(JNIEnv *env, jclass clazz, jlong handle)
{
	renderdocument_t *doc = (renderdocument_t*) (long)handle;
	return (xps_count_pages(doc->ctx));
}

JNIEXPORT jlong JNICALL
	Java_org_ebookdroid_xpsdroid_codec_XpsPage_open
	(JNIEnv *env, jclass clazz, jlong dochandle, jint pageno)
{
	renderdocument_t *doc = (renderdocument_t*) (long)dochandle;
	renderpage_t *page;
	fz_error error;
	fz_obj *obj;
	jclass cls;
	jfieldID fid;

	page = fz_malloc(sizeof(renderpage_t));
	if(!page) 
	{
		xps_throw_exception(env, "Out of Memory");
		return (jlong) (long)NULL;
	}
	
	error = xps_load_page(&page->page, doc->ctx, pageno - 1);
	if (error) 
	{
		xps_throw_exception(env, "error loading page");
		goto cleanup;
	}

//New draw page
	page->pageList = fz_new_display_list();
	fz_device *dev = fz_new_list_device(page->pageList);
	xps_run_page(doc->ctx, page->page , dev, fz_identity);
	fz_free_device(dev);
//

cleanup:
	/* nothing yet */

	DEBUG("XpsPage.nativeOpenPage(): return handle = %p", page);
	return (jlong) (long)page;
}


JNIEXPORT void JNICALL
	Java_org_ebookdroid_xpsdroid_codec_XpsPage_free
	(JNIEnv *env, jclass clazz, jlong docHandle, jlong handle)
{

	renderdocument_t *doc = (renderdocument_t*) (long)docHandle;
	renderpage_t *page = (renderpage_t*) (long)handle;

	DEBUG("XpsPage_free(%p)",page);

	if(page) {
		if (page->page)
		    xps_free_page(doc->ctx, page->page);
		    
		if (page->pageList)
		    fz_free_display_list(page->pageList);

		fz_free(page);
	}
}

JNIEXPORT jint JNICALL
	Java_org_ebookdroid_xpsdroid_codec_XpsPage_getPageWidth
	(JNIEnv *env, jclass clazz, jlong handle)
{
	renderpage_t *page = (renderpage_t*) (long)handle;
	return page->page->width;
	                   
}

JNIEXPORT jint JNICALL
	Java_org_ebookdroid_xpsdroid_codec_XpsPage_getPageHeight
	(JNIEnv *env, jclass clazz, jlong handle)
{
	renderpage_t *page = (renderpage_t*) (long)handle;
	return page->page->height;
}


JNIEXPORT void JNICALL
Java_org_ebookdroid_xpsdroid_codec_XpsPage_renderPage
	(JNIEnv *env, jobject this, jlong dochandle, jlong pagehandle,
		jintArray viewboxarray, jfloatArray matrixarray,
		jintArray bufferarray)
{
	renderdocument_t *doc = (renderdocument_t*) (long)dochandle;
	renderpage_t *page = (renderpage_t*) (long)pagehandle;
	DEBUG("XpsView(%p).renderPage(%p, %p)", this, doc, page);
	fz_error error;
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
	pixmap = fz_new_pixmap_with_data(fz_device_bgr, viewbox.x1 - viewbox.x0, viewbox.y1 - viewbox.y0, (unsigned char*)buffer);

	DEBUG("doing the rendering...");
	
	fz_clear_pixmap_with_color(pixmap, 0xff);

	dev = fz_new_draw_device(doc->drawcache, pixmap);
        fz_execute_display_list(page->pageList, dev, ctm, viewbox);
        fz_free_device(dev);

	(*env)->ReleasePrimitiveArrayCritical(env, bufferarray, buffer, 0);
	
	fz_drop_pixmap(pixmap);
	DEBUG("XpsView.renderPage() done");
}
