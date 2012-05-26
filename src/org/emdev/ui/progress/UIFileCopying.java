package org.emdev.ui.progress;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.emdev.utils.FileUtils;
import org.emdev.utils.MathUtils;
import org.emdev.utils.FileUtils.CopingProgress;

public class UIFileCopying implements CopingProgress {

    private final int stringId;
    private final IProgressIndicator delegate;

    private long interval;
    private long contentLength;
    private long copied;
    private long indicated;
    private int bufsize;

    public UIFileCopying(final int stringId, final int bufsize, final IProgressIndicator delegate) {
        this.stringId = stringId;
        this.delegate = delegate;
        this.bufsize = bufsize;
        this.interval = Math.min(64 * 1024, bufsize);
    }

    public void copy(final File source, final File target) throws IOException {
        this.contentLength = source.length();
        this.copied = 0;
        this.indicated = 0;
        this.bufsize = MathUtils.adjust((int) contentLength, 1024, 512 * 1024);

        final BufferedInputStream ins = new BufferedInputStream(new FileInputStream(source), bufsize);
        final BufferedOutputStream outs = new BufferedOutputStream(new FileOutputStream(target), bufsize);
        FileUtils.copy(ins, outs, bufsize, this);

        final String fileSize = FileUtils.getFileSize(contentLength);
        delegate.setProgressDialogMessage(stringId, fileSize, fileSize);
    }

    public void copy(final long contentLength, final InputStream source, final OutputStream target) throws IOException {
        this.contentLength = contentLength;
        this.copied = 0;
        this.indicated = 0;

        FileUtils.copy(source, target, bufsize, this);

        final String fileSize = FileUtils.getFileSize(contentLength);
        delegate.setProgressDialogMessage(stringId, fileSize, fileSize);
    }

    @Override
    public void progress(final long bytes) {
        copied = bytes;
        if (copied - indicated >= interval) {
            indicated = copied;
            final String val1 = FileUtils.getFileSize(Math.min(indicated, contentLength));
            final String val2 = FileUtils.getFileSize(contentLength);
            delegate.setProgressDialogMessage(stringId, val1, val2);
        }

    }
}
