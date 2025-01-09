package com.example.finalproject


import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity2 : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var resultText: TextView
    private lateinit var cameraExecutor: ExecutorService
    private var tfliteInterpreter: Interpreter? = null
    private lateinit var labels: List<String> // Список лейблов

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        previewView = findViewById(R.id.previewView)
        resultText = findViewById(R.id.resultText)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Проверка разрешения камеры
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            setupCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 100)
        }

        // Загружаем модель
        loadModel()



        if (tfliteInterpreter != null) {
            Log.d("MainActivity2", "Модель успешно загружена")
        } else {
            Log.e("MainActivity2", "Ошибка загрузки модели")
            resultText.text = "Ошибка загрузки модели"
        }

        // Загрузка лейблов
        labels = loadLabels("labels.txt")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupCamera()
        } else {
            Toast.makeText(this, "Камера не доступна без разрешений", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadModel() {
        try {
            Log.d("MainActivity2", "Загрузка модели")
            tfliteInterpreter = Interpreter(loadModelFile("1.tflite"))
            Log.d("MainActivity2", "Модель успешно загружена")
        } catch (e: Exception) {
            Log.e("MainActivity2", "Ошибка загрузки модели", e)
        }
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

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(224, 224))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { image ->
                    Log.d("MainActivity2", "Анализ изображения")
                    processImage(image)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (e: Exception) {
                Log.e("MainActivity2", "Ошибка настройки камеры", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun processImage(imageProxy: ImageProxy) {
        System.out.println("ImageProxy: " + imageProxy)
        val bitmap = imageProxyToBitmap(imageProxy)
        if (bitmap != null) {
            Log.d("MainActivity2", "Изображение получено")
            runInference(bitmap)
        } else {
            Log.e("MainActivity2", "Ошибка при преобразовании изображения: bitmap == null")
        }
        imageProxy.close()
    }

    @OptIn(ExperimentalGetImage::class)
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: run {
            Log.e("MainActivity2", "Ошибка при получении изображения из imageProxy: image == null")
            return null
        }
        Log.d("MainActivity2", "Here1")
        val planes = image.planes
        if (planes.isEmpty()) {
            Log.e("MainActivity2", "Ошибка: Нет доступных планов для изображения")
            return null
        }
        Log.d("MainActivity2", "Here1")
        val yPlane = planes[0]
        val uvPlane = planes[1]

        Log.d("MainActivity2", "Размер буфера Y: ${yPlane.buffer.remaining()}, UV: ${uvPlane.buffer.remaining()}")

        val yuvBytes = ByteArray(yPlane.buffer.remaining() + uvPlane.buffer.remaining())
        yPlane.buffer.get(yuvBytes, 0, yPlane.buffer.remaining())
        uvPlane.buffer.get(yuvBytes, yPlane.buffer.remaining(), uvPlane.buffer.remaining())

        try {
            val yuvImage = YuvImage(yuvBytes, ImageFormat.NV21, image.width, image.height, null)
            val outputStream = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, outputStream)
            val byteArray = outputStream.toByteArray()
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (e: Exception) {
            Log.e("MainActivity2", "Ошибка при преобразовании YUV в Bitmap", e)
            return null
        } finally {
            image.close()
        }
    }



    private fun runInference(bitmap: Bitmap) {
        Log.d("MainActivity2", "Запуск инференса")
        val inputBuffer = preprocessInput(bitmap)

        val outputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 1001), DataType.FLOAT32)
        tfliteInterpreter?.run(inputBuffer.buffer, outputBuffer.buffer)

        val results = outputBuffer.floatArray
        Log.d("MainActivity2", "Инференс завершен")
        processResults(results)
    }

    private fun preprocessInput(bitmap: Bitmap): TensorBuffer {
        Log.d("MainActivity2", "Предобработка изображения")
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val inputBuffer = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        val buffer = ByteBuffer.allocateDirect(224 * 224 * 3 * 4).order(ByteOrder.nativeOrder())

        for (y in 0 until 224) {
            for (x in 0 until 224) {
                val pixel = scaledBitmap.getPixel(x, y)
                buffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f) // R
                buffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)  // G
                buffer.putFloat((pixel and 0xFF) / 255.0f)         // B
            }
        }

        inputBuffer.loadBuffer(buffer)
        Log.d("MainActivity2", "Изображение предобработано")

        return inputBuffer
    }




//    private fun processResults(results: FloatArray) {
//        Log.d("MainActivity2", "Обработка результатов")
//        val maxIndex = results.indices.maxByOrNull { results[it] } ?: -1
//        val maxProbability = results[maxIndex]
//        Log.d("MainActivity2", "Результат: Класс $maxIndex, Вероятность: $maxProbability")
//
//        runOnUiThread {
//            resultText.text = "Класс: $maxIndex\nВероятность: $maxProbability"
//        }
//    }

    private fun processResults(results: FloatArray) {
        Log.d("MainActivity2", "Обработка результатов")
        val maxIndex = results.indices.maxByOrNull { results[it] } ?: -1
        val maxProbability = results[maxIndex]

        // Проверяем, есть ли лейблы и соответствуют ли они индексам
        val label = if (maxIndex in labels.indices) labels[maxIndex] else "Неизвестный класс"

        Log.d("MainActivity2", "Результат: Лейбл $label, Индекс: ${maxIndex}")

        runOnUiThread {
            resultText.text = "Лейбл: $label\nВероятность: ${"%.2f".format(maxProbability)}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun loadLabels(labelFileName: String): List<String> {
        val labels = mutableListOf<String>()
        assets.open(labelFileName).bufferedReader().useLines { lines ->
            lines.forEach { labels.add(it) }
        }
        return labels
    }

}


