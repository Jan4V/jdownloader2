package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

public class MpegAudioStream extends InternalAudioStream {

    protected MpegAudioStream(String contentType) {
        super(contentType == null ? "audio/mpeg" : contentType);
    }

    protected int[] mpegVersions;

    public int[] getMpegVersions() {
        return mpegVersions;
    }

    public void setLayers(int[] layers) {
        this.layers = layers;

    }

    public int[] getMpegAudioVersions() {
        return mpegAudioVersions;
    }

    public int[] getLayers() {
        return layers;
    }

    protected int[] mpegAudioVersions;
    protected int[] layers;

}
