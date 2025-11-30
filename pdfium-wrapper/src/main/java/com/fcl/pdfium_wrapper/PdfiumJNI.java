package com.fcl.pdfium_wrapper;

import java.nio.ByteBuffer;

/**
 * Clase JNI para cargar y visualizar documentos PDF usando PDFium en Android.
 * Esta clase proporciona una interfaz Java para las funciones nativas de PDFium.
 */
public class PdfiumJNI {

    // Cargar la biblioteca nativa
    static {
        System.loadLibrary("pdfium");
        System.loadLibrary("pdfium_wrapper");
    }

    // ========== Constantes de Error ==========
    public static final int FPDF_ERR_SUCCESS = 0;
    public static final int FPDF_ERR_UNKNOWN = 1;
    public static final int FPDF_ERR_FILE = 2;
    public static final int FPDF_ERR_FORMAT = 3;
    public static final int FPDF_ERR_PASSWORD = 4;
    public static final int FPDF_ERR_SECURITY = 5;
    public static final int FPDF_ERR_PAGE = 6;

    // ========== Constantes de Rotación ==========
    public static final int ROTATE_0 = 0;
    public static final int ROTATE_90 = 1;
    public static final int ROTATE_180 = 2;
    public static final int ROTATE_270 = 3;

    // ========== Flags de Renderizado ==========
    public static final int FPDF_ANNOT = 0x01;
    public static final int FPDF_LCD_TEXT = 0x02;
    public static final int FPDF_NO_NATIVETEXT = 0x04;
    public static final int FPDF_GRAYSCALE = 0x08;
    public static final int FPDF_RENDER_LIMITEDIMAGECACHE = 0x200;
    public static final int FPDF_RENDER_FORCEHALFTONE = 0x400;
    public static final int FPDF_PRINTING = 0x800;
    public static final int FPDF_RENDER_NO_SMOOTHTEXT = 0x1000;
    public static final int FPDF_RENDER_NO_SMOOTHIMAGE = 0x2000;
    public static final int FPDF_RENDER_NO_SMOOTHPATH = 0x4000;
    public static final int FPDF_REVERSE_BYTE_ORDER = 0x10;

    // ========== Formatos de Bitmap ==========
    public static final int FPDFBitmap_Unknown = 0;
    public static final int FPDFBitmap_Gray = 1;
    public static final int FPDFBitmap_BGR = 2;
    public static final int FPDFBitmap_BGRx = 3;
    public static final int FPDFBitmap_BGRA = 4;

    // ========== Métodos Nativos - Inicialización ==========

    /**
     * Inicializa la biblioteca PDFium.
     * Debe llamarse antes de usar cualquier otra función.
     */
    public static native void initLibrary();

    /**
     * Libera los recursos globales de la biblioteca PDFium.
     * Debe llamarse al finalizar el uso de la biblioteca.
     */
    public static native void destroyLibrary();

    // ========== Métodos Nativos - Carga de Documentos ==========

    /**
     * Carga un documento PDF desde un archivo.
     *
     * @param filePath Ruta al archivo PDF
     * @param password Contraseña del PDF (puede ser null si no tiene)
     * @return Handle del documento o 0 si falla
     */
    public static native long loadDocument(String filePath, String password);

    /**
     * Carga un documento PDF desde memoria.
     *
     * @param data Buffer con los datos del PDF
     * @param password Contraseña del PDF (puede ser null si no tiene)
     * @return Handle del documento o 0 si falla
     */
    public static native long loadMemDocument(byte[] data, String password);

    /**
     * Carga un documento PDF desde un ByteBuffer.
     *
     * @param buffer ByteBuffer con los datos del PDF
     * @param password Contraseña del PDF (puede ser null si no tiene)
     * @return Handle del documento o 0 si falla
     */
    public static native long loadMemDocument(ByteBuffer buffer, String password);

    /**
     * Cierra un documento PDF y libera sus recursos.
     *
     * @param document Handle del documento
     */
    public static native void closeDocument(long document);

    // ========== Métodos Nativos - Información del Documento ==========

    /**
     * Obtiene el número total de páginas del documento.
     *
     * @param document Handle del documento
     * @return Número de páginas
     */
    public static native int getPageCount(long document);

    /**
     * Obtiene la versión del archivo PDF.
     *
     * @param document Handle del documento
     * @return Versión del PDF (14 para 1.4, 15 para 1.5, etc.) o -1 si falla
     */
    public static native int getFileVersion(long document);

    /**
     * Obtiene los permisos del documento.
     *
     * @param document Handle del documento
     * @return Flags de permisos
     */
    public static native long getDocPermissions(long document);

    /**
     * Obtiene el último código de error.
     *
     * @return Código de error
     */
    public static native int getLastError();

    // ========== Métodos Nativos - Operaciones con Páginas ==========

    /**
     * Carga una página del documento.
     *
     * @param document Handle del documento
     * @param pageIndex Índice de la página (0 para la primera)
     * @return Handle de la página o 0 si falla
     */
    public static native long loadPage(long document, int pageIndex);

    /**
     * Cierra una página y libera sus recursos.
     *
     * @param page Handle de la página
     */
    public static native void closePage(long page);

    /**
     * Obtiene el ancho de una página.
     *
     * @param page Handle de la página
     * @return Ancho en puntos (1/72 pulgada)
     */
    public static native double getPageWidth(long page);

    /**
     * Obtiene el alto de una página.
     *
     * @param page Handle de la página
     * @return Alto en puntos (1/72 pulgada)
     */
    public static native double getPageHeight(long page);

    /**
     * Obtiene el tamaño de una página por índice sin cargarla.
     *
     * @param document Handle del documento
     * @param pageIndex Índice de la página
     * @return Array de dos elementos [width, height] o null si falla
     */
    public static native double[] getPageSizeByIndex(long document, int pageIndex);

    // ========== Métodos Nativos - Renderizado ==========

    /**
     * Crea un bitmap para renderizado.
     *
     * @param width Ancho en píxeles
     * @param height Alto en píxeles
     * @param alpha true para usar canal alpha
     * @return Handle del bitmap o 0 si falla
     */
    public static native long createBitmap(int width, int height, boolean alpha);

    /**
     * Crea un bitmap con formato específico.
     *
     * @param width Ancho en píxeles
     * @param height Alto en píxeles
     * @param format Formato del bitmap (FPDFBitmap_*)
     * @return Handle del bitmap o 0 si falla
     */
    public static native long createBitmapEx(int width, int height, int format);

    /**
     * Destruye un bitmap y libera sus recursos.
     *
     * @param bitmap Handle del bitmap
     */
    public static native void destroyBitmap(long bitmap);

    /**
     * Llena un rectángulo del bitmap con un color.
     *
     * @param bitmap Handle del bitmap
     * @param left Posición X izquierda
     * @param top Posición Y superior
     * @param width Ancho del rectángulo
     * @param height Alto del rectángulo
     * @param color Color en formato ARGB 8888
     * @return true si tiene éxito
     */
    public static native boolean fillRect(long bitmap, int left, int top,
                                          int width, int height, int color);

    /**
     * Obtiene el buffer de datos del bitmap.
     *
     * @param bitmap Handle del bitmap
     * @return ByteBuffer con los datos del bitmap
     */
    public static native ByteBuffer getBitmapBuffer(long bitmap);

    /**
     * Obtiene el stride (bytes por línea) del bitmap.
     *
     * @param bitmap Handle del bitmap
     * @return Número de bytes por línea
     */
    public static native int getBitmapStride(long bitmap);

    /**
     * Obtiene el ancho del bitmap.
     *
     * @param bitmap Handle del bitmap
     * @return Ancho en píxeles
     */
    public static native int getBitmapWidth(long bitmap);

    /**
     * Obtiene el alto del bitmap.
     *
     * @param bitmap Handle del bitmap
     * @return Alto en píxeles
     */
    public static native int getBitmapHeight(long bitmap);

    /**
     * Renderiza una página en un bitmap.
     *
     * @param bitmap Handle del bitmap
     * @param page Handle de la página
     * @param startX Posición X inicial en el bitmap
     * @param startY Posición Y inicial en el bitmap
     * @param sizeX Ancho del área de renderizado
     * @param sizeY Alto del área de renderizado
     * @param rotate Rotación (ROTATE_*)
     * @param flags Flags de renderizado (combinación de FPDF_*)
     */
    public static native void renderPageBitmap(long bitmap, long page,
                                               int startX, int startY,
                                               int sizeX, int sizeY,
                                               int rotate, int flags);

    // ========== Métodos Nativos - Conversión de Coordenadas ==========

    /**
     * Convierte coordenadas de dispositivo a coordenadas de página.
     *
     * @param page Handle de la página
     * @param startX Posición X inicial del área de visualización
     * @param startY Posición Y inicial del área de visualización
     * @param sizeX Ancho del área de visualización
     * @param sizeY Alto del área de visualización
     * @param rotate Rotación
     * @param deviceX Coordenada X del dispositivo
     * @param deviceY Coordenada Y del dispositivo
     * @return Array [pageX, pageY] o null si falla
     */
    public static native double[] deviceToPage(long page, int startX, int startY,
                                               int sizeX, int sizeY, int rotate,
                                               int deviceX, int deviceY);

    /**
     * Convierte coordenadas de página a coordenadas de dispositivo.
     *
     * @param page Handle de la página
     * @param startX Posición X inicial del área de visualización
     * @param startY Posición Y inicial del área de visualización
     * @param sizeX Ancho del área de visualización
     * @param sizeY Alto del área de visualización
     * @param rotate Rotación
     * @param pageX Coordenada X de la página
     * @param pageY Coordenada Y de la página
     * @return Array [deviceX, deviceY] o null si falla
     */
    public static native int[] pageToDevice(long page, int startX, int startY,
                                            int sizeX, int sizeY, int rotate,
                                            double pageX, double pageY);
}