package org.ebookdroid.core;

import org.ebookdroid.common.settings.books.BookSettings;
import org.ebookdroid.core.models.DocumentModel;

public class PageIndex {

    public static final PageIndex NULL = new PageIndex(-1, -1);

    public static final PageIndex FIRST = new PageIndex(0, 0);

    public final int docIndex;
    public final int viewIndex;

    public PageIndex(int docIndex, int viewIndex) {
        this.docIndex = docIndex;
        this.viewIndex = viewIndex;
    }

    public Page getActualPage(DocumentModel dm, BookSettings bs) {
        // If now page splitting is switched off:
        // The document index is valid in all cases
        if (!bs.splitPages) {
            return dm.getPageObject(viewIndex);
        }
        // If now page splitting is switched on and bookmark was created in splitting mode (page.docIndex != page.viewIndex)
        // The view index is valid
        if (docIndex != viewIndex) {
            return dm.getPageObject(viewIndex);
        }

        // If now page splitting is switched on and bookmark was created in non-splitting mode (page.docIndex != page.viewIndex)
        for(Page p : dm.getPages()) {
            if (p.index.docIndex == docIndex) {
                return p;
            }
        }

        return null;
    }


    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + docIndex;
        result = prime * result + viewIndex;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PageIndex other = (PageIndex) obj;
        if (docIndex != other.docIndex)
            return false;
        if (viewIndex != other.viewIndex)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "[" + docIndex + ":" + viewIndex + "]";
    }

}
