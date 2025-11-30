package com.fcl.pocvisorpdf;

import android.graphics.Bitmap;
import android.util.Log;

import com.fcl.pdfium_wrapper.PdfiumJNI;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * Clase auxiliar para renderizar documentos PDF usando PDFium
 */
public class PdfRenderer {

    private static final String TAG = "PdfRenderer";
    private long documentHandle = 0;
    private int pageCount = 0;
    private String filePath;

    /**
     * Constructor que carga el documento PDF
     *
     * @param filePath Ruta al archivo PDF
     * @throws Exception Si no se puede cargar el documento
     */
    public PdfRenderer(String filePath) throws Exception {
        this.filePath = filePath;
        
        // Validar que el archivo existe
        File file = new File(filePath);
        if (!file.exists()) {
            throw new Exception("Archivo no encontrado: " + filePath);
        }
        
        if (!file.canRead()) {
            throw new Exception("No hay permisos para leer el archivo: " + filePath);
        }

        Log.d(TAG, "Intentando cargar PDF desde: " + filePath);
        Log.d(TAG, "Tamaño del archivo: " + file.length() + " bytes");
        
        documentHandle = PdfiumJNI.loadDocument(filePath, null);

        if (documentHandle == 0) {
            int error = PdfiumJNI.getLastError();
            Log.e(TAG, "Error al cargar PDF. Código: " + error + " - " + getErrorMessage(error));
            throw new Exception("Error al cargar PDF. Código de error: " + getErrorMessage(error) + " (Ruta: " + filePath + ")");
        }

        pageCount = PdfiumJNI.getPageCount(documentHandle);
        Log.d(TAG, "PDF cargado exitosamente. Páginas: " + pageCount);
    }

    /**
     * Obtiene el número de páginas del documento
     *
     * @return Número de páginas
     */
    public int getPageCount() {
        return pageCount;
    }

    /**
     * Obtiene el ancho de una página
     *
     * @param pageIndex Índice de la página (0-based)
     * @return Ancho en puntos
     */
    public double getPageWidth(int pageIndex) {
        double[] size = PdfiumJNI.getPageSizeByIndex(documentHandle, pageIndex);
        return size != null ? size[0] : 0;
    }

    /**
     * Obtiene el alto de una página
     *
     * @param pageIndex Índice de la página (0-based)
     * @return Alto en puntos
     */
    public double getPageHeight(int pageIndex) {
        double[] size = PdfiumJNI.getPageSizeByIndex(documentHandle, pageIndex);
        return size != null ? size[1] : 0;
    }

    /**
     * Renderiza una página del PDF como un Bitmap
     *
     * @param pageIndex Índice de la página (0-based)
     * @param width Ancho deseado del bitmap
     * @param height Alto deseado del bitmap
     * @return Bitmap con la página renderizada
     * @throws Exception Si hay un error al renderizar
     */
    public Bitmap renderPage(int pageIndex, int width, int height) throws Exception {
        if (pageIndex < 0 || pageIndex >= pageCount) {
            throw new IllegalArgumentException("Índice de página inválido: " + pageIndex);
        }

        // Cargar la página
        long pageHandle = PdfiumJNI.loadPage(documentHandle, pageIndex);
        if (pageHandle == 0) {
            throw new Exception("Error al cargar página " + pageIndex);
        }

        try {
            // Obtener dimensiones de la página
            double pageWidth = PdfiumJNI.getPageWidth(pageHandle);
            double pageHeight = PdfiumJNI.getPageHeight(pageHandle);

            // Calcular dimensiones manteniendo el aspect ratio
            double aspectRatio = pageWidth / pageHeight;
            int bitmapWidth = width;
            int bitmapHeight = (int) (width / aspectRatio);

            if (bitmapHeight > height) {
                bitmapHeight = height;
                bitmapWidth = (int) (height * aspectRatio);
            }

            // Crear bitmap
            long bitmapHandle = PdfiumJNI.createBitmapEx(bitmapWidth, bitmapHeight,
                    PdfiumJNI.FPDFBitmap_BGRA);
            if (bitmapHandle == 0) {
                throw new Exception("Error al crear bitmap");
            }

            try {
                // Llenar el fondo con blanco
                PdfiumJNI.fillRect(bitmapHandle, 0, 0, bitmapWidth, bitmapHeight, 0xFFFFFFFF);

                // Renderizar la página
                PdfiumJNI.renderPageBitmap(
                        bitmapHandle,
                        pageHandle,
                        0, 0,
                        bitmapWidth, bitmapHeight,
                        PdfiumJNI.ROTATE_0,
                        PdfiumJNI.FPDF_ANNOT | PdfiumJNI.FPDF_LCD_TEXT
                );

                // Convertir a Bitmap de Android
                return convertToBitmap(bitmapHandle, bitmapWidth, bitmapHeight);

            } finally {
                PdfiumJNI.destroyBitmap(bitmapHandle);
            }

        } finally {
            PdfiumJNI.closePage(pageHandle);
        }
    }

    /**
     * Convierte el bitmap de PDFium a un Bitmap de Android
     *
     * @param bitmapHandle Handle del bitmap de PDFium
     * @param width Ancho del bitmap
     * @param height Alto del bitmap
     * @return Bitmap de Android
     */
    private Bitmap convertToBitmap(long bitmapHandle, int width, int height) {
        ByteBuffer buffer = PdfiumJNI.getBitmapBuffer(bitmapHandle);
        int stride = PdfiumJNI.getBitmapStride(bitmapHandle);

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // PDFium usa formato BGRA, necesitamos convertir a ARGB
        int[] pixels = new int[width * height];
        buffer.rewind();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int offset = y * stride + x * 4;
                int b = buffer.get(offset) & 0xFF;
                int g = buffer.get(offset + 1) & 0xFF;
                int r = buffer.get(offset + 2) & 0xFF;
                int a = buffer.get(offset + 3) & 0xFF;

                // Convertir BGRA a ARGB
                pixels[y * width + x] = (a << 24) | (r << 16) | (g << 8) | b;
            }
        }

        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    /**
     * Cierra el documento y libera recursos
     */
    public void close() {
        if (documentHandle != 0) {
            PdfiumJNI.closeDocument(documentHandle);
            documentHandle = 0;
        }
    }

    /**
     * Obtiene un mensaje descriptivo del código de error
     *
     * @param errorCode Código de error de PDFium
     * @return Mensaje de error
     */
    private String getErrorMessage(int errorCode) {
        switch (errorCode) {
            case PdfiumJNI.FPDF_ERR_SUCCESS:
                return "Sin error";
            case PdfiumJNI.FPDF_ERR_UNKNOWN:
                return "Error desconocido";
            case PdfiumJNI.FPDF_ERR_FILE:
                return "No se puede abrir o leer el archivo";
            case PdfiumJNI.FPDF_ERR_FORMAT:
                return "Formato de archivo incorrecto o dañado";
            case PdfiumJNI.FPDF_ERR_PASSWORD:
                return "Contraseña incorrecta";
            case PdfiumJNI.FPDF_ERR_SECURITY:
                return "Error de seguridad no soportado";
            case PdfiumJNI.FPDF_ERR_PAGE:
                return "Página no encontrada o error de contenido";
            default:
                return "Error código: " + errorCode;
        }
    }
}