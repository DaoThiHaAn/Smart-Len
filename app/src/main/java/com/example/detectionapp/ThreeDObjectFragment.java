package com.example.detectionapp;

import android.animation.ObjectAnimator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.view.PreviewView;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.common.util.concurrent.ListenableFuture;

public class ThreeDObjectFragment extends Fragment {

    private ObjectAnimator scanAnimator;
    private FrameLayout scanningContainer;
    private View scanningLine;
    private ProgressBar progressBar;
    private TextView selectedFileName;
    private Button removeButton;
    private ScaleGestureDetector scaleGestureDetector;
    private ActivityResultLauncher<String> filePickerLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_three_d_object, container, false);

        // Initialize views
        scanningContainer = view.findViewById(R.id.scanningContainer);
        scanningLine = view.findViewById(R.id.scanningLine);
        progressBar = view.findViewById(R.id.progressBar);
        selectedFileName = view.findViewById(R.id.selectedFileName);
        removeButton = view.findViewById(R.id.removeButton);
        Button addButton = view.findViewById(R.id.addBtn);

        // Initialize the file picker launcher
        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                String fileName = uri.getLastPathSegment(); // Extract file name
                if (fileName != null) {
                    stopScanningAnimation();
                    selectedFileName.setText(fileName);
                    selectedFileName.setVisibility(View.VISIBLE);
                    removeButton.setVisibility(View.VISIBLE);
                }
            }
        });

        // Initialize the permission launcher
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                startCamera(view);
            } else {
                Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show();
            }
        });

        // Show progress bar while starting the camera
        progressBar.setVisibility(View.VISIBLE);

        // Check for camera permissions
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            startCamera(view);
        }

        // Initialize scanning animation
        startScanningAnimation();

        // Add button click listener
        addButton.setOnClickListener(v -> openFilePicker());

        // Remove button click listener
        removeButton.setOnClickListener(v -> removeObject());

        // Initialize touch gestures
        setupTouchGestures(view);

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera(getView());
        } else {
            Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera(View view) {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // Camera setup logic here (e.g., bind preview, analysis, etc.)
                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                PreviewView previewView = view.findViewById(R.id.previewView);
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                cameraProvider.bindToLifecycle(this, cameraSelector, preview);

                // Hide progress bar after camera starts
                progressBar.setVisibility(View.GONE);

                // Start scanning animation after camera setup
                startScanningAnimation();

                Toast.makeText(requireContext(), "Camera started", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                progressBar.setVisibility(View.GONE); // Hide progress bar on failure
                Toast.makeText(requireContext(), "Failed to start camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void startScanningAnimation() {
        scanningContainer.setVisibility(View.VISIBLE);
        scanningLine.setTranslationY(0f); // Reset line position
        scanAnimator = ObjectAnimator.ofFloat(scanningLine, "translationY", 0f, scanningContainer.getHeight());
        scanAnimator.setDuration(2000);
        scanAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        scanAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        scanAnimator.start();
    }

    private void stopScanningAnimation() {
        if (scanAnimator != null && scanAnimator.isRunning()) {
            scanAnimator.cancel();
        }
        scanningContainer.setVisibility(View.GONE);
    }

    private void openFilePicker() {
        filePickerLauncher.launch("application/octet-stream"); // Adjust MIME type for 3D object files
    }

    private void removeObject() {
        selectedFileName.setText("");
        selectedFileName.setVisibility(View.GONE);
        removeButton.setVisibility(View.GONE);
        startScanningAnimation();
    }

    private void setupTouchGestures(View view) {
        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                // Handle zoom gesture
                float scaleFactor = detector.getScaleFactor();
                // Apply scaling logic to the 3D object
                return true;
            }
        });

        // Add touch listener to the AR view
        View arView = view.findViewById(R.id.previewView);
        if (arView != null) {
            arView.setOnTouchListener((v, event) -> {
                scaleGestureDetector.onTouchEvent(event);
                // Handle rotation and movement gestures here
                return true;
            });
        }
    }
}