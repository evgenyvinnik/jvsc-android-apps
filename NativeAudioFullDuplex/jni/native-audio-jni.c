/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/* This is a JNI example where we use native methods to play sounds
 * using OpenSL ES. See the corresponding Java source file located at:
 *
 *   src/com/example/nativeaudio/NativeAudio/NativeAudio.java
 */
#include <assert.h>
#include <jni.h>
#include <string.h>

// for __android_log_print(ANDROID_LOG_INFO, "YourApp", "formatted message");
// #include <android/log.h>

// for native audio
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

// for native asset manager
#include <sys/types.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

// engine interfaces
static SLObjectItf engineObject = NULL;
static SLEngineItf engineEngine;

// output mix interfaces
static SLObjectItf outputMixObject = NULL;

// buffer queue player interfaces
static SLObjectItf bqPlayerObject = NULL;
static SLPlayItf bqPlayerPlay;
static SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;
static SLEffectSendItf bqPlayerEffectSend;
static SLMuteSoloItf bqPlayerMuteSolo;
static SLVolumeItf bqPlayerVolume;

// aux effect on the output mix, used by the buffer queue player
static const SLEnvironmentalReverbSettings reverbSettings =
		SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;

// file descriptor player interfaces
static SLObjectItf fdPlayerObject = NULL;
static SLPlayItf fdPlayerPlay;
static SLSeekItf fdPlayerSeek;
static SLMuteSoloItf fdPlayerMuteSolo;
static SLVolumeItf fdPlayerVolume;

// recorder interfaces
static SLObjectItf recorderObject = NULL;
static SLRecordItf recorderRecord;
static SLAndroidSimpleBufferQueueItf recorderBufferQueue;

// 5 seconds of recorded audio at 16 kHz mono, 16-bit signed little endian
#define RECORDER_FRAMES (16000 * 5)
static short recorderBuffer[RECORDER_FRAMES];
static unsigned recorderSize = 0;
static SLmilliHertz recorderSR;

// pointer and size of the next player buffer to enqueue, and number of remaining buffers
static short *nextBuffer;
static unsigned nextSize;
static int nextCount;

// synthesize a mono sawtooth wave and place it into a buffer (called automatically on load)
__attribute__((constructor)) static void onDlOpen(void)
{

}

// this callback handler is called every time a buffer finishes playing
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
	assert(bq == bqPlayerBufferQueue);
	assert(NULL == context);
	// for streaming playback, replace this test by logic to find and fill the next buffer
	if (--nextCount > 0 && NULL != nextBuffer && 0 != nextSize)
	{
		SLresult result;
		// enqueue another buffer
		result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue,
				nextBuffer, nextSize);
		// the most likely other result is SL_RESULT_BUFFER_INSUFFICIENT,
		// which for this code example would indicate a programming error
		assert(SL_RESULT_SUCCESS == result);
	}
}

// this callback handler is called every time a buffer finishes recording
void bqRecorderCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
	assert(bq == bqRecorderBufferQueue);
	assert(NULL == context);
	// for streaming recording, here we would call Enqueue to give recorder the next buffer to fill
	// but instead, this is a one-time buffer so we stop recording
	SLresult result;
	result = (*recorderRecord)->SetRecordState(recorderRecord,
			SL_RECORDSTATE_STOPPED);
	if (SL_RESULT_SUCCESS == result)
	{
		recorderSize = RECORDER_FRAMES * sizeof(short);
		recorderSR = SL_SAMPLINGRATE_16;
	}
}

// create the engine and output mix objects
void Java_com_example_nativeaudio_NativeAudio_createEngine(JNIEnv* env,
		jclass clazz)
{
	SLresult result;

	// create engine
	result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
	assert(SL_RESULT_SUCCESS == result);

	// realize the engine
	result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
	assert(SL_RESULT_SUCCESS == result);

	// get the engine interface, which is needed in order to create other objects
	result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE,
			&engineEngine);
	assert(SL_RESULT_SUCCESS == result);

	// create output mix, with environmental reverb specified as a non-required interface
	const SLInterfaceID ids[1] = { SL_IID_ENVIRONMENTALREVERB };
	const SLboolean req[1] = { SL_BOOLEAN_FALSE };
	result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject,
			1, ids, req);
	assert(SL_RESULT_SUCCESS == result);

	// realize the output mix
	result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
	assert(SL_RESULT_SUCCESS == result);


}

// create buffer queue audio player
void Java_com_example_nativeaudio_NativeAudio_createBufferQueueAudioPlayer(
		JNIEnv* env, jclass clazz)
{
	SLresult result;

	// configure audio source
	SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {
			SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2 };
	SLDataFormat_PCM format_pcm = { SL_DATAFORMAT_PCM, 1, SL_SAMPLINGRATE_8,
			SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
			SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN };
	SLDataSource audioSrc = { &loc_bufq, &format_pcm };

	// configure audio sink
	SLDataLocator_OutputMix loc_outmix = { SL_DATALOCATOR_OUTPUTMIX,
			outputMixObject };
	SLDataSink audioSnk = { &loc_outmix, NULL };

	// create audio player
	const SLInterfaceID ids[3] = { SL_IID_BUFFERQUEUE, SL_IID_EFFECTSEND,
	/*SL_IID_MUTESOLO,*/SL_IID_VOLUME };
	const SLboolean req[3] = { SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE,
	/*SL_BOOLEAN_TRUE,*/SL_BOOLEAN_TRUE };
	result = (*engineEngine)->CreateAudioPlayer(engineEngine, &bqPlayerObject,
			&audioSrc, &audioSnk, 3, ids, req);
	assert(SL_RESULT_SUCCESS == result);

	// realize the player
	result = (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);
	assert(SL_RESULT_SUCCESS == result);

	// get the play interface
	result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_PLAY,
			&bqPlayerPlay);
	assert(SL_RESULT_SUCCESS == result);

	// get the buffer queue interface
	result = (*bqPlayerObject)->GetInterface(bqPlayerObject,
			SL_IID_BUFFERQUEUE, &bqPlayerBufferQueue);
	assert(SL_RESULT_SUCCESS == result);

	// register callback on the buffer queue
	result = (*bqPlayerBufferQueue)->RegisterCallback(bqPlayerBufferQueue,
			bqPlayerCallback, NULL);
	assert(SL_RESULT_SUCCESS == result);

	// get the effect send interface
	result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_EFFECTSEND,
			&bqPlayerEffectSend);
	assert(SL_RESULT_SUCCESS == result);

#if 0   // mute/solo is not supported for sources that are known to be mono, as this is
	// get the mute/solo interface
	result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_MUTESOLO, &bqPlayerMuteSolo);
	assert(SL_RESULT_SUCCESS == result);
#endif

	// get the volume interface
	result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_VOLUME,
			&bqPlayerVolume);
	assert(SL_RESULT_SUCCESS == result);

	// set the player's state to playing
	result = (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_PLAYING);
	assert(SL_RESULT_SUCCESS == result);

}



// select the desired clip and play count, and enqueue the first buffer if idle
jboolean Java_com_example_nativeaudio_NativeAudio_selectClip(JNIEnv* env,
		jclass clazz, jint which, jint count)
{
	short *oldBuffer = nextBuffer;
	switch (which)
	{
		case 0: // CLIP_NONE
			nextBuffer = (short *) NULL;
			nextSize = 0;
			break;
		case 4: // CLIP_PLAYBACK
			// we recorded at 16 kHz, but are playing buffers at 8 Khz, so do a primitive down-sample
			if (recorderSR == SL_SAMPLINGRATE_16)
			{
				unsigned i;
				for (i = 0; i < recorderSize; i += 2 * sizeof(short))
				{
					recorderBuffer[i >> 2] = recorderBuffer[i >> 1];
				}
				recorderSR = SL_SAMPLINGRATE_8;
				recorderSize >>= 1;
			}
			nextBuffer = recorderBuffer;
			nextSize = recorderSize;
			break;
		default:
			nextBuffer = NULL;
			nextSize = 0;
			break;
	}
	nextCount = count;
	if (nextSize > 0)
	{
		// here we only enqueue one buffer because it is a long clip,
		// but for streaming playback we would typically enqueue at least 2 buffers to start
		SLresult result;
		result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue,
				nextBuffer, nextSize);
		if (SL_RESULT_SUCCESS != result)
		{
			return JNI_FALSE;
		}
	}

	return JNI_TRUE;
}


// create audio recorder
jboolean Java_com_example_nativeaudio_NativeAudio_createAudioRecorder(
		JNIEnv* env, jclass clazz)
{
	SLresult result;

	// configure audio source
	SLDataLocator_IODevice loc_dev = { SL_DATALOCATOR_IODEVICE,
			SL_IODEVICE_AUDIOINPUT, SL_DEFAULTDEVICEID_AUDIOINPUT, NULL };
	SLDataSource audioSrc = { &loc_dev, NULL };

	// configure audio sink
	SLDataLocator_AndroidSimpleBufferQueue loc_bq = {
			SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2 };
	SLDataFormat_PCM format_pcm = { SL_DATAFORMAT_PCM, 1, SL_SAMPLINGRATE_16,
			SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
			SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN };
	SLDataSink audioSnk = { &loc_bq, &format_pcm };

	// create audio recorder
	// (requires the RECORD_AUDIO permission)
	const SLInterfaceID id[1] = { SL_IID_ANDROIDSIMPLEBUFFERQUEUE };
	const SLboolean req[1] = { SL_BOOLEAN_TRUE };
	result = (*engineEngine)->CreateAudioRecorder(engineEngine,
			&recorderObject, &audioSrc, &audioSnk, 1, id, req);
	if (SL_RESULT_SUCCESS != result)
	{
		return JNI_FALSE;
	}

	// realize the audio recorder
	result = (*recorderObject)->Realize(recorderObject, SL_BOOLEAN_FALSE);
	if (SL_RESULT_SUCCESS != result)
	{
		return JNI_FALSE;
	}

	// get the record interface
	result = (*recorderObject)->GetInterface(recorderObject, SL_IID_RECORD,
			&recorderRecord);
	assert(SL_RESULT_SUCCESS == result);

	// get the buffer queue interface
	result = (*recorderObject)->GetInterface(recorderObject,
			SL_IID_ANDROIDSIMPLEBUFFERQUEUE, &recorderBufferQueue);
	assert(SL_RESULT_SUCCESS == result);

	// register callback on the buffer queue
	result = (*recorderBufferQueue)->RegisterCallback(recorderBufferQueue,
			bqRecorderCallback, NULL);
	assert(SL_RESULT_SUCCESS == result);

	return JNI_TRUE;
}

// set the recording state for the audio recorder
void Java_com_example_nativeaudio_NativeAudio_startRecording(JNIEnv* env,
		jclass clazz)
{
	SLresult result;

	// in case already recording, stop recording and clear buffer queue
	result = (*recorderRecord)->SetRecordState(recorderRecord,
			SL_RECORDSTATE_STOPPED);
	assert(SL_RESULT_SUCCESS == result);
	result = (*recorderBufferQueue)->Clear(recorderBufferQueue);
	assert(SL_RESULT_SUCCESS == result);

	// the buffer is not valid for playback yet
	recorderSize = 0;

	// enqueue an empty buffer to be filled by the recorder
	// (for streaming recording, we would enqueue at least 2 empty buffers to start things off)
	result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue,
			recorderBuffer, RECORDER_FRAMES * sizeof(short));
	// the most likely other result is SL_RESULT_BUFFER_INSUFFICIENT,
	// which for this code example would indicate a programming error
	assert(SL_RESULT_SUCCESS == result);

	// start recording
	result = (*recorderRecord)->SetRecordState(recorderRecord,
			SL_RECORDSTATE_RECORDING);
	assert(SL_RESULT_SUCCESS == result);

}

// shut down the native audio system
void Java_com_example_nativeaudio_NativeAudio_shutdown(JNIEnv* env,
		jclass clazz)
{

	// destroy buffer queue audio player object, and invalidate all associated interfaces
	if (bqPlayerObject != NULL)
	{
		(*bqPlayerObject)->Destroy(bqPlayerObject);
		bqPlayerObject = NULL;
		bqPlayerPlay = NULL;
		bqPlayerBufferQueue = NULL;
		bqPlayerEffectSend = NULL;
		bqPlayerMuteSolo = NULL;
		bqPlayerVolume = NULL;
	}

	// destroy file descriptor audio player object, and invalidate all associated interfaces
	if (fdPlayerObject != NULL)
	{
		(*fdPlayerObject)->Destroy(fdPlayerObject);
		fdPlayerObject = NULL;
		fdPlayerPlay = NULL;
		fdPlayerSeek = NULL;
		fdPlayerMuteSolo = NULL;
		fdPlayerVolume = NULL;
	}

	// destroy audio recorder object, and invalidate all associated interfaces
	if (recorderObject != NULL)
	{
		(*recorderObject)->Destroy(recorderObject);
		recorderObject = NULL;
		recorderRecord = NULL;
		recorderBufferQueue = NULL;
	}

	// destroy output mix object, and invalidate all associated interfaces
	if (outputMixObject != NULL)
	{
		(*outputMixObject)->Destroy(outputMixObject);
		outputMixObject = NULL;
	}

	// destroy engine object, and invalidate all associated interfaces
	if (engineObject != NULL)
	{
		(*engineObject)->Destroy(engineObject);
		engineObject = NULL;
		engineEngine = NULL;
	}

}
