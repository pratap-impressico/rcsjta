/*
 * Copyright 2013, France Telecom
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 *    http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.joyn.media.video;

import java.lang.String;

/**
 * Class VideoRenderer.
 */
public class VideoRenderer extends org.gsma.joyn.media.IMediaRenderer.Stub {
    /**
     * Creates a new instance of VideoRenderer.
     */
    public VideoRenderer() {
        super();
    }

    /**
     * Creates a new instance of VideoRenderer.
     *
     * @param arg1 The arg1 array.
     */
    public VideoRenderer(org.gsma.joyn.media.MediaCodec[] arg1) {
        super();
    }

    public void start() {

    }

    public void stop() {

    }

    public void close() {

    }

    /**
     *
     * @param arg1 The arg1.
     * @param arg2 The arg2.
     */
    public void open(String arg1, int arg2) {

    }

    /**
     *
     * @return  The boolean.
     */
    public boolean isStarted() {
        return false;
    }

    /**
     * Adds a listener.
     *
     * @param arg1 The arg1.
     */
    public void addListener(org.gsma.joyn.media.IMediaEventListener arg1) {

    }

    /**
     * Removes a all listeners.
     */
    public void removeAllListeners() {

    }

    /**
     * Sets the media codec.
     *
     * @param arg1 The media codec.
     */
    public void setMediaCodec(org.gsma.joyn.media.MediaCodec arg1) {

    }

    /**
     * Sets the video surface.
     *
     * @param arg1 The video surface.
     */
    public void setVideoSurface(VideoSurfaceView arg1) {

    }

    /**
     * Returns the video start time.
     *
     * @return  The video start time.
     */
    public long getVideoStartTime() {
        return 0l;
    }

    /**
     * Returns the local rtp port.
     *
     * @return  The local rtp port.
     */
    public int getLocalRtpPort() {
        return 0;
    }

    /**
     *
     * @return  The boolean.
     */
    public boolean isOpened() {
        return false;
    }

    /**
     * Returns the supported media codecs.
     *
     * @return  The supported media codecs array.
     */
    public org.gsma.joyn.media.MediaCodec[] getSupportedMediaCodecs() {
        return (org.gsma.joyn.media.MediaCodec []) null;
    }

    /**
     * Returns the media codec.
     *
     * @return  The media codec.
     */
    public org.gsma.joyn.media.MediaCodec getMediaCodec() {
        return (org.gsma.joyn.media.MediaCodec) null;
    }

} // end VideoRenderer