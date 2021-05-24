
package org.tensorflow.lite.examples.soundclassifier

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.tensorflow.lite.examples.soundclassifier.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
  private val probabilitiesAdapter by lazy { ProbabilitiesAdapter() }

  private lateinit var soundClassifier: SoundClassifier

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)

    soundClassifier = SoundClassifier(this, SoundClassifier.Options()).also {
      it.lifecycleOwner = this
    }

    with(binding) {
      recyclerView.apply {
        setHasFixedSize(true)
        adapter = probabilitiesAdapter.apply {
          labelList = soundClassifier.labelList
        }
      }

      keepScreenOn(inputSwitch.isChecked)
      inputSwitch.setOnCheckedChangeListener { _, isChecked ->
        soundClassifier.isPaused = !isChecked
        keepScreenOn(isChecked)
      }

      overlapFactorSlider.value = soundClassifier.overlapFactor
      overlapFactorSlider.addOnChangeListener { _, value, _ ->
        soundClassifier.overlapFactor = value
      }
    }

    soundClassifier.probabilities.observe(this) { resultMap ->
      if (resultMap.isEmpty() || resultMap.size > soundClassifier.labelList.size) {
        Log.w(TAG, "Invalid size of probability output! (size: ${resultMap.size})")
        return@observe
      }
      probabilitiesAdapter.probabilityMap = resultMap
      probabilitiesAdapter.notifyDataSetChanged()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      requestMicrophonePermission()
    } else {
      soundClassifier.start()
    }
  }

  override fun onTopResumedActivityChanged(isTopResumedActivity: Boolean) {
    // Handles "top" resumed event on multi-window environment
    if (isTopResumedActivity) {
      soundClassifier.start()
    } else {
      soundClassifier.stop()
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    if (requestCode == REQUEST_RECORD_AUDIO) {
      if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Log.i(TAG, "Audio permission granted :)")
        soundClassifier.start()
      } else {
        Log.e(TAG, "Audio permission not granted :(")
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.M)
  private fun requestMicrophonePermission() {
    if (ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.RECORD_AUDIO
      ) == PackageManager.PERMISSION_GRANTED
    ) {
      soundClassifier.start()
    } else {
      requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
    }
  }

  private fun keepScreenOn(enable: Boolean) =
    if (enable) {
      window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    } else {
      window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

  companion object {
    const val REQUEST_RECORD_AUDIO = 1337
    private const val TAG = "AudioDemo"
  }
}
