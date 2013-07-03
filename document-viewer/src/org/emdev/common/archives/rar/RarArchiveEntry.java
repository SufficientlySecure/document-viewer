//package org.emdev.common.archives.rar;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//
//import org.emdev.common.archives.ArchiveEntry;
//import org.emdev.common.cache.CacheManager;
//import org.emdev.utils.bytes.ByteString;
//
//public class RarArchiveEntry implements ArchiveEntry {
//
//    final RarArchive archive;
//    final ByteString path;
//    final ByteString name;
//
//    private File cached;
//
//    RarArchiveEntry(final RarArchive archive, final ByteString path, final ByteString name) {
//        this.archive = archive;
//        this.path = path;
//        this.name = name;
//    }
//
//    /**
//     * {@inheritDoc}
//     * 
//     * @see org.emdev.common.archives.ArchiveEntry#getName()
//     */
//    @Override
//    public String getName() {
//        return name.toString();
//    }
//
//    /**
//     * {@inheritDoc}
//     * 
//     * @see org.emdev.common.archives.ArchiveEntry#isDirectory()
//     */
//    @Override
//    public boolean isDirectory() {
//        return false;
//    }
//
//    /**
//     * {@inheritDoc}
//     * 
//     * @see org.emdev.common.archives.ArchiveEntry#open()
//     */
//    @Override
//    public InputStream open() throws IOException {
//        if (!archive.decodeInFile) {
//            final Process process = startExtracting();
//            return process.getInputStream();
//        }
//
//        if (cached == null || !cached.exists()) {
//            final Process process = startExtracting();
//            cached = CacheManager.createTempFile(process.getInputStream(), ".page");
//        }
//        FileInputStream tempin = new FileInputStream(cached);
//        return tempin;
//    }
//
//    private Process startExtracting() throws IOException {
//        final Process process = UnrarBridge.exec("p", "-inul", "-n@", archive.rarfile.getAbsolutePath());
//        final OutputStream outputStream = process.getOutputStream();
//        path.writeTo(outputStream);
//        outputStream.close();
//        return process;
//    }
//}
