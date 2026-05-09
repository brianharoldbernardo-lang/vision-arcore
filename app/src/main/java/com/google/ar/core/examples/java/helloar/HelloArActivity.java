/*
 * Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ar.core.examples.java.helloar;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.media.Image;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

//import com.google.ai.client.generativeai.GenerativeModel;
//import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.ArCoreApk.Availability;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Config.InstantPlacementMode;
import com.google.ar.core.DepthPoint;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.InstantPlacementPoint;
import com.google.ar.core.LightEstimate;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.google.ar.core.examples.java.common.helpers.CameraPermissionHelper;
import com.google.ar.core.examples.java.common.helpers.DepthSettings;
import com.google.ar.core.examples.java.common.helpers.DisplayRotationHelper;
import com.google.ar.core.examples.java.common.helpers.FullScreenHelper;
import com.google.ar.core.examples.java.common.helpers.InstantPlacementSettings;
import com.google.ar.core.examples.java.common.helpers.SnackbarHelper;
import com.google.ar.core.examples.java.common.helpers.TapHelper;
import com.google.ar.core.examples.java.common.helpers.TrackingStateHelper;
import com.google.ar.core.examples.java.common.samplerender.Framebuffer;
import com.google.ar.core.examples.java.common.samplerender.GLError;
import com.google.ar.core.examples.java.common.samplerender.Mesh;
import com.google.ar.core.examples.java.common.samplerender.SampleRender;
import com.google.ar.core.examples.java.common.samplerender.Shader;
import com.google.ar.core.examples.java.common.samplerender.Texture;
import com.google.ar.core.examples.java.common.samplerender.VertexBuffer;
import com.google.ar.core.examples.java.common.samplerender.arcore.BackgroundRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.PlaneRenderer;
import com.google.ar.core.examples.java.common.samplerender.arcore.SpecularCubemapFilter;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import android.graphics.Bitmap;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import com.google.ar.core.examples.java.helloar.TTSHelper;
// Keep these imports!
//import com.google.ai.client.generativeai.type.Content;
//import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
// ... and the others we added
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
/**
 * This is a simple example that shows how to create an augmented reality (AR) application using the
 * ARCore API. The application will display any detected planes and will allow the user to tap on a
 * plane to place a 3D model.
 */
public class HelloArActivity extends AppCompatActivity implements SampleRender.Renderer {
  // Keep these variables!
  private boolean isSceneAnalysisMode = false;
  //private GenerativeModelFutures model;


  private static final String TAG = HelloArActivity.class.getSimpleName();
  private int visionFrameCounter = 0;
  private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";
  private static final String WAITING_FOR_TAP_MESSAGE = "Tap on a surface to place an object.";
  private boolean isProcessingVision = false;
  // See the definition of updateSphericalHarmonicsCoefficients for an explanation of these
  // constants.
  private OverlayView overlayView;
  private static final float[] sphericalHarmonicFactors = {
    0.282095f,
    -0.325735f,
    0.325735f,
    -0.325735f,
    0.273137f,
    -0.273137f,
    0.078848f,
    -0.273137f,
    0.136569f,
  };

  private static final float Z_NEAR = 0.1f;
  private static final float Z_FAR = 100f;

  private static final int CUBEMAP_RESOLUTION = 16;
  private static final int CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES = 32;

  // Rendering. The Renderers are created here, and initialized when the GL surface is created.
  private GLSurfaceView surfaceView;

  private boolean installRequested;

  private Session session;
  private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
  private DisplayRotationHelper displayRotationHelper;
  private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);
  private TapHelper tapHelper;
  private SampleRender render;

  private PlaneRenderer planeRenderer;
  private BackgroundRenderer backgroundRenderer;
  private Framebuffer virtualSceneFramebuffer;
  private boolean hasSetTextureNames = false;

  private final DepthSettings depthSettings = new DepthSettings();
  private boolean[] depthSettingsMenuDialogCheckboxes = new boolean[2];

  private final InstantPlacementSettings instantPlacementSettings = new InstantPlacementSettings();
  private boolean[] instantPlacementSettingsMenuDialogCheckboxes = new boolean[1];
  // Assumed distance from the device camera to the surface on which user will try to place objects.
  // This value affects the apparent scale of objects while the tracking method of the
  // Instant Placement point is SCREENSPACE_WITH_APPROXIMATE_DISTANCE.
  // Values in the [0.2, 2.0] meter range are a good choice for most AR experiences. Use lower
  // values for AR experiences where users are expected to place objects on surfaces close to the
  // camera. Use larger values for experiences where the user will likely be standing and trying to
  // place an object on the ground or floor in front of them.
  private static final float APPROXIMATE_DISTANCE_METERS = 2.0f;

  // Point Cloud
  private VertexBuffer pointCloudVertexBuffer;
  private Mesh pointCloudMesh;
  private Shader pointCloudShader;
  // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
  // was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
  private long lastPointCloudTimestamp = 0;

  // Virtual object (ARCore pawn)
  private Mesh virtualObjectMesh;
  private Shader virtualObjectShader;
  private Texture virtualObjectAlbedoTexture;
  private Texture virtualObjectAlbedoInstantPlacementTexture;

  private final List<WrappedAnchor> wrappedAnchors = new ArrayList<>();

  // Environmental HDR
  private Texture dfgTexture;
  private SpecularCubemapFilter cubemapFilter;

  // Temporary matrix allocated here to reduce number of allocations for each frame.
  private final float[] modelMatrix = new float[16];
  private final float[] viewMatrix = new float[16];
  private final float[] projectionMatrix = new float[16];
  private final float[] modelViewMatrix = new float[16]; // view x model
  private final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model
  private final float[] sphericalHarmonicsCoefficients = new float[9 * 3];
  private final float[] viewInverseMatrix = new float[16];
  private final float[] worldLightDirection = {0.0f, 0.0f, 0.0f, 0.0f};
  private final float[] viewLightDirection = new float[4]; // view x world light direction
  private Bitmap rotatedBitmap;
  private Bitmap inputToAI;
  private YoloDetector detector;
  private YuvToRgbConverter yuvToRgbConverter;
  private Bitmap yoloBitmap;
  private TTSHelper tts;
  private boolean welcomeSpoken = false;

  private static final long TTS_COOLDOWN = 1000; // 1 second cooldown for path TTS
  private String lastSpokenDirection = "";
  private long lastTtsTime = 0;
  // Temporal smoothing for path suggestion
  private int[] lastTargetCols = new int[5]; // keep last 5 frames
  private int lastTargetIndex = 0;
  // Circular buffer for temporal smoothing of target column

  private String lastSpokenSteering = "";     // track last TTS direction

  private float latestDepthValue = 0.0f;




  // ADD THESE (The Groq variables)
  private final OkHttpClient httpClient = new OkHttpClient();
  private final String GROQ_API_KEY = BuildConfig.GROQ_API_KEY;

  private boolean isArSupportedMode = true;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    surfaceView = findViewById(R.id.surfaceview);
    displayRotationHelper = new DisplayRotationHelper(/* context= */ this);
    overlayView = findViewById(R.id.overlay_view);
    tts = new TTSHelper(this);

    // FORCE the overlay to sit on the very top of the stack
    overlayView.setZ(1000f);
    overlayView.bringToFront();
    // Set up touch listener.
    tapHelper = new TapHelper(/* context= */ this);
    surfaceView.setOnTouchListener(tapHelper);

    // Set up renderer.
    render = new SampleRender(surfaceView, this, getAssets());

    installRequested = false;

    depthSettings.onCreate(this);
    instantPlacementSettings.onCreate(this);
    ImageButton settingsButton = findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            PopupMenu popup = new PopupMenu(HelloArActivity.this, v);
            popup.setOnMenuItemClickListener(HelloArActivity.this::settingsMenuClick);
            popup.inflate(R.menu.settings_menu);
            popup.show();
          }
        });
    try {
      // 1. Initialize the YOLO detector with your specific model names
      detector = new YoloDetector(this, "best-fp16-new.tflite", "labels.txt");

      // 2. Initialize the high-speed camera converter
      yuvToRgbConverter = new YuvToRgbConverter(this);

      // 3. Create the 640x640 "canvas" for the AI to look at
      yoloBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888);

      Log.d("NAV_APP", "AI Vision Engine Initialized Successfully!");
    } catch (Exception e) {
      Log.e("NAV_APP", "CRITICAL VISION ERROR: ", e);
    }


    //model = GenerativeModelFutures.from(gm);


    // Inside onCreate, after setContentView
    Button explainBtn = findViewById(R.id.btn_explain_scene);
    explainBtn.setOnClickListener(v -> {
      runSceneAnalysis();
    });
  }

  /** Menu button to launch feature specific settings. */
  protected boolean settingsMenuClick(MenuItem item) {
    if (item.getItemId() == R.id.depth_settings) {
      launchDepthSettingsMenuDialog();
      return true;
    }
    else if (item.getItemId() == R.id.language_settings) {
      launchLanguageSettingsDialog();
      return true;
    }else if (item.getItemId() == R.id.menu_scene_explanation) { // <--- Match this ID with your XML
      runSceneAnalysis();
      return true;
    }
    return false;
  }
  private void launchLanguageSettingsDialog() {
    String[] languages = {"English", "Filipino"};
    new AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setSingleChoiceItems(languages, -1, null)
            .setPositiveButton("OK", (dialog, which) -> {
              int selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
              if (selectedPosition == 0) {
                tts.setLanguage("en");
                Toast.makeText(this, "Language set to English", Toast.LENGTH_SHORT).show();
              } else if (selectedPosition == 1) {
                tts.setLanguage("fil");
                Toast.makeText(this, "Language set to Filipino", Toast.LENGTH_SHORT).show();
              }
            })
            .setNegativeButton("Cancel", null)
            .show();
  }

  @Override
  protected void onDestroy() {
    if (session != null) {
      // Explicitly close ARCore Session to release native resources.
      // Review the API reference for important considerations before calling close() in apps with
      // more complicated lifecycle requirements:
      // https://developers.google.com/ar/reference/java/arcore/reference/com/google/ar/core/Session#close()
      session.close();
      session = null;
    }
    if (tts != null) {
      tts.shutdown();
      tts = null;
    }
    super.onDestroy();
  }

  @Override
  protected void onResume() {
    super.onResume();

    if (session == null) {
      Exception exception = null;
      String message = null;
      try {
        Availability availability = ArCoreApk.getInstance().checkAvailability(this);

        if (availability != Availability.SUPPORTED_INSTALLED) {
          switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
            case INSTALL_REQUESTED:
              installRequested = true;
              return;
            case INSTALLED:
              break;
          }
        }

        if (!CameraPermissionHelper.hasCameraPermission(this)) {
          CameraPermissionHelper.requestCameraPermission(this);
          return;
        }

        // Create the session.
        session = new Session(this);
        // SUCCESS: If we reach here, hardware supports basic ARCore.
        try {
          session = new Session(this);
          // 1. Configure first
          configureSession();
          // 2. NOW check for depth support
          isArSupportedMode = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
          // 3. Resume
          session.resume();
          Log.d(TAG, "AR Session Resumed. Depth Supported: " + isArSupportedMode);
        } catch (Exception e) {
          isArSupportedMode = false;
          session = null;
        }

      } catch (UnavailableDeviceNotCompatibleException e) {
        message = "AR not supported. Analysis mode active.";
        exception = e;
        isArSupportedMode = false;
        session = null; // CRITICAL: Ensure session is null so we don't try to use it
      } catch (Exception e) {
        message = "Failed to create AR session";
        exception = e;
        isArSupportedMode = false;
        session = null;
      }

      if (message != null) {
        messageSnackbarHelper.showError(this, message);
        Log.e(TAG, "Exception creating session", exception);
        // We do NOT return. We want the app to stay open for Groq/Llama.
      }
    }

    // --- CRITICAL CHANGE START ---
    // Only resume the session if it was successfully created.
    if (session != null) {
      try {
        configureSession();
        session.resume();
      } catch (CameraNotAvailableException e) {
        messageSnackbarHelper.showError(this, "Camera not available.");
        session = null;
        return;
      }
    } else {
      // FALLBACK: If there is no AR session, we can't use the AR background renderer.
      // For your Capstone, you can log this or show a "Simple View" message.
      Log.d(TAG, "Running in Non-AR Fallback Mode.");
    }
    // --- CRITICAL CHANGE END ---

    surfaceView.onResume();
    displayRotationHelper.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (session != null) {
      displayRotationHelper.onPause();
      surfaceView.onPause();
      session.pause();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(requestCode, permissions, results);
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      // Use toast instead of snackbar here since the activity will exit.
      Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
          .show();
      if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
        // Permission denied with checking "Do not ask again".
        CameraPermissionHelper.launchPermissionSettings(this);
      }
      finish();
    }
  }

  @Override
  public void onWindowFocusChanged(boolean hasFocus) {
    super.onWindowFocusChanged(hasFocus);
    FullScreenHelper.setFullScreenOnWindowFocusChanged(this, hasFocus);
  }

  @Override
  public void onSurfaceCreated(SampleRender render) {
    // Prepare the rendering objects. This involves reading shaders and 3D model files, so may throw
    // an IOException.
    rotatedBitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888); // Match your camera res
    inputToAI = Bitmap.createBitmap(640, 640, Bitmap.Config.ARGB_8888);
    try {
      planeRenderer = new PlaneRenderer(render);
      backgroundRenderer = new BackgroundRenderer(render);
      virtualSceneFramebuffer = new Framebuffer(render, /* width= */ 1, /* height= */ 1);

      cubemapFilter =
          new SpecularCubemapFilter(
              render, CUBEMAP_RESOLUTION, CUBEMAP_NUMBER_OF_IMPORTANCE_SAMPLES);
      // Load DFG lookup table for environmental lighting
      dfgTexture =
          new Texture(
              render,
              Texture.Target.TEXTURE_2D,
              Texture.WrapMode.CLAMP_TO_EDGE,
              /* useMipmaps= */ false);
      // The dfg.raw file is a raw half-float texture with two channels.
      final int dfgResolution = 64;
      final int dfgChannels = 2;
      final int halfFloatSize = 2;

      ByteBuffer buffer =
          ByteBuffer.allocateDirect(dfgResolution * dfgResolution * dfgChannels * halfFloatSize);
      try (InputStream is = getAssets().open("models/dfg.raw")) {
        is.read(buffer.array());
      }
      // SampleRender abstraction leaks here.
      GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, dfgTexture.getTextureId());
      GLError.maybeThrowGLException("Failed to bind DFG texture", "glBindTexture");
      GLES30.glTexImage2D(
          GLES30.GL_TEXTURE_2D,
          /* level= */ 0,
          GLES30.GL_RG16F,
          /* width= */ dfgResolution,
          /* height= */ dfgResolution,
          /* border= */ 0,
          GLES30.GL_RG,
          GLES30.GL_HALF_FLOAT,
          buffer);
      GLError.maybeThrowGLException("Failed to populate DFG texture", "glTexImage2D");

      // Point cloud
      pointCloudShader =
          Shader.createFromAssets(
                  render,
                  "shaders/point_cloud.vert",
                  "shaders/point_cloud.frag",
                  /* defines= */ null)
              .setVec4(
                  "u_Color", new float[] {31.0f / 255.0f, 188.0f / 255.0f, 210.0f / 255.0f, 1.0f})
              .setFloat("u_PointSize", 5.0f);
      // four entries per vertex: X, Y, Z, confidence
      pointCloudVertexBuffer =
          new VertexBuffer(render, /* numberOfEntriesPerVertex= */ 4, /* entries= */ null);
      final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};
      pointCloudMesh =
          new Mesh(
              render, Mesh.PrimitiveMode.POINTS, /* indexBuffer= */ null, pointCloudVertexBuffers);

      // Virtual object to render (ARCore pawn)
      virtualObjectAlbedoTexture =
          Texture.createFromAsset(
              render,
              "models/pawn_albedo.png",
              Texture.WrapMode.CLAMP_TO_EDGE,
              Texture.ColorFormat.SRGB);
      virtualObjectAlbedoInstantPlacementTexture =
          Texture.createFromAsset(
              render,
              "models/pawn_albedo_instant_placement.png",
              Texture.WrapMode.CLAMP_TO_EDGE,
              Texture.ColorFormat.SRGB);
      Texture virtualObjectPbrTexture =
          Texture.createFromAsset(
              render,
              "models/pawn_roughness_metallic_ao.png",
              Texture.WrapMode.CLAMP_TO_EDGE,
              Texture.ColorFormat.LINEAR);

      virtualObjectMesh = Mesh.createFromAsset(render, "models/pawn.obj");
      virtualObjectShader =
          Shader.createFromAssets(
                  render,
                  "shaders/environmental_hdr.vert",
                  "shaders/environmental_hdr.frag",
                  /* defines= */ new HashMap<String, String>() {
                    {
                      put(
                          "NUMBER_OF_MIPMAP_LEVELS",
                          Integer.toString(cubemapFilter.getNumberOfMipmapLevels()));
                    }
                  })
              .setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture)
              .setTexture("u_RoughnessMetallicAmbientOcclusionTexture", virtualObjectPbrTexture)
              .setTexture("u_Cubemap", cubemapFilter.getFilteredCubemapTexture())
              .setTexture("u_DfgTexture", dfgTexture);
    } catch (IOException e) {
      Log.e(TAG, "Failed to read a required asset file", e);
      messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
    }
  }

  @Override
  public void onSurfaceChanged(SampleRender render, int width, int height) {
    displayRotationHelper.onSurfaceChanged(width, height);
    virtualSceneFramebuffer.resize(width, height);
  }

    @Override
    public void onDrawFrame(SampleRender render) {
      // 1. Safety Guard for startup/unsupported
      if (session == null) {
        render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 1f);
        return;
      }

      // DEBUG: Monitor state in Logcat
      Log.d("DEBUG_STATE", "Session: true | Depth Support: " + isArSupportedMode + " | Latest Depth: " + latestDepthValue);

      if (!hasSetTextureNames) {
        session.setCameraTextureNames(new int[] {backgroundRenderer.getCameraColorTexture().getTextureId()});
        hasSetTextureNames = true;
      }

      displayRotationHelper.updateSessionIfNeeded(session);

      Frame frame;
      try {
        frame = session.update();
      } catch (CameraNotAvailableException e) {
        return;
      }

      // Update global depth variable for "Explain Scenery" button
      if (isArSupportedMode) {
        try (Image depthImage = frame.acquireDepthImage16Bits()) {
          this.latestDepthValue = getCenterDepthMeters(depthImage);
        } catch (Exception e) {
          Log.e("DepthCapture", "Depth not ready");
        }
      }

      Camera camera = frame.getCamera();
      visionFrameCounter++;

      // --- YOLO VISION & STEERING CODE ---
      if (visionFrameCounter % 15 == 0 && !isProcessingVision && !isSceneAnalysisMode) {
        isProcessingVision = true;

        Image cameraImage = null;
        Image depthImage = null;
        try {
          cameraImage = frame.acquireCameraImage();
          if (isArSupportedMode) {
            depthImage = frame.acquireDepthImage16Bits();
          }
        } catch (NotYetAvailableException e) {
          isProcessingVision = false;
        }

        if (cameraImage != null) {
          final Image finalCameraImage = cameraImage;
          final Image finalDepthImage = depthImage;

          new Thread(() -> {
            android.graphics.Bitmap rotatedBitmap = null;
            try {
              // Convert and Rotate
              yuvToRgbConverter.yuvToRgb(finalCameraImage, yoloBitmap);
              android.graphics.Matrix matrix = new android.graphics.Matrix();
              matrix.postRotate(90);
              rotatedBitmap = android.graphics.Bitmap.createBitmap(yoloBitmap, 0, 0, yoloBitmap.getWidth(), yoloBitmap.getHeight(), matrix, true);

              // Detection
              List<YoloDetector.Detection> results = detector.detect(rotatedBitmap);

              YoloDetector.Detection nearest = null;
              float minDist = Float.MAX_VALUE;

              // --- INTEGRATED NAVIGATION LOGIC ---
              if (finalDepthImage != null && isArSupportedMode) {
                // A. Calculate distances for detections
                for (YoloDetector.Detection result : results) {
                  float normX = result.boundingBox.centerX() / 640f;
                  float normY = result.boundingBox.centerY() / 640f;

                  float distance = getDistanceFromCapturedImage(finalDepthImage, normX, normY);
                  result.distance = distance;
                  result.direction = getDirection(result.boundingBox, 640);

                  if (distance > 0 && distance < minDist) {
                    minDist = distance;
                    nearest = result;
                  }
                }

                // B. Column-based pathfinding (RESTORED)
                final float SAFE_DISTANCE = 1.5f;
                int NUM_COLUMNS = 9;
                boolean[] columnBlocked = new boolean[NUM_COLUMNS];

                for (int col = 0; col < NUM_COLUMNS; col++) {
                  float startX = col / (float) NUM_COLUMNS;
                  float endX = (col + 1) / (float) NUM_COLUMNS;
                  float medianDepth = computeRegionMedian(finalDepthImage, startX, 0f, endX, 1f);
                  columnBlocked[col] = (medianDepth < SAFE_DISTANCE);
                }

                for (YoloDetector.Detection det : results) {
                  if (det.distance < SAFE_DISTANCE) {
                    int dirIndex = det.direction.ordinal();
                    int startCol = dirIndex * 3;
                    for (int c = startCol; c < startCol + 3; c++) columnBlocked[c] = true;
                  }
                }

                // C. Find widest stretch
                int bestStart = -1, bestLength = 0;
                int currentStart = -1, currentLength = 0;
                for (int i = 0; i < NUM_COLUMNS; i++) {
                  if (!columnBlocked[i]) {
                    if (currentStart == -1) currentStart = i;
                    currentLength++;
                  } else {
                    if (currentLength > bestLength) {
                      bestLength = currentLength;
                      bestStart = currentStart;
                    }
                    currentStart = -1;
                    currentLength = 0;
                  }
                }
                if (currentLength > bestLength) {
                  bestLength = currentLength;
                  bestStart = currentStart;
                }

                // D. Steering calculation
                String steering;
                if (bestLength < 2) {
                  steering = "stop";
                } else {
                  int targetCol = bestStart + (bestLength / 2);
                  if (targetCol < 3) steering = "left";
                  else if (targetCol < 6) steering = "forward";
                  else steering = "right";
                }

                // E. TTS Call
                String label = (nearest != null && nearest.distance < 4.0f) ? nearest.label : "Unknown";
                float distToUse = (nearest != null && nearest.distance < 4.0f) ? nearest.distance : minDist;
                if (distToUse == Float.MAX_VALUE) distToUse = 1.0f;
                YoloDetector.Direction dir = (nearest != null) ? nearest.direction : YoloDetector.Direction.CENTER;

                tts.speakNavigation(label, distToUse, steering, dir);

              } else {
                // FALLBACK: If no depth, just show labels and basic TTS
                Log.d("Vision", "2D Mode - No Steering");
                if (!results.isEmpty()) {
                }
              }

              // Update UI Overlay
              runOnUiThread(() -> {
                if (overlayView != null) {
                  overlayView.setResults(results);
                  overlayView.invalidate();
                }
              });

            } catch (Exception e) {
              Log.e("VisionThread", "Error: " + e.getMessage());
            } finally {
              if (rotatedBitmap != null) rotatedBitmap.recycle();
              if (finalCameraImage != null) finalCameraImage.close();
              if (finalDepthImage != null) finalDepthImage.close();
              isProcessingVision = false;
            }
          }).start();
        } else {
          isProcessingVision = false;
        }
      }

      // --- RENDERING SECTION ---
      try {
        backgroundRenderer.setUseDepthVisualization(render, isArSupportedMode && depthSettings.depthColorVisualizationEnabled());
        backgroundRenderer.setUseOcclusion(render, isArSupportedMode && depthSettings.useDepthForOcclusion());
      } catch (IOException e) {
        return;
      }

      backgroundRenderer.updateDisplayGeometry(frame);
      if (frame.getTimestamp() != 0) {
        backgroundRenderer.drawBackground(render);
      }

      if (isArSupportedMode && camera.getTrackingState() == TrackingState.TRACKING) {
        try (Image depthImage = frame.acquireDepthImage16Bits()) {
          backgroundRenderer.updateCameraDepthTexture(depthImage);
        } catch (Exception e) { /* Ignore */ }
      }

    if (camera.getTrackingState() == TrackingState.TRACKING
        && (depthSettings.useDepthForOcclusion()
            || depthSettings.depthColorVisualizationEnabled())) {
      try (Image depthImage = frame.acquireDepthImage16Bits()) {
        backgroundRenderer.updateCameraDepthTexture(depthImage);
      } catch (NotYetAvailableException e) {
        // This normally means that depth data is not available yet. This is normal so we will not
        // spam the logcat with this.
      }
    }

    // Handle one tap per frame.
    handleTap(frame, camera);

    // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
    trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

    // Show a message based on whether tracking has failed, if planes are detected, and if the user
    // has placed any objects.
    String message = null;
    if (camera.getTrackingState() == TrackingState.PAUSED) {
      if (camera.getTrackingFailureReason() == TrackingFailureReason.NONE) {
        message = SEARCHING_PLANE_MESSAGE;
      } else {
        message = TrackingStateHelper.getTrackingFailureReasonString(camera);
      }
    } else if (hasTrackingPlane()) {
      if (wrappedAnchors.isEmpty()) {
        message = WAITING_FOR_TAP_MESSAGE;
      }
    } else {
      message = SEARCHING_PLANE_MESSAGE;
    }
    if (message == null) {
      messageSnackbarHelper.hide(this);
    } else {
      messageSnackbarHelper.showMessage(this, message);
    }

    // -- Draw background

    if (frame.getTimestamp() != 0) {
      // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
      // drawing possible leftover data from previous sessions if the texture is reused.
      backgroundRenderer.drawBackground(render);
    }

    // If not tracking, don't draw 3D objects.
    if (camera.getTrackingState() == TrackingState.PAUSED) {
      return;
    }

    // -- Draw non-occluded virtual objects (planes, point cloud)

    // Get projection matrix.
    camera.getProjectionMatrix(projectionMatrix, 0, Z_NEAR, Z_FAR);

    // Get camera matrix and draw.
    camera.getViewMatrix(viewMatrix, 0);

    // Visualize tracked points.
    // Use try-with-resources to automatically release the point cloud.
    /*try (PointCloud pointCloud = frame.acquirePointCloud()) {
      if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
        pointCloudVertexBuffer.set(pointCloud.getPoints());
        lastPointCloudTimestamp = pointCloud.getTimestamp();
      }
      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
      pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
      //render.draw(pointCloudMesh, pointCloudShader);
    }*/

    // Visualize planes.
    /*planeRenderer.drawPlanes(
        render,
        session.getAllTrackables(Plane.class),
        camera.getDisplayOrientedPose(),
        projectionMatrix);*/

    // -- Draw occluded virtual objects

    // Update lighting parameters in the shader
    updateLightEstimation(frame.getLightEstimate(), viewMatrix);

    // Visualize anchors created by touch.
    render.clear(virtualSceneFramebuffer, 0f, 0f, 0f, 0f);
    for (WrappedAnchor wrappedAnchor : wrappedAnchors) {
      Anchor anchor = wrappedAnchor.getAnchor();
      Trackable trackable = wrappedAnchor.getTrackable();
      if (anchor.getTrackingState() != TrackingState.TRACKING) {
        continue;
      }

      // Get the current pose of an Anchor in world space. The Anchor pose is updated
      // during calls to session.update() as ARCore refines its estimate of the world.
      anchor.getPose().toMatrix(modelMatrix, 0);

      // Calculate model/view/projection matrices
      Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
      Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

      // Update shader properties and draw
      virtualObjectShader.setMat4("u_ModelView", modelViewMatrix);
      virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);

      if (trackable instanceof InstantPlacementPoint
          && ((InstantPlacementPoint) trackable).getTrackingMethod()
              == InstantPlacementPoint.TrackingMethod.SCREENSPACE_WITH_APPROXIMATE_DISTANCE) {
        virtualObjectShader.setTexture(
            "u_AlbedoTexture", virtualObjectAlbedoInstantPlacementTexture);
      } else {
        virtualObjectShader.setTexture("u_AlbedoTexture", virtualObjectAlbedoTexture);
      }

      render.draw(virtualObjectMesh, virtualObjectShader, virtualSceneFramebuffer);
    }

    // Compose the virtual scene with the background.
    //backgroundRenderer.drawVirtualScene(render, virtualSceneFramebuffer, Z_NEAR, Z_FAR);
  }
  // FOR THE DIRRECTION OF THE BOUNDING BOX
  private YoloDetector.Direction getDirection(RectF box, int screenWidth) {
    float centerX = (box.left + box.right) / 2f;

    if (centerX < screenWidth * 0.33f) {
      return YoloDetector.Direction.LEFT;
    } else if (centerX > screenWidth * 0.66f) {
      return YoloDetector.Direction.RIGHT;
    } else {
      return YoloDetector.Direction.CENTER;
    }
  }
  private void runSceneAnalysis() {
    isSceneAnalysisMode = true;
    tts.stop();

    // Choose the initial message based on hardware support
    if (isArSupportedMode) {
      tts.speak("Analyzing room layout");
    } else {
      tts.speak("Analyzing room layout");
    }

    // FALLBACK LOGIC: If AR is not supported, latestDepthValue will likely be 0.
    // Llama 4 Scout can estimate distance from pixels alone if depth is 0.
    // If AR is off, send 0.0. Llama 4 Scout will use visual estimation instead.
    final float finalDepth = (isArSupportedMode && session != null) ? this.latestDepthValue : 0.0f;

    Log.d("GROQ_DEBUG", "Mode: " + (isArSupportedMode ? "AR" : "Standard") + " | Depth: " + finalDepth + "m");

    // Capture the screenshot from the SurfaceView
    Bitmap screenshot = Bitmap.createBitmap(surfaceView.getWidth(), surfaceView.getHeight(), Bitmap.Config.ARGB_8888);
    PixelCopy.request(surfaceView, screenshot, (copyResult) -> {
      if (copyResult == PixelCopy.SUCCESS) {
        // Use the resized bitmap to save bandwidth and speed up Groq's response
        Bitmap smallBitmap = getResizedBitmap(screenshot, 1024);

        // Pass the smallBitmap and the depth (which is 0.0 for non-AR users)
        sendToGroq(smallBitmap, finalDepth);
      } else {
        tts.speak("Capture failed. Please try again.");
        isSceneAnalysisMode = false;
      }
    }, new Handler(Looper.getMainLooper()));
  }



  private void sendToGroq(Bitmap capturedImage, float sensorDepth) {
    String languagePrompt = tts.getCurrentLanguage().equals("fil") ? "Sumagot sa wikang Tagalog." : "Respond in English.";
    String sensorInfo = (sensorDepth > 0) ? String.format("Our sensors detect the closest center object is %.1f meters away. ", sensorDepth) : "";

    String systemInstructions = "You are a spatial navigation expert for the blind. " +
            "Perform a wide-angle expanse analysis. " +
            sensorInfo +
            "Using the image and the provided sensor depth, describe the room layout and tell the user to move or turn left or right or forward. " +
            "Suggest the safest walking path only if one is clear. " +
            "IMPORTANT: If there is no clear path for the user or the way is fully blocked or a large object 1 meter according to the depth sensor provided is blocking, say 'STOP' and 'Turn around to look for a clearer path.'  " +
            "Be precise with distances. Limit to 25 words. " + languagePrompt;

    String base64Image = encodeImageToBase64(capturedImage);

    try {
      JSONObject jsonBody = new JSONObject();
      jsonBody.put("model", "meta-llama/llama-4-scout-17b-16e-instruct");

      JSONArray messages = new JSONArray();
      JSONObject userMessage = new JSONObject();
      userMessage.put("role", "user");

      JSONArray content = new JSONArray();
      content.put(new JSONObject().put("type", "text").put("text", systemInstructions));
      content.put(new JSONObject().put("type", "image_url")
              .put("image_url", new JSONObject().put("url", "data:image/jpeg;base64," + base64Image)));

      userMessage.put("content", content);
      messages.put(userMessage);
      jsonBody.put("messages", messages);

      RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.get("application/json; charset=utf-8"));
      Request request = new Request.Builder()
              .url("https://api.groq.com/openai/v1/chat/completions")
              .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
              .post(body)
              .build();

      httpClient.newCall(request).enqueue(new Callback() {
        @Override
        public void onResponse(Call call, Response response) throws IOException {
          if (response.isSuccessful()) {
            try {
              JSONObject resJson = new JSONObject(response.body().string());
              String aiResponse = resJson.getJSONArray("choices")
                      .getJSONObject(0).getJSONObject("message").getString("content");

              runOnUiThread(() -> {
                tts.speak(aiResponse);
                new Handler(Looper.getMainLooper()).postDelayed(() -> isSceneAnalysisMode = false, 6000);
              });
            } catch (Exception e) { e.printStackTrace(); }
          }
        }
        @Override
        public void onFailure(Call call, IOException e) {
          runOnUiThread(() -> { tts.speak("Connection error."); isSceneAnalysisMode = false; });
        }
      });
    } catch (Exception e) { e.printStackTrace(); }
  }
  private Bitmap getResizedBitmap(Bitmap image, int maxSize) {
    int width = image.getWidth();
    int height = image.getHeight();

    float bitmapRatio = (float) width / (float) height;
    if (bitmapRatio > 1) {
      width = maxSize;
      height = (int) (width / bitmapRatio);
    } else {
      height = maxSize;
      width = (int) (height * bitmapRatio);
    }
    return Bitmap.createScaledBitmap(image, width, height, true);
  }
  private String encodeImageToBase64(Bitmap bitmap) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
    byte[] b = baos.toByteArray();
    return Base64.encodeToString(b, Base64.NO_WRAP);
  }

  private float getCenterDepthMeters(Image depthImage) {
    Image.Plane plane = depthImage.getPlanes()[0];
    int width = depthImage.getWidth();
    int height = depthImage.getHeight();

    // Target the center pixel
    int x = width / 2;
    int y = height / 2;

    java.nio.ByteBuffer buffer = plane.getBuffer().order(java.nio.ByteOrder.nativeOrder());
    int byteIndex = x * plane.getPixelStride() + y * plane.getRowStride();

    // Depth is stored in millimeters in 16-bit
    int depthMillis = Short.toUnsignedInt(buffer.getShort(byteIndex));
    return depthMillis / 1000.0f;
  }
  private float computeRegionMedian(Image depthImage, float startXNorm, float startYNorm, float endXNorm, float endYNorm) {
    Image.Plane plane = depthImage.getPlanes()[0];
    java.nio.ByteBuffer buffer = plane.getBuffer();

    int width = depthImage.getWidth();
    int height = depthImage.getHeight();
    int startX = (int) (startXNorm * width);
    int startY = (int) (startYNorm * height);
    int endX = (int) (endXNorm * width);
    int endY = (int) (endYNorm * height);

    ArrayList<Float> validDepths = new ArrayList<>();

    for (int y = startY; y < endY; y += 4) { // sample every 4th pixel for speed
      for (int x = startX; x < endX; x += 4) {
        int index = y * width + x;
        if (index * 2 + 1 >= buffer.capacity()) continue; // safety check

        short depthShort = buffer.getShort(index * 2);
        float depthMeters = depthShort / 1000.0f; // convert mm to meters

        if (depthMeters > 0 && depthMeters < 10f) { // filter invalid
          validDepths.add(depthMeters);
        }
      }
    }

    if (validDepths.isEmpty()) {
      Log.d("DEPTH_DEBUG", "Column has no valid depth, treating as blocked");
      return -1f; // -1 indicates unknown
    }


    java.util.Collections.sort(validDepths);
    return validDepths.get(validDepths.size() / 2); // median
  }


  // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
  private void handleTap(Frame frame, Camera camera) {
    MotionEvent tap = tapHelper.poll();
    if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
      List<HitResult> hitResultList;
      if (instantPlacementSettings.isInstantPlacementEnabled()) {
        hitResultList =
            frame.hitTestInstantPlacement(tap.getX(), tap.getY(), APPROXIMATE_DISTANCE_METERS);
      } else {
        hitResultList = frame.hitTest(tap);
      }
      for (HitResult hit : hitResultList) {
        // If any plane, Oriented Point, or Instant Placement Point was hit, create an anchor.
        Trackable trackable = hit.getTrackable();
        // If a plane was hit, check that it was hit inside the plane polygon.
        // DepthPoints are only returned if Config.DepthMode is set to AUTOMATIC.
        if ((trackable instanceof Plane
                && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
            || (trackable instanceof Point
                && ((Point) trackable).getOrientationMode()
                    == OrientationMode.ESTIMATED_SURFACE_NORMAL)
            || (trackable instanceof InstantPlacementPoint)
            || (trackable instanceof DepthPoint)) {
          // Cap the number of objects created. This avoids overloading both the
          // rendering system and ARCore.
          if (wrappedAnchors.size() >= 20) {
            wrappedAnchors.get(0).getAnchor().detach();
            wrappedAnchors.remove(0);
          }

          // Adding an Anchor tells ARCore that it should track this position in
          // space. This anchor is created on the Plane to place the 3D model
          // in the correct position relative both to the world and to the plane.
          wrappedAnchors.add(new WrappedAnchor(hit.createAnchor(), trackable));
          // For devices that support the Depth API, shows a dialog to suggest enabling
          // depth-based occlusion. This dialog needs to be spawned on the UI thread.
          this.runOnUiThread(this::showOcclusionDialogIfNeeded);

          // Hits are sorted by depth. Consider only closest hit on a plane, Oriented Point, or
          // Instant Placement Point.
          break;
        }
      }
    }
  }

  /**
   * Shows a pop-up dialog on the first call, determining whether the user wants to enable
   * depth-based occlusion. The result of this dialog can be retrieved with useDepthForOcclusion().
   */
  private void showOcclusionDialogIfNeeded() {
    boolean isDepthSupported = session.isDepthModeSupported(Config.DepthMode.AUTOMATIC);
    if (!depthSettings.shouldShowDepthEnableDialog() || !isDepthSupported) {
      return; // Don't need to show dialog.
    }

    // Asks the user whether they want to use depth-based occlusion.
    new AlertDialog.Builder(this)
        .setTitle(R.string.options_title_with_depth)
        .setMessage(R.string.depth_use_explanation)
        .setPositiveButton(
            R.string.button_text_enable_depth,
            (DialogInterface dialog, int which) -> {
              depthSettings.setUseDepthForOcclusion(true);
            })
        .setNegativeButton(
            R.string.button_text_disable_depth,
            (DialogInterface dialog, int which) -> {
              depthSettings.setUseDepthForOcclusion(false);
            })
        .show();
  }

  private void launchInstantPlacementSettingsMenuDialog() {
    resetSettingsMenuDialogCheckboxes();
    Resources resources = getResources();
    new AlertDialog.Builder(this)
        .setTitle(R.string.options_title_instant_placement)
        .setMultiChoiceItems(
            resources.getStringArray(R.array.instant_placement_options_array),
            instantPlacementSettingsMenuDialogCheckboxes,
            (DialogInterface dialog, int which, boolean isChecked) ->
                instantPlacementSettingsMenuDialogCheckboxes[which] = isChecked)
        .setPositiveButton(
            R.string.done,
            (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
        .setNegativeButton(
            android.R.string.cancel,
            (DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
        .show();
  }

  /** Shows checkboxes to the user to facilitate toggling of depth-based effects. */
  private void launchDepthSettingsMenuDialog() {
    // Retrieves the current settings to show in the checkboxes.
    resetSettingsMenuDialogCheckboxes();

    // Shows the dialog to the user.
    Resources resources = getResources();
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
      // With depth support, the user can select visualization options.
      new AlertDialog.Builder(this)
          .setTitle(R.string.options_title_with_depth)
          .setMultiChoiceItems(
              resources.getStringArray(R.array.depth_options_array),
              depthSettingsMenuDialogCheckboxes,
              (DialogInterface dialog, int which, boolean isChecked) ->
                  depthSettingsMenuDialogCheckboxes[which] = isChecked)
          .setPositiveButton(
              R.string.done,
              (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
          .setNegativeButton(
              android.R.string.cancel,
              (DialogInterface dialog, int which) -> resetSettingsMenuDialogCheckboxes())
          .show();
    } else {
      // Without depth support, no settings are available.
      new AlertDialog.Builder(this)
          .setTitle(R.string.options_title_without_depth)
          .setPositiveButton(
              R.string.done,
              (DialogInterface dialogInterface, int which) -> applySettingsMenuDialogCheckboxes())
          .show();
    }
  }

  private void applySettingsMenuDialogCheckboxes() {
    depthSettings.setUseDepthForOcclusion(depthSettingsMenuDialogCheckboxes[0]);
    depthSettings.setDepthColorVisualizationEnabled(depthSettingsMenuDialogCheckboxes[1]);
    instantPlacementSettings.setInstantPlacementEnabled(
        instantPlacementSettingsMenuDialogCheckboxes[0]);
    configureSession();
  }

  private void resetSettingsMenuDialogCheckboxes() {
    depthSettingsMenuDialogCheckboxes[0] = depthSettings.useDepthForOcclusion();
    depthSettingsMenuDialogCheckboxes[1] = depthSettings.depthColorVisualizationEnabled();
    instantPlacementSettingsMenuDialogCheckboxes[0] =
        instantPlacementSettings.isInstantPlacementEnabled();
  }

  /** Checks if we detected at least one plane. */
  private boolean hasTrackingPlane() {
    for (Plane plane : session.getAllTrackables(Plane.class)) {
      if (plane.getTrackingState() == TrackingState.TRACKING) {
        return true;
      }
    }
    return false;
  }

  /** Update state based on the current frame's light estimation. */
  private void updateLightEstimation(LightEstimate lightEstimate, float[] viewMatrix) {
    if (lightEstimate.getState() != LightEstimate.State.VALID) {
      virtualObjectShader.setBool("u_LightEstimateIsValid", false);
      return;
    }
    virtualObjectShader.setBool("u_LightEstimateIsValid", true);

    Matrix.invertM(viewInverseMatrix, 0, viewMatrix, 0);
    virtualObjectShader.setMat4("u_ViewInverse", viewInverseMatrix);

    updateMainLight(
        lightEstimate.getEnvironmentalHdrMainLightDirection(),
        lightEstimate.getEnvironmentalHdrMainLightIntensity(),
        viewMatrix);
    updateSphericalHarmonicsCoefficients(
        lightEstimate.getEnvironmentalHdrAmbientSphericalHarmonics());
    cubemapFilter.update(lightEstimate.acquireEnvironmentalHdrCubeMap());
  }

  private void updateMainLight(float[] direction, float[] intensity, float[] viewMatrix) {
    // We need the direction in a vec4 with 0.0 as the final component to transform it to view space
    worldLightDirection[0] = direction[0];
    worldLightDirection[1] = direction[1];
    worldLightDirection[2] = direction[2];
    Matrix.multiplyMV(viewLightDirection, 0, viewMatrix, 0, worldLightDirection, 0);
    virtualObjectShader.setVec4("u_ViewLightDirection", viewLightDirection);
    virtualObjectShader.setVec3("u_LightIntensity", intensity);
  }

  private void updateSphericalHarmonicsCoefficients(float[] coefficients) {
    // Pre-multiply the spherical harmonics coefficients before passing them to the shader. The
    // constants in sphericalHarmonicFactors were derived from three terms:
    //
    // 1. The normalized spherical harmonics basis functions (y_lm)
    //
    // 2. The lambertian diffuse BRDF factor (1/pi)
    //
    // 3. A <cos> convolution. This is done to so that the resulting function outputs the irradiance
    // of all incoming light over a hemisphere for a given surface normal, which is what the shader
    // (environmental_hdr.frag) expects.
    //
    // You can read more details about the math here:
    // https://google.github.io/filament/Filament.html#annex/sphericalharmonics

    if (coefficients.length != 9 * 3) {
      throw new IllegalArgumentException(
          "The given coefficients array must be of length 27 (3 components per 9 coefficients");
    }

    // Apply each factor to every component of each coefficient
    for (int i = 0; i < 9 * 3; ++i) {
      sphericalHarmonicsCoefficients[i] = coefficients[i] * sphericalHarmonicFactors[i / 3];
    }
    virtualObjectShader.setVec3Array(
        "u_SphericalHarmonicsCoefficients", sphericalHarmonicsCoefficients);
  }

  /** Configures the session with feature settings. */
  private void configureSession() {
    Config config = session.getConfig();

    // Mute the AR fluff to save CPU for YOLO
    config.setLightEstimationMode(Config.LightEstimationMode.DISABLED);
    config.setPlaneFindingMode(Config.PlaneFindingMode.DISABLED); // NO MORE PLANES
    config.setInstantPlacementMode(Config.InstantPlacementMode.DISABLED);

    // Keep the core sensor active
    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
      config.setDepthMode(Config.DepthMode.AUTOMATIC);
    }

    // Ensure we get the latest frames without lag
    config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
    session.configure(config);
  }
  // Paste this at the bottom of HelloArActivity.java
  private float getDistanceAtPixel(Frame frame, float x, float y) {
    try (Image depthImage = frame.acquireDepthImage16Bits()) {
      // Convert screen coordinates to depth image coordinates
      int width = depthImage.getWidth();
      int height = depthImage.getHeight();

      // Ensure coordinates are within image bounds
      int cpuX = (int) (x * width / 640f);
      int cpuY = (int) (y * height / 640f);

      if (cpuX < 0 || cpuX >= width || cpuY < 0 || cpuY >= height) return -1f;

      // Get the depth value (in millimeters) from the 16-bit plane
      Image.Plane plane = depthImage.getPlanes()[0];
      ShortBuffer depthBuffer = plane.getBuffer().order(ByteOrder.nativeOrder()).asShortBuffer();

      int index = cpuY * (plane.getRowStride() / 2) + cpuX;
      short depthSample = depthBuffer.get(index);

      // Convert millimeters to meters
      return (depthSample & 0xFFFF) / 1000.0f;

    } catch (Exception e) {
      return -1.0f;
    }

  }
    private float getDistanceFromCapturedImage(Image depthImage, float x, float y) {
      try {
        int width = depthImage.getWidth();
        int height = depthImage.getHeight();

        // Since we center-cropped the camera, we must sample the center-crop of the depth map
        // 1. Determine the square area of the depth map
        int size = Math.min(width, height);
        int offsetX = (width - size) / 2;
        int offsetY = (height - size) / 2;

        // 2. Map the 640x640 AI coordinate to that square area
        int cpuX = offsetX + (int) (x * size / 640f);
        int cpuY = offsetY + (int) (y * size / 640f);

        if (cpuX < 0 || cpuX >= width || cpuY < 0 || cpuY >= height) return -1f;

        Image.Plane plane = depthImage.getPlanes()[0];
        ShortBuffer depthBuffer = plane.getBuffer().order(ByteOrder.nativeOrder()).asShortBuffer();

        int index = cpuY * (plane.getRowStride() / 2) + cpuX;
        short depthSample = depthBuffer.get(index);

        return (depthSample & 0xFFFF) / 1000.0f; // Distance in Meters
      } catch (Exception e) {
        return -1.0f;
      }
    }


}

/**
 * Associates an Anchor with the trackable it was attached to. This is used to be able to check
 * whether or not an Anchor originally was attached to an {@link InstantPlacementPoint}.
 */
class WrappedAnchor {
  private Anchor anchor;
  private Trackable trackable;

  public WrappedAnchor(Anchor anchor, Trackable trackable) {
    this.anchor = anchor;
    this.trackable = trackable;
  }

  public Anchor getAnchor() {
    return anchor;
  }

  public Trackable getTrackable() {
    return trackable;
  }
}
