#ifndef MUSAGI_LOG_H
#define MUSAGI_LOG_H

void LogStart(void);

void LogPrint(const char *string, ...);
void LogPrintf(const char *string, ...);

void LogEnable();
void LogDisable();

#endif
