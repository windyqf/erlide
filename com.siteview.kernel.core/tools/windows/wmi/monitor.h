#pragma once


// ������뽫λ������ָ��ƽ̨֮ǰ��ƽ̨��ΪĿ�꣬���޸����ж��塣
// �йز�ͬƽ̨��Ӧֵ��������Ϣ����ο� MSDN��
#ifndef WINVER				// ����ʹ���ض��� Windows XP ����߰汾�Ĺ��ܡ�
#define WINVER 0x0501		// ����ֵ����Ϊ��Ӧ��ֵ���������� Windows �������汾��
#endif

#ifndef _WIN32_WINNT		// ����ʹ���ض��� Windows XP ����߰汾�Ĺ��ܡ�
#define _WIN32_WINNT 0x0501	// ����ֵ����Ϊ��Ӧ��ֵ���������� Windows �������汾��
#endif						

#ifndef _WIN32_WINDOWS		// ����ʹ���ض��� Windows 98 ����߰汾�Ĺ��ܡ�
#define _WIN32_WINDOWS 0x0410 // ����ֵ����Ϊ�ʵ���ֵ����ָ���� Windows Me ����߰汾��ΪĿ�ꡣ
#endif

#ifndef _WIN32_IE			// ����ʹ���ض��� IE 6.0 ����߰汾�Ĺ��ܡ�
#define _WIN32_IE 0x0600	// ����ֵ����Ϊ��Ӧ��ֵ���������� IE �������汾��
#endif

#define WIN32_LEAN_AND_MEAN		// �� Windows ͷ���ų�����ʹ�õ�����
// Windows ͷ�ļ�:
#include <windows.h>

#include <string>
#include <map>
#include <list>
#include <vector>
#include <algorithm>
#include <functional>
#include <cctype> 
using namespace std;

#import "progid:WbemScripting.SWbemLocator" named_guids

#include <comdef.h>
#include <wbemcli.h>

#include <winbase.h>

#pragma comment(lib,"WbemUuid.Lib")

#define BUFFER_SIZE 256

typedef enum _wmi_operation_type_
{
	cpu = 1,
	memory,
	disk,
	service,
	process,
	network,
	directory
} WMI_OPERATION_TYPE;

typedef enum _os_type_
{
	Win32s = 1,
	Win95,
	Win98,
	WinME,
	WinNT351,
	WinNT4,
	Win2000,
	WinXP,
	Win2003,
	WinCE,
	Win2008,
	Vista,
	Win7
} OS_TYPE;


typedef struct _wmi_login_info_
{
	int os;
	char machine[BUFFER_SIZE];
	char user[BUFFER_SIZE];
	char password[BUFFER_SIZE];
} WMI_LOGIN_INFO, *PWMI_LOGIN_INFO;

BOOL IsLocalHost(string host);

BOOL ConnectServer(PWMI_LOGIN_INFO login, char* buffer, WbemScripting::ISWbemServicesPtr &services);

BOOL AnsiToUtf8(char* strAnsi, char* strUtf8, int size);