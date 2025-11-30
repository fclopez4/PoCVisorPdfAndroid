#include "pdfium_jni.h"
#include "../jniLibs/include/fpdfview.h"
#include <android/log.h>
#include <string>
#include <cstring>

// Tag para logging
#define LOG_TAG "PdfiumJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Macros para conversión de handles
#define TO_FPDF_DOCUMENT(ptr) reinterpret_cast<FPDF_DOCUMENT>(ptr)
#define TO_FPDF_PAGE(ptr) reinterpret_cast<FPDF_PAGE>(ptr)
#define TO_FPDF_BITMAP(ptr) reinterpret_cast<FPDF_BITMAP>(ptr)
#define TO_JLONG(ptr) reinterpret_cast<jlong>(ptr)

// ========== Funciones de Utilidad ==========

/**
 * Convierte un jstring a const char* UTF-8
 */
const char* jstringToChar(JNIEnv* env, jstring jStr) {
    if (jStr == nullptr) return nullptr;
    return env->GetStringUTFChars(jStr, nullptr);
}

/**
 * Libera un string convertido
 */
void releaseString(JNIEnv* env, jstring jStr, const char* cStr) {
    if (jStr != nullptr && cStr != nullptr) {
        env->ReleaseStringUTFChars(jStr, cStr);
    }
}

extern "C" {
// ========== Inicialización y Destrucción ==========

JNIEXPORT void JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_initLibrary
        (JNIEnv* env, jclass clazz) {
    LOGD("Inicializando PDFium...");
    FPDF_InitLibrary();
    LOGD("PDFium inicializado correctamente");
}

JNIEXPORT void JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_destroyLibrary
        (JNIEnv* env, jclass clazz) {
    LOGD("Destruyendo PDFium...");
    FPDF_DestroyLibrary();
    LOGD("PDFium destruido correctamente");
}

// ========== Carga de Documentos ==========

JNIEXPORT jlong JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_loadDocument
        (JNIEnv* env, jclass clazz, jstring filePath, jstring password) {

    const char* cFilePath = jstringToChar(env, filePath);
    const char* cPassword = jstringToChar(env, password);

    LOGD("Cargando documento: %s", cFilePath ? cFilePath : "null");

    FPDF_DOCUMENT document = FPDF_LoadDocument(cFilePath, cPassword);

    if (document == nullptr) {
        unsigned long error = FPDF_GetLastError();
        LOGE("Error al cargar documento: %lu", error);
    } else {
        LOGD("Documento cargado exitosamente");
    }

    releaseString(env, filePath, cFilePath);
    releaseString(env, password, cPassword);

    return TO_JLONG(document);
}

JNIEXPORT jlong JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_loadMemDocument___3BLjava_lang_String_2
        (JNIEnv* env, jclass clazz, jbyteArray data, jstring password) {

    if (data == nullptr) {
        LOGE("loadMemDocument: data es null");
        return 0;
    }

    jsize dataLen = env->GetArrayLength(data);
    jbyte* dataPtr = env->GetByteArrayElements(data, nullptr);

    const char* cPassword = jstringToChar(env, password);

    LOGD("Cargando documento desde memoria (%d bytes)", dataLen);

    FPDF_DOCUMENT document = FPDF_LoadMemDocument(dataPtr, dataLen, cPassword);

    if (document == nullptr) {
        unsigned long error = FPDF_GetLastError();
        LOGE("Error al cargar documento desde memoria: %lu", error);
    } else {
        LOGD("Documento cargado exitosamente desde memoria");
    }

    releaseString(env, password, cPassword);
    env->ReleaseByteArrayElements(data, dataPtr, JNI_ABORT);

    return TO_JLONG(document);
}

JNIEXPORT jlong JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_loadMemDocument__Ljava_nio_ByteBuffer_2Ljava_lang_String_2
        (JNIEnv* env, jclass clazz, jobject buffer, jstring password) {

    if (buffer == nullptr) {
        LOGE("loadMemDocument: buffer es null");
        return 0;
    }

    void* bufferPtr = env->GetDirectBufferAddress(buffer);
    jlong capacity = env->GetDirectBufferCapacity(buffer);

    if (bufferPtr == nullptr || capacity <= 0) {
        LOGE("loadMemDocument: buffer inválido");
        return 0;
    }

    const char* cPassword = jstringToChar(env, password);

    LOGD("Cargando documento desde ByteBuffer (%lld bytes)", (long long) capacity);

    FPDF_DOCUMENT document = FPDF_LoadMemDocument(bufferPtr, static_cast<int>(capacity), cPassword);

    if (document == nullptr) {
        unsigned long error = FPDF_GetLastError();
        LOGE("Error al cargar documento desde ByteBuffer: %lu", error);
    } else {
        LOGD("Documento cargado exitosamente desde ByteBuffer");
    }

    releaseString(env, password, cPassword);

    return TO_JLONG(document);
}

JNIEXPORT void JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_closeDocument
        (JNIEnv* env, jclass clazz, jlong document) {

    if (document == 0) {
        LOGE("closeDocument: documento es null");
        return;
    }

    LOGD("Cerrando documento");
    FPDF_CloseDocument(TO_FPDF_DOCUMENT(document));
}

// ========== Información del Documento ==========

JNIEXPORT jint JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_getPageCount
        (JNIEnv* env, jclass clazz, jlong document) {

    if (document == 0) {
        LOGE("getPageCount: documento es null");
        return 0;
    }

    int count = FPDF_GetPageCount(TO_FPDF_DOCUMENT(document));
    LOGD("Número de páginas: %d", count);
    return count;
}

JNIEXPORT jint JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_getFileVersion
        (JNIEnv* env, jclass clazz, jlong document) {

    if (document == 0) {
        LOGE("getFileVersion: documento es null");
        return -1;
    }

    int fileVersion = 0;
    if (FPDF_GetFileVersion(TO_FPDF_DOCUMENT(document), &fileVersion)) {
        LOGD("Versión del archivo PDF: %d", fileVersion);
        return fileVersion;
    }

    LOGE("Error al obtener versión del archivo");
    return -1;
}

JNIEXPORT jlong JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_getDocPermissions
        (JNIEnv* env, jclass clazz, jlong document) {

    if (document == 0) {
        LOGE("getDocPermissions: documento es null");
        return 0;
    }

    unsigned long permissions = FPDF_GetDocPermissions(TO_FPDF_DOCUMENT(document));
    LOGD("Permisos del documento: %lu", permissions);
    return static_cast<jlong>(permissions);
}

JNIEXPORT jint JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_getLastError
        (JNIEnv* env, jclass clazz) {

    unsigned long error = FPDF_GetLastError();
    if (error != FPDF_ERR_SUCCESS) {
        LOGE("Último error de PDFium: %lu", error);
    }
    return static_cast<jint>(error);
}

// ========== Operaciones con Páginas ==========

JNIEXPORT jlong JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_loadPage
        (JNIEnv* env, jclass clazz, jlong document, jint pageIndex) {

    if (document == 0) {
        LOGE("loadPage: documento es null");
        return 0;
    }

    LOGD("Cargando página %d", pageIndex);

    FPDF_PAGE page = FPDF_LoadPage(TO_FPDF_DOCUMENT(document), pageIndex);

    if (page == nullptr) {
        unsigned long error = FPDF_GetLastError();
        LOGE("Error al cargar página %d: %lu", pageIndex, error);
    } else {
        LOGD("Página %d cargada exitosamente", pageIndex);
    }

    return TO_JLONG(page);
}

JNIEXPORT void JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_closePage
        (JNIEnv* env, jclass clazz, jlong page) {

    if (page == 0) {
        LOGE("closePage: página es null");
        return;
    }

    LOGD("Cerrando página");
    FPDF_ClosePage(TO_FPDF_PAGE(page));
}

JNIEXPORT jdouble JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_getPageWidth
        (JNIEnv* env, jclass clazz, jlong page) {

    if (page == 0) {
        LOGE("getPageWidth: página es null");
        return 0.0;
    }

    double width = FPDF_GetPageWidth(TO_FPDF_PAGE(page));
    LOGD("Ancho de página: %.2f", width);
    return width;
}

JNIEXPORT jdouble JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_getPageHeight
        (JNIEnv* env, jclass clazz, jlong page) {

    if (page == 0) {
        LOGE("getPageHeight: página es null");
        return 0.0;
    }

    double height = FPDF_GetPageHeight(TO_FPDF_PAGE(page));
    LOGD("Alto de página: %.2f", height);
    return height;
}

JNIEXPORT jdoubleArray JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_getPageSizeByIndex
        (JNIEnv* env, jclass clazz, jlong document, jint pageIndex) {

    if (document == 0) {
        LOGE("getPageSizeByIndex: documento es null");
        return nullptr;
    }

    double width = 0.0;
    double height = 0.0;

    int result = FPDF_GetPageSizeByIndex(TO_FPDF_DOCUMENT(document), pageIndex, &width, &height);

    if (result == 0) {
        LOGE("Error al obtener tamaño de página %d", pageIndex);
        return nullptr;
    }

    LOGD("Tamaño de página %d: %.2fx%.2f", pageIndex, width, height);

    jdoubleArray jArray = env->NewDoubleArray(2);
    if (jArray == nullptr) {
        LOGE("Error al crear array de doubles");
        return nullptr;
    }

    jdouble temp[2] = {width, height};
    env->SetDoubleArrayRegion(jArray, 0, 2, temp);

    return jArray;
}

// ========== Renderizado - Bitmaps ==========

JNIEXPORT jlong JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_createBitmap
        (JNIEnv* env, jclass clazz, jint width, jint height, jboolean alpha) {

    if (width <= 0 || height <= 0) {
        LOGE("createBitmap: dimensiones inválidas (%dx%d)", width, height);
        return 0;
    }

    LOGD("Creando bitmap: %dx%d, alpha=%d", width, height, alpha);

    FPDF_BITMAP bitmap = FPDFBitmap_Create(width, height, alpha ? 1 : 0);

    if (bitmap == nullptr) {
        LOGE("Error al crear bitmap");
    } else {
        LOGD("Bitmap creado exitosamente");
    }

    return TO_JLONG(bitmap);
}

JNIEXPORT jlong JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_createBitmapEx
        (JNIEnv* env, jclass clazz, jint width, jint height, jint format) {

    if (width <= 0 || height <= 0) {
        LOGE("createBitmapEx: dimensiones inválidas (%dx%d)", width, height);
        return 0;
    }

    LOGD("Creando bitmap Ex: %dx%d, formato=%d", width, height, format);

    FPDF_BITMAP bitmap = FPDFBitmap_CreateEx(width, height, format, nullptr, 0);

    if (bitmap == nullptr) {
        LOGE("Error al crear bitmap Ex");
    } else {
        LOGD("Bitmap Ex creado exitosamente");
    }

    return TO_JLONG(bitmap);
}

JNIEXPORT void JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_destroyBitmap
        (JNIEnv* env, jclass clazz, jlong bitmap) {

    if (bitmap == 0) {
        LOGE("destroyBitmap: bitmap es null");
        return;
    }

    LOGD("Destruyendo bitmap");
    FPDFBitmap_Destroy(TO_FPDF_BITMAP(bitmap));
}

JNIEXPORT jboolean JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_fillRect
        (JNIEnv* env, jclass clazz, jlong bitmap, jint left, jint top, jint width, jint height, jint color) {

    if (bitmap == 0) {
        LOGE("fillRect: bitmap es null");
        return JNI_FALSE;
    }

    LOGD("Llenando rectángulo: (%d,%d) %dx%d, color=0x%08X", left, top, width, height, color);

    FPDF_BOOL result = FPDFBitmap_FillRect(TO_FPDF_BITMAP(bitmap), left, top, width, height,
                                           static_cast<FPDF_DWORD>(color));

    return result ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jobject JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_getBitmapBuffer
        (JNIEnv* env, jclass clazz, jlong bitmap) {

    if (bitmap == 0) {
        LOGE("getBitmapBuffer: bitmap es null");
        return nullptr;
    }

    void* buffer = FPDFBitmap_GetBuffer(TO_FPDF_BITMAP(bitmap));
    if (buffer == nullptr) {
        LOGE("Error al obtener buffer del bitmap");
        return nullptr;
    }

    int width = FPDFBitmap_GetWidth(TO_FPDF_BITMAP(bitmap));
    int height = FPDFBitmap_GetHeight(TO_FPDF_BITMAP(bitmap));
    int stride = FPDFBitmap_GetStride(TO_FPDF_BITMAP(bitmap));

    jlong capacity = static_cast<jlong>(stride) * height;

    LOGD("Buffer del bitmap: %dx%d, stride=%d, capacity=%lld", width, height, stride, (long long) capacity);

    return env->NewDirectByteBuffer(buffer, capacity);
}

JNIEXPORT jint JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_getBitmapStride
        (JNIEnv* env, jclass clazz, jlong bitmap) {

    if (bitmap == 0) {
        LOGE("getBitmapStride: bitmap es null");
        return 0;
    }

    int stride = FPDFBitmap_GetStride(TO_FPDF_BITMAP(bitmap));
    LOGD("Stride del bitmap: %d", stride);
    return stride;
}

JNIEXPORT jint JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_getBitmapWidth
        (JNIEnv* env, jclass clazz, jlong bitmap) {

    if (bitmap == 0) {
        LOGE("getBitmapWidth: bitmap es null");
        return 0;
    }

    int width = FPDFBitmap_GetWidth(TO_FPDF_BITMAP(bitmap));
    LOGD("Ancho del bitmap: %d", width);
    return width;
}

JNIEXPORT jint JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_getBitmapHeight
        (JNIEnv* env, jclass clazz, jlong bitmap) {

    if (bitmap == 0) {
        LOGE("getBitmapHeight: bitmap es null");
        return 0;
    }

    int height = FPDFBitmap_GetHeight(TO_FPDF_BITMAP(bitmap));
    LOGD("Alto del bitmap: %d", height);
    return height;
}

JNIEXPORT void JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_renderPageBitmap
        (JNIEnv* env, jclass clazz, jlong bitmap, jlong page,
         jint startX, jint startY, jint sizeX, jint sizeY, jint rotate, jint flags) {

    if (bitmap == 0 || page == 0) {
        LOGE("renderPageBitmap: bitmap o página es null");
        return;
    }

    LOGD("Renderizando página: start=(%d,%d), size=(%dx%d), rotate=%d, flags=0x%X",
         startX, startY, sizeX, sizeY, rotate, flags);

    FPDF_RenderPageBitmap(TO_FPDF_BITMAP(bitmap), TO_FPDF_PAGE(page),
                          startX, startY, sizeX, sizeY, rotate, flags);

    LOGD("Página renderizada exitosamente");
}

// ========== Conversión de Coordenadas ==========

JNIEXPORT jdoubleArray JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_deviceToPage
        (JNIEnv* env, jclass clazz, jlong page, jint startX, jint startY,
         jint sizeX, jint sizeY, jint rotate, jint deviceX, jint deviceY) {

    if (page == 0) {
        LOGE("deviceToPage: página es null");
        return nullptr;
    }

    double pageX = 0.0;
    double pageY = 0.0;

    FPDF_BOOL result = FPDF_DeviceToPage(TO_FPDF_PAGE(page), startX, startY,
                                         sizeX, sizeY, rotate, deviceX, deviceY,
                                         &pageX, &pageY);

    if (!result) {
        LOGE("Error en conversión deviceToPage");
        return nullptr;
    }

    LOGD("deviceToPage: device=(%d,%d) -> page=(%.2f,%.2f)", deviceX, deviceY, pageX, pageY);

    jdoubleArray jArray = env->NewDoubleArray(2);
    if (jArray == nullptr) {
        LOGE("Error al crear array de doubles");
        return nullptr;
    }

    jdouble temp[2] = {pageX, pageY};
    env->SetDoubleArrayRegion(jArray, 0, 2, temp);

    return jArray;
}

JNIEXPORT jintArray JNICALL Java_com_fcl_pdfium_1wrapper_PdfiumJNI_pageToDevice
        (JNIEnv* env, jclass clazz, jlong page, jint startX, jint startY,
         jint sizeX, jint sizeY, jint rotate, jdouble pageX, jdouble pageY) {

    if (page == 0) {
        LOGE("pageToDevice: página es null");
        return nullptr;
    }

    int deviceX = 0;
    int deviceY = 0;

    FPDF_BOOL result = FPDF_PageToDevice(TO_FPDF_PAGE(page), startX, startY,
                                         sizeX, sizeY, rotate, pageX, pageY,
                                         &deviceX, &deviceY);

    if (!result) {
        LOGE("Error en conversión pageToDevice");
        return nullptr;
    }

    LOGD("pageToDevice: page=(%.2f,%.2f) -> device=(%d,%d)", pageX, pageY, deviceX, deviceY);

    jintArray jArray = env->NewIntArray(2);
    if (jArray == nullptr) {
        LOGE("Error al crear array de ints");
        return nullptr;
    }

    jint temp[2] = {deviceX, deviceY};
    env->SetIntArrayRegion(jArray, 0, 2, temp);

    return jArray;
}
}