#ifndef musagi_platform_h
#define musagi_platform_h

#include "DPInput.h"

extern DPInput *input;

struct KeyboardFrequency
{
	int scancode;
	int frequency;
};

const KeyboardFrequency g_KeyboardFrequencies[] =
{
	{ 52,	0 },	// w azerty, z qwerty, C
	{ 39,	1 },	// s, C#
	{ 53,	2 },	// x, D
	{ 40,	3 },	// d, D#
	{ 54,	4 },	// c, E
	{ 55,	5 },	// v, F
	{ 42,	6 },	// g, F#
	{ 56,	7 },	// b, G
	{ 43,	8 },	// h, G#
	{ 57,	9 },	// n, A
	{ 44,	10 },	// j, A#
	{ 58,	11 },	// , azerty, m qwerty, B
	{ 59,	12 },	// ; azerty, , qwerty, C
	{ 46,	13 },	// l, C#
	{ 60,	14 },	// : azerty, . qwerty, D
	{ 47,	15 },	// m azerty, ?? qwerty, D#
	{ 61,	16 },	// ! azerty, / qwerty, E
	{ 24,	12 },	// a/q, C
	{ 11,	13 },	// 2, C#
	{ 25,	14 },	// z/w, D
	{ 12,	15 },	// 3, D#
	{ 26,	16 },	// e, E
	{ 27,	17 },	// r, F
	{ 14,	18 },	// 5, F#
	{ 28,	19 },	// t, G
	{ 15,	20 },	// 6, G#
	{ 29,	21 },	// y, A
	{ 16,	22 },	// 7, A#
	{ 30,	23 },	// u, B
	{ 31,	24 },	// i, C
	{ 18,	25 },	// 9, C#
	{ 32,	26 },	// o, D
	{ 19,	27 },	// 0, D#
	{ 33,	28 }	// p, E
};

#define NUM_KB_KEYS (sizeof(g_KeyboardFrequencies)/sizeof(g_KeyboardFrequencies[0]))

#ifndef _WIN32
#include <SDL/SDL.h>
static inline Uint32 timeGetTime()
{
	return SDL_GetTicks();
}
void Sleep(int msec);
#endif

#ifdef MIDI
extern HANDLE hMidiThread;

void midiplay_routine();
DWORD WINAPI midiplay_thread(LPVOID lpParam);
#endif

void platform_Sleep(int msec);

#ifdef _WIN32
DWORD GetHwnd();
#endif

enum MessageBoxType {
	MESSAGEBOX_OK = 0,
	MESSAGEBOX_OKCANCEL = 1
};

bool platform_MessageBox(const char* body, const char* title, MessageBoxType type);

#if 0
void platform_GetExecutablePath(char* str);
#endif
const char *platform_GetConfigDir(void);

void platform_set_portaudio_latency(const char* latency);

void platform_init_keyboard();

#ifdef MIDI
void platform_start_midi_thread();
#endif

#endif
