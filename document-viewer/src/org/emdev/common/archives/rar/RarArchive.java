//package org.emdev.common.archives.rar;
//
//
//import java.io.BufferedInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.IOException;
//import java.util.Collections;
//import java.util.Enumeration;
//import java.util.HashSet;
//import java.util.LinkedList;
//import java.util.List;
//import java.util.Set;
//
//import org.emdev.common.archives.ArchiveFile;
//import org.emdev.common.log.LogContext;
//import org.emdev.common.log.LogManager;
//import org.emdev.utils.FileUtils;
//import org.emdev.utils.bytes.ByteString;
//
//public class RarArchive implements ArchiveFile<RarArchiveEntry> {
//
//    private static final LogContext LCTX = LogManager.root().lctx("Unrar");
//
//    final File rarfile;
//    final List<RarArchiveEntry> entries = new LinkedList<RarArchiveEntry>();
//    final boolean decodeInFile;
//
//    /**
//     * Constructor.
//     * 
//     * @param file
//     *            archive file
//     * @throws IOException
//     *             thrown on error
//     */
//    public RarArchive(final File file, boolean decodeInFile) throws IOException {
//        this.rarfile = file;
//        this.decodeInFile = decodeInFile;
//
//        final Set<ByteString> dirs = new HashSet<ByteString>();
//
//        final Process process = UnrarBridge.exec("vb", rarfile.getAbsolutePath());
//
//        final ByteArrayOutputStream buf = new ByteArrayOutputStream();
//
//        FileUtils.copy(new BufferedInputStream(process.getInputStream(), 8 * 1024), buf);
//
//        final ByteString bs = new ByteString(buf.toByteArray());
//        final List<ByteString> entries = bs.split('\n');
//
//        LCTX.i("List archive entries for: " + rarfile.getAbsolutePath());
//
//        for (final ByteString s : entries) {
//            if (dirs.contains(s)) {
//                LCTX.i("Dir: " + s);
//                continue;
//            }
//            LCTX.i("Entry: " + s);
//
//            final LinkedList<ByteString> segments = s.split('/');
//            final ByteString name = segments.getLast();
//            this.entries.add(new RarArchiveEntry(this, s, name));
//
//            if (segments.size() > 1) {
//                int count = 0;
//                int length = 0;
//                final int nameIndex = segments.size() - 1;
//                for (final ByteString b : segments) {
//                    if (count++ >= nameIndex) {
//                        break;
//                    }
//                    final int newlength = length + b.length();
//                    final ByteString dir = new ByteString(s, 0, newlength);
//                    dirs.add(dir);
//                    length = newlength + 1;
//                }
//            }
//        }
//        try {
//            process.waitFor();
//        } catch (final InterruptedException ex) {
//            Thread.interrupted();
//            throw new IOException(ex.getMessage());
//        }
//    }
//
//    @Override
//    protected void finalize() throws Throwable {
//        super.finalize();
//    }
//
//    /**
//     * {@inheritDoc}
//     * 
//     * @see org.emdev.common.archives.ArchiveFile#randomAccessAllowed()
//     */
//    @Override
//    public boolean randomAccessAllowed() {
//        return true;
//    }
//
//    /**
//     * {@inheritDoc}
//     * 
//     * @see org.emdev.common.archives.ArchiveFile#entries()
//     */
//    @Override
//    public Enumeration<RarArchiveEntry> entries() {
//        return Collections.enumeration(entries);
//    }
//
//    @Override
//    public void close() throws IOException {
//    }
//}
