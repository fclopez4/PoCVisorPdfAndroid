package com.fcl.pocvisorpdf;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fcl.pdfium_wrapper.PdfiumJNI;
import com.fcl.pocvisorpdf.databinding.ActivitySecondBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SecondActivity extends AppCompatActivity {

    private static final String TAG = "SecondActivity";
    private ActivitySecondBinding binding;
    private PdfRenderer pdfRenderer;
    private int currentPage = 0;

    // Storage Access Framework - para seleccionar archivos
    private final ActivityResultLauncher<Intent> openDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri fileUri = result.getData().getData();
                    if (fileUri != null) {
                        Log.d(TAG, "Archivo seleccionado: " + fileUri);
                        loadPdfFromUri(fileUri);
                    }
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                Log.d(TAG, "Permiso solicitado. Resultado: " + isGranted);
                if (isGranted) {
                    Log.d(TAG, "Permiso concedido");
                    openFileSelector();
                } else {
                    Log.w(TAG, "Permiso denegado");
                    openFileSelector();  // Intentar abrir selector de todas formas
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySecondBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Handle back button
        binding.btnBackToFirst.setOnClickListener(v -> {
            this.getOnBackPressedDispatcher().onBackPressed();
        });
        // Inicializar PDFium
        PdfiumJNI.initLibrary();

        setupUI();
        checkPermissionsAndOpenFile();
    }

    private void setupUI() {
        binding.btnPrevPage.setOnClickListener(v -> {
            Log.d(TAG, "Botón Previous presionado");
            if (pdfRenderer != null && currentPage > 0) {
                currentPage--;
                renderCurrentPage();
            }
        });

        binding.btnNextPage.setOnClickListener(v -> {
            Log.d(TAG, "Botón Next presionado");
            if (pdfRenderer != null && currentPage < pdfRenderer.getPageCount() - 1) {
                currentPage++;
                renderCurrentPage();
            }
        });

        binding.btnLoadPdf.setOnClickListener(v -> {
            Log.d(TAG, "Botón Load PDF presionado");
            checkPermissionsAndOpenFile();
        });
    }

    /**
     * Verifica permisos y abre el selector de archivos (Storage Access Framework)
     */
    private void checkPermissionsAndOpenFile() {
        Log.d(TAG, "checkPermissionsAndOpenFile() - API Level: " + Build.VERSION.SDK_INT);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+)
            Log.d(TAG, "Android 13+ detectado - Solicitando READ_MEDIA_IMAGES");
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "READ_MEDIA_IMAGES ya concedido");
                openFileSelector();
            } else {
                Log.d(TAG, "Pidiendo READ_MEDIA_IMAGES...");
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else {
            // Android 12 y anteriores
            Log.d(TAG, "Android 12 o anterior - Solicitando READ_EXTERNAL_STORAGE");
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "READ_EXTERNAL_STORAGE ya concedido");
                openFileSelector();
            } else {
                Log.d(TAG, "Pidiendo READ_EXTERNAL_STORAGE...");
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    /**
     * Abre el Storage Access Framework para seleccionar un archivo PDF
     */
    private void openFileSelector() {
        Log.d(TAG, "openFileSelector() - Abriendo selector de archivos");
        
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");  // Filtrar solo archivos PDF
        
        try {
            openDocumentLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error abriendo selector: " + e.getMessage());
            Toast.makeText(this, "No se pudo abrir el selector de archivos", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Carga el PDF desde un Uri obtenido del Storage Access Framework
     */
    private void loadPdfFromUri(Uri fileUri) {
        Log.d(TAG, "loadPdfFromUri() - Uri: " + fileUri);
        
        try {
            ContentResolver contentResolver = getContentResolver();
            
            // Obtener InputStream desde el Uri
            InputStream inputStream = contentResolver.openInputStream(fileUri);
            if (inputStream == null) {
                throw new Exception("No se pudo obtener InputStream del Uri");
            }
            
            // Copiar a un archivo temporal en caché de la app
            File cacheDir = getCacheDir();
            File tempPdfFile = new File(cacheDir, "temp_sample.pdf");
            
            try (FileOutputStream fos = new FileOutputStream(tempPdfFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            inputStream.close();
            
            Log.d(TAG, "PDF copiado al caché: " + tempPdfFile.getAbsolutePath());
            loadPdfFromPath(tempPdfFile.getAbsolutePath());
            
        } catch (Exception e) {
            String errorMsg = "Error cargando PDF: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            binding.txtInfo.setText(errorMsg);
        }
    }

    /**
     * Carga el PDF desde una ruta específica (después de copiar a caché)
     */
    private void loadPdfFromPath(String pdfPath) {
        Log.d(TAG, "loadPdfFromPath() - Ruta: " + pdfPath);
        
        // Validar que el archivo existe y es legible
        File file = new File(pdfPath);
        if (!file.exists()) {
            String errorMsg = "Archivo no existe: " + pdfPath;
            Log.e(TAG, errorMsg);
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            binding.txtInfo.setText(errorMsg);
            return;
        }
        
        if (!file.canRead()) {
            String errorMsg = "No hay permisos para leer: " + pdfPath;
            Log.e(TAG, errorMsg);
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            binding.txtInfo.setText(errorMsg);
            return;
        }
        
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
            }

            pdfRenderer = new PdfRenderer(pdfPath);
            currentPage = 0;
            renderCurrentPage();

            String successMsg = "PDF cargado: " + pdfRenderer.getPageCount() + " páginas";
            Log.d(TAG, successMsg);
            Toast.makeText(this, successMsg, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            String errorMsg = "Error al cargar PDF: " + e.getMessage();
            Log.e(TAG, errorMsg, e);
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
            binding.txtInfo.setText(errorMsg);
        }
    }

    private void renderCurrentPage() {
        if (pdfRenderer == null) return;

        try {
            // Obtener dimensiones de la vista
            int viewWidth = binding.imgPdfPage.getWidth();
            int viewHeight = binding.imgPdfPage.getHeight();

            if (viewWidth == 0 || viewHeight == 0) {
                // Si la vista aún no tiene dimensiones, usar valores por defecto
                viewWidth = 1080;
                viewHeight = 1920;
            }

            // Renderizar la página
            Bitmap bitmap = pdfRenderer.renderPage(currentPage, viewWidth, viewHeight);
            binding.imgPdfPage.setImageBitmap(bitmap);

            // Actualizar información
            String info = String.format("Página %d de %d\nAncho: %.2f pts\nAlto: %.2f pts",
                    currentPage + 1,
                    pdfRenderer.getPageCount(),
                    pdfRenderer.getPageWidth(currentPage),
                    pdfRenderer.getPageHeight(currentPage));
            binding.txtInfo.setText(info);

            // Actualizar botones
            binding.btnPrevPage.setEnabled(currentPage > 0);
            binding.btnNextPage.setEnabled(currentPage < pdfRenderer.getPageCount() - 1);

        } catch (Exception e) {
            Toast.makeText(this, "Error al renderizar página: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pdfRenderer != null) {
            pdfRenderer.close();
            pdfRenderer = null;
        }
        PdfiumJNI.destroyLibrary();
    }
}