LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

# compile the needed libraries into one big archive file

LOCAL_MODULE := mupdf

# freetype
# (flat file hierarchy, use 
# "cp .../freetype-.../src/*/*.[ch] freetype/"
#  and copy over the full include/ subdirectory)

# Homepage: http://freetype.org/
# Original Licence: GPL 2 (or its own, but for the purposes
#                   of this project, GPL is fine)
# 

MY_FREETYPE_C_INCLUDES := \
	$(LOCAL_PATH)/freetype-overlay/include \
	$(LOCAL_PATH)/freetype/include

MY_FREETYPE_CFLAGS := -DFT2_BUILD_LIBRARY -DDARWIN_NO_CARBON

# libz provided by the Android-3 Stable Native API:
MY_FREETYPE_LDLIBS := -lz

# see freetype/doc/INSTALL.ANY for further customization,
# currently, all sources are being built
MY_FREETYPE_SRC_FILES := \
	freetype/src/base/ftsystem.c \
	freetype/src/base/ftinit.c \
	freetype/src/base/ftdebug.c \
	freetype/src/base/ftbase.c \
	freetype/src/base/ftbbox.c \
	freetype/src/base/ftglyph.c \
	freetype/src/base/ftbitmap.c \
	freetype/src/base/ftcid.c \
	freetype/src/base/ftfstype.c \
	freetype/src/base/ftgasp.c \
	freetype/src/base/ftgxval.c \
	freetype/src/base/ftlcdfil.c \
	freetype/src/base/ftmm.c \
	freetype/src/base/ftotval.c \
	freetype/src/base/ftpatent.c \
	freetype/src/base/ftstroke.c \
	freetype/src/base/ftsynth.c \
	freetype/src/base/fttype1.c \
	freetype/src/base/ftxf86.c \
	freetype/src/cff/cff.c \
	freetype/src/cid/type1cid.c \
	freetype/src/sfnt/sfnt.c \
	freetype/src/truetype/truetype.c \
	freetype/src/type1/type1.c \
	freetype/src/raster/raster.c \
	freetype/src/smooth/smooth.c \
	freetype/src/cache/ftcache.c \
	freetype/src/gzip/ftgzip.c \
	freetype/src/gxvalid/gxvalid.c \
	freetype/src/otvalid/otvalid.c \
	freetype/src/psaux/psaux.c \
	freetype/src/pshinter/pshinter.c \
	freetype/src/psnames/psnames.c

#	freetype/src/base/ftpfr.c \
#	freetype/src/base/ftbdf.c \
#	freetype/src/base/ftwinfnt.c \

#	freetype/src/autofit/autofit.c \
#	freetype/src/pfr/pfr.c \
#	freetype/src/type42/type42.c \
#	freetype/src/pcf/pcf.c \
#	freetype/src/bdf/bdf.c \
#	freetype/src/winfonts/winfnt.c \
#	freetype/src/lzw/ftlzw.c \

# jbig2dec
MY_JBIG2DEC_CFLAGS := -DHAVE_CONFIG_H
MY_JBIG2DEC_SRC_FILES := \
	jbig2dec/jbig2.c \
	jbig2dec/jbig2_arith.c \
	jbig2dec/jbig2_arith_iaid.c \
	jbig2dec/jbig2_arith_int.c \
	jbig2dec/jbig2_generic.c \
	jbig2dec/jbig2_halftone.c \
	jbig2dec/jbig2_huffman.c \
	jbig2dec/jbig2_image.c \
	jbig2dec/jbig2_image_pbm.c \
	jbig2dec/jbig2_metadata.c \
	jbig2dec/jbig2_mmr.c \
	jbig2dec/jbig2_page.c \
	jbig2dec/jbig2_refinement.c \
	jbig2dec/jbig2_segment.c \
	jbig2dec/jbig2_symbol_dict.c \
	jbig2dec/jbig2_text.c \
	jbig2dec/jbig2dec.c \
	jbig2dec/sha1.c \
	jbig2dec/memento.c

#	jbig2dec/getopt.c
#	jbig2dec/getopt1.c
#	jbig2dec/memcmp.c
#	jbig2dec/snprintf.c
#   jbig2dec/jbig2_image_png.c

# openjpeg
MY_OPENJPEG_SRC_FILES := \
	openjpeg/bio.c \
	openjpeg/cio.c \
	openjpeg/cidx_manager.c \
	openjpeg/dwt.c \
	openjpeg/event.c \
	openjpeg/image.c \
	openjpeg/j2k.c \
	openjpeg/j2k_lib.c \
	openjpeg/jp2.c \
	openjpeg/jpt.c \
	openjpeg/mct.c \
	openjpeg/mqc.c \
	openjpeg/openjpeg.c \
	openjpeg/phix_manager.c \
	openjpeg/pi.c \
	openjpeg/ppix_manager.c \
	openjpeg/raw.c \
	openjpeg/t1.c \
	openjpeg/t2.c \
	openjpeg/tcd.c \
	openjpeg/tgt.c \
	openjpeg/thix_manager.c \
	openjpeg/tpix_manager.c


# mupdf
# pristine source tree

# Homepage: http://ccxvii.net/mupdf/
# Licence: GPL 3
# MuPDF is Copyright 2006-2009 Artifex Software, Inc. 

MY_MUPDF_C_INCLUDES := \
	$(LOCAL_PATH)/freetype/include \
	$(LOCAL_PATH)/../jpeg \
	$(LOCAL_PATH)/jbig2dec \
	$(LOCAL_PATH)/openjpeg \
	$(LOCAL_PATH)/mupdf/fitz \
	$(LOCAL_PATH)/mupdf/pdf \
	$(LOCAL_PATH)/mupdf/xps \
	$(LOCAL_PATH)/mupdf/generated \
	$(LOCAL_PATH)

# use this to build w/o a CJK font built-in:
#MY_MUPDF_CFLAGS := -Drestrict= -DNOCJK
# but see caveat below, unexpected breakage may occur.
# ATM, the irony is that CJK compiles in a bit-wise copy
# of Androids own droid.ttf ... Maybe resort to pointing
# to it in the filesystem? But this would violate proper
# API use. Bleh.
MY_MUPDF_CFLAGS := -DAA_BITS=8

MY_MUPDF_SRC_FILES := \
	mupdf/pdf/pdf_annot.c \
	mupdf/pdf/pdf_cmap.c \
	mupdf/pdf/pdf_cmap_load.c \
	mupdf/pdf/pdf_cmap_parse.c \
	mupdf/pdf/pdf_cmap_table.c \
	mupdf/pdf/pdf_colorspace.c \
	mupdf/pdf/pdf_crypt.c \
	mupdf/pdf/pdf_encoding.c \
	mupdf/pdf/pdf_event.c \
	mupdf/pdf/pdf_font.c \
	mupdf/pdf/pdf_fontfile.c \
	mupdf/pdf/pdf_form.c \
	mupdf/pdf/pdf_function.c \
	mupdf/pdf/pdf_image.c \
	mupdf/pdf/pdf_interpret.c \
	mupdf/pdf/pdf_js_none.c \
	mupdf/pdf/pdf_lex.c \
	mupdf/pdf/pdf_metrics.c \
	mupdf/pdf/pdf_nametree.c \
	mupdf/pdf/pdf_object.c \
	mupdf/pdf/pdf_outline.c \
	mupdf/pdf/pdf_page.c \
	mupdf/pdf/pdf_parse.c \
	mupdf/pdf/pdf_pattern.c \
	mupdf/pdf/pdf_repair.c \
	mupdf/pdf/pdf_shade.c \
	mupdf/pdf/pdf_store.c \
	mupdf/pdf/pdf_stream.c \
	mupdf/pdf/pdf_type3.c \
	mupdf/pdf/pdf_unicode.c \
	mupdf/pdf/pdf_write.c \
	mupdf/pdf/pdf_xobject.c \
	mupdf/pdf/pdf_xref_aux.c \
	mupdf/pdf/pdf_xref.c \
	mupdf/xps/xps_common.c \
	mupdf/xps/xps_doc.c \
	mupdf/xps/xps_glyphs.c \
	mupdf/xps/xps_gradient.c \
	mupdf/xps/xps_image.c \
	mupdf/xps/xps_outline.c \
	mupdf/xps/xps_path.c \
	mupdf/xps/xps_resource.c \
	mupdf/xps/xps_tile.c \
	mupdf/xps/xps_util.c \
	mupdf/xps/xps_xml.c \
	mupdf/xps/xps_zip.c \
	mupdf/draw/draw_affine.c \
	mupdf/draw/draw_blend.c \
	mupdf/draw/draw_device.c \
	mupdf/draw/draw_edge.c \
	mupdf/draw/draw_glyph.c \
	mupdf/draw/draw_mesh.c \
	mupdf/draw/draw_paint.c \
	mupdf/draw/draw_path.c \
	mupdf/draw/draw_simple_scale.c \
	mupdf/draw/draw_unpack.c \
	mupdf/fitz/base_context.c \
	mupdf/fitz/base_error.c \
	mupdf/fitz/base_hash.c \
	mupdf/fitz/base_memory.c \
	mupdf/fitz/base_string.c \
	mupdf/fitz/base_geometry.c \
	mupdf/fitz/base_trans.c \
	mupdf/fitz/crypt_aes.c \
	mupdf/fitz/crypt_arc4.c \
	mupdf/fitz/crypt_md5.c \
	mupdf/fitz/crypt_sha2.c \
	mupdf/fitz/doc_document.c \
	mupdf/fitz/doc_outline.c \
	mupdf/fitz/doc_link.c \
	mupdf/fitz/stm_buffer.c \
	mupdf/fitz/stm_comp_buf.c \
	mupdf/fitz/stm_open.c \
	mupdf/fitz/stm_read.c \
	mupdf/fitz/filt_basic.c \
	mupdf/fitz/filt_dctd.c \
	mupdf/fitz/filt_faxd.c \
	mupdf/fitz/filt_flate.c \
	mupdf/fitz/filt_lzwd.c \
	mupdf/fitz/filt_predict.c \
	mupdf/fitz/filt_jbig2d.c \
	mupdf/fitz/image_png.c \
	mupdf/fitz/image_save.c \
	mupdf/fitz/image_md5.c \
	mupdf/fitz/image_jpeg.c \
	mupdf/fitz/image_jpx.c \
	mupdf/fitz/image_tiff.c \
	mupdf/fitz/res_bitmap.c \
	mupdf/fitz/res_colorspace.c \
	mupdf/fitz/res_font.c \
	mupdf/fitz/res_halftone.c \
	mupdf/fitz/res_shade.c \
	mupdf/fitz/res_store.c \
	mupdf/fitz/res_pixmap.c \
	mupdf/fitz/res_text.c \
	mupdf/fitz/res_path.c \
	mupdf/fitz/dev_list.c \
	mupdf/fitz/dev_null.c \
	mupdf/fitz/dev_text.c \
	mupdf/fitz/dev_bbox.c

# omit this when building w/o CJK support:
#	fonts/font_cjk.c
# but note that this also breaks some CMaps, giving
# unexpected results even with files that have all fonts
# embedded and are just assuming that external CMaps are present

# uses libz, which is officially supported for NDK API
MY_MUPDF_LDLIBS := -lz

ifeq ($(TARGET_ARCH_ABI),armeabi)
    LOCAL_ARM_MODE := arm
endif # TARGET_ARCH_ABI == armeabi

LOCAL_CFLAGS := \
	$(MY_FREETYPE_CFLAGS) \
	$(MY_JBIG2DEC_CFLAGS) \
	$(MY_MUPDF_CFLAGS) 
LOCAL_C_INCLUDES := \
	$(MY_FREETYPE_C_INCLUDES) \
	$(MY_MUPDF_C_INCLUDES)
LOCAL_LDLIBS := \
	$(MY_FREETYPE_LDLIBS) \
	$(MY_MUPDF_LDLIBS)
LOCAL_SRC_FILES := \
	$(MY_FREETYPE_SRC_FILES) \
	$(MY_OPENJPEG_SRC_FILES) \
	$(MY_JBIG2DEC_SRC_FILES) \
	$(MY_MUPDF_SRC_FILES)

include $(BUILD_STATIC_LIBRARY)
