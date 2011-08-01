package org.ebookdroid.cbdroid.codec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.ebookdroid.core.OutlineLink;
import org.ebookdroid.core.PageLink;
import org.ebookdroid.core.codec.CodecContext;
import org.ebookdroid.core.codec.CodecDocument;
import org.ebookdroid.core.codec.CodecPage;
import org.ebookdroid.core.codec.CodecPageInfo;

import android.util.Log;

import de.innosystec.unrar.Archive;
import de.innosystec.unrar.exception.RarException;
import de.innosystec.unrar.rarfile.FileHeader;

public class CbrDocument implements CodecDocument {

	private Archive rarfile;
	private Map<String, FileHeader> pages = new TreeMap<String, FileHeader>();

	public CbrDocument(String fileName) {
		try {
			rarfile = new Archive(new File(fileName));
			init();
		} catch (IOException e) {
		} catch (RarException e) {
		}
	}

	private void init() {
		if (rarfile != null) {

			if (rarfile.isEncrypted()) {
				Log.d("CbrDocument", "Archive is encrypted. Cannot extract");
				return;
			}
			FileHeader fh = null;
			while (true) {
				fh = rarfile.nextFileHeader();
				if (fh == null) {
					break;
				}
				if (fh.isEncrypted()) {
					Log.d("CbrDocument", "File is encrypted. Cannot extract: " + fh.getFileNameString());
					continue;
				}
				if (!fh.isDirectory() && (fh.getFileNameString().toLowerCase().endsWith(".jpg") || fh.getFileNameString().toLowerCase().endsWith(".png"))) {
					pages.put(fh.getFileNameString().toLowerCase(), fh);
				}
			}
		}
	}

	@Override
	public List<OutlineLink> getOutline() {
		return null;
	}

	@Override
	public CodecPage getPage(int pageNumber) {
		Collection<FileHeader> values = pages.values();
		int index = 0;
		for (FileHeader fh : values) {
			if (index++ == pageNumber) {
				return new CbrPage(rarfile, fh);
			}
		}
		return new CbrPage(null, null);
	}

	@Override
	public int getPageCount() {
		return pages.size();
	}

	@Override
	public CodecPageInfo getPageInfo(int pageIndex, CodecContext codecContext) {
		return null;
	}

	@Override
	public ArrayList<PageLink> getPageLinks(int pageNuber) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void recycle() {
		try {
			rarfile.close();
		} catch (IOException e) {
		}

	}

	public static CodecDocument openDocument(String fileName) {
		return new CbrDocument(fileName);
	}
}
