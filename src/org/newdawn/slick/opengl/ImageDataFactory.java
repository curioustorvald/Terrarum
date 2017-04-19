package org.newdawn.slick.opengl;

/**
 * Modifications: Added support for .tga.gz
 *
 * Created by SKYHi14 on 2017-04-19.
 */

import net.torvald.slick.opengl.TGAGzImageData;
import org.newdawn.slick.util.Log;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A static utility to create the appropriate image data for a particular reference.
 *
 * @author kevin
 */
public class ImageDataFactory {
    /** True if we're going to use the native PNG loader - cached so it doesn't have
     *  the security check repeatedly
     */
    private static final boolean usePngLoader = false;
    /** True if the PNG loader property has been checked */
    private static boolean pngLoaderPropertyChecked = false;

    /** The name of the PNG loader configuration property */
    private static final String PNG_LOADER = "org.newdawn.slick.pngloader";

    /**
     * Check PNG loader property. If set the native PNG loader will
     * not be used.
     */
    private static void checkProperty() {
        if (!pngLoaderPropertyChecked) {
            pngLoaderPropertyChecked = true;

            try {
                AccessController.doPrivileged(new PrivilegedAction() {
                    public Object run() {
                        String val = System.getProperty(PNG_LOADER);
                        if ("false".equalsIgnoreCase(val)) {
                            //usePngLoader = false;
                        }

                        Log.info("Use Java PNG Loader = " + usePngLoader);
                        return null;
                    }
                });
            } catch (Throwable e) {
                // ignore, security failure - probably an applet
            }
        }
    }

    /**
     * Create an image data that is appropriate for the reference supplied
     *
     * @param ref The reference to the image to retrieve
     * @return The image data that can be used to retrieve the data for that resource
     */
    public static LoadableImageData getImageDataFor(String ref) {
        LoadableImageData imageData;
        checkProperty();

        ref = ref.toLowerCase();

        if (ref.endsWith(".tga")) {
            return new TGAImageData();
        }
        if (ref.endsWith(".tga.gz")) {
            return new TGAGzImageData();
        }
        if (ref.endsWith(".png")) {
            CompositeImageData data = new CompositeImageData();
            if (usePngLoader) {
                data.add(new PNGImageData());
            }
            data.add(new ImageIOImageData());

            return data;
        }

        return new ImageIOImageData();
    }
}
