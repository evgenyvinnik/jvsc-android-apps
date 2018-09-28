#include <string.h>

#include "Log.h"
#include "platform.h"

static bool commonlib_logactive;
static char logfilename[512];
static bool log_mutex=false;

void LogStart(void)
{
	strcpy(logfilename, platform_GetConfigDir());
	strcat(logfilename, "log.txt");
	//printf("logfile: %s", logfilename);
	FILE *file;
	file=fopen(logfilename, "w");
	if(file)
		fclose(file);
	//else
	//	printf("error creating log.txt");
	commonlib_logactive=true;
}

void LogPrint(const char *string, ...)
{
	if(!commonlib_logactive)
		return;

	while(log_mutex) { Sleep(0); }
	log_mutex=true;

	FILE *file;
	char temp[1024];
	va_list args;

	va_start(args, string);
	vsprintf(temp, string, args);
	va_end(args);

	file=fopen(logfilename, "a");
	if(file)
	{
		fprintf(file,"%s\n", temp);
		fclose(file);
	}

	log_mutex=false;
}

void LogPrintf(const char *string, ...)
{
	if(!commonlib_logactive)
		return;

	while(log_mutex) { Sleep(0); }
	log_mutex=true;

	FILE *file;
	char temp[1024];
	va_list args;

	va_start(args, string);
	vsprintf(temp, string, args);
	va_end(args);

	file=fopen(logfilename, "a");
	if(file)
	{
		fprintf(file,"%s", temp);
		fclose(file);
	}

	log_mutex=false;
}

void LogEnable()
{
	LogPrint("Log enabled");
	commonlib_logactive=true;
}

void LogDisable()
{
	LogPrint("Log disabled");
	commonlib_logactive=false;
}
