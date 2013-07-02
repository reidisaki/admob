/*
 *	Commons for FW Modules
 */

#ifndef XASSIGN
#define XASSIGN
#define XRETAIN 
#define XCOPY 
#define XATOMIC 
#define XREADONLY
#define XIBOUTLET
#define XNIL nil
#define XSUPERINIT if(!(self=[super init])){return nil;}
#define XSELFINIT if(!(self=[self init])){return nil;}
#define XSINGLETON \
+ (void)initialize { if (!_instance) { _instance = [[self alloc] init]; } } \
+ (id)instance { return _instance; }
#define FWLog(format, ...) [_rendererController log:[NSString stringWithFormat:(@"%s #%d " format), __PRETTY_FUNCTION__, __LINE__, ##__VA_ARGS__]];
#define FWDebug FWLog
#define FW_RDK_VERSION [_rendererController performSelector:@selector(getVersionNumber:) withObject:FW_SDK_VERSION]
#endif
