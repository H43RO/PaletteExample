package com.haero_kim.paletteexample

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.palette.graphics.Palette
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.String

class MainActivity : AppCompatActivity() {

    private var palette: Palette? = null
    private var vibrantSwatch: Palette.Swatch? = null

    private lateinit var imageUri: Uri
    private lateinit var vibrantTextView: TextView
    private lateinit var vibrantIntTextView: TextView
    private lateinit var vibrantColorImageView: ImageView
    private lateinit var loadedImageView: ImageView
    private lateinit var changeImageButton: Button
    private lateinit var splitImageButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vibrantTextView = findViewById(R.id.vibrant_color_text_view)
        vibrantIntTextView = findViewById(R.id.vibrant_int_text_view)
        vibrantColorImageView = findViewById(R.id.vibrant_color_image_view)
        changeImageButton = findViewById(R.id.change_image_button)
        loadedImageView = findViewById(R.id.loaded_image_view)
        splitImageButton = findViewById(R.id.split_image_button)

        changeImageButton.setOnClickListener {
            CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setActivityTitle("이미지 선택")
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setCropMenuCropButtonTitle("완료")
                .setRequestedSize(1920, 1080)
                .start(this)
        }

        splitImageButton.setOnClickListener {
            startActivity(Intent(this, ImageSplitActivity::class.java))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 업로드를 위한 사진이 선택 및 편집되면 Uri 형태로 결과가 반환됨
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)

            if (resultCode == Activity.RESULT_OK) {
                val resultUri = result.uri
                val loadedBitmap =
                        MediaStore.Images.Media.getBitmap(this.contentResolver, resultUri)
                imageUri = bitmapToFile(loadedBitmap!!) // Uri
                loadedImageView.setImageBitmap(loadedBitmap)

                Palette.from(loadedBitmap).generate(Palette.PaletteAsyncListener {
                    it?.let {
                        palette = it
                        vibrantSwatch = it.vibrantSwatch
                    }

                    vibrantSwatch?.let {
                        val hexColor = String.format("#%06X", 0xFFFFFF and it.rgb)

                        vibrantColorImageView.setBackgroundColor(it.rgb)
                        vibrantTextView.setTextColor(it.rgb)
                        vibrantIntTextView.setText(it.rgb.toString())
                        vibrantTextView.text = it.rgb.toString()
                        vibrantTextView.text = hexColor
                    }

                })

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Log.e("Error Image Selecting", "이미지 선택 및 편집 오류")
            }
        }
    }

    /**  Bitmap 이미지를 Local에 저장하고, URI를 반환함  **/
    private fun bitmapToFile(bitmap: Bitmap): Uri {
        val wrapper = ContextWrapper(this)

        // Bitmap 파일 저장을 위한 File 객체
        var file = wrapper.getDir("Images", Context.MODE_PRIVATE)
        file = File(file, "write_image.jpg")

        try {
            // Bitmap 파일을 JPEG 형태로 압축해서 출력
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("Error Saving Image", e.message)
        }

        return Uri.parse(file.absolutePath)
    }
}