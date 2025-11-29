package com.fcl.pdfium_wrapper;

public class NativeLib {

    // Used to load the 'pdfium_wrapper' library on application startup.
    static {
        System.loadLibrary("pdfium_wrapper");
    }

    /**
     * A native method that is implemented by the 'pdfium_wrapper' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}