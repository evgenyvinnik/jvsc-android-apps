#include <assert.h>
#include <stdlib.h>
#include <sys/stat.h>
#include "platform.h"

DPInput *input;

#ifdef MIDI
HANDLE hMidiThread;

DWORD WINAPI midiplay_thread(LPVOID lpParam)
{
	midiplay_routine();
	return 0;
}
#endif

void platform_Sleep(int msec)
{
#ifdef _WIN32
	Sleep(msec);
#else
	if(msec==0)
		msec++;
	SDL_Delay(msec);
#endif
}

#ifndef _WIN32
	void Sleep(int ignored)
	{
		SDL_Delay(1);
	}
#endif

#ifdef _WIN32
//HWND GetHwnd()
DWORD GetHwnd()
{
	return (DWORD)hWndMain;
}

bool platform_MessageBox(const char* body, const char* title, MessageBoxType type)
{
	HWND hwnd = (HWND)GetHwnd();
	int result/*=-1*/;
	if(type==MESSAGEBOX_OK)
		result=MessageBox(hwnd, body, title, MB_ICONEXCLAMATION);
	else //if(type==MESSAGEBOX_OKCANCEL)
		result=MessageBox(hwnd, body, title, MB_ICONEXCLAMATION|MB_OKCANCEL);
	return result==IDOK;
}
#else
#include <gtk/gtk.h>

bool platform_MessageBox(const char *body, const char *title, MessageBoxType type)
{
	bool result/*=false*/;
	GtkWidget *pDialog;

	gtk_init(0, NULL);

	if(type==MESSAGEBOX_OK)
	{
		pDialog = gtk_message_dialog_new(
			NULL,
			GTK_DIALOG_MODAL,
			GTK_MESSAGE_INFO,
			GTK_BUTTONS_OK,
			body
		);
	}
	else //if(type==MESSAGEBOX_OKCANCEL)
	{
		pDialog = gtk_message_dialog_new(
			NULL,
			GTK_DIALOG_MODAL,
			GTK_MESSAGE_QUESTION,
			GTK_BUTTONS_OK_CANCEL,
			body
		);
	}
	gtk_window_set_title(GTK_WINDOW(pDialog), title);
	result = gtk_dialog_run(GTK_DIALOG(pDialog));
	//printf( "result = %s\n", result ? "true" : "false" );
	gtk_widget_destroy(pDialog);
	// The following loop is needed for the destroy event to be processed
	while (gtk_events_pending ())
		gtk_main_iteration ();
	return result;
}
#endif

#if 0
void platform_GetExecutablePath(char* str)
{
	GetModuleFileName(NULL, str, 512);
}
#endif

/**
 * Creates if necessary and returns the directory where config.txt can be
 * stored (trailing slash included).
 */
const char *platform_GetConfigDir(void)
{
	//printf("GetConfigDir\n");

	const int DOTCONFIG_LEN=7;
	const int MUSAGI_LEN=6;
	const char *basedir;
	static char *fulldir;

	if(fulldir)
		return fulldir;

	basedir=getenv("XDG_CONFIG_HOME");
	if(basedir)
	{
		//printf("GetConfigDir xdg\n");
		fulldir=(char *)malloc(strlen(basedir)+1+MUSAGI_LEN+1+1);
		if(fulldir==NULL)
			return "./";
		strcpy(fulldir, basedir);
	}
	else
	{
		// NOTE: on Mac OS X, should use "$HOME/Library/Preferences/"
		//       on Windown, "$APPDATA"
		//printf("GetConfigDir no xdg\n");
		basedir=getenv("HOME");
		if(basedir==NULL)
			return "./";
		fulldir=(char *)malloc(strlen(basedir)+1+DOTCONFIG_LEN+1+MUSAGI_LEN+1+1);
		if(fulldir==NULL)
			return "./";
		strcpy(fulldir, basedir);
		strcat(fulldir, "/.config");
	}
	// Create global config dir if it doesn't exist (unlikely)
	// NOTE: mkdir() is not ANSI but there's no dir function in ANSI C.
	mkdir(fulldir, 0700);
	strcat(fulldir, "/musagi");
	// Create dir if it doesn't exist
	mkdir(fulldir, 0700);
	strcat(fulldir, "/");
	//printf("GetConfigDir %s\n", fulldir);
	return fulldir;
}

/**
 * Changes the minimal latency accepted by PortAudio.
 * Maybe not a good idea; see http://www.portaudio.com/docs/latency.html
 */
void platform_set_portaudio_latency(const char *latency)
{
	setenv("PA_MIN_LATENCY_MSEC", latency, 1);
}

#ifdef MIDI
void platform_start_midi_thread()
{
	DWORD dwThreadId;
	hMidiThread=CreateThread(NULL,  			   // default security attributes
        					0,                 // use default stack size
				            midiplay_thread,        // thread function
				            (LPVOID)0,             // argument to thread function
				            0,                 // use default creation flags
				            &dwThreadId);   // returns the thread identifier
}
#endif
