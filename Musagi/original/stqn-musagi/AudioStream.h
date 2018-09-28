#ifndef AudioStream_h
#define AudioStream_h

class AudioStream;

#include <stdio.h>
#include <portaudio.h>

#include "Config.h"
#include "timer.h"
#include "musagi.h"
#include "part.h"
#include "song.h"

#ifdef VST
#include "pluginterfaces/vst2.x/aeffectx.h"
#endif

//static int pa_callback(void*, void*, unsigned long, PaTimestamp, void*);
//int pa_callback(void*, void*, unsigned long, PaTimestamp, void*);

struct PPart
{
	Part *part;
	int start;
	bool expired;
};

class AudioStream
{
public:
	PaStream *stream;
	PaError mPaInitError; // Indicates success or failure initialising PortAudio
	PaDeviceIndex mPaOutputDevice;

	StereoBufferP buffer;
	StereoBufferP in_buffer;

	GearStack **gearstacks;
	int num_gearstacks;
	GearStack **newlist;

	PPart *parts;
	int numparts;

	Song *song;

	unsigned int globaltick;
	unsigned int songtick; // globaltick but reset on play

	int metronome;
	int metrocount;
	int blipcount;
	float blipvol;
	float blipangle;

	float master_volume;
	float speaker_volume;

	bool midi_mode;

	// for file output
	FILE *foutput;
	bool file_output;
	int file_stereosampleswritten;
	int foutstream_datasize;
	bool record_song;

	// for performance measurement
	CTimer *perftimer;
	float dtime;
	int dtime_num;
	float cpu_usage;

	float peak_left;
	float peak_right;
	int clip_left;
	int clip_right;

	bool has_input_stream;

#ifdef VST
	VstTimeInfo vst_timeinfo;
#endif

//public:
	AudioStream()
	{
		LogPrint("AudioStream: init");
		perftimer=new CTimer();
		perftimer->init();
		dtime=0;
		dtime_num=0;
		cpu_usage=0.0f;

		globaltick=0;
		songtick=0;

		metronome=0;
		blipcount=0;

		midi_mode=false;

		buffer.left=NULL;
		buffer.right=NULL;
		buffer.size=0;

		in_buffer.left=NULL;
		in_buffer.right=NULL;
		in_buffer.size=0;

		peak_left=0.0f;
		peak_right=0.0f;
		clip_left=false;
		clip_right=false;

		master_volume=0.25f;
		speaker_volume=0.25f;

		gearstacks=(GearStack**)malloc(512*sizeof(GearStack*));
		num_gearstacks=0;
//		newlist=NULL;
		newlist=(GearStack**)malloc(512*sizeof(GearStack*));

		song=NULL;

		parts=(PPart*)malloc(1024*sizeof(PPart));
		numparts=0;

		file_output=false;
		record_song=false;

		// init portaudio
		//int pa_error=paNoError;
		mPaInitError=Pa_Initialize();
		if(mPaInitError!=paNoError)
		{
			LogPrint("*** portaudio error, in Pa_Initialize: %s",
			 Pa_GetErrorText(mPaInitError));
		}
		else
		{
			// List host APIs and devices and try to find the "default" ALSA
			// device.
			PaHostApiIndex numHostAPIs=Pa_GetHostApiCount();
			PaHostApiIndex alsaHostAPI=-1;
			LogPrint("portaudio reports %i host APIs:", numHostAPIs);
			if(numHostAPIs<0)
			{
				LogPrint("*** portaudio error, in Pa_GetHostApiCount: %s",
				 Pa_GetErrorText(numHostAPIs));
			}
			else
				for(int i=0;i<numHostAPIs;i++)
				{
					const PaHostApiInfo *info;
					info=Pa_GetHostApiInfo(i);
					const char *def;
					if(Pa_GetDefaultHostApi()==i)
						def=" (default)";
					else
						def="";
					LogPrint("%i - %s%s", i, info->name, def);
					if(strcmp(info->name, "ALSA")==0)
						alsaHostAPI=i;
				}

			mPaOutputDevice=-1;
			int num=Pa_GetDeviceCount();
			LogPrint("portaudio reports %i devices:", num);
			if(num<0)
			{
				LogPrint("*** portaudio error, in Pa_GetDeviceCount: %s",
				 Pa_GetErrorText(num));
			}
			else
				for(int i=0;i<num;i++)
				{
					const PaDeviceInfo* info;
					info=Pa_GetDeviceInfo(i);
					const char *def;
					if(Pa_GetDefaultOutputDevice()==i)
						def=" (default)";
					else
						def="";
					LogPrint("%i - %s (API %d)%s", i, info->name, info->hostApi,
					 def);
					if(info->hostApi==alsaHostAPI && strcmp(info->name, "default")==0)
						mPaOutputDevice=i;
				}
		}

		stream=NULL;
		LogPrint("AudioStream: init done");
	};

	~AudioStream()
	{
		if(stream!=NULL)
			StopStream();
		// close portaudio

		if(mPaInitError==paNoError)
		    Pa_Terminate();

		delete perftimer;

		free(buffer.left);
		free(buffer.right);

		free(gearstacks);
		free(newlist);
		free(parts);
	};

	void AdvanceTick(unsigned int length)
	{
		globaltick+=length;
		songtick+=length;

#ifdef VST
		double cursample=(double)GetTick(1)*8.0;
		double playsample=(double)GetTick(2)*8.0;
		vst_timeinfo.samplePos=cursample; // ok
		vst_timeinfo.sampleRate=44100.0;
		vst_timeinfo.nanoSeconds=cursample*(1000000.0/44.1); // ok
		int samples_per_beat=32*320*GetTempo()/1600;
		vst_timeinfo.ppqPos=playsample/(double)samples_per_beat; // ok
		vst_timeinfo.tempo=(double)UpdateTempo(); // ok
		vst_timeinfo.barStartPos=floor(vst_timeinfo.ppqPos); // ok
		vst_timeinfo.cycleStartPos=0.0; // ? (what is cycle?)
		vst_timeinfo.cycleEndPos=1000000.0*16.0; // ? (what is cycle?)
		vst_timeinfo.timeSigNumerator=GetBeatLength(); // ok
		vst_timeinfo.timeSigDenominator=4; // ok
		double sample_to_subframe=30.0*80.0/44100.0;
		vst_timeinfo.smpteOffset=(int)(playsample*sample_to_subframe); // ok
		vst_timeinfo.smpteFrameRate=30; // constant 30 fps (arbitrary)
		vst_timeinfo.samplesToNextClock=(int)(cursample-(double)vst_timeinfo.smpteOffset/sample_to_subframe); // ok
		vst_timeinfo.flags=0;

		vst_timeinfo.flags|=kVstNanosValid;
		vst_timeinfo.flags|=kVstPpqPosValid;
		vst_timeinfo.flags|=kVstTempoValid;
		vst_timeinfo.flags|=kVstBarsValid;
		vst_timeinfo.flags|=kVstTimeSigValid;
		vst_timeinfo.flags|=kVstSmpteValid;
		vst_timeinfo.flags|=kVstClockValid;
		// TODO: add info about play state etc
//		vst_timeinfo.flags|=;
//		vst_timeinfo.flags|=;
/*
enum VstTimeInfoFlags
{
//-------------------------------------------------------------------------------------------------------
	kVstTransportChanged     = 1,		///< indicates that play, cycle or record state has changed
	kVstTransportPlaying     = 1 << 1,	///< set if Host sequencer is currently playing
	kVstTransportCycleActive = 1 << 2,	///< set if Host sequencer is in cycle mode
	kVstTransportRecording   = 1 << 3,	///< set if Host sequencer is in record mode
	kVstAutomationWriting    = 1 << 6,	///< set if automation write mode active (record parameter changes)
	kVstAutomationReading    = 1 << 7,	///< set if automation read mode active (play parameter changes)
	kVstNanosValid           = 1 << 8,	///< VstTimeInfo::nanoSeconds valid
	kVstPpqPosValid          = 1 << 9,	///< VstTimeInfo::ppqPos valid
	kVstTempoValid           = 1 << 10,	///< VstTimeInfo::tempo valid
	kVstBarsValid            = 1 << 11,	///< VstTimeInfo::barStartPos valid
	kVstCyclePosValid        = 1 << 12,	///< VstTimeInfo::cycleStartPos and VstTimeInfo::cycleEndPos valid
	kVstTimeSigValid         = 1 << 13,	///< VstTimeInfo::timeSigNumerator and VstTimeInfo::timeSigDenominator valid
	kVstSmpteValid           = 1 << 14,	///< VstTimeInfo::smpteOffset and VstTimeInfo::smpteFrameRate valid
	kVstClockValid           = 1 << 15	///< VstTimeInfo::samplesToNextClock valid
//-------------------------------------------------------------------------------------------------------
};
*/
#endif // VST
	};

#ifdef VST
	VstTimeInfo* GetVstTimeInfo()
	{
		return &vst_timeinfo;
	};
#endif // VST

	unsigned int GetTick(int mode)
	{
		switch(mode)
		{
		case 1:
			return globaltick;
		case 2:
			return songtick;
		case 3: // GetTick sets it... heh, don't kill me
			songtick=0;
			return 0;
		default:
			return globaltick*8; // will break after about 24 hours
		}
	};

	bool StartStream(int buffersize, int (*pa_callback)(const void*, void*, unsigned long, const PaStreamCallbackTimeInfo*, PaStreamCallbackFlags, void*))
	{
		LogPrint("AudioStream: start stream");
		if(stream!=NULL) // already streaming
			return false;
		if(mPaInitError!=paNoError)
			return false;

		if(buffer.size!=buffersize)
		{
			buffer.size=buffersize;
			free(buffer.left);
			free(buffer.right);
			buffer.left=(float*)malloc(buffer.size*sizeof(float));
			buffer.right=(float*)malloc(buffer.size*sizeof(float));

			in_buffer.size=buffersize;
			free(in_buffer.left);
			free(in_buffer.right);
			in_buffer.left=(float*)malloc(in_buffer.size*sizeof(float));
			in_buffer.right=(float*)malloc(in_buffer.size*sizeof(float));
		}

		int pa_error=paNoError;
		has_input_stream=false;
		if(mPaOutputDevice<0)
		{
			has_input_stream=true;
			pa_error=Pa_OpenDefaultStream(
											&stream,
											2,				// input channels
											2,				// output channels
											paFloat32,		// 32 bit floating point output
											44100,
											buffer.size,		// frames per buffer
											pa_callback,
											this);
		}
		else
		{
			LogPrint("Trying to open stream on output device %d.",
			 mPaOutputDevice);
			int latencyms;
			sscanf(gConfig.portaudio_latency, "%i", &latencyms);
			PaStreamParameters outStreamParams = {
				mPaOutputDevice,
				2, // channel count
				paFloat32, // sample format
				latencyms/1000., // suggested latency
				NULL };
			pa_error=Pa_OpenStream( &stream, NULL, &outStreamParams, 44100,
			 buffer.size, paNoFlag, pa_callback, this);
		}

		if(pa_error!=paNoError)
		{
			LogPrint("*** portaudio error, in Pa_Open(Default)Stream: %s", Pa_GetErrorText(pa_error));
			LogPrint("trying without input stream...");
			pa_error=Pa_OpenDefaultStream(
											&stream,
											0,				// input channels
											2,				// output channels
											paFloat32,		// 32 bit floating point output
											44100,
											buffer.size,		// frames per buffer
											pa_callback,
											this);
			if(pa_error!=paNoError)
				LogPrint("*** portaudio error, in Pa_OpenDefaultStream: %s", Pa_GetErrorText(pa_error));
		}

		if(pa_error==paNoError)
		{
		    pa_error=Pa_StartStream(stream);
			if(pa_error!=paNoError)
				LogPrint("*** portaudio error, in Pa_StartStream: %s", Pa_GetErrorText(pa_error));
		}

		// TODO: cleanup when memory allocation or stream opening fails

	    return true;
	};

	bool StopStream()
	{
		if(stream==NULL)
			return false;
		if(mPaInitError!=paNoError)
			return false;

	    Pa_StopStream(stream);
	    Pa_CloseStream(stream);
	    stream=NULL;

	    if(buffer.size>0)
	    {
			free(buffer.left);
			free(buffer.right);
			buffer.left=NULL;
			buffer.right=NULL;
			buffer.size=0;
		}

	    return true;
	};

	// pause to make sure callback function has realized that a variable changed (not needed?)
/*	void WaitForCallback()
	{
		Sleep(1);
	};
*/
	void StartFileOutput(const char *filename, bool srec)
	{
		foutput=fopen(filename, "wb");
		// write wav header
//		char string[32];
		unsigned int dword=0;
		unsigned short word=0;
		fwrite("RIFF", 4, 1, foutput); // "RIFF"
		dword=0;
		fwrite(&dword, 1, 4, foutput); // remaining file size
		fwrite("WAVE", 4, 1, foutput); // "WAVE"

		fwrite("fmt ", 4, 1, foutput); // "fmt "
		dword=16;
		fwrite(&dword, 1, 4, foutput); // chunk size
		word=1;
		fwrite(&word, 1, 2, foutput); // compression code
		word=2;
		fwrite(&word, 1, 2, foutput); // channels
		dword=44100;
		fwrite(&dword, 1, 4, foutput); // sample rate
		dword=44100*4;
		fwrite(&dword, 1, 4, foutput); // bytes/sec
		word=4;
		fwrite(&word, 1, 2, foutput); // block align
		word=16;
		fwrite(&word, 1, 2, foutput); // bits per sample

		fwrite("data", 4, 1, foutput); // "data"
		dword=0;
		foutstream_datasize=ftell(foutput);
		fwrite(&dword, 1, 4, foutput); // chunk size

		// sample data

		file_stereosampleswritten=0;
		file_output=true;
		record_song=srec;
	};

	void StopFileOutput()
	{
		if(file_output)
		{
			file_output=false;
			record_song=false;

//			WaitForCallback();

			// seek back to header and write size info
			fseek(foutput, 4, SEEK_SET);
			unsigned int dword=0;
			dword=foutstream_datasize-4+file_stereosampleswritten*4;
			fwrite(&dword, 1, 4, foutput); // remaining file size
			fseek(foutput, foutstream_datasize, SEEK_SET);
			dword=file_stereosampleswritten*4;
			fwrite(&dword, 1, 4, foutput); // chunk size (data)
			fclose(foutput);
		}
	};

	void AddGearStack(GearStack *new_stack)
	{
		LogPrint("AudioStream: add gearstack, instrument [%.8X]", new_stack->instrument);
		gearstacks[num_gearstacks]=new_stack;
		num_gearstacks++;
	};

	void RemoveGearStack(GearStack *expired_stack)
	{
		LogPrint("AudioStream: remove gearstack %.8X", expired_stack);
		// create new array to avoid messing up the old one while it's in use
//		if(newlist!=NULL)
//			free(newlist);
//		LogPrint("as: allocated new list");
		int ni=0;
//		int ei=0;
		for(int i=0;i<num_gearstacks;i++)
			if(gearstacks[i]!=expired_stack)
				newlist[ni++]=gearstacks[i];
			else
				gearstacks[i]=NULL;
		num_gearstacks--;
		for(int i=0;i<num_gearstacks;i++)
			gearstacks[i]=newlist[i];
//		gearstacks=newstacks;
//		LogPrint("as: filled new list");
	};
/*
	void FlushGearstackList() // bleh, damn threading/callback issues
	{
		if(newlist!=NULL)
		{
			LogPrint("AudioStream: reassigning gearstack list");
			GearStack **oldlist=gearstacks;
			num_gearstacks--;
			gearstacks=newlist;
//			LogPrint("as: reassigned old list");
			free(oldlist);
			newlist=NULL;
//			LogPrint("as: released old list");
			for(int i=0;i<num_gearstacks;i++)
				LogPrint("AudioStream: %i: %.8X", i, gearstacks[i]);
		}
	};
*/
	void ResetClipMarkers()
	{
		clip_left=0;
		clip_right=0;
	};

	void AddPart(Part *part)
	{
		RemovePart(part); // in case it's already in... ?

		numparts++;
		parts[numparts-1].part=part;
		parts[numparts-1].start=globaltick;
		parts[numparts-1].expired=false;
//		LogPrint("audiostream: added part %i (%.8X) at tick %i", numparts-1, part, globaltick);
	};

	void RemovePart(Part *part)
	{
		bool found=false;
		int pi;
		for(pi=0;pi<numparts;pi++)
			if(parts[pi].part==part) // TODO: could there be more than one instance present? clones?
			{
				found=true;
				break;
			}
		if(found)
		{
			LogPrint("audiostream: removed part %i (%.8X)", pi, parts[pi].part);
			for(int j=pi;j<numparts-1;j++)
				parts[j]=parts[j+1];
			numparts--;
		}
	};

	void RemoveAllParts()
	{
		numparts=0;
	};

	void Flush()
	{
		RemoveAllParts();
		num_gearstacks=0;
	};

	void SetSong(Song *s)
	{
		song=s;
	};
};

#endif

