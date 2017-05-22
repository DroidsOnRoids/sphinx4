package pl.droidsonroids.sphinxtest

import android.Manifest
import android.app.Activity
import android.media.AudioFormat.CHANNEL_IN_MONO
import android.media.AudioFormat.ENCODING_PCM_16BIT
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource.MIC
import android.os.Bundle
import android.os.Environment
import com.jakewharton.rxbinding2.view.RxView
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)
		RxView.clicks(button)
				.compose(RxPermissions(this)
						.ensure(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE))
				.filter { it }
				.observeOn(Schedulers.computation())
				.map { AudioRecord.getMinBufferSize(16000, CHANNEL_IN_MONO, ENCODING_PCM_16BIT) }
				.doOnNext {
					when (it) {
						AudioRecord.ERROR or AudioRecord.ERROR_BAD_VALUE -> throw IllegalStateException("error $it")
					}
				}
				.map { AudioRecord(MIC, 16000, CHANNEL_IN_MONO, ENCODING_PCM_16BIT, it) }
				.doOnNext {
					when {
						it.state != AudioRecord.STATE_INITIALIZED -> throw IllegalStateException("state ${it.state}")
					}
				}
				.flatMap { recorder ->
					Completable.complete().subscribeOn(Schedulers.newThread()).delay(3, TimeUnit.SECONDS).subscribe { recorder.stop() }
					return@flatMap Observable.just(recorder)
							.subscribeOn(Schedulers.io())
							.doFinally { recorder.release() }
							.doOnNext { it.startRecording() }
							.doOnNext {
								File(Environment.getExternalStorageDirectory(), "test.wav")
							}
				}
				.subscribe({ }, { it.printStackTrace() })
	}

}
