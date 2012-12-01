package org.ebookdroid.common.cache;

import org.ebookdroid.core.Page;
import org.ebookdroid.core.codec.CodecPageInfo;

import android.graphics.RectF;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.emdev.common.log.LogContext;
import org.emdev.utils.collections.SparseArrayEx;

public class DocumentCacheFile extends File {

    private static final int TAG_PAGE_COUNTS = 0;
    private static final int TAG_CODEC_PAGE_INFO = 1;
    private static final int TAG_AUTO_CROPPING = 2;
    private static final int TAG_MANUAL_CROPPING = 3;

    private static final long serialVersionUID = 6836895806027391288L;

    private static final LogContext LCTX = CacheManager.LCTX;

    DocumentCacheFile(final File dir, final String name) {
        super(dir, name);
    }

    public DocumentInfo load() {
        try {
            LCTX.d("Loading document info...");
            final DocumentInfo info = new DocumentInfo();
            final DataInputStream in = new DataInputStream(new FileInputStream(this));
            try {
                while (true) {
                    byte tag = -1;
                    try {
                        tag = in.readByte();
                    } catch (EOFException ex) {
                        return info;
                    }
                    final byte id = (byte) (tag & 0x3F);
                    final boolean docPage = (tag & 0x80) == 0;
                    final boolean leftPage = (tag & 0x40) == 0;

                    switch (id) {
                        case TAG_PAGE_COUNTS:
                            // Number of pages
                            info.loadPageCounts(in);
                            break;
                        case TAG_CODEC_PAGE_INFO:
                            // CodecPageInfo - only for docs
                            info.loadCodePageInfo(in);
                            break;
                        case TAG_AUTO_CROPPING:
                            // Auto cropping
                            info.loadAutoCropping(in, docPage, leftPage);
                            break;
                        case TAG_MANUAL_CROPPING:
                            // Manual cropping
                            info.loadManualCropping(in, docPage, leftPage);
                            break;
                    }
                }
            } catch (final EOFException ex) {
                LCTX.e("Loading document info failed: " + ex.getMessage());
            } catch (final IOException ex) {
                LCTX.e("Loading document info failed: " + ex.getMessage());
            } finally {
                try {
                    in.close();
                } catch (final IOException ex) {
                }
            }
        } catch (final FileNotFoundException ex) {
            LCTX.e("Loading document info failed: " + ex.getMessage());
        }
        return null;
    }

    public void save(final DocumentInfo info) {
        try {
            LCTX.d("Saving document info...");
            final DataOutputStream out = new DataOutputStream(new FileOutputStream(this));
            try {
                info.savePageCounts(out);
                info.saveCodePageInfo(out);
                info.saveAutoCropping(out);
                info.saveManualCropping(out);

                LCTX.d("Saving document info finished");
            } catch (final IOException ex) {
                LCTX.e("Saving document info failed: " + ex.getMessage());
            } finally {
                try {
                    out.close();
                } catch (final IOException ex) {
                }
            }
        } catch (final IOException ex) {
            LCTX.e("Saving document info failed: " + ex.getMessage());
        }
    }

    public static class DocumentInfo {

        public int docPageCount;
        public int viewPageCount;

        public final SparseArrayEx<PageInfo> docPages = new SparseArrayEx<PageInfo>();
        public final SparseArrayEx<PageInfo> leftPages = new SparseArrayEx<PageInfo>();
        public final SparseArrayEx<PageInfo> rightPages = new SparseArrayEx<PageInfo>();

        void loadPageCounts(final DataInputStream in) throws IOException {
            this.docPageCount = in.readShort();
            this.viewPageCount = in.readShort();
        }

        void savePageCounts(final DataOutputStream out) throws IOException {
            out.writeByte(TAG_PAGE_COUNTS);
            out.writeShort(this.docPageCount);
            out.writeShort(this.viewPageCount);
        }

        void loadCodePageInfo(final DataInputStream in) throws IOException {
            final int index = in.readShort();
            PageInfo pageInfo = this.docPages.get(index, null);
            if (pageInfo == null) {
                pageInfo = new PageInfo(index);
                this.docPages.append(index, pageInfo);
            }
            pageInfo.info = new CodecPageInfo(in.readInt(), in.readInt());
        }

        void saveCodePageInfo(final DataOutputStream out) throws IOException {
            for (final PageInfo info : this.docPages) {
                if (info.info != null) {
                    out.writeByte(TAG_CODEC_PAGE_INFO);
                    out.writeShort(info.index);
                    out.writeInt(info.info.width);
                    out.writeInt(info.info.height);
                }
            }
        }

        void loadAutoCropping(final DataInputStream in, final boolean docPage, final boolean leftPage)
                throws IOException {
            final int index = in.readShort();
            final SparseArrayEx<PageInfo> target = getPages(docPage, leftPage);
            PageInfo pageInfo = target.get(index, null);
            if (pageInfo == null) {
                pageInfo = new PageInfo(index);
                target.append(index, pageInfo);
            }
            pageInfo.autoCropping = new RectF(in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat());
        }

        void loadManualCropping(final DataInputStream in, final boolean docPage, final boolean leftPage)
                throws IOException {

            final int index = in.readShort();
            final SparseArrayEx<PageInfo> target = getPages(docPage, leftPage);
            PageInfo pageInfo = target.get(index, null);
            if (pageInfo == null) {
                pageInfo = new PageInfo(index);
                target.append(index, pageInfo);
            }
            pageInfo.manualCropping = new RectF(in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat());

        }

        void saveAutoCropping(final DataOutputStream out) throws IOException {
            for (final PageInfo info : this.docPages) {
                final RectF cropping = info.autoCropping;
                if (cropping != null) {
                    saveCropping(out, TAG_AUTO_CROPPING, info.index, cropping);
                }
            }
            for (final PageInfo info : this.leftPages) {
                final RectF cropping = info.autoCropping;
                if (cropping != null) {
                    saveCropping(out, TAG_AUTO_CROPPING | 0x80, info.index, cropping);
                }
            }
            for (final PageInfo info : this.rightPages) {
                final RectF cropping = info.autoCropping;
                if (cropping != null) {
                    saveCropping(out, TAG_AUTO_CROPPING | 0x80 | 0x40, info.index, cropping);
                }
            }
        }

        void saveManualCropping(final DataOutputStream out) throws IOException {
            for (final PageInfo info : this.docPages) {
                final RectF cropping = info.manualCropping;
                if (cropping != null) {
                    saveCropping(out, TAG_MANUAL_CROPPING, info.index, cropping);
                }
            }
            for (final PageInfo info : this.leftPages) {
                final RectF cropping = info.manualCropping;
                if (cropping != null) {
                    saveCropping(out, TAG_MANUAL_CROPPING | 0x80, info.index, cropping);
                }
            }
            for (final PageInfo info : this.rightPages) {
                final RectF cropping = info.manualCropping;
                if (cropping != null) {
                    saveCropping(out, TAG_MANUAL_CROPPING | 0x80 | 0x40, info.index, cropping);
                }
            }
        }

        void saveCropping(final DataOutputStream out, final int tag, final int index, final RectF cropping)
                throws IOException {
            out.writeByte(tag);
            out.writeShort(index);
            out.writeFloat(cropping.left);
            out.writeFloat(cropping.top);
            out.writeFloat(cropping.right);
            out.writeFloat(cropping.bottom);
        }

        SparseArrayEx<PageInfo> getPages(final boolean docPage, final boolean leftPage) {
            return docPage ? this.docPages : leftPage ? this.leftPages : this.rightPages;
        }

        public PageInfo getPageInfo(Page page) {
            SparseArrayEx<PageInfo> arr = null;
            switch (page.type) {
                case FULL_PAGE:
                    arr = docPages;
                    break;
                case LEFT_PAGE:
                    arr = leftPages;
                    break;
                case RIGHT_PAGE:
                    arr = rightPages;
                    break;
            }
            int key = page.index.docIndex;
            PageInfo pi = arr.get(key, null);
            if (pi == null) {
                pi = new PageInfo(key);
                arr.append(key, pi);
            }
            return pi;
        }
    }

    public static class PageInfo {

        public final int index;
        public CodecPageInfo info;
        public RectF autoCropping;
        public RectF manualCropping;

        public PageInfo(final int index) {
            this.index = index;
        }

    }
}
