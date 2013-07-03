package org.emdev.common.filesystem;

public class ExternalMedia {

    public final String path;
    public MediaState state;

    public ExternalMedia(String path, MediaState state) {
        this.path = path;
        this.state = state;
    }

    @Override
    public String toString() {
        return path + " " + state;
    }
}
