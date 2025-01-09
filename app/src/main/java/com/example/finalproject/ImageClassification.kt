package com.example.finalproject

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.text.Spanned
import android.text.style.StyleSpan
import android.graphics.Typeface
import androidx.core.content.ContextCompat

class ImageClassification : AppCompatActivity() {



    private lateinit var imageView: ImageView
    private lateinit var resultTextView: TextView
    private lateinit var captureButton: ImageButton
    private lateinit var interpreter: Interpreter
    private lateinit var labels: List<String>
    private lateinit var photoFile: File
    private lateinit var thresholdSeekBar: SeekBar
    private lateinit var thresholdLabel: TextView
    private var probabilityThreshold: Float = 0.1f // начальное значение порога

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.d("Here", "Here")

        // Инициализация элементов UI
        imageView = findViewById(R.id.imageView)
        resultTextView = findViewById(R.id.resultTextView)
        captureButton = findViewById(R.id.captureButton)
        thresholdSeekBar = findViewById(R.id.thresholdSeekBar)
        thresholdLabel = findViewById(R.id.thresholdLabel)






        // Запрашиваем разрешение на использование камеры
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            1
        )
        Log.d("Here", "Here")
        // Загрузка модели
        interpreter = Interpreter(loadModelFile("1.tflite"))

        // Загрузка меток классов
        labels = loadLabels("labels.txt")

        // Настройка SeekBar для выбора порога
        thresholdSeekBar.progress = (probabilityThreshold * 100).toInt()
        thresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                probabilityThreshold = progress / 100.0f // перевод в диапазон 0.0 - 1.0
                thresholdLabel.text = "Threshold: ${"%.1f".format(probabilityThreshold)}"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Обработчик нажатия на кнопку "Сделать фото"
        captureButton.setOnClickListener {
            openCamera()
        }
    }

    // Открыть камеру для захвата изображения
    private fun openCamera() {
        photoFile = createImageFile()
        val photoUri: Uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            photoFile
        )
        val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri)
        }
        cameraLauncher.launch(cameraIntent)
    }

    // Обработчик результата камеры
    private val cameraLauncher =
        registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                imageView.setImageBitmap(bitmap)

                classifyImage(bitmap)
            } else {
                Toast.makeText(this, "Камера закрыта без снимка", Toast.LENGTH_SHORT).show()
            }
        }

    // Создание файла для хранения снимка
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "IMG_", /* префикс */
            ".jpg", /* суффикс */
            storageDir /* директория */
        )
    }

    // Классификация изображения
    private fun classifyImage(bitmap: Bitmap) {
        val inputData = preprocessImage(bitmap)

        // Преобразование в формат [1, 224, 224, 3]
        val inputTensor = Array(1) { Array(224) { Array(224) { FloatArray(3) } } }
        var index = 0
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                inputTensor[0][y][x][0] = inputData[index++] // Красный
                inputTensor[0][y][x][1] = inputData[index++] // Зеленый
                inputTensor[0][y][x][2] = inputData[index++] // Синий
            }
        }

        val outputData = Array(1) { FloatArray(1001) } // Пример для 1000 классов
        interpreter.run(inputTensor, outputData)

        // Получение нескольких лучших результатов с порогом вероятности
        val topResults = getTopResults(outputData[0], probabilityThreshold)



// Формирование строки для отображения
        val resultText = SpannableStringBuilder()
        for ((label, confidence) in topResults) {
            // Форматирование строки для класса
            val labelText = label
            val labelStart = resultText.length
            resultText.append(labelText)
            val labelEnd = resultText.length
            resultText.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(resultTextView.context, R.color.labelColor)), // Используем context из TextView
                labelStart,
                labelEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            resultText.setSpan(
                StyleSpan(Typeface.BOLD),
                labelStart,
                labelEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Добавление пробела для отступа между классом и вероятностью
            resultText.append(" - ")

            // Форматирование строки для вероятности
            val confidenceText = "${"%.0f".format(confidence * 100)}%"
            val confidenceStart = resultText.length
            resultText.append(confidenceText)
            val confidenceEnd = resultText.length
            resultText.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(resultTextView.context, R.color.confidenceColor)), // Используем context из TextView
                confidenceStart,
                confidenceEnd,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Добавление новой строки
            resultText.append("\n")
        }

// Отображение результатов
        resultTextView.text = resultText

    }

    // Получение нескольких лучших результатов с порогом вероятности
    private fun getTopResults(output: FloatArray, threshold: Float): List<Pair<String, Float>> {
        val results = mutableListOf<Pair<String, Float>>()
        for (i in output.indices) {
            if (output[i] >= threshold) {
                val label = if (i in labels.indices) labels[i] else "Неизвестный класс"
                results.add(Pair(label, output[i]))
            }
        }
        // Сортировать по убыванию вероятности
        return results.sortedByDescending { it.second }
    }

    // Предобработка изображения
    private fun preprocessImage(bitmap: Bitmap): FloatArray {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true) // Размер 224x224 подходит для многих моделей
        val input = FloatArray(224 * 224 * 3)
        var index = 0
        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = resizedBitmap.getPixel(x, y)
                input[index++] = (pixel shr 16 and 0xFF) / 255.0f // Красный
                input[index++] = (pixel shr 8 and 0xFF) / 255.0f  // Зеленый
                input[index++] = (pixel and 0xFF) / 255.0f       // Синий
            }
        }
        return input
    }

    private fun loadModelFile(modelName: String): MappedByteBuffer {
        val assetFileDescriptor = assets.openFd(modelName)
        val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = fileInputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            assetFileDescriptor.startOffset,
            assetFileDescriptor.declaredLength
        )
    }

    private fun loadLabels(labelFileName: String): List<String> {
        val labels = mutableListOf<String>()
        assets.open(labelFileName).bufferedReader().useLines { lines ->
            lines.forEach { labels.add(it) }
        }
        return labels
    }

//    fun onClick2(view: View) {
//        val intent = Intent(this, ImageClassification::class.java)
//        startActivity(intent)
//    }

}