/*
 * The MIT License Copyright (c) 2014 Krayushkin Konstantin (jangokvk@gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ru.jango.j0widget.camera.fragment;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.hardware.Camera;

import java.net.URI;

import ru.jango.j0widget.camera.BitmapProcessor;
import ru.jango.j0widget.camera.BitmapProcessor.BitmapProcessorListener;

/**
 * Special camera fragment, that also applies some asynchronous checks and changes after taking
 * pictures. Fragment could be used, if made photo basically will be used in interface and doesn't
 * required to be extremely huge (that is, less than 2048x2048 px).
 */
public class SimpleCameraFragment extends AbstractCameraFragment implements BitmapProcessorListener {

    public static final int DEFAULT_MAX_CACHE_SIZE = 5;

    protected CameraFragmentListener cameraListener;
    private Point thumbnailSize;

    public SimpleCameraFragment() {
        thumbnailSize = null;
    }

    ///////////////////////////////////////////////////////////////
    //
    // 					Getters and setters
    //
    ///////////////////////////////////////////////////////////////

    public void setCameraFragmentListener(CameraFragmentListener listener) {
        this.cameraListener = listener;
    }

    public CameraFragmentListener getCameraFragmentListener() {
        return cameraListener;
    }

    /**
     * After taking photo, it could be automatically thumbnailed and then passed into
     * {@link SimpleCameraFragment.CameraFragmentListener#onProcessingFinished(java.net.URI, byte[], android.graphics.Bitmap)}.
     * If thumbnail size is not specified - no thumbnail would be created.
     */
    public Point getThumbnailSize() {
        return thumbnailSize;
    }

    /**
     * After taking photo, it could be automatically thumbnailed and then passed into
     * {@link SimpleCameraFragment.CameraFragmentListener#onProcessingFinished(java.net.URI, byte[], android.graphics.Bitmap)}.
     * If thumbnail size is not specified - no thumbnail would be created.
     */
    public void setThumbnailSize(Point size) {
        this.thumbnailSize = size;
    }

    ///////////////////////////////////////////////////////////////
    //
    //						Camera staff
    //
    ///////////////////////////////////////////////////////////////

    private void processBitmap(URI dataID, byte[] data) {
        if (dataID == null || data == null)
            return;

        final BitmapProcessor bmpProc = new BitmapProcessor(data, dataID, this);
        bmpProc.setPictureRotation(getRotation());
        bmpProc.setPictureSize(getPictureSize());
        bmpProc.setThumbnailSize(thumbnailSize);

        new Thread(bmpProc).start();
    }

    @Override
    public void onPictureTaken(byte[] data, final Camera camera) {
        if (cameraListener != null)
            processBitmap(cameraListener.onPictureTaken(), data);

        restartPreview();
    }

    @Override
    public void onProcessingFinished(URI dataID, byte[] data, Bitmap thumbnail) {
        if (cameraListener != null)
            cameraListener.onProcessingFinished(dataID, data, thumbnail);
    }

    @Override
    public void onProcessingFailed(URI dataID, Exception e) {
        if (cameraListener != null)
            cameraListener.onProcessingFailed(dataID, e);
    }

    public interface CameraFragmentListener {

        /**
         * Called when Camera API has finished it's work. That is, photo has been created,
         * but has not been passed for processing into {@link ru.jango.j0widget.camera.BitmapProcessor}.
         *
         * @return  {@link java.net.URI}, that would be treated as photo ID in future - it would be
         * passed into {@link #onProcessingFinished(java.net.URI, byte[], android.graphics.Bitmap)} and
         * {@link #onProcessingFailed(java.net.URI, Exception)}.
         */
        public URI onPictureTaken();

        /**
         * Called when processing was successfully finished.
         *
         * @param dataID        {@link java.net.URI} aka photo ID; this object was previously returned
         *                      from {@link #onPictureTaken()}
         * @param data          transformed photo as byte array
         * @param thumbnail     small thumbnail of the photo, that could be actually shown on the screen
         *
         * @see #setThumbnailSize(android.graphics.Point)
         */
        public void onProcessingFinished(URI dataID, byte[] data, Bitmap thumbnail);

        /**
         * Called when processing was stopped due to some error.
         *
         * @param dataID    {@link java.net.URI} aka photo ID; this object was previously returned
         *                   from {@link #onPictureTaken()}
         * @param e         fail reason
         */
        public void onProcessingFailed(URI dataID, Exception e);
    }
}
