
#define MAX_CLIENTES_CON 5
#define MAX_CLIENTES 20
#define MAX_EMPRESAS 30
#define MAX_TOP_EMPRESAS 10

#define REG_KEY _T("Software\\SO2\\TP")
#define REG_NAME _T("NCLIENTS")
#define EVENT_RUN _T("EVENT_RUN")

#define SHM_NAME _T("SHM_NAME")

#define MUTEX_NAME _T("MUTEX_NAME")     
#define MUTEX_BOLSA _T("MUTEX_BOLSA")          
#define EVENT_COMP _T("EVENT_COMP")
#define EVENT_TRANSAC _T("EVENT_TRANSAC")

#define CLOSE_BOARD _T("CLOSE_BOARD")


#define SEMAPHORE_NAME _T("SEMAPHORE_NAME")

#define CLOSE_EVENT _T("CLOSE")

#define BUFFER_SIZE 256
#define BUFFER_PIPE 6000

#define lpCompaniesFileName _T("empresas.txt")

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

typedef struct _Carteira {
	Empresa* empresa;
	unsigned int nAcoesCompradas;
	unsigned int nAcoesParaVenda;
} Carteira;

typedef struct _Cliente {
	TCHAR nome[BUFFER_SIZE];
	TCHAR password[BUFFER_SIZE];
	float saldo;
	unsigned int estado;
	unsigned int nCarteiraEmpresas;
	Carteira carteira[5];
} Cliente;

typedef struct _TopEmpresas {
	Empresa empresas[MAX_TOP_EMPRESAS];
	int nEmpresas;
} TopEmpresas;

typedef struct _Transacao {
	TCHAR nome[BUFFER_SIZE];
	int nAcoes;
	float valorTotal;
} Transacao;

typedef struct SharedMessage {
	TopEmpresas topEmpresas;
	Transacao transacao;
} SharedMessage;


typedef struct _SharedMemory {
	HANDLE hMapFile; //createfilemaping
	SharedMessage* sharedMessage;
	//utlima transação
	int threadMustContinue; //trinco
	HANDLE CompEvent; //evento que sinaliza a chegada de mensagem
	HANDLE TransacEvent; //evento que sinaliza a chegada de transação
	HANDLE CloseBoardEvent;
	HANDLE hRWMutex;
} SharedMemory;

typedef struct _ControlData {
	unsigned int shutdown;  // flag "continua". 0 = continua, 1 = deve terminar
	HANDLE alredyRunning;
	Empresa empresas[MAX_EMPRESAS];
	Cliente clientes[MAX_CLIENTES];
	HANDLE pipeClientes[MAX_CLIENTES];
	unsigned int nEmpresas;
	unsigned int maxClientes;
	unsigned int nClientes;
	unsigned int pause;
	HANDLE hSemaphore;
	HANDLE CloseEvent;
	SharedMemory sharedMemory;
	HANDLE hMutexBolsa;
} ControlData;

typedef struct _ThreadClient {
	HANDLE* hPipe;
	ControlData* controlData;
} ThreadClient;

#define TOPSIZE sizeof(TopEmpresas)
#define TRANSACAOSIZE sizeof(Transacao)
#define SHAREDMEMSIZE sizeof(SharedMessage)

void initReg(ControlData* cd);
BOOL initMemAndSync(ControlData* cdata);
BOOL loadUsers(ControlData* cdata);
void companiesAndTransactions(ControlData* cdata);
void checkTop10(ControlData* cdata);
void sortCompanies(ControlData* cdata);
void HandleAddCompany(ControlData* cd, const TCHAR* params);
BOOL loadCompanies(ControlData* cdata);
void WriteToClient(HANDLE hPipe, const Mensagens* message);
void HandleListCompanies(ControlData* cd);
void HandleSetStockPrice(ControlData* cd, const TCHAR* params);
void HandleListUsers(ControlData* cd);
void HandlePauseOperations(ControlData* cd, const TCHAR* params);
void HandleClosePlatform(ControlData* cd);
void Broadcast(ControlData* cd, Mensagens mensagem);
int ProcessCommand(ControlData* cd, TCHAR* command);
void ProcessClientCommand(LPTSTR buffer, ControlData* cd, Cliente* currentClient, HANDLE hPipe);
DWORD WINAPI ReadFromClient(LPVOID lpParam);
DWORD WINAPI CreatePipeAndHandleClients(LPVOID lpParam);
DWORD WINAPI ThreadPause(LPVOID lpParam);