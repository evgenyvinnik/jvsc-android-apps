#ifndef FILESELECTOR_H
#define FILESELECTOR_H

enum FileType {
	FILETYPE_SONG = 0,
	FILETYPE_PART = 1,
	FILETYPE_INST = 2,
	FILETYPE_WAVE = 3,
	FILETYPE_MIDI = 4,
	FILETYPE_VSTI = 5,
	MAX_FILETYPES
};

#define FILENAME_BUFFER_LEN 512

const char *GetCurDir(FileType type);
void SetCurDir(FileType type, const char *string, bool stripFile);

#ifdef _WIN32
#include <windows.h>

bool FileSelectorSave(HWND hwnd, char *filename, int type);
bool FileSelectorLoad(HWND hwnd, char *filename, int type);
bool FileSelectorLoad(HWND hwnd, char *filename, int type, char* title);
#else
//#include <SDL/SDL.h>

bool FileSelectorSave(/*SDL_Surface *screen,*/ char *filename, FileType type);
bool FileSelectorLoad(/*SDL_Surface *screen,*/ char *filename, FileType type);
bool FileSelectorLoad(/*SDL_Surface *screen,*/ char *filename, FileType type, char *title);
#endif

void FileSelectorInit();

#endif

