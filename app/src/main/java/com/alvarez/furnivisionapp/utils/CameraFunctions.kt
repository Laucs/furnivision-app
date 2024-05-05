package com.alvarez.furnivisionapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.graphics.drawable.BitmapDrawable
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import com.alvarez.furnivisionapp.R

class CameraFunctions(
    private val cameraLayout: RelativeLayout,
    private val galleryLayout: RelativeLayout,
    private val videoLayout: RelativeLayout,
    private val galleryImageView: ImageView,
    private val galleryBtn: Button,
    private val galleryBtn2: Button,
    private val cameraBtn: Button,
    private val cameraBtn2: Button,
    private val captureButton: Button,
    private val videoBtn: Button,
    private val galleryPrevBtn: Button,
    private val galleryNextBtn: Button,
    private val vidTextureView: TextureView,
    private val vidRecordButton: Button,
    private val filesDir: File,
    private val textureView: TextureView,
    private val cameraManager: CameraManager,
    private val context: Context
) {

    lateinit var capReq: CaptureRequest.Builder
    lateinit var handler: Handler
    private lateinit var handlerThread: HandlerThread
    lateinit var cameraCaptureSession: CameraCaptureSession
    lateinit var cameraDevice: CameraDevice

    lateinit var imageReader: ImageReader
    private val capturedImages = mutableListOf<String>()
    private lateinit var activeLayout: RelativeLayout

    init {
        initializeCamera()
    }

    fun initializeCamera() {
        var imagePath:String?
        activeLayout = cameraLayout

        galleryBtn.setOnClickListener {
            switchLayout(galleryLayout)
            displayImagesInGallery()
        }

        galleryBtn2.setOnClickListener {
            switchLayout(galleryLayout)
            displayImagesInGallery()
        }

        cameraBtn.setOnClickListener {
            switchLayout(cameraLayout)
        }

        cameraBtn2.setOnClickListener {
            switchLayout(cameraLayout)
        }

        videoBtn.setOnClickListener {
            switchLayout(videoLayout)
        }

        galleryPrevBtn.setOnClickListener {
            val directory = File(filesDir, "Pictures")
            val imageFiles = directory.listFiles { file ->
                file.isFile && file.name.endsWith(".jpeg")
            }

            if (imageFiles != null && imageFiles.isNotEmpty()) {
                val currentImageFile = getCurrentImageFile()
                val currentIndex = imageFiles.indexOf(currentImageFile)

                if (currentIndex > 0) {
                    val previousImageFile = imageFiles[currentIndex - 1]
                    val bitmap = BitmapFactory.decodeFile(previousImageFile.absolutePath)
                    galleryImageView.setImageBitmap(bitmap)
                }
            }
        }

        galleryNextBtn.setOnClickListener {
            val directory = File(filesDir, "Pictures")
            val imageFiles = directory.listFiles { file ->
                file.isFile && file.name.endsWith(".jpeg")
            }

            if (imageFiles != null && imageFiles.isNotEmpty()) {
                val currentImageFile = getCurrentImageFile()
                val currentIndex = imageFiles.indexOf(currentImageFile)

                if (currentIndex < imageFiles.size - 1) {
                    val nextImageFile = imageFiles[currentIndex + 1]
                    val bitmap = BitmapFactory.decodeFile(nextImageFile.absolutePath)
                    galleryImageView.setImageBitmap(bitmap)
                }
            }
        }

        handlerThread= HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler((handlerThread).looper)

        textureView.surfaceTextureListener = object: TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

            }
        }

        imageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener({ p0: ImageReader? ->

            var image = p0?.acquireLatestImage()
            var buffer = image!!.planes[0].buffer
            var bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val directory = File(filesDir, "Pictures")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            var file = File(directory, "img.jpeg")
            var opStream = FileOutputStream(file)
            imagePath = file.absolutePath

            Log.d("FilePath", "File Path: $imagePath")
            if (imagePath != null) {
                opStream.write(bytes)
                opStream.close()
                image.close()
                Toast.makeText(context, "Image captured", Toast.LENGTH_SHORT).show()
            } else {
                Log.e("FilePath", "Error: File path is null")
                Toast.makeText(context, "Error capturing image", Toast.LENGTH_SHORT).show()
            }
        }, handler)

        captureButton.apply {
            setOnClickListener{
                capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                capReq.addTarget(imageReader.surface)
                cameraCaptureSession.capture(capReq.build(), null, null)

            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun open_camera(){
        cameraManager.openCamera(cameraManager.cameraIdList[0], object: CameraDevice.StateCallback(){
            override fun onOpened(p0: CameraDevice) {
                cameraDevice = p0
                capReq = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                var surface = Surface(textureView.surfaceTexture)
                capReq.addTarget(surface)

                cameraDevice.createCaptureSession(listOf(surface, imageReader.surface), object: CameraCaptureSession.StateCallback(){
                    override fun onConfigured(p0: CameraCaptureSession) {
                        cameraCaptureSession = p0
                        cameraCaptureSession.setRepeatingRequest(capReq.build(), null, null)
                    }

                    override fun onConfigureFailed(p0: CameraCaptureSession) {

                    }
                }, handler)
            }

            override fun onDisconnected(p0: CameraDevice) {

            }

            override fun onError(p0: CameraDevice, p1: Int) {

            }
        }, handler)
    }

    private fun displayImagesInGallery() {
        val directory = File(filesDir, "Pictures")
        val imageFiles = directory.listFiles { file ->
            file.isFile && file.name.endsWith(".jpeg")
        }

        if (imageFiles != null && imageFiles.isNotEmpty()) {
            val latestImageFile = imageFiles.last()
            val bitmap = BitmapFactory.decodeFile(latestImageFile.absolutePath)
            galleryImageView.setImageBitmap(bitmap)
        } else {
            galleryImageView.setImageResource(R.drawable.cam_placeholder_image)
        }
    }

    private fun getCurrentImageFile(): File? {
        val directory = File(filesDir, "Pictures")
        val imageFiles = directory.listFiles { file ->
            file.isFile && file.name.endsWith(".jpeg")
        }

        if (imageFiles != null && imageFiles.isNotEmpty()) {
            val currentImage = (galleryImageView.drawable as BitmapDrawable).bitmap
            for (imageFile in imageFiles) {
                val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                if (bitmap.sameAs(currentImage)) {
                    return imageFile
                }
            }
        }

        return null
    }

    private fun switchLayout(layout: RelativeLayout){
        activeLayout.visibility = View.GONE
        if (layout == cameraLayout){
            activeLayout = cameraLayout
            cameraLayout.visibility = View.VISIBLE
        }
        else if (layout == galleryLayout){
            activeLayout = galleryLayout
            galleryLayout.visibility = View.VISIBLE
        }
        else{
            activeLayout = videoLayout
            videoLayout.visibility = View.VISIBLE
        }
    }

}