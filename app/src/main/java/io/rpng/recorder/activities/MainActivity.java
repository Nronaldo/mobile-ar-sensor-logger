package io.rpng.recorder.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;

import io.rpng.recorder.managers.CameraManager;
import io.rpng.recorder.R;
import io.rpng.recorder.views.AutoFitTextureView;


public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";
    private static final int RESULT_SETTINGS = 1;
    private static final int RESULT_RESULT = 2;

    private static Intent intentSettings;
    private static Intent intentResults;

    private static ImageView camera2View;
    private static TextView camera2Captured;
    private AutoFitTextureView mTextureView;

    public static CameraManager mCameraManager;
    private static SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Pass to super
        super.onCreate(savedInstanceState);

        // Create our layout
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Add our listeners
        this.addButtonListeners();

        // Get our surfaces
        camera2View = (ImageView) findViewById(R.id.camera2_preview);
        camera2Captured = (TextView) findViewById(R.id.camera2_captures);
        mTextureView = (AutoFitTextureView) findViewById(R.id.camera2_texture);

        // Update the textview with starting values
        camera2Captured.setText("Capture Success: 0\nCapture Tries: 0");

        // Create the camera manager
        mCameraManager = new CameraManager(this, mTextureView, camera2View);

        // Set our shared preferences
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Build the result activities for later
        intentSettings = new Intent(this, SettingsActivity.class);
        intentResults = new Intent(this, ResultsActivity.class);

        // Lets by default launch into the settings view
        startActivityForResult(intentSettings, RESULT_SETTINGS);



    }

    private void addButtonListeners() {

        // When the done button is pressed, we want to calibrate
        // This should start the calibration activity, and then start the calibration
        Button button_done = (Button) findViewById(R.id.button_done);
        button_done.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(intentResults, RESULT_RESULT);
            }
        });

        // We we want to "capture" the current grid, we should record the current corners
        Button button_record = (Button) findViewById(R.id.button_record);
        button_record.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add the corners
                //mCameraCalibrator.addCorners();
                // Update the text view
                //camera2Captured.setText("Capture Success: " + mCameraCalibrator.getCornersBufferSize()
                //        + "\nCapture Tries: " + mCameraCalibrator.getCaptureTries());
            }
        });
    }

    @Override
    public void onResume() {
        // Pass to our super
        super.onResume();
        // Start the background thread
        mCameraManager.startBackgroundThread();
        // Open the camera
        // This should take care of the permissions requests
        if (mTextureView.isAvailable()) {
            mCameraManager.openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(mCameraManager.mSurfaceTextureListener);
        }
    }

    @Override
    public void onPause() {
        // Stop background thread
        mCameraManager.stopBackgroundThread();
        // Close our camera, note we will get permission errors if we try to reopen
        // And we have not closed the current active camera
        mCameraManager.closeCamera();
        // Call the super
        super.onPause();
    }

    // Taken from OpenCamera project
    // URL: https://github.com/almalence/OpenCamera/blob/master/src/com/almalence/opencam/cameracontroller/Camera2Controller.java#L3455
    public final static ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader ir) {

            // Contrary to what is written in Aptina presentation acquireLatestImage is not working as described
            // Google: Also, not working as described in android docs (should work the same as acquireNextImage in
            // our case, but it is not)
            // Image im = ir.acquireLatestImage();

            // Get the next image from the queue
            Image image = ir.acquireNextImage();

            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();

            // Get the YUV planes, and combine them into a single data byte array
            Image.Plane Y = image.getPlanes()[0];
            Image.Plane U = image.getPlanes()[1];
            Image.Plane V = image.getPlanes()[2];
            int Yb = Y.getBuffer().remaining();
            int Ub = U.getBuffer().remaining();
            int Vb = V.getBuffer().remaining();


            byte[] data = new byte[Yb + Ub + Vb];
            Y.getBuffer().get(data, 0, Yb);
            U.getBuffer().get(data, Yb, Ub);
            V.getBuffer().get(data, Yb + Ub, Vb);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
            yuvImage.compressToJpeg(new Rect(0, 0, image.getWidth(), image.getHeight()), 50, out);
            byte[] imageBytes = out.toByteArray();

            Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            MainActivity.camera2View.setImageBitmap(bmp);

            // Make sure we close the image
            image.close();
        }
    };


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, RESULT_SETTINGS);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            // Call back from end of settings activity
            case RESULT_SETTINGS:

                // The settings have changed, so reset the calibrator
                //mCameraCalibrator.clearCorners();

                // Update the textview with starting values
                camera2Captured.setText("Capture Success: 0\nCapture Tries: 0");

                break;

            // Call back from end of settings activity
            case RESULT_RESULT:

                // The settings have changed, so reset the calibrator
                //mCameraCalibrator.clearCorners();

                // Update the textview with starting values
                camera2Captured.setText("Capture Success: 0\nCapture Tries: 0");

                break;

        }

    }
}
