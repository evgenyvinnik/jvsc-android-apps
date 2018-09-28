#ifdef _WIN32
#define _WIN32_WINDOWS 0xBAD
#define _WIN32_WINNT 0x0400

#include <windows.h>
#endif
#include <SDL/SDL.h>
#include <GL/gl.h>
#include <GL/glu.h>
#include <GL/glext.h>
#include <stdio.h>
#include <math.h>
#include <string.h>
#include "Log.h"

#ifdef _WIN32
HWND		hWndMain=NULL;
HINSTANCE	hInstanceMain;
#endif

#define DWORD unsigned long

#include "platform.h"
#include "Texture.h"

#include "glkit_global.h"
/*
// vsync stuff
typedef BOOL	(APIENTRY * PFNWGLSWAPINTERVALEXT)(int interval);
typedef int		(APIENTRY * PFNWGLGETSWAPINTERVALEXT)();
PFNWGLSWAPINTERVALEXT		wglSwapIntervalEXT = NULL;
PFNWGLGETSWAPINTERVALEXT	wglGetSwapIntervalEXT = NULL;
#define LOAD_EXTENSION(name) *((void**)&name) = wglGetProcAddress(#name)
bool	vsync_control_enabled;
*/

//HDC		hDC=NULL;
//HGLRC	hRC=NULL;
//LRESULT	CALLBACK WndProc(HWND, UINT, WPARAM, LPARAM);
SDL_Surface *screen;

bool	keys[256];
bool	active=true;
//bool    fullscreen;

int glkit_width;
int glkit_height;
bool glkit_fullscreen; // always false, not saved in config...
bool glkit_close=false;
bool glkit_tracking=false;
bool glkit_resized=false;

int glkit_winx;
int glkit_winy;
//int glkit_winmax;
bool glkit_render=true;

bool glkit_halfres=false;

bool glkitHalfRes()
{
	return glkit_halfres;
}

void glkitSetHalfRes(bool value)
{
	glkit_halfres=value;
}

glkit_mouse glkmouse;
char glkkey;

//HCURSOR cursor_arrow;

//int paint_requests; // to detect if the main loop is running/rendering or if we need to handle WM_PAINT

void glkPreInit();
void glkInit(char* cmd);
void glkFree();
void glkRenderFrame(bool disabled);
bool glkCalcFrame();

/*HWND glkitGetHwnd()
{
	return hWndMain;
}*/

char glkitGetKey()
{
	return glkkey;
}

void glkitResetKey()
{
	glkkey=0;
}

int glkitGetWidth()
{
	if(glkit_halfres)
		return glkit_width/2;
	else
		return glkit_width;
}

int glkitGetHeight()
{
	if(glkit_halfres)
		return glkit_height/2;
	else
		return glkit_height;
}

void glkitShowMouse(bool on)
{
	if(on)
		SDL_ShowCursor(SDL_ENABLE);
		//ShowCursor(TRUE);
	else
		SDL_ShowCursor(SDL_DISABLE);
		//ShowCursor(FALSE);
}

void glkitInternalRender(bool disabled) // disabled==true means no input should be handled
{
	glDisable(GL_SCISSOR_TEST);
	glClear(GL_COLOR_BUFFER_BIT);
	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	glEnable(GL_BLEND);

	glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

	glkRenderFrame(disabled);

	if(abort_render)
	{
		LogPrint("abort_render in glkit");
		abort_render=false;
		return;
	}

	glDisable(GL_BLEND);
	glFlush();

	//SwapBuffers(hDC);
	SDL_GL_SwapBuffers();
}

GLvoid ReSizeGLScene(GLsizei width, GLsizei height)
{
	if(height==0) height=1;

	glkit_width=width;
	glkit_height=height;

	glViewport(0,0,width,height);

	glMatrixMode(GL_PROJECTION);
	glLoadIdentity();
	if(glkit_halfres)
		glOrtho(0,width/2, height/2,0, 1, -1);
	else
		glOrtho(0,width, height,0, 1, -1);
	glMatrixMode(GL_MODELVIEW);
	glLoadIdentity();
}

bool InitGL(void)
{
	glEnable(GL_TEXTURE_2D);
	glShadeModel(GL_SMOOTH);
	glClearColor(0.5f, 0.5f, 0.5f, 0);
	glClearDepth(1.0f);
	glDepthMask(0);
	glDisable(GL_DEPTH_TEST);
	glHint(GL_PERSPECTIVE_CORRECTION_HINT, GL_NICEST);
	glPolygonMode(GL_FRONT_AND_BACK,GL_FILL);
	glDisable(GL_CULL_FACE);

/*	LOAD_EXTENSION(wglSwapIntervalEXT);
	LOAD_EXTENSION(wglGetSwapIntervalEXT);
	if( wglSwapIntervalEXT==NULL ||
		wglGetSwapIntervalEXT==NULL)
			vsync_control_enabled=false;
	else
		vsync_control_enabled=true;
	if(vsync_control_enabled)
		wglSwapIntervalEXT(1);
*/
	return true;
}

GLvoid KillGLWindow(void)
{
	SDL_Quit();
}

bool CreateGLWindow(const char* title, int width, int height, int bits, bool fullscreen)
{
	LogPrint("CreateGLWindow: start");

    Uint32 flags = 0;
    if (fullscreen)
        flags |= SDL_FULLSCREEN;
    flags |= SDL_OPENGL;
    flags |= SDL_GL_DOUBLEBUFFER;
    flags |= SDL_HWPALETTE;
    flags |= SDL_RESIZABLE;

    SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1);

    screen = SDL_SetVideoMode(width, height, bits, flags);

    if (!screen)
        return false;

    SDL_WM_SetCaption(title, NULL); // title, icon

    InitGL();

    ReSizeGLScene(width,height);

	LogPrint("CreateGLWindow: done");

    return true;
}

bool HandleEvent(SDL_Event event)
{
	switch (event.type)									// Check For Windows Messages
	{
	case SDL_ACTIVEEVENT:							// Watch For Window Activate Message
		if (event.active.gain)					// Check Minimization State
			active=true;						// Program Is Active
		else
			active=false;						// Program Is No Longer Active
		return 0;								// Return To The Message Loop

        // We could perhaps do something with SDL_SYSWMEVENT here, but it looks
        // like it might get hairy, so I'm ignoring it for now.
        /*
	case WM_SYSCOMMAND:							// Intercept System Commands
		switch (wParam)							// Check System Calls
		{
		case SC_SCREENSAVE:					// Screensaver Trying To Start?
		case SC_MONITORPOWER:				// Monitor Trying To Enter Powersave?
			return 0;							// Prevent From Happening
		}
		break;									// Exit
        */
	case SDL_QUIT:								// Did We Receive A Close Message?
		glkit_close=true;
		//PostQuitMessage(0);						// Send A Quit Message
		return 0;								// Jump Back

	case SDL_VIDEORESIZE:								// Resize The Window
		glkit_resized=true;
		glkit_width=event.resize.w;
		glkit_height=event.resize.h;
		ReSizeGLScene(glkit_width, glkit_height);  // LoWord=Width, HiWord=Height
		return 0;								// Jump Back

	case SDL_MOUSEMOTION:
                // What's this?
		/*if(!glkit_tracking)
		{
			glkit_tracking=true;
			TRACKMOUSEEVENT tme;
			tme.cbSize=sizeof(TRACKMOUSEEVENT);
			tme.dwFlags=TME_LEAVE;
			tme.hwndTrack=hWndMain;
			tme.dwHoverTime=HOVER_DEFAULT;
			TrackMouseEvent(&tme);
		}*/
		int curwidth, curheight;
//                SDL_Rect rect;
		//GetClientRect(hWndMain, &rect);
		curwidth=screen->w;//rect.right-rect.left;
		curheight=screen->h;//rect.bottom-rect.top;
		glkmouse.glk_mousex=(int)((float)event.motion.x/curwidth*glkit_width);
		glkmouse.glk_mousey=(int)((float)event.motion.y/curheight*glkit_height);
		if(glkit_halfres)
		{
			glkmouse.glk_mousex/=2;
			glkmouse.glk_mousey/=2;
		}
		return 0;
        // Moving this code to case SDL_MOUSEBUTTONDOWN
	/*case WM_MOUSEWHEEL:
		if((wParam>>16)&0x7FFF)
		{
		}
		return 0;*/
	/*case WM_LBUTTONDBLCLK:
		glkmouse.glk_mousedoubleclick=true;
		return 0;*/
	case SDL_MOUSEBUTTONDOWN://WM_LBUTTONDOWN:
            switch(event.button.button) {
            case SDL_BUTTON_LEFT:
		// Detects double clicks
		{
			static Uint32 last_left_click_tick=0;
			Uint32 tick = SDL_GetTicks();
			if(tick-last_left_click_tick<500)	// I'd love to use system settings but it's a pain.
			{
				glkmouse.glk_mousedoubleclick=true;
				last_left_click_tick=0;	// Prevents creation of 2 double clicks from 3 clicks in a row
			}
			else
			{
				last_left_click_tick=tick;
			}
		}
		glkmouse.glk_mouseleft=true;
		glkmouse.glk_mouseleftclick=5;
		//GetClientRect(hWndMain, &rect);
		curwidth=screen->w;//rect.right-rect.left;
		curheight=screen->h;//rect.bottom-rect.top;
		glkmouse.glk_cmx=(int)((float)event.button.x/curwidth*glkit_width);
		glkmouse.glk_cmy=(int)((float)event.button.y/curheight*glkit_height);
		if(glkit_halfres)
		{
			glkmouse.glk_cmx/=2;
			glkmouse.glk_cmy/=2;
		}
		return 0;
            case SDL_BUTTON_RIGHT:
		glkmouse.glk_mouseright=true;
		glkmouse.glk_mouserightclick=5;
		curwidth=screen->w;
		curheight=screen->h;
		glkmouse.glk_cmx=(int)((float)event.button.x/curwidth*glkit_width);
		glkmouse.glk_cmy=(int)((float)event.button.y/curheight*glkit_height);
		if(glkit_halfres)
		{
			glkmouse.glk_cmx/=2;
			glkmouse.glk_cmy/=2;
		}
		return 0;
            case SDL_BUTTON_MIDDLE:
		glkmouse.glk_mousemiddle=true;
		glkmouse.glk_mousemiddleclick=5;
		curwidth=screen->w;
		curheight=screen->h;
		glkmouse.glk_cmx=(int)((float)event.button.x/curwidth*glkit_width);
		glkmouse.glk_cmy=(int)((float)event.button.y/curheight*glkit_height);
		if(glkit_halfres)
		{
			glkmouse.glk_cmx/=2;
			glkmouse.glk_cmy/=2;
		}
		return 0;
            case SDL_BUTTON_WHEELUP:
                glkmouse.glk_mousewheel=-1;
                return 0;
            case SDL_BUTTON_WHEELDOWN:
                glkmouse.glk_mousewheel=1;
                return 0;
            }
        case SDL_MOUSEBUTTONUP:
            switch(event.button.button) {
            case SDL_BUTTON_LEFT:
		glkmouse.glk_mouseleft=false;
		return 0;
            case SDL_BUTTON_RIGHT:
		glkmouse.glk_mouseright=false;
		return 0;
            case SDL_BUTTON_MIDDLE:
		glkmouse.glk_mousemiddle=false;
		return 0;
            }
	case SDL_KEYDOWN:
                if ((event.key.keysym.unicode & 0xFF80) == 0) {
                    glkkey=event.key.keysym.unicode & 0x7F;
                }
                else {
                    glkkey=0; // this is kinda not right, but what can I do?
                }
		//printf("key scancode=%d, sim=%d, unicode=%d\n", event.key.keysym.scancode, event.key.keysym.sym, event.key.keysym.unicode);
		input->SetKeyPressed(event.key.keysym.scancode/*, event.key.keysym.sym*/, true);
		return 0;
	case SDL_KEYUP:
		input->SetKeyPressed(event.key.keysym.scancode/*, event.key.keysym.sym*/, false);
		return 0;
	}

	return 1;
}


int main(int argc, char **argv)
{
	SDL_Event event;

	glkkey=0;

	glkPreInit();

	SDL_Init(SDL_INIT_VIDEO);

	if(!CreateGLWindow("musagi-stqn 1.0.1 beta", glkit_width, glkit_height, 32, glkit_fullscreen))
		return 0;

	Uint32 startTime;
	Uint32 frequency;
	bool glkit_timeravailable;

	/*if(!QueryPerformanceFrequency(&frequency))
		glkit_timeravailable=false;
	else
	{
		glkit_timeravailable=true;
		QueryPerformanceCounter(&startTime);
	}*/
        glkit_timeravailable=true;
        frequency = 1000;
        startTime = SDL_GetTicks();

	char lpCmdLine[512];
	lpCmdLine[0]='\0';
	for(int i=1;i<argc;i++)
	{
		strcat(lpCmdLine, argv[i]);
		strcat(lpCmdLine, " ");
	}
	if(argc>0)
		lpCmdLine[strlen(lpCmdLine)-1]='\0';

	glkInit(lpCmdLine);

	bool done=false;
	while(!done)
	{
		SDL_Delay(5);//Sleep(5);

		glkit_resized=false;
		glkmouse.glk_mousewheel=0;
		glkmouse.glk_mousedoubleclick=false;
                while (SDL_PollEvent(&event)) {
                    HandleEvent(event);
                }

		// No idea what this is for... It lets us detect very short mouse clicks,
		// but why make them last 5 frames and not 1?
		if(glkmouse.glk_mouseleft && glkmouse.glk_mouseleftclick==5) glkmouse.glk_mouseleftclick=0;
		if(glkmouse.glk_mouseright && glkmouse.glk_mouserightclick==5) glkmouse.glk_mouserightclick=0;
		if(glkmouse.glk_mousemiddle && glkmouse.glk_mousemiddleclick==5) glkmouse.glk_mousemiddleclick=0;
		if(glkmouse.glk_mouseleftclick>0) glkmouse.glk_mouseleft=true;
		if(glkmouse.glk_mouserightclick>0) glkmouse.glk_mouseright=true;
		if(glkmouse.glk_mousemiddleclick>0) glkmouse.glk_mousemiddle=true;

		if(!glkCalcFrame())
		{
		    done=true;
		    break;
		}

		if(glkit_render)
			glkitInternalRender(false);

		if(glkmouse.glk_mouseleft && glkmouse.glk_mouseleftclick==5) glkmouse.glk_mouseleftclick=0;
		if(glkmouse.glk_mouseright && glkmouse.glk_mouserightclick==5) glkmouse.glk_mouserightclick=0;
		if(glkmouse.glk_mousemiddle && glkmouse.glk_mousemiddleclick==5) glkmouse.glk_mousemiddleclick=0;
		if(glkmouse.glk_mouseleftclick>0) glkmouse.glk_mouseleft=true;
		if(glkmouse.glk_mouserightclick>0) glkmouse.glk_mouseright=true;
		if(glkmouse.glk_mousemiddleclick>0) glkmouse.glk_mousemiddle=true;
	}

	//glkit_winmax=IsZoomed(hWndMain);

	glkFree();

	KillGLWindow();

	LogPrint("end of WinMain() -> exit program");

	return 0;
}
