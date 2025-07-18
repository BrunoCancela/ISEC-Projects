#pragma once

#define MAX_CLIENTES_LOGADOS 5
#define MAX_EMPRESAS 30
#define MAX_TOP_EMPRESAS 10

#define EVENT_RUN _T("EVENT_RUN")
#define BUFFER_SIZE 256
#define CLOSE_BOARD _T("CLOSE_BOARD")

#define MSGTEXT_SZ 365
#define SHM_NAME _T("SHM_NAME")
#define MUTEX_NAME _T("MUTEX_NAME")
#define EVENT_COMP _T("EVENT_COMP")
#define EVENT_TRANSAC _T("EVENT_TRANSAC")

typedef struct _Empresa {
    TCHAR nome[BUFFER_SIZE];
    unsigned int acoes;
    unsigned int acoesDisponives;
    float preco;
} Empresa;

typedef struct _Transacao {
    TCHAR nome[BUFFER_SIZE];
    int nAcoes;
    float valorTotal;
} Transacao;

typedef struct _TopEmpresas {
    Empresa empresas[MAX_TOP_EMPRESAS];
    int nEmpresas;
} TopEmpresas;

typedef struct SharedMessage {
    TopEmpresas topEmpresas;
    Transacao transacao;
} SharedMessage;

typedef struct _SharedMemory {
    HANDLE hMapFile;
    SharedMessage* sharedMessage;
    int threadMustContinue;
    HANDLE CompEvent;
    HANDLE TransacEvent;
    HANDLE CloseBoardEvent;
    HANDLE hRWMutex;
} SharedMemory;

typedef struct _ControlData {
    HANDLE shutdown;
    SharedMemory sharedMemory;
    unsigned int nEmpresasToShow;
    int stop;
} ControlData;

#define TOPSIZE sizeof(TopEmpresas)
#define TRANSACAOSIZE sizeof(Transacao)
#define SHAREDMEMSIZE sizeof(SharedMessage)

BOOL initMemAndSync(ControlData* cdata);
DWORD WINAPI topCompanies(LPVOID p);
DWORD WINAPI showTransanction(LPVOID p);

