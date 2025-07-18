#pragma once
#define MAX_CLIENTES_CON 5
#define MAX_CLIENTES 20
#define MAX_EMPRESAS 30
#define MAX_TOP_EMPRESAS 10

#define REG_KEY _T("Software\\SO2\\TP")
#define REG_NAME _T("NCLIENTS")
#define EVENT_RUN _T("EVENT_RUN")

#define SHM_NAME _T("SHM_NAME")

#define MUTEX_NAME _T("MUTEX")          // nome do mutex   -> em casa devem pensar numa solução para ter mutex´s distintos de forma a não existir perda de performance 
#define EVENT_COMP _T("EVENT_COMP")
#define EVENT_TRANSAC _T("EVENT_TRANSAC")
#define SEMAPHORE_NAME _T("SEMAPHORE_NAME")
#define BUFFER_SIZE 256

#define PIPE_BOLSA TEXT("\\\\.\\pipe\\bolsa")

typedef struct _Empresa {
    TCHAR nome[BUFFER_SIZE];
    unsigned int acoes;
    unsigned int acoesDisponives;
    float preco;
} Empresa;

typedef struct _Mensagens {
    Empresa empresas[MAX_EMPRESAS];
    TCHAR mensagem[BUFFER_SIZE];
    unsigned int tipo;
    unsigned int nEmpresas;
} Mensagens;

typedef struct _ControlData {
    unsigned int shutdown;
    HANDLE alredyRunning;
    HANDLE hPipe;
    HANDLE hSemaphore;
    int loggedIn;
    int bolsaClosed;
} ControlData;

HANDLE ConnectToServer();
DWORD WINAPI ReadFromServer(LPVOID lpParam);
void WriteToServer(HANDLE hPipe, const TCHAR* message);
int HandleLogin(ControlData* cd, TCHAR* command);
int HandleBuy(ControlData* cd, TCHAR* command);
int HandleSell(ControlData* cd, TCHAR* command);
int ProcessCommand(ControlData* cd, TCHAR* command);
