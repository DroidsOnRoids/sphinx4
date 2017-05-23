package pl.droidsonroids.sphinxtest

import android.Manifest
import android.app.Activity
import android.media.AudioFormat.CHANNEL_IN_MONO
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.MediaRecorder.AudioSource.MIC
import android.os.Bundle
import android.os.Environment
import com.jakewharton.rxbinding2.view.RxView
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import omrecorder.AudioSource
import omrecorder.OmRecorder
import omrecorder.PullTransport
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {

	lateinit var disposable: Disposable

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		disposable = RxView.clicks(button)
				.compose(RxPermissions(this)
						.ensure(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE))
				.observeOn(Schedulers.io())
				.filter { it }
				.map {
					OmRecorder.wav(PullTransport.Default(AudioSource.Smart(MIC, ENCODING_PCM_16BIT,	CHANNEL_IN_MONO, 44100), { _ -> }),
							File(Environment.getExternalStorageDirectory(), "test.wav"))
				}
				.flatMap {
					Observable.just(it)
							.doOnNext { it.startRecording() }
							.mergeWith(
									Observable.just(it)
											.subscribeOn(Schedulers.computation())
											.delay(3, TimeUnit.SECONDS)
											.doOnComplete {
												it.stopRecording()
											}
							)
				}
				.subscribe({ }, { it.printStackTrace() })
	}

	override fun onDestroy() {
		disposable.dispose()
		super.onDestroy()
	}

}
