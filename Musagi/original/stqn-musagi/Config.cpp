// Loading and saving of config.txt

#include <stdio.h>
#include <stdlib.h>
#include "Config.h"
#include "fileselector.h"
#include "glkit_global.h"
#include "Log.h"
#include "musagi.h"
#include "platform.h"

Config gConfig;

/**
 * Fill Config struct with default values.
 */
void config_SetDefaultValues(void)
{
	gConfig.winx=0;
	gConfig.winy=0;
	gConfig.winwidth=950;
	gConfig.winheight=650;
	gConfig.winmax=0; // unused
	strcpy(gConfig.portaudio_latency, "100");
	gConfig.midiInputDevice=0;
	gConfig.tooltips_on=1;
	gConfig.scrollfollow=1;
	gConfig.speaker_volume=0.5f;
	gConfig.metronome_vol=0.5f;
	gConfig.kbkboctave=2;
	gConfig.midioctave=1;

	// reset path strings
	for(int pi=0; pi<MAX_FILETYPES; pi++)
		SetCurDir( (FileType)pi, "", false );
}

/**
 * Save config.
 */
void config_Save(void)
{
	char cfgpath[512];
	strcpy(cfgpath, platform_GetConfigDir());
	strcat(cfgpath, "config.txt");
	LogPrint("Writing cfg to \"%s\"", cfgpath);

	FILE *cfg=fopen(cfgpath, "w");
	if(cfg)
	{
		int doublesize=0;
		if(glkitHalfRes())
			doublesize=1;

		fprintf(cfg, "window: %i %i %i %i %i\n", gConfig.winx, gConfig.winy, gConfig.winwidth, gConfig.winheight, gConfig.winmax);
		fprintf(cfg, "log: %i\n", gConfig.log_on);
		fprintf(cfg, "latency: %s\n", gConfig.portaudio_latency);
		fprintf(cfg, "mididevice: %i\n", gConfig.midiInputDevice);
		fprintf(cfg, "doublesize: %i\n", doublesize);
		fprintf(cfg, "tooltips: %i\n", gConfig.tooltips_on);
		fprintf(cfg, "scrollfollow: %i\n", gConfig.scrollfollow);
		fprintf(cfg, "speaker: %.2f\n", gConfig.speaker_volume);
		fprintf(cfg, "metronome: %.2f\n", gConfig.metronome_vol);
		fprintf(cfg, "keyboard octave: %i\n", gConfig.kbkboctave);
		fprintf(cfg, "midi octave: %i\n", gConfig.midioctave);

		// write stored path strings
		for(int pi=0;pi<MAX_FILETYPES;pi++)
			fprintf(cfg, "%s\n", GetCurDir((FileType)pi));
		fclose(cfg);
	}
}

/**
 * Load config.
 */
void config_Load(void)
{
	char cfgpath[512];
	strcpy(cfgpath, platform_GetConfigDir());
	strcat(cfgpath, "config.txt");
	LogPrint("Reading cfg from \"%s\"", cfgpath);

	FILE *cfg=fopen(cfgpath, "r");
	if(cfg)
	{
		char junk[32];
		int doublesize=0;

		fscanf(cfg, "%s %i %i %i %i %i", junk, &gConfig.winx, &gConfig.winy,
		 &gConfig.winwidth, &gConfig.winheight, &gConfig.winmax);
		fscanf(cfg, "%s %i", junk, &gConfig.log_on);
		fscanf(cfg, "%s %s", junk, gConfig.portaudio_latency);
		fscanf(cfg, "%s %i", junk, &gConfig.midiInputDevice);
		fscanf(cfg, "%s %i", junk, &doublesize);
		fscanf(cfg, "%s %i", junk, &gConfig.tooltips_on);
		fscanf(cfg, "%s %i", junk, &gConfig.scrollfollow);
		fscanf(cfg, "%s %f", junk, &gConfig.speaker_volume);
		fscanf(cfg, "%s %f", junk, &gConfig.metronome_vol);
		fscanf(cfg, "%s %s %i", junk, junk, &gConfig.kbkboctave);
		fscanf(cfg, "%s %s %i", junk, junk, &gConfig.midioctave);

		if(doublesize==1)
			glkitSetHalfRes(true);

		// read stored path strings
		char ch=' ';
		fread(&ch, 1, 1, cfg); // read '\n'
		for(int pi=0;pi<MAX_FILETYPES;pi++)
		{
			char dirstring[FILENAME_BUFFER_LEN];
			int si=0;
			fread(&ch, 1, 1, cfg);
			while(ch!='\n')
			{
				dirstring[si++]=ch;
				fread(&ch, 1, 1, cfg);
			}
			dirstring[si]='\0';
			SetCurDir( (FileType)pi, dirstring, false );
			// Note: logging is not enabled at this point...
			//LogPrint("main: loaded dir for type %i: \"%s\"", pi,
			// GetCurDir((FileType)pi));
		}

		fclose(cfg);
	}
}
