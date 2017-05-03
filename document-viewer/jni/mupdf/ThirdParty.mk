LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

MY_ROOT := $(LOCAL_PATH)/mupdf

LOCAL_C_INCLUDES := \
	$(MY_ROOT)/include/ \
	$(MY_ROOT)/thirdparty/harfbuzz/src \
	$(MY_ROOT)/thirdparty/jbig2dec \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2 \
	$(MY_ROOT)/thirdparty/libjpeg \
	$(MY_ROOT)/thirdparty/mujs \
	$(MY_ROOT)/thirdparty/zlib \
	$(MY_ROOT)/thirdparty/freetype/include \
	$(MY_ROOT)/scripts/freetype \
	$(MY_ROOT)/scripts/libjpeg

LOCAL_CFLAGS := \
	-DFT2_BUILD_LIBRARY -DDARWIN_NO_CARBON -DHAVE_STDINT_H \
	-DOPJ_HAVE_STDINT_H -DOPJ_HAVE_INTTYPES_H -DUSE_JPIP \
	'-DFT_CONFIG_MODULES_H="slimftmodules.h"' \
	'-DFT_CONFIG_OPTIONS_H="slimftoptions.h"' \
	-Dhb_malloc_impl=hb_malloc -Dhb_calloc_impl=hb_calloc \
	-Dhb_realloc_impl=hb_realloc -Dhb_free_impl=hb_free \
	-DHAVE_OT -DHAVE_UCDN -DHB_NO_MT
ifdef NDK_PROFILER
LOCAL_CFLAGS += -pg -DNDK_PROFILER -O2
endif
ifdef MEMENTO
LOCAL_CFLAGS += -DMEMENTO -DMEMENTO_LEAKONLY
endif

LOCAL_CPP_EXTENSION := .cc


MY_ROOT := mupdf

LOCAL_MODULE := mupdfthirdparty
LOCAL_SRC_FILES := \
	$(MY_ROOT)/thirdparty/mujs/one.c \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-blob.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-buffer.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-buffer-serialize.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-common.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-face.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-fallback-shape.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-font.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ft.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-font.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-layout.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-map.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-shape-complex-arabic.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-shape-complex-default.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-shape-complex-hangul.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-shape-complex-hebrew.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-shape-complex-indic-table.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-shape-complex-indic.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-shape-complex-myanmar.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-shape-complex-thai.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-shape-complex-tibetan.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-shape-complex-use-table.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-shape-complex-use.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-shape-fallback.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-shape-normalize.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-shape.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ot-tag.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-set.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-shape-plan.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-shape.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-shaper.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-ucdn.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-unicode.cc \
	$(MY_ROOT)/thirdparty/harfbuzz/src/hb-warning.cc \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2.c \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2_arith.c \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2_arith_iaid.c \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2_arith_int.c \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2_generic.c \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2_halftone.c \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2_huffman.c \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2_image.c \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2_metadata.c \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2_mmr.c \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2_page.c \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2_refinement.c \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2_segment.c \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2_symbol_dict.c \
	$(MY_ROOT)/thirdparty/jbig2dec/jbig2_text.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/bio.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/cidx_manager.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/cio.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/dwt.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/event.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/function_list.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/image.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/invert.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/j2k.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/jp2.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/mct.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/mqc.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/openjpeg.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/phix_manager.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/pi.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/ppix_manager.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/raw.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/t1.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/t2.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/tcd.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/tgt.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/thix_manager.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/thread.c \
	$(MY_ROOT)/thirdparty/openjpeg/src/lib/openjp2/tpix_manager.c \
	$(MY_ROOT)/thirdparty/libjpeg/jaricom.c \
	$(MY_ROOT)/thirdparty/libjpeg/jcomapi.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdapimin.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdapistd.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdarith.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdatadst.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdatasrc.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdcoefct.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdcolor.c \
	$(MY_ROOT)/thirdparty/libjpeg/jddctmgr.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdhuff.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdinput.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdmainct.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdmarker.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdmaster.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdmerge.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdpostct.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdsample.c \
	$(MY_ROOT)/thirdparty/libjpeg/jdtrans.c \
	$(MY_ROOT)/thirdparty/libjpeg/jerror.c \
	$(MY_ROOT)/thirdparty/libjpeg/jfdctflt.c \
	$(MY_ROOT)/thirdparty/libjpeg/jfdctfst.c \
	$(MY_ROOT)/thirdparty/libjpeg/jfdctint.c \
	$(MY_ROOT)/thirdparty/libjpeg/jidctflt.c \
	$(MY_ROOT)/thirdparty/libjpeg/jidctfst.c \
	$(MY_ROOT)/thirdparty/libjpeg/jidctint.c \
	$(MY_ROOT)/thirdparty/libjpeg/jmemmgr.c \
	$(MY_ROOT)/thirdparty/libjpeg/jquant1.c \
	$(MY_ROOT)/thirdparty/libjpeg/jquant2.c \
	$(MY_ROOT)/thirdparty/libjpeg/jutils.c \
	$(MY_ROOT)/thirdparty/zlib/adler32.c \
	$(MY_ROOT)/thirdparty/zlib/compress.c \
	$(MY_ROOT)/thirdparty/zlib/crc32.c \
	$(MY_ROOT)/thirdparty/zlib/deflate.c \
	$(MY_ROOT)/thirdparty/zlib/inffast.c \
	$(MY_ROOT)/thirdparty/zlib/inflate.c \
	$(MY_ROOT)/thirdparty/zlib/inftrees.c \
	$(MY_ROOT)/thirdparty/zlib/trees.c \
	$(MY_ROOT)/thirdparty/zlib/uncompr.c \
	$(MY_ROOT)/thirdparty/zlib/zutil.c \
	$(MY_ROOT)/thirdparty/zlib/gzlib.c \
	$(MY_ROOT)/thirdparty/zlib/gzwrite.c \
	$(MY_ROOT)/thirdparty/zlib/gzclose.c \
	$(MY_ROOT)/thirdparty/zlib/gzread.c \
	$(MY_ROOT)/thirdparty/freetype/src/base/ftbase.c \
	$(MY_ROOT)/thirdparty/freetype/src/base/ftbbox.c \
	$(MY_ROOT)/thirdparty/freetype/src/base/ftbitmap.c \
	$(MY_ROOT)/thirdparty/freetype/src/base/ftfntfmt.c \
	$(MY_ROOT)/thirdparty/freetype/src/base/ftgasp.c \
	$(MY_ROOT)/thirdparty/freetype/src/base/ftglyph.c \
	$(MY_ROOT)/thirdparty/freetype/src/base/ftinit.c \
	$(MY_ROOT)/thirdparty/freetype/src/base/ftstroke.c \
	$(MY_ROOT)/thirdparty/freetype/src/base/ftsynth.c \
	$(MY_ROOT)/thirdparty/freetype/src/base/ftsystem.c \
	$(MY_ROOT)/thirdparty/freetype/src/base/fttype1.c \
	$(MY_ROOT)/thirdparty/freetype/src/cff/cff.c \
	$(MY_ROOT)/thirdparty/freetype/src/cid/type1cid.c \
	$(MY_ROOT)/thirdparty/freetype/src/psaux/psaux.c \
	$(MY_ROOT)/thirdparty/freetype/src/pshinter/pshinter.c \
	$(MY_ROOT)/thirdparty/freetype/src/psnames/psnames.c \
	$(MY_ROOT)/thirdparty/freetype/src/raster/raster.c \
	$(MY_ROOT)/thirdparty/freetype/src/smooth/smooth.c \
	$(MY_ROOT)/thirdparty/freetype/src/sfnt/sfnt.c \
	$(MY_ROOT)/thirdparty/freetype/src/truetype/truetype.c \
	$(MY_ROOT)/thirdparty/freetype/src/type1/type1.c

#LOCAL_SRC_FILES := $(addprefix ../, $(LOCAL_SRC_FILES))

include $(BUILD_STATIC_LIBRARY)
