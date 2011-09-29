#include <jni.h>

#include <android/log.h>

/*JNI BITMAP API */

#include <nativebitmap.h>

//#ifdef USE_JNI_BITMAP_API
//#include <android/bitmap.h>
//#endif

#include <errno.h>

#include <fitz.h>
#include <mupdf.h>

/* Debugging helper */

#define DEBUG(args...) \
	__android_log_print(ANDROID_LOG_DEBUG, "PdfDroid", args)

#define ERROR(args...) \
	__android_log_print(ANDROID_LOG_ERROR, "PdfDroid", args)

#define INFO(args...) \
	__android_log_print(ANDROID_LOG_INFO, "PdfDroid", args)


typedef struct renderdocument_s renderdocument_t;
struct renderdocument_s
{
	pdf_xref *xref;
	pdf_outline *outline;
	fz_glyph_cache *drawcache;
};

typedef struct renderpage_s renderpage_t;
struct renderpage_s
{
	pdf_page *page;
	fz_display_list *pageList;
	int pageno;
};

static int current_page = 0;

#define RUNTIME_EXCEPTION "java/lang/RuntimeException"

void throw_exception(JNIEnv *env, char *message)
{
	jthrowable new_exception = (*env)->FindClass(env, RUNTIME_EXCEPTION);
	if(new_exception == NULL) {
		return;
	} else {
		DEBUG("Exception '%s', Message: '%s'", RUNTIME_EXCEPTION, message);
	}
	(*env)->ThrowNew(env, new_exception, message);
}


static void pdf_free_document(renderdocument_t* doc)
{
    if(doc)
    {
	if(doc->outline)
	    pdf_free_outline(doc->outline);
	doc->outline = NULL;

	if (doc->drawcache)
	    fz_free_glyph_cache(doc->drawcache);
	doc->drawcache = NULL;

	if(doc->xref)
	{
	    if(doc->xref->store)
		pdf_free_store(doc->xref->store);
	    doc->xref->store = NULL;

	    pdf_free_xref(doc->xref);
	}
	doc->xref = NULL;

	fz_free(doc);
	doc = NULL;
    }
}



JNIEXPORT jlong JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfDocument_open
	(JNIEnv *env, jclass clazz,
			jint fitzmemory, jstring fname, jstring pwd)
{
	fz_error error;
	fz_obj *obj;
	renderdocument_t *doc;
	jboolean iscopy;
	jclass cls;
	jfieldID fid;
	char *filename;
	char *password;

	filename = (char*)(*env)->GetStringUTFChars(env, fname, &iscopy);
	password = (char*)(*env)->GetStringUTFChars(env, pwd, &iscopy);

	doc = fz_malloc(sizeof(renderdocument_t));
	if(!doc)
	{
		throw_exception(env, "Out of Memory");
		goto cleanup;
	}
	doc->xref = NULL;
	doc->outline = NULL;
	doc->drawcache = NULL;

	/* initialize renderer */

	doc->drawcache = fz_new_glyph_cache();
	if (!doc->drawcache)
	{
		pdf_free_document(doc);
		throw_exception(env, "Cannot create new renderer");
		goto cleanup;
	}

	/*
	 * Open PDF and load xref table
	 */
	error = pdf_open_xref(&(doc->xref), filename, NULL);

	if (error || (!doc->xref))
	{
		pdf_free_document(doc);
		throw_exception(env, "PDF file not found or corrupted");
		goto cleanup;
	}

	/*
	 * Handle encrypted PDF files
	 */

	if (pdf_needs_password(doc->xref))
	{
		if(strlen(password))
		{
			int ok = pdf_authenticate_password(doc->xref, password);
			if(!ok)
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

	error = pdf_load_page_tree(doc->xref);
	if (error)
	{
		pdf_free_document(doc);
    	    	throw_exception(env, "error loading pagetree");
		goto cleanup;
	}

cleanup:

	(*env)->ReleaseStringUTFChars(env, fname, filename);
	(*env)->ReleaseStringUTFChars(env, pwd, password);

	DEBUG("PdfDocument.nativeOpen(): return handle = %p", doc);
	return (jlong) (long)doc;
}

JNIEXPORT void JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfDocument_free
	(JNIEnv *env, jclass clazz, jlong handle)
{
	renderdocument_t *doc = (renderdocument_t*) (long)handle;
	pdf_free_document(doc);
}

JNIEXPORT jint JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfDocument_getPageInfo(JNIEnv *env,
                                    jclass cls,
                                    jlong handle,
                                    jint pageNumber,
                                    jobject cpi)
{

    renderdocument_t *doc = (renderdocument_t*) (long)handle;

///


    jclass clazz;
    jfieldID fid;

    fz_error error = 0;
    fz_obj *pageobj = NULL;
    fz_obj *boxobj = NULL;
    fz_rect bbox;
    fz_obj *rotateobj = NULL;
    int rotate = 0;

    pageobj = doc->xref->page_objs[pageNumber - 1];
    boxobj = fz_dict_gets(pageobj, "MediaBox");
    if (boxobj == NULL)
      boxobj = fz_dict_gets(pageobj, "CropBox");
    bbox = pdf_to_rect(boxobj);

    rotateobj = fz_dict_gets(pageobj, "Rotate");
    if (fz_is_int(rotateobj))
	rotate = fz_to_int(rotateobj);
    else
	rotate = 0;

    clazz = (*env)->GetObjectClass(env,cpi);
    if (0 == clazz)
    {
	return(-1);
    }

    fid = (*env)->GetFieldID(env,clazz,"width","I");
    (*env)->SetIntField(env,cpi,fid,bbox.x1 - bbox.x0);

    fid = (*env)->GetFieldID(env,clazz,"height","I");
    (*env)->SetIntField(env,cpi,fid,bbox.y1 - bbox.y0);

    fid = (*env)->GetFieldID(env,clazz,"dpi","I");
    (*env)->SetIntField(env,cpi,fid,0);

    fid = (*env)->GetFieldID(env,clazz,"rotation","I");
    (*env)->SetIntField(env,cpi,fid,rotate);

    fid = (*env)->GetFieldID(env,clazz,"version","I");
    (*env)->SetIntField(env,cpi,fid,0);
    return 0;

///



//	DEBUG("PdfDocument.getPageInfo = %p", doc);
/*
	pdf_page *page = NULL;



	fz_error* error = pdf_load_page(&page, doc->xref, pageNumber - 1);

	if(!error && page)
	{
	    clazz = (*env)->GetObjectClass(env,cpi);
	    if (0 == clazz)
	    {
		return(-1);
	    }

	    fid = (*env)->GetFieldID(env,clazz,"width","I");
	    (*env)->SetIntField(env,cpi,fid,page->mediabox.x1 - page->mediabox.x0);

	    fid = (*env)->GetFieldID(env,clazz,"height","I");
	    (*env)->SetIntField(env,cpi,fid,page->mediabox.y1 - page->mediabox.y0);

	    fid = (*env)->GetFieldID(env,clazz,"dpi","I");
	    (*env)->SetIntField(env,cpi,fid,0);

	    fid = (*env)->GetFieldID(env,clazz,"rotation","I");
	    (*env)->SetIntField(env,cpi,fid,page->rotate);

	    fid = (*env)->GetFieldID(env,clazz,"version","I");
	    (*env)->SetIntField(env,cpi,fid,0);

    	    pdf_free_page(page);
    	    return 0;
	}
	return(-1);
    */
}


JNIEXPORT jobject JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfDocument_getPageLinks
	(JNIEnv *env, jclass clazz, jlong handle, jint pageno)
{

	renderdocument_t *doc = (renderdocument_t*) (long)handle;

	DEBUG("PdfDocument.getLinks = %p", doc);

	pdf_link *link;

	jobject arrayList = NULL;

	pdf_page *page = NULL;

	fz_error* error = pdf_load_page(&page, doc->xref, pageno - 1);

	if(!error && page && page->links) {
	    jclass arrayListClass = (*env)->FindClass(env,"java/util/ArrayList");
	    if(!arrayListClass)
	        return arrayList;

	    jmethodID alInitMethodId = (*env)->GetMethodID(env,arrayListClass, "<init>", "()V");
	    if(!alInitMethodId)
	        return arrayList;

	    jmethodID alAddMethodId = (*env)->GetMethodID(env,arrayListClass, "add", "(Ljava/lang/Object;)Z");
	    if(!alAddMethodId)
	        return arrayList;

	    arrayList = (*env)->NewObject(env,arrayListClass, alInitMethodId);
	    if(!arrayList)
	        return arrayList;


	     for (link = page->links; link; link = link->next)
	     {

	    	jclass pagelinkClass = (*env)->FindClass(env,"org/ebookdroid/core/PageLink");
		if(!pagelinkClass)
		    return arrayList;

		jmethodID plInitMethodId = (*env)->GetMethodID(env,pagelinkClass, "<init>", "(Ljava/lang/String;I[I)V");
		if(!plInitMethodId)
		    return arrayList;

		jint data[4];
		data[0] = link->rect.x0;
		data[1] = link->rect.y0;
		data[2] = link->rect.x1;
		data[3] = link->rect.y1;
		jintArray points = (*env)->NewIntArray(env,4);
		(*env)->SetIntArrayRegion(env,points, 0, 4, data);


		char linkbuf[128];
		int number;

		if (link->kind == PDF_LINK_URI)
		{
		    int len = (fz_to_str_len(link->dest) < 127)?fz_to_str_len(link->dest):127;
		    snprintf(linkbuf,len,"%s",fz_to_str_buf(link->dest));
		    linkbuf[len] = 0;
		}
		else if (link->kind == PDF_LINK_GOTO)
		{
		    number = pdf_find_page_number(doc->xref, fz_array_get(link->dest, 0));
		    if (number < 0)
			return NULL;
		    snprintf(linkbuf,127,"#%d",number+1);
		}

		jstring jstr = (*env)->NewStringUTF(env,linkbuf);

		jobject hl = (*env)->NewObject(env,pagelinkClass, plInitMethodId, jstr, (jint)1 ,points);

		(*env)->DeleteLocalRef(env,jstr);
		(*env)->DeleteLocalRef(env,points);


		if(hl)
		    (*env)->CallBooleanMethod(env,arrayList, alAddMethodId, hl);
		//jenv->DeleteLocalRef(hl);
	    }
	    pdf_free_page(page);
	}
	return arrayList;
}




JNIEXPORT jint JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfDocument_getPageCount
	(JNIEnv *env, jclass clazz, jlong handle)
{
	renderdocument_t *doc = (renderdocument_t*) (long)handle;
	return (pdf_count_pages(doc->xref));
}

JNIEXPORT jlong JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfPage_open
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
		throw_exception(env, "Out of Memory");
		return (jlong) (long)NULL;
	}

	error = pdf_load_page(&page->page, doc->xref, pageno - 1);
	if (error)
	{
		throw_exception(env, "error loading page");
		goto cleanup;
	}
	page->pageno = pageno - 1;

	pdf_age_store(doc->xref->store, 2);

//New draw page
	page->pageList = fz_new_display_list();
	fz_device *dev = fz_new_list_device(page->pageList);
	pdf_run_page(doc->xref, page->page , dev, fz_identity);
	fz_free_device(dev);
//

cleanup:
	/* nothing yet */

	DEBUG("PdfPage.nativeOpenPage(): return handle = %p", page);
	return (jlong) (long)page;
}





JNIEXPORT void JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfPage_free
	(JNIEnv *env, jclass clazz, jlong handle)
{

	renderpage_t *page = (renderpage_t*) (long)handle;
	DEBUG("PdfPage_free(%p)",page);

	if(page) {
		if (page->page)
			pdf_free_page(page->page);
//New draw page
		if (page->pageList)
		    fz_free_display_list(page->pageList);
//
		fz_free(page);
	}
}


JNIEXPORT void JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfPage_getMediaBox
	(JNIEnv *env, jclass clazz, jlong handle, jfloatArray mediabox)
{
	renderpage_t *page = (renderpage_t*) (long)handle;
	jfloat *bbox = (*env)->GetPrimitiveArrayCritical(env, mediabox, 0);
	if(!bbox) return;
//	DEBUG("Mediabox: %f %f %f %f", page->page->mediabox.x0, page->page->mediabox.y0, page->page->mediabox.x1, page->page->mediabox.y1);
	bbox[0] = page->page->mediabox.x0;
	bbox[1] = page->page->mediabox.y0;
	bbox[2] = page->page->mediabox.x1;
	bbox[3] = page->page->mediabox.y1;
	(*env)->ReleasePrimitiveArrayCritical(env, mediabox, bbox, 0);
}

JNIEXPORT jint JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfPage_getRotate
	(JNIEnv *env, jclass clazz, jlong handle)
{
	renderpage_t *page = (renderpage_t*) (long)handle;
	return page->page->rotate;
}

JNIEXPORT void JNICALL
Java_org_ebookdroid_pdfdroid_codec_PdfPage_renderPage
	(JNIEnv *env, jobject this, jlong dochandle, jlong pagehandle,
		jintArray viewboxarray, jfloatArray matrixarray,
		jintArray bufferarray)
{
	renderdocument_t *doc = (renderdocument_t*) (long)dochandle;
	renderpage_t *page = (renderpage_t*) (long)pagehandle;
	DEBUG("PdfView(%p).renderPage(%p, %p)", this, doc, page);
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

	if(current_page != page->pageno)
	{
		pdf_age_store(doc->xref->store, 1);
		current_page = page->pageno;
	}

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

	pixmap = fz_new_pixmap_with_data(fz_device_bgr, viewbox.x1 - viewbox.x0, viewbox.y1 - viewbox.y0, (unsigned char*)buffer);

	DEBUG("doing the rendering...");

	fz_clear_pixmap_with_color(pixmap, 0xff);

	dev = fz_new_draw_device(doc->drawcache, pixmap);
        fz_execute_display_list(page->pageList, dev, ctm, viewbox);
        fz_free_device(dev);

	(*env)->ReleasePrimitiveArrayCritical(env, bufferarray, buffer, 0);

	fz_drop_pixmap(pixmap);

	DEBUG("PdfView.renderPage() done");
}

/*JNI BITMAP API*/

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_pdfdroid_codec_PdfPage_isNativeGraphicsAvailable
	(JNIEnv *env, jobject this)
{
    return NativePresent();
//#ifdef USE_JNI_BITMAP_API
//	return 1;
//#else
//	return 0;
//#endif
}

JNIEXPORT jboolean JNICALL
Java_org_ebookdroid_pdfdroid_codec_PdfPage_renderPageBitmap
    (JNIEnv *env, jobject this, jlong dochandle, jlong pagehandle,
	jintArray viewboxarray, jfloatArray matrixarray,
	jobject bitmap)
{
//#ifdef USE_JNI_BITMAP_API
    renderdocument_t *doc = (renderdocument_t*) (long)dochandle;
    renderpage_t *page = (renderpage_t*) (long)pagehandle;
    DEBUG("PdfView(%p).renderPageBitmap(%p, %p)", this, doc, page);
    fz_error error;
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
    
    if(current_page != page->pageno)
    {
	pdf_age_store(doc->xref->store, 1);
	current_page = page->pageno;
    }

    

    if ((ret = NativeBitmap_getInfo(env, bitmap, &info)) < 0) {
            ERROR("AndroidBitmap_getInfo() failed ! error=%d", ret);
            return 0;
    }

    DEBUG("Checking format\n");
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
            ERROR("Bitmap format is not RGBA_8888 !");
            return 0;
    }

    DEBUG("locking pixels\n");
    if ((ret = NativeBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
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

    pixmap = fz_new_pixmap_with_data(fz_device_rgb, viewbox.x1 - viewbox.x0, viewbox.y1 - viewbox.y0, pixels);

    DEBUG("doing the rendering...");

    fz_clear_pixmap_with_color(pixmap, 0xff);

    dev = fz_new_draw_device(doc->drawcache, pixmap);
    fz_execute_display_list(page->pageList, dev, ctm, viewbox);
    fz_free_device(dev);
    fz_drop_pixmap(pixmap);

    DEBUG("PdfView.renderPage() done");

    NativeBitmap_unlockPixels(env, bitmap);

    return 1;
//#else
//	DEBUG("PdfView(%p).renderPageBitmap(): not implemented", this);
//	return 0;
//#endif
}

//Outline
JNIEXPORT jlong JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfOutline_open
	(JNIEnv *env, jclass clazz, jlong dochandle)
{
    renderdocument_t *doc = (renderdocument_t*) (long)dochandle;
    if(!doc->outline)
        doc->outline = pdf_load_outline(doc->xref);
    DEBUG("PdfOutline.open(): return handle = %p", doc->outline);
    return (jlong) (long)doc->outline;
}

JNIEXPORT void JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfOutline_free
	(JNIEnv *env, jclass clazz, jlong dochandle)
{
	renderdocument_t *doc = (renderdocument_t*) (long)dochandle;
	DEBUG("PdfOutline_free(%p)",doc);
	if(doc)
	{
	    if(doc->outline)
		pdf_free_outline(doc->outline);
	    doc->outline = NULL;
	}
}

JNIEXPORT jstring JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfOutline_getTitle
	(JNIEnv *env, jclass clazz, jlong outlinehandle)
{
	pdf_outline *outline = (pdf_outline*) (long)outlinehandle;
//	DEBUG("PdfOutline_getTitle(%p)",outline);
	if(outline)
	    return (*env)->NewStringUTF(env, outline->title);
	return NULL;
}

JNIEXPORT jstring JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfOutline_getLink
	(JNIEnv *env, jclass clazz, jlong outlinehandle, jlong dochandle)
{
	pdf_outline *outline = (pdf_outline*) (long)outlinehandle;
	renderdocument_t *doc = (renderdocument_t*) (long)dochandle;

//	DEBUG("PdfOutline_getLink(%p)",outline);
	if(!outline)
	    return NULL;

	pdf_link* link = outline->link;
	if(!link)
	    return NULL;

	char linkbuf[128];
	int number;

	if (link->kind == PDF_LINK_URI)
	{
	    int len = (fz_to_str_len(link->dest) < 127)?fz_to_str_len(link->dest):127;
	    snprintf(linkbuf,len,"%s",fz_to_str_buf(link->dest));
		linkbuf[len] = 0;
	}
	else if (link->kind == PDF_LINK_GOTO)
	{
	    number = pdf_find_page_number(doc->xref, fz_array_get(link->dest, 0));
	    if (number < 0)
		return NULL;
	    snprintf(linkbuf,127,"#%d",number+1);
	}
	return (*env)->NewStringUTF(env, linkbuf);
}


JNIEXPORT jlong JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfOutline_getNext
	(JNIEnv *env, jclass clazz, jlong outlinehandle)
{
	pdf_outline *outline = (pdf_outline*) (long)outlinehandle;
//	DEBUG("PdfOutline_getNext(%p)",outline);
	if(!outline)
	    return 0;
	return (jlong) (long)outline->next;
}

JNIEXPORT jlong JNICALL
	Java_org_ebookdroid_pdfdroid_codec_PdfOutline_getChild
	(JNIEnv *env, jclass clazz, jlong outlinehandle)
{
	pdf_outline *outline = (pdf_outline*) (long)outlinehandle;
//	DEBUG("PdfOutline_getChild(%p)",outline);
	if(!outline)
	    return 0;
	return (jlong) (long)outline->child;
}
