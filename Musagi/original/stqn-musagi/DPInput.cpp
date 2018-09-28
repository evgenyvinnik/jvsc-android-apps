#include <assert.h>
#include "DPInput.h"
#include "Log.h"
#include <SDL/SDL.h>

DPInput *ginput;

DPInput::DPInput()
{
	ginput=this;
        joystick = NULL;
        if (SDL_NumJoysticks() > 0)
            joystick = SDL_JoystickOpen(0);
	SDL_EnableUNICODE(1);
	//memset( m_keys, 0, 256 );
	memset( m_keysScancode, 0, 256 );
};

DPInput::~DPInput()
{
};

void DPInput::Update()
{
	m_keys = SDL_GetKeyState(&m_numkeys);
};

void DPInput::SetKeyPressed(int scancode/*, SDLKey key*/, bool pressed)
{
	assert(scancode>=0 && scancode<=255);
	//assert(key>=0 && key<=255);
	//m_keys[key]=pressed;
	m_keysScancode[scancode]=pressed;
}

bool DPInput::KeyPressedScancode(int key)
{
	return m_keysScancode[key];
};

bool DPInput::KeyPressed(SDLKey key)
{
//	if(diKeys[key]&0x80)
//		return true;
        //if (diKeys[key])
	assert(key>=0 && key<m_numkeys);
	return m_keys[key];
};

#if 0
bool DPInput::JoyButton(int index)
{
	if(!enabled)
		return false;
//	LogPrint("dinput: checking button %i (numbuttons=%i)", index, joysticks[cur_joystick].num_buttons);
	index+=4; // Translate to actual button mapping (to allow for d-pad buttons)

//	if(nojoystick || index<0 || index>=joysticks[cur_joystick].num_buttons+4)
//		return false;

/*	if(joysticks[cur_joystick].button[index])
		LogPrint("dinput: joybutton %i=true", index);
	else
		LogPrint("dinput: joybutton %i=false", index);*/
	return joysticks[cur_joystick].button[index];
};
/*
bool DPInput::IsAnalog()
{
	return analog;
};
*/
int DPInput::NumJoyAxes()
{
	if(nojoystick)
		return 0;
	return joysticks[cur_joystick].num_axes;
};

int DPInput::NumJoyButtons()
{
	if(nojoystick)
		return 0;
	return joysticks[cur_joystick].num_buttons;
};

void DPInput::SelectJoystick(int id)
{
	if(id<0) id=0;
	if(id>=num_joysticks) id=num_joysticks-1;
	cur_joystick=id;
};
#endif // if 0

float DPInput::JoyAxis(int axis)
{
    Sint16 intvalue;
    if (joystick) {
        intvalue = SDL_JoystickGetAxis(joystick, axis);
        return ((float)intvalue + 0.5) / 32767.5f;
    }
    return 0.0f;
};

void DPInput::Disable()
{
	enabled=false;
};

void DPInput::Enable()
{
	enabled=true;
};


