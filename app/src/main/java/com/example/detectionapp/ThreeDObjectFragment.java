package com.example.detectionapp;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler; // Import Handler
import android.os.Looper; // Import Looper
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast; // Import Toast

import androidx.activity.result.ActivityResultLauncher; // Import ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts; // Import ActivityResultContracts
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.rajawali3d.view.SurfaceView;

import java.util.concurrent.ExecutorService; // Import ExecutorService
import java.util.concurrent.Executors; // Import Executors

public class ThreeDObjectFragment extends Fragment {

    private static final String TAG = "3DObjectFragment"; // Tag for logging

    private SurfaceView rajawaliSurfaceView;
    private CustomRajawaliRenderer renderer;
    private ScaleGestureDetector scaleGestureDetector;
    private float previousX, previousY;
    private ProgressBar progressBar;
    private TextView selectedFileNameTextView; // Renamed for clarity
    private Button removeButton;
    private Button addButton;

    // Executor for background tasks
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    // Handler for posting results back to the main thread
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    // Activity Result Launcher for file picking
    private ActivityResultLauncher<String> filePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the file picker launcher
        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                Log.d(TAG, "File selected: " + uri.toString());
                // Update UI immediately (optional)
                String fileName = getFileNameFromUri(uri); // Helper method needed
                selectedFileNameTextView.setText(fileName);
                selectedFileNameTextView.setVisibility(View.VISIBLE);
                removeButton.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.VISIBLE); // Show progress

                // Launch background loading
                loadModelInBackground(uri);
            } else {
                Log.d(TAG, "File selection cancelled.");
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_three_d_object, container, false);

        // Initialize Views
        rajawaliSurfaceView = view.findViewById(R.id.rajawaliSurfaceView);
        progressBar = view.findViewById(R.id.progressBar);
        selectedFileNameTextView = view.findViewById(R.id.selectedFileName);
        removeButton = view.findViewById(R.id.removeButton);
        addButton = view.findViewById(R.id.addBtn);

        // --- Setup Rajawali ---
        renderer = new CustomRajawaliRenderer(requireContext(), rajawaliSurfaceView); // Pass SurfaceView
        rajawaliSurfaceView.setSurfaceRenderer(renderer);

        // Important for transparency if your renderer background is transparent
        rajawaliSurfaceView.setZOrderMediaOverlay(true);
        rajawaliSurfaceView.getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);

        // --- Setup Listeners ---
        addButton.setOnClickListener(v -> openFilePicker());
        removeButton.setOnClickListener(v -> removeCurrentObject());

        // --- Setup Touch Handling ---
        setupTouchListener();
        scaleGestureDetector = new ScaleGestureDetector(requireContext(), new ScaleListener());

        return view;
    }

    private void setupTouchListener() {
        rajawaliSurfaceView.setOnTouchListener((v, event) -> {
            // Pass event to ScaleGestureDetector FIRST
            scaleGestureDetector.onTouchEvent(event);

            // Don't handle drag if scaling is in progress
            if (scaleGestureDetector.isInProgress()) {
                return true;
            }

            // Handle drag
            switch (event.getActionMasked()) { // Use getActionMasked for multi-touch compatibility
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG, "Touch Down");
                    previousX = event.getX();
                    previousY = event.getY();
                    break;
                case MotionEvent.ACTION_MOVE:
                    // Only rotate if one pointer is down (prevents rotate while pinching)
                    if (event.getPointerCount() == 1) {
                        float dx = event.getX() - previousX;
                        float dy = event.getY() - previousY;

                        // Send drag info to renderer
                        renderer.handleDrag(dx, dy);

                        // Update previous coordinates for next move event
                        previousX = event.getX();
                        previousY = event.getY();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    Log.d(TAG, "Touch Up/Cancel");
                    // No action needed here for simple rotation
                    break;
            }
            // We handled the touch event
            return true;
        });
    }

    private void openFilePicker() {
        // You can try being more specific, but octet-stream is a safe fallback
        // filePickerLauncher.launch("model/obj");
        // filePickerLauncher.launch("model/*");
        filePickerLauncher.launch("*/*"); // Allow any file type initially
    }

    private void loadModelInBackground(Uri uri) {
        backgroundExecutor.execute(() -> {
            // Perform loading in the background thread
            final boolean success = renderer.loadObjInBackground(uri);

            // Post result back to the main thread to update UI
            mainThreadHandler.post(() -> {
                progressBar.setVisibility(View.GONE); // Hide progress
                if (success) {
                    Toast.makeText(getContext(), "Model loaded successfully", Toast.LENGTH_SHORT).show();
                    // UI updated via filePickerLauncher callback already for name/button
                } else {
                    Toast.makeText(getContext(), "Failed to load model", Toast.LENGTH_LONG).show();
                    removeCurrentObject(); // Clear UI if loading failed
                }
            });
        });
    }

    private void removeCurrentObject() {
        // Request renderer to clear the object (will happen on render thread)
        renderer.objectNeedsAdding = true; // Set flag
        renderer.objectToAdd = null;      // Set object to add as null
        rajawaliSurfaceView.requestRender();          // Trigger render to process removal

        // Update UI
        selectedFileNameTextView.setText("");
        selectedFileNameTextView.setVisibility(View.GONE);
        removeButton.setVisibility(View.GONE);
        Log.d(TAG, "Object removal requested.");
    }

    // Helper to get filename (implementation depends on URI type)
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting filename from content URI", e);
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        // Fallback if still null
        return result != null ? result : "Unknown File";
    }

    // --- Scale Gesture Listener ---
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // Pass scale factor to renderer
            // detector.getScaleFactor() gives the relative scale change since the last event
            renderer.handleScale(detector.getScaleFactor());
            return true; // We handled the scale event
        }
    }

    // --- Fragment Lifecycle ---

    @Override
    public void onResume() {
        super.onResume();
        if (renderer != null) {
            renderer.onResume(); // Resume Rajawali renderer
        }
        if (rajawaliSurfaceView != null) {
            rajawaliSurfaceView.onResume(); // Resume SurfaceView
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        if (renderer != null) {
            renderer.onPause(); // Pause Rajawali renderer
        }
        if (rajawaliSurfaceView != null) {
            rajawaliSurfaceView.onPause(); // Pause SurfaceView
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up renderer resources
        if (renderer != null) {
            renderer.cleanup(); // Custom cleanup method
        }
        // Shut down the executor
        backgroundExecutor.shutdown();
        Log.d(TAG, "onDestroyView: Executor shutdown requested.");
        // Nullify views to prevent leaks
        rajawaliSurfaceView = null;
        renderer = null;
        progressBar = null;
        // ... nullify other views ...
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Final check for executor shutdown if not done in onDestroyView
        if (!backgroundExecutor.isShutdown()) {
            backgroundExecutor.shutdownNow(); // Force shutdown if needed
            Log.w(TAG, "onDestroy: Executor forced shutdown.");
        }
    }
}