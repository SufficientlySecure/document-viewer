LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := mupdfthirdparty
LOCAL_SRC_FILES := \
	mupdf/thirdparty/mujs/one.c \
	mupdf/thirdparty/jbig2dec/jbig2.c \
	mupdf/thirdparty/jbig2dec/jbig2_arith.c \
	mupdf/thirdparty/jbig2dec/jbig2_arith_iaid.c \
	mupdf/thirdparty/jbig2dec/jbig2_arith_int.c \
	mupdf/thirdparty/jbig2dec/jbig2_generic.c \
	mupdf/thirdparty/jbig2dec/jbig2_halftone.c \
	mupdf/thirdparty/jbig2dec/jbig2_huffman.c \
	mupdf/thirdparty/jbig2dec/jbig2_image.c \
	mupdf/thirdparty/jbig2dec/jbig2_metadata.c \
	mupdf/thirdparty/jbig2dec/jbig2_mmr.c \
	mupdf/thirdparty/jbig2dec/jbig2_page.c \
	mupdf/thirdparty/jbig2dec/jbig2_refinement.c \
	mupdf/thirdparty/jbig2dec/jbig2_segment.c \
	mupdf/thirdparty/jbig2dec/jbig2_symbol_dict.c \
	mupdf/thirdparty/jbig2dec/jbig2_text.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/bio.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/cidx_manager.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/cio.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/dwt.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/event.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/function_list.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/image.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/invert.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/j2k.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/jp2.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/mct.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/mqc.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/openjpeg.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/opj_clock.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/phix_manager.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/pi.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/ppix_manager.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/raw.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/t1.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/t1_generate_luts.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/t2.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/tcd.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/tgt.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/thix_manager.c \
	mupdf/thirdparty/openjpeg/libopenjpeg/tpix_manager.c \
	mupdf/thirdparty/jpeg/jaricom.c \
	mupdf/thirdparty/jpeg/jcomapi.c \
	mupdf/thirdparty/jpeg/jdapimin.c \
	mupdf/thirdparty/jpeg/jdapistd.c \
	mupdf/thirdparty/jpeg/jdarith.c \
	mupdf/thirdparty/jpeg/jdatadst.c \
	mupdf/thirdparty/jpeg/jdatasrc.c \
	mupdf/thirdparty/jpeg/jdcoefct.c \
	mupdf/thirdparty/jpeg/jdcolor.c \
	mupdf/thirdparty/jpeg/jddctmgr.c \
	mupdf/thirdparty/jpeg/jdhuff.c \
	mupdf/thirdparty/jpeg/jdinput.c \
	mupdf/thirdparty/jpeg/jdmainct.c \
	mupdf/thirdparty/jpeg/jdmarker.c \
	mupdf/thirdparty/jpeg/jdmaster.c \
	mupdf/thirdparty/jpeg/jdmerge.c \
	mupdf/thirdparty/jpeg/jdpostct.c \
	mupdf/thirdparty/jpeg/jdsample.c \
	mupdf/thirdparty/jpeg/jdtrans.c \
	mupdf/thirdparty/jpeg/jerror.c \
	mupdf/thirdparty/jpeg/jfdctflt.c \
	mupdf/thirdparty/jpeg/jfdctfst.c \
	mupdf/thirdparty/jpeg/jfdctint.c \
	mupdf/thirdparty/jpeg/jidctflt.c \
	mupdf/thirdparty/jpeg/jidctfst.c \
	mupdf/thirdparty/jpeg/jidctint.c \
	mupdf/thirdparty/jpeg/jmemmgr.c \
	mupdf/thirdparty/jpeg/jquant1.c \
	mupdf/thirdparty/jpeg/jquant2.c \
	mupdf/thirdparty/jpeg/jutils.c \
	mupdf/thirdparty/zlib/adler32.c \
	mupdf/thirdparty/zlib/compress.c \
	mupdf/thirdparty/zlib/crc32.c \
	mupdf/thirdparty/zlib/deflate.c \
	mupdf/thirdparty/zlib/inffast.c \
	mupdf/thirdparty/zlib/inflate.c \
	mupdf/thirdparty/zlib/inftrees.c \
	mupdf/thirdparty/zlib/trees.c \
	mupdf/thirdparty/zlib/uncompr.c \
	mupdf/thirdparty/zlib/zutil.c \
	mupdf/thirdparty/freetype/src/base/ftbase.c \
	mupdf/thirdparty/freetype/src/base/ftbbox.c \
	mupdf/thirdparty/freetype/src/base/ftbitmap.c \
	mupdf/thirdparty/freetype/src/base/ftgasp.c \
	mupdf/thirdparty/freetype/src/base/ftglyph.c \
	mupdf/thirdparty/freetype/src/base/ftinit.c \
	mupdf/thirdparty/freetype/src/base/ftstroke.c \
	mupdf/thirdparty/freetype/src/base/ftsynth.c \
	mupdf/thirdparty/freetype/src/base/ftsystem.c \
	mupdf/thirdparty/freetype/src/base/fttype1.c \
	mupdf/thirdparty/freetype/src/base/ftxf86.c \
	mupdf/thirdparty/freetype/src/cff/cff.c \
	mupdf/thirdparty/freetype/src/cid/type1cid.c \
	mupdf/thirdparty/freetype/src/psaux/psaux.c \
	mupdf/thirdparty/freetype/src/pshinter/pshinter.c \
	mupdf/thirdparty/freetype/src/psnames/psnames.c \
	mupdf/thirdparty/freetype/src/raster/raster.c \
	mupdf/thirdparty/freetype/src/smooth/smooth.c \
	mupdf/thirdparty/freetype/src/sfnt/sfnt.c \
	mupdf/thirdparty/freetype/src/truetype/truetype.c \
	mupdf/thirdparty/freetype/src/type1/type1.c

LOCAL_CFLAGS := \
	-DFT2_BUILD_LIBRARY -DDARWIN_NO_CARBON -DHAVE_STDINT_H \
	-DOPJ_HAVE_STDINT_H \
	'-DFT_CONFIG_MODULES_H="slimftmodules.h"' \
	'-DFT_CONFIG_OPTIONS_H="slimftoptions.h"'
	
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/mupdf/thirdparty/jbig2dec \
	$(LOCAL_PATH)/mupdf/thirdparty/openjpeg/libopenjpeg \
	$(LOCAL_PATH)/mupdf/thirdparty/jpeg \
	$(LOCAL_PATH)/mupdf/thirdparty/mujs \
	$(LOCAL_PATH)/mupdf/thirdparty/zlib \
	$(LOCAL_PATH)/mupdf/thirdparty/freetype/include \
	$(LOCAL_PATH)/mupdf/thirdparty/freetype/include/freetype \
	$(LOCAL_PATH)/mupdf/scripts/freetype \
	$(LOCAL_PATH)/mupdf/scripts/jpeg \
	$(LOCAL_PATH)/mupdf/scripts/openjpeg

#LOCAL_SRC_FILES := $(addprefix ../, $(LOCAL_SRC_FILES))

include $(BUILD_STATIC_LIBRARY)
