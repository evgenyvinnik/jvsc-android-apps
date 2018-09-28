# dependencies are not handled so everything is rebuilt each time.

OBJECTS=Config.o DPInput.o fftsg.o fileselector.o Log.o main.o part.o \
 platform.o song.o Texture.o

musagi: clean ${OBJECTS}
	g++ -o $@ ${OBJECTS} `pkg-config --libs sdl gl glu gtk+-2.0 portaudio-2.0`

%.o: %.cpp %.h
	g++ `pkg-config --cflags sdl gl glu gtk+-2.0 portaudio-2.0` -c -o $@ $<

.PHONY: clean
clean:
	rm -f ${OBJECTS} musagi
