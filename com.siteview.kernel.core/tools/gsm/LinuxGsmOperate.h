#include <stdio.h>      /*��׼�����������*/
#include <stdlib.h>     /*��׼�����ⶨ��*/
#include <unistd.h>     /*Unix ��׼��������*/
#include <sys/types.h>  
#include <sys/stat.h>   
#include <fcntl.h>      /*�ļ����ƶ���*/
#include <termios.h>    /*PPSIX �ն˿��ƶ���*/
#include <errno.h>      /*����Ŷ���*/

#include  <string.h>
#include   <iconv.h>
#include  <stdint.h>


#define MaxSMSLen 70 //�����ų���

#define GSM_7BIT                        0
#define GSM_8BIT                        4
#define GSM_UCS2                      8

typedef unsigned long DWORD;



enum
    {
        OpenPortFailed = -1,//�򿪶˿�ʧ��
        NoSetCenter = -2,//û�����ö�������
        ShortSleepTime = 500
    };

struct MsgList
	{
		char chMsg[200];
		struct MsgList *pNext;
	};

    
struct SM_PARAM
	{
        char SCA[16];       // ����Ϣ�������ĺ���(SMSC��ַ)
        char TPA[16];       // Ŀ������ظ�����(TP-DA��TP-RA)
        char TP_PID;        // �û���ϢЭ���ʶ(TP-PID)
        char TP_DCS;        // �û���Ϣ���뷽ʽ(TP-DCS)
        char TP_SCTS[16];   // ����ʱ����ַ���(TP_SCTS), ����ʱ�õ�
        char TP_UD[161];    // ԭʼ�û���Ϣ(����ǰ�������TP-UD)
        char index;         // ����Ϣ��ţ��ڶ�ȡʱ�õ�
	};    


    
int  fd;
char* m_strMsgContent;
char* m_strRecvPhone;
char* m_SmsCenterNum;    
//�ɴ�ӡ�ַ���ת��Ϊ�ֽ�����    
//int gsmString2Bytes(const char*, unsigned char*, int);  

//�ֽ�����ת��Ϊ�ɴ�ӡ�ַ���    
//int gsmBytes2String(const unsigned char*, char*, int); 

//PDU���뺯�������ڽ��ա��Ķ�����Ϣ      
//int gsmDecodePdu(const char*, SM_PARAM*);

//PDU���뺯�������ڱ��ơ����Ͷ���Ϣ    
//int gsmEncodePdu(const SM_PARAM*, char*);
    
int open_SerialPort(const char *);

int initPort(const char *,int ,int ,int ,int );

void set_speed(int , int);

int set_Parity(int ,int ,int ,int);

int WritePort(int ,char *, int);

int ReadPort(int ,char *, int);

int gsmDeleteMessage(int ,const int );

int gsmString2Bytes(const char* , unsigned char* , int );

int gsmBytes2String(const unsigned char* , char* , int );

int gsmSerializeNumbers(const char* , char* , int );

int gsmInvertNumbers(const char* , char* , int );

int gsmDecodePdu(const char* , struct SM_PARAM* );

int gsmEncodePdu(const struct SM_PARAM* , char* );

int gsmDecode8bit(const unsigned char *, char *, int );

int gsmEncode8bit(const char *, unsigned char *, int );

int mygsmDecodeUcs2(char* , char* , int);

int mygsmEncodeUcs2(char* , char* , int );

int gsmDecode7bit(const unsigned char* , char* , int );

int gsmEncode7bit(const char* , unsigned char* , int );

int unicode2utf8(uint16_t *, int , uint8_t **);

int ucs2Count(const unsigned char *);

int gsmSendMessage(int ,const struct SM_PARAM* );

int send_msg(int ,char*  , char* , int);

void close_SerialPort(int);  

int test();
