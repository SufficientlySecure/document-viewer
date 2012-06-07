package org.ebookdroid.opds.model;

public enum LinkKind {

    FACET_FEED {

        @Override
        public boolean accept(final String rel, final String type) {
            return type != null && type.contains("profile=opds-catalog")
                    && "http://www.feedbooks.com/opds/facet".equals(rel);
        }

    },
    NEXT_FEED {

        @Override
        public boolean accept(final String rel, final String type) {
            return type != null && type.contains("profile=opds-catalog") && "next".equals(rel);
        }

    },

    FEED {

        @Override
        public boolean accept(final String rel, final String type) {
            return type != null && type.contains("profile=opds-catalog");
        }
    },

    BOOK_DOWNLOAD {

        @Override
        public boolean accept(final String rel, final String type) {
            return rel != null
                    && (rel.equals("http://opds-spec.org/acquisition") || rel
                            .equals("http://opds-spec.org/acquisition/open-access"));
        }
    },

    BOOK_THUMBNAIL {

        @Override
        public boolean accept(final String rel, final String type) {
            return rel != null
                    && (rel.equals("http://opds-spec.org/thumbnail") || rel
                            .equals("http://opds-spec.org/image/thumbnail"));
        }
    },

    UNKNOWN;

    public boolean accept(final String rel, final String type) {
        return true;
    }

    public static LinkKind valueOf(final String rel, final String type) {
        for (final LinkKind k : values()) {
            if (k.accept(rel, type)) {
                return k;
            }
        }
        return UNKNOWN;
    }

    public static Link create(final String ref, final String rel, final String type) {
        for (final LinkKind k : values()) {
            if (k.accept(rel, type)) {
                return new Link(k, ref, rel, type);
            }
        }
        return new Link(UNKNOWN, ref, rel, type);
    }
}
