package com.example.detectionapp;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
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
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
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

    private ObjectAnimator scanAnimator;
    private static final String TAG = "QRScanner";
    private ExecutorService cameraExecutor;
    private TextView qrCodeResult;
    private CameraControl cameraControl;

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
        View scanningLine = view.findViewById(R.id.scanningLine);

        // Scanning animation
        PreviewView previewView = view.findViewById(R.id.previewView);
        ObjectAnimator animator = ObjectAnimator.ofFloat(scanningLine, "translationY", 0f, previewView.getHeight());
        animator.setDuration(2000);
        animator.setRepeatCount(ObjectAnimator.INFINITE);
        animator.setRepeatMode(ObjectAnimator.REVERSE);
        animator.start();

        // Check and request camera permission
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            progressBar.setVisibility(View.VISIBLE); // Show progress bar
            startCamera(view, progressBar); // Pass both View and ProgressBar
        }

        ScaleGestureDetector scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            private float currentZoomRatio = 1.0f;

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                currentZoomRatio *= detector.getScaleFactor();
                currentZoomRatio = Math.max(1.0f, Math.min(currentZoomRatio, 10.0f)); // Clamp zoom ratio
                cameraControl.setZoomRatio(currentZoomRatio); // Set zoom ratio
                return true;
            }
        });

        previewView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);
            return true;
        });
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
                                    if (barcodes.isEmpty()) {
                                        // No barcodes detected, show scanning animation
                                        View scanningContainer = getView().findViewById(R.id.scanningContainer);
                                        scanningContainer.setVisibility(View.VISIBLE);

                                        if (scanAnimator != null && !scanAnimator.isRunning()) {
                                            scanAnimator.start();
                                        }

                                    } else {
                                        for (Barcode barcode : barcodes) {
                                            String rawValue = barcode.getRawValue();
                                            if (rawValue != null) {
                                                // Stop scanning animation
                                                View scanningContainer = getView().findViewById(R.id.scanningContainer);
                                                scanningContainer.setVisibility(View.GONE);
                                                if (scanAnimator != null && scanAnimator.isRunning()) {
                                                    scanAnimator.cancel();
                                                }

                                                // Truncate long URLs for display
                                                String displayText = rawValue.length() > 50 ? rawValue.substring(0, 47) + "..." : rawValue;
                                                qrCodeResult.setText(displayText);

                                                // Make the TextView clickable if it's a URL
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

                View scanningContainer = view.findViewById(R.id.scanningContainer);
                View scanningLine = view.findViewById(R.id.scanningLine);

                // Show scanning box after camera starts
                scanningContainer.setVisibility(View.VISIBLE);

                scanningContainer.post(() -> {
                    float containerHeight = scanningContainer.getHeight();
                    scanningLine.setTranslationY(0f); // Reset line
                
                    scanAnimator = ObjectAnimator.ofFloat(scanningLine, "translationY", 0f, containerHeight);
                    scanAnimator.setDuration(2000);
                    scanAnimator.setRepeatCount(ObjectAnimator.INFINITE);
                    scanAnimator.setRepeatMode(ObjectAnimator.REVERSE);
                    scanAnimator.start();
                });


                // Get CameraControl for zoom functionality
                cameraControl = camera.getCameraControl();

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