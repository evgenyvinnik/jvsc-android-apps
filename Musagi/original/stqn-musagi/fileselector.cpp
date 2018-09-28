#include "fileselector.h"
#include "musagi.h"

static char g_chosen_filename[MAX_FILETYPES][FILENAME_BUFFER_LEN];
static char g_chosen_file[MAX_FILETYPES][FILENAME_BUFFER_LEN];
#ifdef _WIN32
static char fsel_wave[128]="Wave Files (*.wav)\0*.wav\0All Files (*.*)\0*.*\0";
static char fsel_inst[128]="Instrument Files (*.imu)\0*.imu\0All Files (*.*)\0*.*\0";
static char fsel_part[128]="Part Files (*.pmu)\0*.pmu\0All Files (*.*)\0*.*\0";
static char fsel_song[128]="Song Files (*.smu)\0*.smu\0All Files (*.*)\0*.*\0";
static char fsel_midi[128]="Midi Files (*.mid)\0*.mid\0All Files (*.*)\0*.*\0";
#ifdef VST
static char fsel_vsti[128]="VSTi Files (*.dll)\0*.dll\0All Files (*.*)\0*.*\0";
#endif
static char fsel_defext[8];
#endif

void FileSelectorInit()
{
	for(int i=0;i<MAX_FILETYPES;i++)
	{
		g_chosen_filename[i][0]='\0';
		g_chosen_file[i][0]='\0';
	}
}

static char g_dirstrings[MAX_FILETYPES][FILENAME_BUFFER_LEN];

const char *GetCurDir(FileType type)
{
	return g_dirstrings[type];
}

void SetCurDir(FileType type, const char *string, bool stripFile)
{
	strcpy(g_dirstrings[type], string);

	if( stripFile )
	{
		int slen=strlen(string);
		int end;
		for(end=slen-1;end>0;end--)
			if(string[end]=='/')
				break;
		g_dirstrings[type][end]='\0';
	}
}

#ifdef _WIN32
char* GetFilter(FileType type)
{
	switch(type)
	{
	case 0:
		return fsel_song;
	case 1:
		return fsel_part;
	case 2:
		return fsel_inst;
	case 3:
		return fsel_wave;
	case 4:
		return fsel_midi;
#ifdef VST
	case 5:
		return fsel_vsti;
#endif
	}
	return NULL;
}
#endif

#ifdef _WIN32
bool FileSelectorSave(HWND hwnd, char *filename, FileType type)
{
	char *filter=GetFilter(type);
	static OPENFILENAME dia;
	dia.lStructSize = sizeof(OPENFILENAME);
	dia.hwndOwner = hwnd;
	dia.lpstrFile = g_chosen_filename[type];
	dia.nMaxFile = _MAX_DIR;
	dia.lpstrFileTitle = g_chosen_file[type];
	dia.nMaxFileTitle = _MAX_FNAME;
	dia.lpstrInitialDir = GetCurDir(type);
	dia.lpstrFilter = filter;
	dia.lpstrDefExt = fsel_defext;
	dia.lpstrTitle = "Save As";
	dia.Flags = OFN_EXPLORER | OFN_OVERWRITEPROMPT;
	if(!GetSaveFileName(&dia))
		return false;
	strcpy(filename, g_chosen_filename[type]);
	SetCurDir(type, g_chosen_filename[type]);
	return true;
}

bool FileSelectorLoad(HWND hwnd, char *filename, FileType type, char* title)
{
	char *filter=GetFilter(type);
	static OPENFILENAME dia;
	dia.lStructSize = sizeof(OPENFILENAME);
	dia.hwndOwner = hwnd;
	dia.lpstrFile = g_chosen_filename[type];
	dia.nMaxFile = _MAX_DIR;
	dia.lpstrFileTitle = g_chosen_file[type];
	dia.nMaxFileTitle = _MAX_FNAME;
	dia.lpstrInitialDir = GetCurDir(type);
	dia.lpstrFilter = filter;
	dia.lpstrDefExt = fsel_defext;
	dia.lpstrTitle = title;
	dia.Flags = OFN_EXPLORER | OFN_PATHMUSTEXIST;
	if(!GetOpenFileName(&dia))
		return false;
	strcpy(filename, g_chosen_filename[type]);
	SetCurDir(type, g_chosen_filename[type]);
	return true;
}

bool FileSelectorLoad(HWND hwnd, char *filename, FileType type)
{
	return FileSelectorLoad(hwnd, filename, type, "Load");
}
#else
#include <gtk/gtk.h>

bool FileSelector(char *filename, FileType type, const char *title, GtkFileChooserAction action);

bool FileSelectorSave(char *filename, FileType type)
{
	return FileSelector(filename, type, "Save...", GTK_FILE_CHOOSER_ACTION_SAVE);
}

bool FileSelectorLoad(char *filename, FileType type, const char *title)
{
	return FileSelector(filename, type, title, GTK_FILE_CHOOSER_ACTION_OPEN);
}

bool FileSelector(char *filename, FileType type, const char *title, GtkFileChooserAction action)
{
	GtkWidget *pFileSelection;

	gtk_init(0, NULL);

	pFileSelection = gtk_file_chooser_dialog_new(
		title,
		NULL,
		action,
		GTK_STOCK_CANCEL, GTK_RESPONSE_CANCEL,
		GTK_STOCK_OPEN, GTK_RESPONSE_OK,
		NULL
	);

	gtk_file_chooser_set_filename (GTK_FILE_CHOOSER (pFileSelection), g_chosen_filename[type]);

	bool ret;
	gchar *path;
	switch(gtk_dialog_run(GTK_DIALOG(pFileSelection)))
	{
		case GTK_RESPONSE_OK:
			path = gtk_file_chooser_get_filename(GTK_FILE_CHOOSER(pFileSelection));
			strcpy(filename, path);
			strcpy(g_chosen_filename[type], filename);
			SetCurDir(type, filename, true);
			g_free(path);
			ret = true;
			break;
		default:
			ret = false;
			break;
	}
	gtk_widget_destroy(pFileSelection);
	// The following loop is needed for the destroy event to be processed
	while (gtk_events_pending ())
		gtk_main_iteration ();
	return ret;
}

bool FileSelectorLoad(char *filename, FileType type)
{
	return FileSelectorLoad(filename, type, "Load...");
}
#endif
