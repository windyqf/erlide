//linux�´��ڶ���è��������
//ʹ�õ�libiconv-1.13.1
//��̬�����GsmOperate_driver_output

#include "LinuxGsmOperate.h"

int test()
{
    int a;
    a = 100;
    return a;
}

/********************
*@brief     �򿪴���
*@param  DEVICE: ���� int,  �򿪴��ڵ��ļ����
*@return  fd: ���� int, ���ھ��
********************/
int open_SerialPort(const char *DEVICE){
    int fd;
    fd = open(DEVICE,O_RDWR);
    if (-1==fd){
        /*���ܴ򿪴���*/
        perror("can not open this serial port!");        
    }
    return fd;    
}

/**********************
*@brief     �رմ���
***********************/
void close_SerialPort(int fd)
{
    close(fd);
}

/******************
*@brief  ��ʼ������
*@param fd ���� int  �򿪴��ڵ��ļ����
*@param  speed  ���� int, �����ٶ�
*@param databits ���� int,  ����λ   ȡֵ Ϊ 7 ����8
*@param stopbits ���� int,  ֹͣλ   ȡֵΪ 1 ����2
*@param parity ���� int,  Ч������ ȡֵΪN,E,O,,S
*@return  int
******************/
int initPort(const char *DEVICE,int speed,int databits,int stopbits,int parity){
    int fd;
    int pt;
    fd = open_SerialPort(DEVICE);
    if (-1 == fd){
        perror("can not open this serial port!");
        exit(-1);        
    }
    set_speed(fd,speed);
    pt = set_Parity(fd, databits,stopbits,parity);
    return pt;    
}

/*******************
*@brief  ���ô���ͨ������
*@param  fd     ���� int  �򿪴��ڵ��ļ����
*@param  speed  ���� int  �����ٶ�
*@return  void
********************/
int speed_arr[] = { B38400, B19200, B9600, B4800, B2400, B1200, B300,
					B38400, B19200, B9600, B4800, B2400, B1200, B300,};
int name_arr[] = {38400,  19200,  9600,  4800,  2400,  1200,  300, 38400,  
					19200,  9600, 4800, 2400, 1200,  300,};
void set_speed(int fd, int speed){
	int   i; 
	int   status; 
	struct termios   Opt;
	tcgetattr(fd, &Opt); 
	for ( i= 0;  i < sizeof(speed_arr) / sizeof(int);  i++) { 
		if  (speed == name_arr[i]) {     
			tcflush(fd, TCIOFLUSH);     
			cfsetispeed(&Opt, speed_arr[i]);  
			cfsetospeed(&Opt, speed_arr[i]);   
			status = tcsetattr(fd, TCSANOW, &Opt);  
			if  (status != 0) {        
				perror("tcsetattr fd");  
				return;     
			}    
			tcflush(fd,TCIOFLUSH);   
		}  
	}
}


/**********************
*@brief   ���ô�������λ��ֹͣλ��Ч��λ
*@param  fd     ����  int  �򿪵Ĵ����ļ����
*@param  databits ����  int ����λ   ȡֵ Ϊ 7 ����8
*@param  stopbits ����  int ֹͣλ   ȡֵΪ 1 ����2
*@param  parity  ����  int  Ч������ ȡֵΪN,E,O,,S
***********************/
int set_Parity(int fd,int databits,int stopbits,int parity)
{ 
	struct termios options; 
	if  ( tcgetattr( fd,&options)  !=  0) { 
		perror("SetupSerial 1");     
		return(-1);  
	}
	options.c_cflag &= ~CSIZE; 
	switch (databits) /*��������λ��*/
	{   
	case 7:		
		options.c_cflag |= CS7; 
		break;
	case 8:     
		options.c_cflag |= CS8;
		break;   
	default:    
		fprintf(stderr,"Unsupported data size\n"); 
        return (-1);  
	}
switch (parity) 
{   
	case 'n':
	case 'N':    
		options.c_cflag &= ~PARENB;   /* Clear parity enable */
		options.c_iflag &= ~INPCK;     /* Enable parity checking */ 
		break;  
	case 'o':   
	case 'O':     
		options.c_cflag |= (PARODD | PARENB); /* ����Ϊ��Ч��*/  
		options.c_iflag |= INPCK;             /* Disnable parity checking */ 
		break;  
	case 'e':  
	case 'E':   
		options.c_cflag |= PARENB;     /* Enable parity */    
		options.c_cflag &= ~PARODD;   /* ת��ΪżЧ��*/     
		options.c_iflag |= INPCK;       /* Disnable parity checking */
		break;
	case 'S': 
	case 's':  /*as no parity*/   
	    options.c_cflag &= ~PARENB;
		options.c_cflag &= ~CSTOPB;break;  
	default:   
		fprintf(stderr,"Unsupported parity\n");    
		return (-1);  
	}  
/* ����ֹͣλ*/  
switch (stopbits)
{   
	case 1:    
		options.c_cflag &= ~CSTOPB;  
		break;  
	case 2:    
		options.c_cflag |= CSTOPB;  
	   break;
	default:    
		 fprintf(stderr,"Unsupported stop bits\n");  
		 return (-1); 
} 
/* Set input parity option */ 
if (parity != 'n')   
	options.c_iflag |= INPCK; 
tcflush(fd,TCIFLUSH);
options.c_cc[VTIME] = 150; /* ���ó�ʱ15 seconds*/   
options.c_cc[VMIN] = 0; /* Update the options and do it NOW */
if (tcsetattr(fd,TCSANOW,&options) != 0)   
{ 
	perror("SetupSerial 3");   
	return (-1);  
} 
return (0);  
}


/*
*@brief   д���ڡ�
*@param   fd:�����ļ������
*@param   pData: ��д�����ݻ�����ָ�롣
*@param  nLength: ��д�����ݳ��ȡ�
*@return  int�� 
*/
int WritePort(int fd,char *pData, int nLength)
{   
    return write(fd,pData,nLength);
}

/*
*@brief     �����ڡ�
*@param   fd:�����ļ������
*@param   pData: ���������ݻ�����ָ�롣
*@param   nLength: ������������ݳ��ȡ�
*@return   ʵ�ʶ�������ݳ��ȡ� 
*/
int ReadPort(int fd,char *pData, int nLength)
{  
    char buff[nLength];
    int nread = read(fd,buff,nLength);    
    return nread;
}

/* 
*@brief     �ɴ�ӡ�ַ���ת��Ϊ�ֽ�����,�磺"C8329BFD0E01" --> {0xC8, 0x32, 0x9B, 0xFD, 0x0E, 0x01}                        

                                            
*@param   pSrc: Դ�ַ���ָ��                                                    
*@param  pDst: Ŀ������ָ��
*@param  nSrcLength: Դ�ַ�������   
*@return  Ŀ�����ݳ���    
*/
int gsmString2Bytes(const char* pSrc, unsigned char* pDst, int nSrcLength)
{
    for(int i=0; i<nSrcLength; i+=2)
    {
        // �����4λ
        if(*pSrc>='0' && *pSrc<='9')
        {
            *pDst = (*pSrc - '0') << 4;
		}
        else
        {
            *pDst = (*pSrc - 'A' + 10) << 4;
        }
    
        pSrc++;
        // �����4λ
        if(*pSrc>='0' && *pSrc<='9')
        {
            *pDst |= *pSrc - '0';
        }
        else
		{
            *pDst |= *pSrc - 'A' + 10;
        }
        pSrc++;
        pDst++;
    }   
    // ����Ŀ�����ݳ���
    return nSrcLength / 2;
}

/* 
*@brief     �ֽ�����ת��Ϊ�ɴ�ӡ�ַ���,�磺�磺{0xC8, 0x32, 0x9B, 0xFD, 0x0E, 0x01} --> "C8329BFD0E01"                    

                                                
*@param   pSrc: Դ����ָ��                                                    
*@param   pDst: Ŀ���ַ���ָ��
*@param  nSrcLength: Դ���ݳ���   
*@return  Ŀ���ַ�������     
*/
int gsmBytes2String(const unsigned char* pSrc, char* pDst, int nSrcLength)
{
    const char tab[]="0123456789ABCDEF";    // 0x0-0xf���ַ����ұ�    
    for(int i=0; i<nSrcLength; i++)
    {
		// �����4λ
        *pDst++ = tab[*pSrc >> 4];                                        
        // �����4λ
        *pDst++ = tab[*pSrc & 0x0f];    
        pSrc++;
	}    
    // ����ַ����Ӹ�������
    *pDst = '\0';    
    // ����Ŀ���ַ�������
    return nSrcLength * 2;
}


/*
* ������gsmSerializeNumbers                                                   
*˵���������ߵ����ַ���ת��Ϊ����˳����ַ���                               
*       �磺"8613910199192" --> "683119109991F2"                              
* ������                                                                      
*      pSrc: Դ�ַ���ָ��                                                     
*      pDst: Ŀ���ַ���ָ��                                                   
*      nSrcLength: Դ�ַ�������                                              
* ����: Ŀ���ַ�������                                                       
*/
int gsmSerializeNumbers(const char* pSrc, char* pDst, int nSrcLength)
{
    int nDstLength;   // Ŀ���ַ�������
    char ch;          // ���ڱ���һ���ַ�
    
    // ���ƴ�����
    nDstLength = nSrcLength;
	  // �����ߵ�
    for(int i=0; i<nSrcLength;i+=2)
    {
        ch = *pSrc++;        // �����ȳ��ֵ��ַ�
        *pDst++ = *pSrc++;   // ���ƺ���ֵ��ַ�
        *pDst++ = ch;        // �����ȳ��ֵ��ַ�
    }
    
    // �����ַ���'F'��
    if(*(pDst-1) == 'F')
    {
        pDst--;
        nDstLength--;        // Ŀ���ַ������ȼ�1
    }
    
    // ����ַ����Ӹ�������
    *pDst = '\0';
    
    // ����Ŀ���ַ�������
    return nDstLength;
}

/*
* ������gsmInvertNumbers                                                     
* ˵����PDU���еĺ����ʱ�䣬���������ߵ����ַ����������������������ɽ������� 
*      �任������˳����ַ���ת��Ϊ�����ߵ����ַ�����������Ϊ��������'F'�ճ�ż
*      �����磺"8613910199192" --> "683119109991F2"                           
* ������                                                                      
*      pSrc: Դ�ַ���ָ��                                                     
*      pDst: Ŀ���ַ���ָ��                                                   
*      nSrcLength: Դ�ַ�������                                               
* ����: Ŀ���ַ�������                                                       
*/
int gsmInvertNumbers(const char* pSrc, char* pDst, int nSrcLength)
{
    int nDstLength;   // Ŀ���ַ�������
    char ch;          // ���ڱ���һ���ַ�
    
    // ���ƴ�����
    nDstLength = nSrcLength;
    
    // �����ߵ�
    for(int i=0; i<nSrcLength;i+=2)
    {
        ch = *pSrc++;        // �����ȳ��ֵ��ַ�
        *pDst++ = *pSrc++;   // ���ƺ���ֵ��ַ�
        *pDst++ = ch;        // �����ȳ��ֵ��ַ�
    }
    
    // Դ��������������
    if(nSrcLength & 1)
   {
        *(pDst-2) = 'F';     // ��'F'
        nDstLength++;        // Ŀ�괮���ȼ�1
    }
    
    // ����ַ����Ӹ�������
    *pDst = '\0';
    
    // ����Ŀ���ַ�������
    return nDstLength;
}

/* 
*@brief     8-bit����                                                                   
*@param  pSrc: Դ���봮ָ��                                                
*@param  pDst: Ŀ���ַ���ָ��
*@param  nSrcLength: Դ���봮���� 
*@return  Ŀ���ַ�������
*/
int gsmDecode8bit(const unsigned char *pSrc, char *pDst, int nSrcLength)
{
    // �򵥸���
	memcpy(pDst, pSrc, nSrcLength);
	// ����ַ����Ӹ�������
	*pDst = '\0';
	return nSrcLength;
}

/* 
*@brief     8-bit����                                                                   
*@param  pSrc: Դ���봮ָ��                                                
*@param  pDst: Ŀ���ַ���ָ��
*@param  nSrcLength: Դ���봮����  
*@return  Ŀ���ַ�������   
*/
int gsmEncode8bit(const char *pSrc, unsigned char *pDst, int nSrcLength)
{
    // �򵥸���
	memcpy(pDst, pSrc, nSrcLength);
	return nSrcLength;
}

/* 
*@brief     7-bit����                                                                   
*@param  pSrc: Դ���봮ָ��                                                
*@param  pDst: Ŀ���ַ���ָ��
*@param  nSrcLength: Դ���봮����  
*@return  Ŀ���ַ�������   
*/
int gsmDecode7bit(const unsigned char* pSrc, char* pDst, int nSrcLength)
{
    int nSrc;        // Դ�ַ����ļ���ֵ
    int nDst;        // Ŀ����봮�ļ���ֵ
    int nByte;       // ��ǰ���ڴ���������ֽڵ���ţ���Χ��0-6
    unsigned char nLeft;    // ��һ�ֽڲ��������    
    // ����ֵ��ʼ��
    nSrc = 0;
    nDst = 0;    
    // �����ֽ���źͲ������ݳ�ʼ��
    nByte = 0;
    nLeft = 0;    
    // ��Դ����ÿ7���ֽڷ�Ϊһ�飬��ѹ����8���ֽ�
    // ѭ���ô�����̣�ֱ��Դ���ݱ�������
    // ������鲻��7�ֽڣ�Ҳ����ȷ����
    while(nSrc<nSrcLength)
    {
        // ��Դ�ֽ��ұ߲��������������ӣ�ȥ�����λ���õ�һ��Ŀ������ֽ�
        *pDst = ((*pSrc << nByte) | nLeft) & 0x7f;
        // �����ֽ�ʣ�µ���߲��֣���Ϊ�������ݱ�������
        nLeft = *pSrc >> (7-nByte);    
        // �޸�Ŀ�괮��ָ��ͼ���ֵ
        pDst++;
        nDst++;
        // �޸��ֽڼ���ֵ
        nByte++;    
        // ����һ������һ���ֽ�
        if(nByte == 7)
        {
            // ����õ�һ��Ŀ������ֽ�
            *pDst = nLeft;    
            // �޸�Ŀ�괮��ָ��ͼ���ֵ
            pDst++;
            nDst++;    
            // �����ֽ���źͲ������ݳ�ʼ��
            nByte = 0;
            nLeft = 0;
        }    
        // �޸�Դ����ָ��ͼ���ֵ
        pSrc++;
        nSrc++;
	}
    
    *pDst = 0;    
    // ����Ŀ�괮����
    return nDst;
}

/* 
*@brief     7bit����                                                                   
*@param  pSrc: Դ�ַ���ָ��                                                
*@param  pDst: Ŀ����봮ָ��
*@param  nSrcLength: Դ�ַ�������  
*@return  Ŀ����봮����
*/
int gsmEncode7bit(const char* pSrc, unsigned char* pDst, int nSrcLength)
{
    int nSrc;        // Դ�ַ����ļ���ֵ
    int nDst;        // Ŀ����봮�ļ���ֵ
    int nChar;       // ��ǰ���ڴ���������ַ��ֽڵ���ţ���Χ��0-7
    unsigned char nLeft;    // ��һ�ֽڲ��������
    
    // ����ֵ��ʼ��
    nSrc = 0;
    nDst = 0;
    
    // ��Դ��ÿ8���ֽڷ�Ϊһ�飬ѹ����7���ֽ�
    // ѭ���ô�����̣�ֱ��Դ����������
    // ������鲻��8�ֽڣ�Ҳ����ȷ����
    while(nSrc<nSrcLength)
    {
        // ȡԴ�ַ����ļ���ֵ�����3λ
        nChar = nSrc & 7;
		  // ����Դ����ÿ���ֽ�
        if(nChar == 0)
        {
            // ���ڵ�һ���ֽڣ�ֻ�Ǳ�����������������һ���ֽ�ʱʹ��
            nLeft = *pSrc;
        }
        else
        {
            // ���������ֽڣ������ұ߲��������������ӣ��õ�һ��Ŀ������ֽ�
            *pDst = (*pSrc << (8-nChar)) | nLeft;
      // �����ֽ�ʣ�µ���߲��֣���Ϊ�������ݱ�������
            nLeft = *pSrc >> nChar;
            // �޸�Ŀ�괮��ָ��ͼ���ֵ pDst++;
            nDst++; 
        } 
        
        // �޸�Դ����ָ��ͼ���ֵ
        pSrc++; nSrc++;
    }
    
    // ����Ŀ�괮����
    return nDst; 
}

/*
*@brief UnicodeתUTF-8
*@param  in: Դunicode�ַ�ָ��                                                
*@param  out: Ŀ���ַ���ָ���ָ��
*@param  insize: Դ���ݳ���                                                   
*@return  0
*/
int unicode2utf8(uint16_t *in, int insize, uint8_t **out)
{
    int i = 0;
    int outsize = 0;
    int charscount = 0;
    uint8_t *result = NULL;
    uint8_t *tmp = NULL;

    charscount = insize / sizeof(uint16_t);
    result = (uint8_t *)malloc(charscount * 3 + 1);
    memset(result, 0, charscount * 3 + 1);
    tmp = result;
    for (i = 0; i < charscount; i++)
    {
        uint16_t unicode = in[i];        
        if (unicode >= 0x0000 && unicode <= 0x007f)
        {
            *tmp = (uint8_t)unicode;
            tmp += 1;
            outsize += 1;
        }
        else if (unicode >= 0x0080 && unicode <= 0x07ff)
        {
            *tmp = 0xc0 | (unicode >> 6);
            tmp += 1;
            *tmp = 0x80 | (unicode & (0xff >> 2));
            tmp += 1;
            outsize += 2;
        }
        else if (unicode >= 0x0800 && unicode <= 0xffff)
        {
            *tmp = 0xe0 | (unicode >> 12);
            tmp += 1;
            *tmp = 0x80 | (unicode >> 6 & 0x00ff);
            tmp += 1;
            *tmp = 0x80 | (unicode & (0xff >> 2));
            tmp += 1;
            outsize += 3;
        }

    }

    *tmp = '\0';
    *out = result;
    return 0;
}

// �����UCS2����ĳ��Ȳ����س���
int ucs2Count(const unsigned char *pSrc)
{
    int i;
    for (i = 0; pSrc[i]+pSrc[i+1] != 0; i += 2);
    //ע�������и�";"
    return i;
}

/* 
*@brief PDU���룬���ڽ��ա��Ķ�����Ϣ                                                                   
*@param  pSrc: ԴPDU��ָ��                                                
*@param  pDst: Ŀ��PDU����ָ��
*@return  �û���Ϣ������   
*/

int gsmDecodePdu(const char* pSrc, struct SM_PARAM* pDst)
{
    int nDstLength;          // Ŀ��PDU������
    unsigned char tmp;       // �ڲ��õ���ʱ�ֽڱ���
    unsigned char buf[256];  // �ڲ��õĻ�����

    // SMSC��ַ��Ϣ��
    gsmString2Bytes(pSrc, &tmp, 2);    // ȡ����
    tmp = (tmp - 1) * 2;    // SMSC���봮����
    pSrc += 4;              // ָ�����
    gsmSerializeNumbers(pSrc, (*pDst).SCA, tmp);    // ת��SMSC���뵽Ŀ��PDU��
    pSrc += tmp;        // ָ�����
    
    // TPDU�λ����������ظ���ַ��
    gsmString2Bytes(pSrc, &tmp, 2);    // ȡ��������
    pSrc += 2;        // ָ�����
    //if(tmp & 0x80)
    {
        // �����ظ���ַ��ȡ�ظ���ַ��Ϣ
        gsmString2Bytes(pSrc, &tmp, 2);    // ȡ����
        if(tmp & 1) tmp += 1;    // ������ż��
        pSrc += 4;          // ָ�����
        gsmSerializeNumbers(pSrc, (*pDst).TPA, tmp);    // ȡTP-RA����
		m_strRecvPhone = (*pDst).TPA;
        pSrc += tmp;        // ָ�����
    }
    
    // TPDU��Э���ʶ�����뷽ʽ���û���Ϣ��
    gsmString2Bytes(pSrc, (unsigned char*)(*pDst).TP_PID, 2);    // ȡЭ���ʶ(TP-PID)
    pSrc += 2;        // ָ�����
    gsmString2Bytes(pSrc, (unsigned char*)(*pDst).TP_DCS, 2);    // ȡ���뷽ʽ(TP-DCS)
    pSrc += 2;        // ָ�����
    gsmSerializeNumbers(pSrc, (*pDst).TP_SCTS, 14);        // ����ʱ����ַ���(TP_SCTS) 
    pSrc += 14;       // ָ�����
    gsmString2Bytes(pSrc, &tmp, 2);    // �û���Ϣ����(TP-UDL)
    pSrc += 2;        // ָ�����
    (*pDst).TP_DCS=8;
    if((*pDst).TP_DCS == GSM_7BIT)    
    {
        // 7-bit����
        nDstLength = gsmString2Bytes(pSrc, buf, tmp & 7 ? (int)tmp * 7 / 4 + 2 : (int)tmp * 7 / 4);  // ��ʽת��
        gsmDecode7bit(buf, (*pDst).TP_UD, nDstLength);    // ת����TP-DU
        nDstLength = tmp;
    }
    //else if((*pDst).TP_DCS == GSM_UCS2)
    //{
        // UCS2����
        //nDstLength = gsmString2Bytes(pSrc, buf, tmp * 2);        // ��ʽת��
        //nDstLength = mygsmDecodeUcs2(buf, (*pDst).TP_UD, nDstLength);    // ת����TP-DU
    //}
    else
    {
        // 8-bit����
        nDstLength = gsmString2Bytes(pSrc, buf, tmp * 2);        // ��ʽת��
        nDstLength = gsmDecode8bit(buf, (*pDst).TP_UD, nDstLength);    // ת����TP-DU
		// nDstLength = gsmDecodeUcs2(buf, pDst->TP_UD, nDstLength);    // ת����TP-DU		
    }
    
    // ����Ŀ���ַ�������
    return nDstLength;
}


/* 
*@brief     PDU���룬���ڱ��ơ����Ͷ���Ϣ                                                                   
*@param  pSrc:   ԴPDU����ָ��                                                
*@param  pDst:  Ŀ��PDU��ָ�� 
*@return  Ŀ��PDU������  
*/
int gsmEncodePdu(const struct SM_PARAM* pSrc, char* pDst)
{
    int nLength;             // �ڲ��õĴ�����
    int nDstLength;          // Ŀ��PDU������
    unsigned char buf[256];  // �ڲ��õĻ�����
    
    // SMSC��ַ��Ϣ��
    nLength = (int)strlen((*pSrc).SCA);    // SMSC��ַ�ַ����ĳ���    
    buf[0] = (char)((nLength & 1) == 0 ? nLength : nLength + 1) / 2 + 1;    // SMSC��ַ��Ϣ����
    buf[1] = 0x91;        // �̶�: �ù��ʸ�ʽ����
    nDstLength = gsmBytes2String(buf, pDst, 2);        // ת��2���ֽڵ�Ŀ��PDU��
    nDstLength += gsmInvertNumbers((*pSrc).SCA, &pDst[nDstLength], nLength);    // ת��SMSC��Ŀ��PDU��
     // TPDU�λ���������Ŀ���ַ��
    nLength = (int)strlen((*pSrc).TPA);    // TP-DA��ַ�ַ����ĳ���
	m_strRecvPhone = (*pSrc).TPA;
    buf[0] = 0x11;            // �Ƿ��Ͷ���(TP-MTI=01)��TP-VP����Ը�ʽ(TP-VPF=10)
    buf[1] = 0;               // TP-MR=0
    buf[2] = (char)nLength;   // Ŀ���ַ���ָ���(TP-DA��ַ�ַ�����ʵ����)
    buf[3] = 0x91;            // �̶�: �ù��ʸ�ʽ����
    nDstLength += gsmBytes2String(buf, &pDst[nDstLength], 4);  // ת��4���ֽڵ�Ŀ��PDU��
    nDstLength += gsmInvertNumbers((*pSrc).TPA, &pDst[nDstLength], nLength); // ת��TP-DA��Ŀ��PDU��
    
    // TPDU��Э���ʶ�����뷽ʽ���û���Ϣ��
    nLength = (int)strlen((*pSrc).TP_UD);    // �û���Ϣ�ַ����ĳ���
    buf[0] = (*pSrc).TP_PID;        // Э���ʶ(TP-PID)
    buf[1] = (*pSrc).TP_DCS;        // �û���Ϣ���뷽ʽ(TP-DCS)
    buf[2] = 0;            // ��Ч��(TP-VP)Ϊ5����
    if((*pSrc).TP_DCS == GSM_7BIT)    
    {
        // 7-bit���뷽ʽ
//        buf[3] = nLength;            // ����ǰ����
//        nLength = gsmEncode7bit(pSrc->TP_UD, &buf[4], nLength+1) + 4; 
		// ת��		TP-DA��Ŀ��PDU��
        buf[3] = gsmEncode8bit((*pSrc).TP_UD, &buf[4], nLength);    // ת��TP-DA��Ŀ��PDU��
        nLength = buf[3] + 4;        // nLength���ڸö����ݳ���
    }
    //else if((*pSrc).TP_DCS == GSM_UCS2)
    //{
        // UCS2���뷽ʽ
        //buf[3] = mygsmEncodeUcs2((*pSrc).TP_UD, &buf[4], nLength);    // ת��TP-DA��Ŀ��PDU��
        //nLength = buf[3] + 4;        // nLength���ڸö����ݳ���
    //}
    else
    {
        // 8-bit���뷽ʽ
        buf[3] = gsmEncode8bit((*pSrc).TP_UD, &buf[4], nLength);    // ת��TP-DA��Ŀ��PDU��
        nLength = buf[3] + 4;        // nLength���ڸö����ݳ���
    }
    nDstLength += gsmBytes2String(buf, &pDst[nDstLength], nLength);        // ת���ö����ݵ�Ŀ��PDU��
    
    // ����Ŀ���ַ�������
    return nDstLength;
}



/*
* ������gsmSendMessage                                                        
* ˵�������Ͷ���Ϣ                                                            
* ������                                                                      
*      pSrc: ԴPDU����ָ��                                                    
*/
int gsmSendMessage(int fd,const struct SM_PARAM* pSrc)
{
    int nPduLength;        // PDU������
    unsigned char nSmscLength;    // SMSC������
    int nLength;           // �����յ������ݳ���
	char cmd[16] = {0};          // ���
	char pdu[512] = {0};         // PDU��
	char ans[128] = {0};         // Ӧ��
	nPduLength = gsmEncodePdu(pSrc, pdu);    // ����PDU����������PDU��
    strcat(pdu, "\x01a");        // ��Ctrl-Z����
    
    gsmString2Bytes(pdu, &nSmscLength, 2);    // ȡPDU���е�SMSC��Ϣ����
    nSmscLength++;        // ���ϳ����ֽڱ���
    // �����еĳ��ȣ�������SMSC��Ϣ���ȣ��������ֽڼ�

    sprintf(cmd, "AT+CMGS=%d\r", nPduLength / 2 - nSmscLength);    // ��������
    nPduLength = gsmEncodePdu(pSrc, pdu);    // ����PDU����������PDU��
        
    gsmString2Bytes(pdu, &nSmscLength, 2);    // ȡPDU���е�SMSC��Ϣ����
    nSmscLength++;        // ���ϳ����ֽڱ���

	strcat(pdu, "\x01A\0");        // ��Ctrl-Z����

    // �����еĳ��ȣ�������SMSC��Ϣ���ȣ��������ֽڼ�
    sprintf(cmd, "AT+CMGS=%d\r", nPduLength / 2 - nSmscLength);    // ��������

	//strcpy( pdu, "0891683108200105F011000D91683181076159F6000800064F60597D0021\0" );
	//strcpy( cmd, "AT+CMGS=21\r" );

	char szTemp[1024] = {0};
	sprintf( szTemp, "������Ϣ���%s, ��Ϣ���ݣ�%s\n\0", cmd, pdu );
	//WriteLog( szTemp );

	//for( int i = 0; i != 6; i++ )
	{
		// ��������
        WritePort(fd,cmd, (int)(strlen(cmd)));
		sleep(100);

		// �����Ϣ
		WritePort(fd,pdu, (int)(strlen(pdu)));
		//WritePort("\x1A", 1);
		sleep(2000);

		// ������Ӧ������
		nLength = ReadPort(fd,ans, 128);
		ans[nLength] = '\0';
		sprintf( szTemp, "���͵�Ӧ��%s\n\0", ans );
		//WriteLog( szTemp );
	}

	 return 0;    
}



/*
*@brief ͨ��ָ���˿ڣ����Ͷ���
*@param  strRecvPhone�������ֻ�����
*@param  strMsgContent����������
*@param  strPortName�����ж˿ڵ�����
*/
int send_msg(int fd,char* strRecvPhone , char* strMsgContent, int nSMSMaxLength)
{
    int i=0;
    size_t t;
    char ans[128];        // Ӧ��
    char TPA[16];       // Ŀ������ظ�����(TP-DA��TP-RA)
    char  cmd[20];
    //struct MsgList *msglist;
	//int iPage = page( strMsgContent.GetBuffer(strMsgContent.GetLength()), msglist, nSMSMaxLength ); //���ض��ŷ�ҳ��
    int iPage = 1;
    //t= m_strRecvPhone.GetLength()+2;
    t = strlen(strRecvPhone) + 2;
	for(i=2;i<t;i++)
    {         
	   //TPA[i]=m_strRecvPhone.GetAt(i-2);
        TPA[i] = *strRecvPhone++; 
    }
    TPA[0]='8';
    TPA[1]='6';
	TPA[t]='\0';
	for(int iIndex=0 ;iIndex < iPage;iIndex++)
	{
		//printf( "Content:%s,Msg:%s\n", strMsgContent.GetBuffer(), msglist->chMsg );
		//WriteErr( "���ţ�" );
		//WriteErr( msglist->chMsg );
        sprintf(cmd , "AT\r");
        WritePort(fd,cmd, (int)strlen(cmd));
	    sleep(ShortSleepTime);
        int nLength = ReadPort(fd,ans, 128);
        ans[nLength] = '\0';

        sprintf(cmd, "ATE0\r"); 
        WritePort(fd,cmd, (int)strlen(cmd));
	    sleep(ShortSleepTime);
        nLength = ReadPort(fd,ans, 128);
        ans[nLength] = '\0';

	    sprintf(cmd, "AT+CSMS=0\r");      
        WritePort(fd,cmd, (int)strlen(cmd));  
 	    sleep(ShortSleepTime);
        nLength = ReadPort(fd,ans, 128);
        ans[nLength] = '\0';
 
	    sprintf(cmd, "AT+CMGF=0\r");      
        WritePort(fd,cmd, (int)strlen(cmd));  
	    sleep(ShortSleepTime);
        nLength = ReadPort(fd,ans, 128);
        ans[nLength] = '\0';
        //sm_param_temp= new SM_PARAM;
        struct SM_PARAM *sm_param_temp;
        strcpy((*sm_param_temp).SCA,m_SmsCenterNum);

        (*sm_param_temp).TP_DCS=0x8;
        (*sm_param_temp).TP_PID=0x0;
        strcpy((*sm_param_temp).TPA,TPA);
        strcpy((*sm_param_temp).TP_UD,strMsgContent);
        if(iPage > 1)
            sprintf((*sm_param_temp).TP_UD,"%d/%d %s" ,iIndex+1,iPage,strMsgContent);
        //printf("Msg :%s\n" ,sm_param_temp->TP_UD);
        //msglist = msglist.pNext;      
        if(!gsmSendMessage(fd,sm_param_temp))//���Ͷ���
        {
            printf("Send SMS Failed\n");
			//WriteLog("Send SMS Failed");
            return -1;
        }
        sleep(5000);
    }
	return 0;   
}

