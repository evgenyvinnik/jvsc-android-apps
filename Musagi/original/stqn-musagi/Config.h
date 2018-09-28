#ifndef MUSAGI_CONFIG_H
#define MUSAGI_CONFIG_H
struct Config
{
	int winx; // unused
	int winy; // unused
	int winwidth;
	int winheight;
	int winmax; // unused
	int log_on;
	char portaudio_latency[8];
	int midiInputDevice;
	// here the config file contains int doublesize.
	int tooltips_on;
	int scrollfollow;
	float speaker_volume;
	float metronome_vol;
	int kbkboctave;
	int midioctave;
};

extern struct Config gConfig;

void config_SetDefaultValues(void);
void config_Load(void);
void config_Save(void);
#endif
