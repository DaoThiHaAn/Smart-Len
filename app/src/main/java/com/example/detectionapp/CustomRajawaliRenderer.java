package com.example.detectionapp;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.MotionEvent;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.loader.LoaderOBJ;
import org.rajawali3d.loader.ParsingException;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.renderer.Renderer;
import org.rajawali3d.view.SurfaceView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CustomRajawaliRenderer extends Renderer {

    private static final String TAG = "CustomRajawaliRenderer";
    private final SurfaceView surfaceView; // Reference to the SurfaceView
    private Object3D object3D;
    volatile boolean objectNeedsAdding = false;
    Object3D objectToAdd = null;

    public CustomRajawaliRenderer(Context context, SurfaceView surfaceView) {
        super(context);
        this.surfaceView = surfaceView; // Store the SurfaceView reference
        setFrameRate(60); // Optional: Set desired frame rate
    }

    @Override
    protected void initScene() {
        getCurrentScene().setBackgroundColor(0x00000000);

        // Add a light source
        DirectionalLight keyLight = new DirectionalLight(0.5, 0.5, -1.0);
        keyLight.setColor(1.0f, 1.0f, 1.0f);
        keyLight.setPower(1.0f);
        getCurrentScene().addLight(keyLight);

        // Add some ambient light
        DirectionalLight ambientLight = new DirectionalLight(0.1, -0.5, -0.5);
        ambientLight.setColor(0.7f, 0.7f, 0.7f);
        ambientLight.setPower(1f);
        getCurrentScene().addLight(ambientLight);

        // Set up the camera
        getCurrentCamera().setPosition(0, 2, 10);
        getCurrentCamera().setLookAt(0, 0, 0);
        getCurrentCamera().setFarPlane(1000);
    }

    public boolean loadObjInBackground(Uri uri) {
        Log.d(TAG, "Starting OBJ load in background for URI: " + uri);
        File tempFile = null;
        try {
            tempFile = File.createTempFile("temp_model_", ".obj", getContext().getCacheDir());
            Log.d(TAG, "Temporary file created: " + tempFile.getAbsolutePath());

            try (InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
                 FileOutputStream outputStream = new FileOutputStream(tempFile)) {

                if (inputStream == null) {
                    Log.e(TAG, "Could not open InputStream for URI: " + uri);
                    return false;
                }

                byte[] buffer = new byte[4096];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                outputStream.flush();
                Log.d(TAG, "Copied InputStream to temporary file.");
            }

            LoaderOBJ loaderOBJ = new LoaderOBJ(this, tempFile);
            Log.d(TAG, "Parsing OBJ file...");
            loaderOBJ.parse();
            Log.d(TAG, "Parsing complete.");

            Object3D newObject = loaderOBJ.getParsedObject();
            if (newObject == null) {
                Log.e(TAG, "Parsed object is null!");
                return false;
            }
            Log.d(TAG, "Parsed object retrieved.");

            // Center the object
            Vector3 min = newObject.getBoundingBox().getTransformedMin();
            Vector3 max = newObject.getBoundingBox().getTransformedMax();
            Vector3 center = Vector3.addAndCreate(min, max).multiply(0.5); // Calculate center
            newObject.setPosition(-center.x, -center.y, -center.z); // Offset to center the object

            // Scale the object
            float width = (float) (max.x - min.x);
            float height = (float) (max.y - min.y);
            float depth = (float) (max.z - min.z);
            float maxDim = Math.max(width, Math.max(height, depth)); // Get the largest dimension

            float targetDim = 4.0f; // Target dimension (e.g., fit within 4 units)
            float scaleFactor = (maxDim > 0) ? targetDim / maxDim : 1.0f; // Avoid division by zero
            newObject.setScale(scaleFactor);
            Log.d(TAG, "Object scaled to fit within target dimension: " + targetDim);

            Object3D oldObject = this.object3D;
            objectToAdd = newObject;
            objectNeedsAdding = true;

            // Use the SurfaceView to request a render
            surfaceView.requestRender();

            if (oldObject != null) {
                Log.d(TAG, "Scheduling removal of old object.");
            }

            return true;

        } catch (ParsingException e) {
            Log.e(TAG, "Rajawali ParsingException during OBJ load", e);
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception during OBJ load", e);
            e.printStackTrace();
            return false;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                if (tempFile.delete()) {
                    Log.d(TAG, "Temporary file deleted.");
                } else {
                    Log.w(TAG, "Failed to delete temporary file: " + tempFile.getAbsolutePath());
                }
            }
        }
    }

    @Override
    protected void onRender(long ellapsedRealtime, double deltaTime) {
        super.onRender(ellapsedRealtime, deltaTime);

        if (objectNeedsAdding) {
            if (this.object3D != null) {
                getCurrentScene().removeChild(this.object3D);
                Log.d(TAG, "Old object removed from scene.");
            }

            if (objectToAdd != null) {
                getCurrentScene().addChild(objectToAdd);
                this.object3D = objectToAdd;
                Log.d(TAG, "New object added to scene.");
            } else {
                this.object3D = null;
            }

            objectToAdd = null;
            objectNeedsAdding = false;
        }
    }

    // Method to be called from Fragment's touch listener
    public void handleDrag(float dx, float dy) {
        if (object3D != null) {
            // Adjust rotation speed (e.g., 0.2 degrees per pixel)
            float rotationSpeed = 0.2f;
            object3D.rotate(Vector3.Axis.Y, dx * rotationSpeed);
            object3D.rotate(Vector3.Axis.X, dy * rotationSpeed);
        }
    }

    // Method to be called from Fragment's scale listener
    public void handleScale(float scaleFactor) {
        if (object3D != null) {
            // Apply scale cumulatively
            object3D.setScale(object3D.getScale().x * scaleFactor); // Assuming uniform scaling
            // Add clamping if needed:
            // double currentScale = object3D.getScale().x;
            // double newScale = Math.max(0.1, Math.min(currentScale * scaleFactor, 10.0)); // Clamp between 0.1x and 10x
            // object3D.setScale(newScale);
        }
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset, float xOffsetStep, float yOffsetStep, int xPixelOffset, int yPixelOffset) {
        // Not typically used for this scenario
    }

    @Override
    public void onTouchEvent(MotionEvent event) {
        // Touch events are handled by the Fragment and forwarded
    }

    // Getter for the object (might be useful for Fragment)
    public Object3D getObject3D() {
        return object3D;
    }

    // Call this from Fragment's onPause
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "Renderer paused");
    }

    // Call this from Fragment's onResume
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "Renderer resumed");
    }

    // Call this from Fragment's onDestroy or onDetach
    public void cleanup() {
        Log.d(TAG, "Cleaning up renderer...");
        // Clean up scene resources if necessary
        if (object3D != null) {
            try {
                getCurrentScene().removeChild(object3D);
                // object3D.destroy(); // Consider destroying if necessary and safe
            } catch (Exception e) {
                Log.e(TAG, "Error during cleanup", e);
            }
            object3D = null;
        }
        // Could also clear lights, etc.
        // stopRendering(); // Already handled by SurfaceView's lifecycle? Check Rajawali docs.
    }
}