# GNU Makefile

OUT := mupdf/build
GEN := mupdf/generated

# --- Variables, Commands, etc... ---

default: all

CFLAGS += -Wall -Imupdf/fitz -Imupdf/pdf -Imupdf/scripts

ifeq "$(verbose)" ""
QUIET_AR = @ echo ' ' ' ' AR $@ ;
QUIET_CC = @ echo ' ' ' ' CC $@ ;
QUIET_GEN = @ echo ' ' ' ' GEN $@ ;
QUIET_LINK = @ echo ' ' ' ' LINK $@ ;
QUIET_MKDIR = @ echo ' ' ' ' MKDIR $@ ;
endif

CC_CMD = $(QUIET_CC) $(CC) $(CFLAGS) -o $@ -c $<
AR_CMD = $(QUIET_AR) $(AR) cru $@ $^
LINK_CMD = $(QUIET_LINK) $(CC) $(LDFLAGS) -o $@ $^ $(LIBS)
MKDIR_CMD = $(QUIET_MKDIR) mkdir -p $@

# --- Rules ---

$(OUT) $(GEN) :
	$(MKDIR_CMD)

$(OUT)/%.a :
	$(AR_CMD)

$(OUT)/% : $(OUT)/%.o
	$(LINK_CMD)
$(OUT)/%.o : mupdf/scripts/%.c | $(OUT)
	$(CC_CMD)

.PRECIOUS : $(OUT)/%.o # Keep intermediates from chained rules

# --- Generated CMAP and FONT files ---

CMAPDUMP := $(OUT)/cmapdump

CMAP_CNS_SRC := $(wildcard mupdf/cmaps/cns/*)
CMAP_GB_SRC := $(wildcard mupdf/cmaps/gb/*)
CMAP_JAPAN_SRC := $(wildcard mupdf/cmaps/japan/*)
CMAP_KOREA_SRC := $(wildcard mupdf/cmaps/korea/*)

$(GEN)/cmap_cns.h : $(CMAP_CNS_SRC)
	$(QUIET_GEN) ./$(CMAPDUMP) $@ $(CMAP_CNS_SRC)
$(GEN)/cmap_gb.h : $(CMAP_GB_SRC)
	$(QUIET_GEN) ./$(CMAPDUMP) $@ $(CMAP_GB_SRC)
$(GEN)/cmap_japan.h : $(CMAP_JAPAN_SRC)
	$(QUIET_GEN) ./$(CMAPDUMP) $@ $(CMAP_JAPAN_SRC)
$(GEN)/cmap_korea.h : $(CMAP_KOREA_SRC)
	$(QUIET_GEN) ./$(CMAPDUMP) $@ $(CMAP_KOREA_SRC)

CMAP_HDR := $(addprefix $(GEN)/, cmap_cns.h cmap_gb.h cmap_japan.h cmap_korea.h)

$(CMAP_HDR) : $(CMAPDUMP) | $(GEN)

$(OUT)/pdf_cmap_table.o : $(CMAP_HDR)
$(OUT)/pdf_fontfile.o : $(FONT_HDR)
$(OUT)/cmapdump.o : mupdf/pdf/pdf_cmap.c mupdf/pdf/pdf_cmap_parse.c



all: $(CMAP_HDR)

clean:
	rm -rf $(OUT) $(GEN)

.PHONY: all clean
