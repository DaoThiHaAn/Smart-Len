package com.example.detectionapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.common.InputImage;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@androidx.camera.core.ExperimentalGetImage
public class QRScannerFragment extends Fragment {

    private static final String TAG = "QRScanner";
    private ExecutorService cameraExecutor;
    private TextView qrCodeResult;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission granted
                    View view = getView();
                    if (view != null) {
                        ProgressBar progressBar = view.findViewById(R.id.progressBar);
                        progressBar.setVisibility(View.VISIBLE); // Show progress bar
                        startCamera(view, progressBar); // Pass both View and ProgressBar
                    }
                } else {
                    // Permission denied
                    Toast.makeText(requireContext(), "Camera permission is required to use this feature", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_q_r_scanner, container, false);

        qrCodeResult = view.findViewById(R.id.qrCodeResult);
        qrCodeResult.setMovementMethod(LinkMovementMethod.getInstance()); // Enable clickable links
        cameraExecutor = Executors.newSingleThreadExecutor();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ProgressBar progressBar = view.findViewById(R.id.progressBar);

        // Check and request camera permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            progressBar.setVisibility(View.VISIBLE); // Show progress bar
            startCamera(view, progressBar); // Pass both View and ProgressBar
        }
    }

    private void startCamera(View view, ProgressBar progressBar) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                preview.setSurfaceProvider(
                        ((androidx.camera.view.PreviewView) view.findViewById(R.id.previewView)).getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                BarcodeScanner scanner = BarcodeScanning.getClient();
                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    if (image.getImage() == null) {
                        image.close();
                        return;
                    }

                    try {
                        InputImage inputImage = InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees());
                        scanner.process(inputImage)
                                .addOnSuccessListener(barcodes -> {
                                    for (Barcode barcode : barcodes) {
                                        String rawValue = barcode.getRawValue();
                                        if (rawValue != null) {
                                            qrCodeResult.setText(rawValue);

                                            // Check if the result is a URL
                                            if (rawValue.startsWith("http://") || rawValue.startsWith("https://")) {
                                                qrCodeResult.setOnClickListener(v -> {
                                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(rawValue));
                                                    startActivity(browserIntent);
                                                });
                                            } else {
                                                qrCodeResult.setOnClickListener(null); // Disable click if not a URL
                                            }
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Barcode scanning failed", e))
                                .addOnCompleteListener(task -> image.close());
                    } catch (Exception e) {
                        image.close();
                        Log.e(TAG, "Camera initialization failed: " + e.getMessage(), e);
                    }
                });

                Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
                progressBar.setVisibility(View.GONE); // Hide progress bar
                Toast.makeText(requireContext(), "Camera started", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                progressBar.setVisibility(View.GONE); // Hide progress bar on failure
                Log.e(TAG, "Camera initialization failed", e);
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        cameraExecutor.shutdown();
    }
}