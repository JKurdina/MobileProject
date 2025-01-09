package com.example.finalproject

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity


import android.view.View

import com.bumptech.glide.Glide


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_classification)

        // Найти элементы интерфейса
        val textView1 = findViewById<View>(R.id.textView1)
        val textView2 = findViewById<View>(R.id.textView2)
        val button1 = findViewById<View>(R.id.button1)
        val button2 = findViewById<View>(R.id.button2)
        val button3 = findViewById<View>(R.id.button3)
        val button4 = findViewById<View>(R.id.button4)
        val gifView = findViewById<ImageView>(R.id.gifView)
        val gifView2 = findViewById<ImageView>(R.id.gifView2)

        // Убедимся, что элементы изначально невидимы
        listOf(textView1, textView2, button1, button2, button3, button4, gifView, gifView2).forEach {
            it.alpha = 0f
            it.visibility = View.VISIBLE // Делаем видимыми для анимации
        }

        // Инициализация анимаций
        val animations = listOf(
            ObjectAnimator.ofFloat(textView1, "alpha", 0f, 1f).setDuration(1000),
            ObjectAnimator.ofFloat(textView2, "alpha", 0f, 1f).setDuration(1000),
            ObjectAnimator.ofFloat(button1, "alpha", 0f, 1f).setDuration(500),
            ObjectAnimator.ofFloat(button2, "alpha", 0f, 1f).setDuration(500),
            ObjectAnimator.ofFloat(button3, "alpha", 0f, 1f).setDuration(500),
            ObjectAnimator.ofFloat(button4, "alpha", 0f, 1f).setDuration(500)
        )

        // Создание анимационного набора
        val animatorSet = AnimatorSet().apply {
            playSequentially(animations)
        }

        // Слушатель окончания анимации
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                // Показываем и запускаем первую GIF
                Glide.with(this@MainActivity)
                    .asGif()
                    .load(R.drawable.macro) // Первая GIF
                    .into(gifView)

                gifView.alpha = 1f // Отображаем первую GIF

                // Замена GIF на статичное изображение через 3 секунды
                gifView.postDelayed({
                    Glide.with(this@MainActivity).clear(gifView) // Очищаем первую GIF

                    // Заменяем на статичное изображение
                    Glide.with(this@MainActivity)
                        .load(R.drawable.macro1) // Статичное изображение
                        .into(gifView)

                    // Плавное появление статичного изображения
                    ObjectAnimator.ofFloat(gifView, "alpha", 0f, 1f).apply {
                        duration = 500
                    }.start()

                    // После отображения статичного изображения запускаем вторую GIF
                    gifView.postDelayed({
                        Glide.with(this@MainActivity)
                            .asGif()
                            .load(R.drawable.video2) // Вторая GIF
                            .into(gifView2)

                        gifView2.alpha = 1f // Отображаем вторую GIF

                        // Замена второй GIF на статичное изображение через 3 секунды
                        gifView2.postDelayed({
                            Glide.with(this@MainActivity).clear(gifView2) // Очищаем вторую GIF

                            // Заменяем на статичное изображение
                            Glide.with(this@MainActivity)
                                .load(R.drawable.video3) // Статичное изображение для второй GIF
                                .into(gifView2)

                            // Плавное появление статичного изображения
                            ObjectAnimator.ofFloat(gifView2, "alpha", 0f, 1f).apply {
                                duration = 500
                            }.start()
                        }, 6000) // Время в миллисекундах для завершения второй GIF
                    }, 1000) // Задержка перед запуском второй GIF
                }, 3000) // Время в миллисекундах для завершения первой GIF
            }
        })

        // Запуск анимации
        animatorSet.start()
    }

    fun onClick(view: View) {
        val intent = Intent(this, ImageClassification::class.java)
        startActivity(intent)
    }
    fun onClick1(view: View) {
        val intent = Intent(this, MainActivity2::class.java)
        startActivity(intent)
    }

}



