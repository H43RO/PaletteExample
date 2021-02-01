package com.haero_kim.paletteexample

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.GridView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.palette.graphics.Palette
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

class ImageSplitActivity : AppCompatActivity() {

    private lateinit var splitImageButton: Button
    private lateinit var gridView: GridView
    private lateinit var colorGridView: GridView
    private var palette: Palette? = null
    private var vibrantSwatch: Palette.Swatch? = null
    private var colorMatrix: ArrayList<Int> = arrayListOf()

    private lateinit var section1TextView: TextView
    private lateinit var section2TextView: TextView
    private lateinit var section3TextView: TextView
    private lateinit var section4TextView: TextView
    private lateinit var section5TextView: TextView

    private lateinit var feedBackTextView: TextView

    private lateinit var chunkedImages: ArrayList<Bitmap>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_split)

        splitImageButton = findViewById(R.id.split_image_button)
        gridView = findViewById(R.id.grid_view)
        colorGridView = findViewById(R.id.color_grid_view)

        section1TextView = findViewById(R.id.section1)
        section2TextView = findViewById(R.id.section2)
        section3TextView = findViewById(R.id.section3)
        section4TextView = findViewById(R.id.section4)
        section5TextView = findViewById(R.id.section5)

        feedBackTextView = findViewById(R.id.feedback)

        splitImageButton.setOnClickListener {
            CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setActivityTitle("이미지 선택")
                .setCropShape(CropImageView.CropShape.RECTANGLE)
                .setCropMenuCropButtonTitle("완료")
                .setRequestedSize(1920, 1080)
                .start(this)
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

                CoroutineScope(Default).launch {
                    chunkedImages = splitImage(loadedBitmap)
                    withContext(Main) {
                        gridView.adapter = GridViewAdapter(
                            context = applicationContext,
                            imageArrayList = chunkedImages
                        )
                    }

                    val dangerSection = analyzeImage(chunkedImages)
                    Log.d("DangerSection", "Analyzing Success!")
                    withContext(Main) {
                        section1TextView.text = dangerSection[0].toString()
                        section2TextView.text = dangerSection[1].toString()
                        section3TextView.text = dangerSection[2].toString()
                        section4TextView.text = dangerSection[3].toString()
                        section5TextView.text = dangerSection[4].toString()

                        colorGridView.adapter = ColorGridViewAdapter(
                            context = applicationContext,
                            imageArrayList = colorMatrix
                        )

                        var minSection: Int = 0
                        dangerSection.forEachIndexed { index, i ->
                            if (dangerSection[minSection] > i) {
                                minSection = index
                            }
                        }

                        when (minSection) {
                            0 -> feedBackTextView.text = "왼쪽 끝으로 향하세요"
                            1 -> feedBackTextView.text = "조금 왼쪽으로 향하세요"
                            2 -> feedBackTextView.text = "직진하세요"
                            3 -> feedBackTextView.text = "조금 오른쪽으로 향하세요"
                            4 -> feedBackTextView.text = "오른쪽 끝으로 향하세요"
                        }
                    }
                }

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Log.e("Error Image Selecting", "이미지 선택 및 편집 오류")
            }
        }
    }

    // 이미지 분할을 위한 상수 선언 (행렬 사이즈, 분할 사이즈 등 정의)
    companion object {
        const val ROWS = 6
        const val COLS = 8

        const val IMAGE_HEIGHT = 1080
        const val IMAGE_WIDTH = 1920

        const val CHUNK_HEIGHT = IMAGE_HEIGHT / ROWS    // 180
        const val CHUNK_WIDTH = IMAGE_WIDTH / COLS      // 240

        const val TOTAL_CHUNK = 48

        const val COLOR_RED = -524288
    }

    suspend fun splitImage(bitmap: Bitmap): ArrayList<Bitmap> {
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, IMAGE_WIDTH, IMAGE_HEIGHT, true)
        val chunkedImages: ArrayList<Bitmap> = ArrayList(TOTAL_CHUNK)
        var yCoord = 0
        for (x in 0 until ROWS) {
            var xCoord = 0
            for (y in 0 until COLS) {
                chunkedImages.add(
                    Bitmap.createBitmap(
                        scaledBitmap,
                        xCoord,
                        yCoord,
                        CHUNK_WIDTH,
                        CHUNK_HEIGHT
                    )
                )
                xCoord += CHUNK_WIDTH
            }
            yCoord += CHUNK_HEIGHT
        }
        return chunkedImages
    }

    suspend fun analyzeImage(chunkedImages: ArrayList<Bitmap>): ArrayList<Int> {
        val dangerSection: ArrayList<Int> = arrayListOf(0, 0, 0, 0, 0)
        colorMatrix.clear()
        for (bitmap in chunkedImages) {
            Palette.from(bitmap).generate(Palette.PaletteAsyncListener {
                it?.let {
                    palette = it
                    vibrantSwatch = it.vibrantSwatch
                }

                vibrantSwatch?.let {
                    colorMatrix.add(it.rgb)
                    val index = colorMatrix.size - 1

                    Log.d("ColorMatrix", it.rgb.toString())
                    Log.d("ColorMatrix", (colorMatrix.size - 1).toString())

                    if (it.rgb == COLOR_RED) {  // 해당 Chunk 가 RED 색상이라면
                        Log.d("RedIndex", index.toString())
                        when (index) {
                            0, 8, 16, 24, 32, 40 -> dangerSection[0]++
                            1, 2, 9, 10, 17, 18, 25, 26, 33, 34, 41, 42 -> dangerSection[1]++
                            3, 4, 11, 12, 19, 20, 27, 28, 35, 36, 43, 44 -> dangerSection[2]++
                            5, 6, 13, 14, 21, 22, 29, 30, 37, 38, 45, 46 -> dangerSection[3]++
                            7, 15, 23, 31, 39, 47 -> dangerSection[4]++
                            else -> Log.d("Index Error", "Not RED! : $index")
                        }
                    }
                }
            })
            Thread.sleep(8)
        }
        Log.d("Test", dangerSection.toString())
        return dangerSection
    }
}