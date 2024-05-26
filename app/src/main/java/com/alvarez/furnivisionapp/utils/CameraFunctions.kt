package com.alvarez.furnivisionapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.graphics.drawable.BitmapDrawable
import android.hardware.camera2.*
import android.media.ExifInterface
import android.media.ImageReader
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import com.alvarez.furnivisionapp.R
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CameraFunctions(
    private val cameraLayout: RelativeLayout,
    private val galleryLayout: RelativeLayout,
    private val galleryImageView: ImageView,
    private val galleryBtn: Button,
    private val galleryBtn2: Button,
    private val cameraBtn: Button,
    private val cameraBtn2: Button,
    private val captureButton: Button,
    private val galleryPrevBtn: Button,
    private val galleryNextBtn: Button,
    private val filesDir: File,
    private val textureView: TextureView,
    private val cameraManager: CameraManager,
    private val context: Context
) {

    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var cameraDevice: CameraDevice
    private lateinit var imageReader: ImageReader
    private lateinit var backgroundHandler: Handler
    private lateinit var backgroundThread: HandlerThread
    private var currentImageIndex: Int = 0
    private lateinit var imageFiles: Array<File>

    private var activeLayout: RelativeLayout = cameraLayout

    init {
        initializeCamera()
    }

    private fun initializeCamera() {
        setupBackgroundThread()
        setupTextureViewListener()
        setupImageReader()
        setupButtons()
    }

    private fun setupBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackgroundThread").apply { start() }
        backgroundHandler = Handler(backgroundThread.looper)
    }

    private fun setupTextureViewListener() {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = true

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private fun setupImageReader() {
        imageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1).apply {
            setOnImageAvailableListener({ reader ->
                reader?.acquireLatestImage()?.use { image ->
                    val buffer = image.planes[0].buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    saveImage(bytes)
                }
            }, backgroundHandler)
        }
    }

    private fun setupButtons() {
        val galleryButtons = arrayOf(galleryBtn, galleryBtn2)
        val cameraButtons = arrayOf(cameraBtn, cameraBtn2)

        galleryButtons.forEach { it.setOnClickListener { switchLayout(galleryLayout); displayImagesInGallery() } }
        cameraButtons.forEach { it.setOnClickListener { switchLayout(cameraLayout) } }

        galleryPrevBtn.setOnClickListener { navigateGallery(-1) }
        galleryNextBtn.setOnClickListener { navigateGallery(1) }
        captureButton.setOnClickListener { captureImage() }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        try {
            cameraManager.openCamera(cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCameraPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    cameraDevice.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    cameraDevice.close()
                    Log.e("CameraError", "Error code: $error")
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e("CameraAccessException", "Error accessing camera: ${e.message}")
        }
    }

    private fun createCameraPreviewSession() {
        try {
            val surface = Surface(textureView.surfaceTexture)
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(surface)
            }

            cameraDevice.createCaptureSession(listOf(surface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraCaptureSession = session
                    cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler)
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    Log.e("CameraPreviewSession", "Configuration failed")
                }
            }, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e("CameraAccessException", "Error creating camera preview session: ${e.message}")
        }
    }

    private fun captureImage() {
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(imageReader.surface)
            }
            cameraCaptureSession.capture(captureRequestBuilder.build(), null, backgroundHandler)
        } catch (e: CameraAccessException) {
            Log.e("CameraAccessException", "Error capturing image: ${e.message}")
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun saveImage(bytes: ByteArray) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_$timeStamp.jpg"

        val storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "YourAppGalleryFolder")
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }

        val imageFile = File(storageDir, imageFileName)

        try {
            FileOutputStream(imageFile).use { output ->
                output.write(bytes)
                // Notify the system about the new image file
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(imageFile.absolutePath),
                    arrayOf("image/jpeg"),
                    null
                )
                // Show a toast message
                Toast.makeText(context, "Image saved to app's gallery", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            Log.e("SaveImage", "Failed to save image: ${e.message}")
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateGallery(direction: Int) {
        val storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "YourAppGalleryFolder")
        val imageFiles = storageDir.listFiles { file -> file.isFile && file.name.endsWith(".jpg") } ?: return

        if (imageFiles.isNotEmpty()) {
            currentImageIndex += direction
            currentImageIndex = currentImageIndex.coerceIn(0, imageFiles.size - 1)
            val newImageFile = imageFiles[currentImageIndex]

            Glide.with(context)
                .load(newImageFile)
                .into(galleryImageView)
        }
    }

    private fun displayImagesInGallery() {
        val storageDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "YourAppGalleryFolder")
        imageFiles = storageDir.listFiles { file -> file.isFile && file.name.endsWith(".jpg") } ?: return

        if (imageFiles.isNotEmpty()) {
            val latestImageFile = imageFiles.last()
            val bitmap = BitmapFactory.decodeFile(latestImageFile.absolutePath)
            galleryImageView.setImageBitmap(bitmap)
        } else {
            // If there are no images in the folder, you can set a placeholder image
            galleryImageView.setImageResource(R.drawable.cam_placeholder_image)
        }
    }

    private fun switchLayout(layout: RelativeLayout) {
        activeLayout.visibility = View.GONE
        activeLayout = layout.apply { visibility = View.VISIBLE }
    }
}