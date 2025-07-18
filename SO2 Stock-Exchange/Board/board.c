#include <windows.h>
#include <tchar.h>
#include <fcntl.h>
#include <stdio.h>
#include <io.h>
#include <time.h>
#include "board.h"


BOOL initMemAndSync(ControlData* cdata)
{
	cdata->sharedMemory.hMapFile = CreateFileMapping(
		INVALID_HANDLE_VALUE,
		NULL, 
		PAGE_READWRITE,
		0,
		SHAREDMEMSIZE,
		SHM_NAME);
	if (cdata->sharedMemory.hMapFile == NULL)
	{
		_tprintf(TEXT("Error: CreateFileMapping (%d)\n"), GetLastError());
		return FALSE;
	}


	cdata->sharedMemory.sharedMessage = (SharedMessage*)MapViewOfFile(cdata->sharedMemory.hMapFile,
		FILE_MAP_ALL_ACCESS, 
		0,
		0,
		TOPSIZE); 

	if (cdata->sharedMemory.sharedMessage == NULL)
	{
		_tprintf(TEXT("Error: MapViewOfFile (%d)\n"), GetLastError());
		CloseHandle(cdata->sharedMemory.hMapFile);
		return FALSE;
	}




	cdata->sharedMemory.hRWMutex = CreateMutex(NULL,
		FALSE,
		MUTEX_NAME); 
	if (cdata->sharedMemory.hRWMutex == NULL)
	{
		_tprintf(TEXT("Error: CreateMutex (%d)\n"), GetLastError());
		UnmapViewOfFile(cdata->sharedMemory.sharedMessage);
		CloseHandle(cdata->sharedMemory.hMapFile);
		return FALSE;
	}

	
	cdata->sharedMemory.CompEvent = CreateEvent(NULL, 
		TRUE, 
		FALSE, 
		EVENT_COMP);
	if (cdata->sharedMemory.CompEvent == NULL)
	{
		_tprintf(TEXT("Error: CreateEvent (%d)\n"), GetLastError());
		UnmapViewOfFile(cdata->sharedMemory.sharedMessage);
		CloseHandle(cdata->sharedMemory.hMapFile);
		CloseHandle(cdata->sharedMemory.hRWMutex);
		return FALSE;
	}
	cdata->sharedMemory.TransacEvent = CreateEvent(NULL,
		TRUE,
		FALSE,
		EVENT_TRANSAC);
	if (cdata->sharedMemory.TransacEvent == NULL)
	{
		_tprintf(TEXT("Error: CreateEvent (%d)\n"), GetLastError());
		UnmapViewOfFile(cdata->sharedMemory.sharedMessage);
		CloseHandle(cdata->sharedMemory.hMapFile);
		CloseHandle(cdata->sharedMemory.hRWMutex);
		CloseHandle(cdata->sharedMemory.CompEvent);
		return FALSE;
	}

	cdata->sharedMemory.CloseBoardEvent = CreateEvent(NULL,
		TRUE,
		FALSE,
		CLOSE_BOARD);
	if (cdata->sharedMemory.TransacEvent == NULL)
	{
		_tprintf(TEXT("Error: CreateEvent (%d)\n"), GetLastError());
		UnmapViewOfFile(cdata->sharedMemory.sharedMessage);
		CloseHandle(cdata->sharedMemory.hMapFile);
		CloseHandle(cdata->sharedMemory.hRWMutex);
		CloseHandle(cdata->sharedMemory.CompEvent);
		CloseHandle(cdata->sharedMemory.TransacEvent);
		return FALSE;
	}



	return TRUE;
}

DWORD WINAPI topCompanies(LPVOID p){
	ControlData* cdata = (ControlData*)p;
	WaitForSingleObject(cdata->sharedMemory.hRWMutex, INFINITE);
	if (cdata->sharedMemory.sharedMessage->topEmpresas.nEmpresas != 0) {
		if (cdata->nEmpresasToShow < cdata->sharedMemory.sharedMessage->topEmpresas.nEmpresas)
			_tprintf(_T("\nTop %d companies:\n"), cdata->nEmpresasToShow);
		else
			_tprintf(_T("\nTop %d companies:\n"), cdata->sharedMemory.sharedMessage->topEmpresas.nEmpresas);
	}

	for (int i = 0; i < cdata->sharedMemory.sharedMessage->topEmpresas.nEmpresas && i < cdata->nEmpresasToShow; i++) {
		_tprintf(_T("%d - Empresa: %s | Ações Disponivies: %d | Preço Atual: %.2f\n"),
			i + 1,
			cdata->sharedMemory.sharedMessage->topEmpresas.empresas[i].nome,
			cdata->sharedMemory.sharedMessage->topEmpresas.empresas[i].acoesDisponives,
			cdata->sharedMemory.sharedMessage->topEmpresas.empresas[i].preco);
	}
	ReleaseMutex(cdata->sharedMemory.hRWMutex);

	int ranTime;
	HANDLE handles[3] = { cdata->shutdown,  cdata->sharedMemory.CloseBoardEvent, cdata->sharedMemory.CompEvent };

	while (1) {

		DWORD waitResult = WaitForMultipleObjects(3, handles, FALSE, INFINITE);
		if (waitResult == WAIT_OBJECT_0) {
			break;
		}
		else if (waitResult == WAIT_OBJECT_0 + 1) {
			_tprintf(_T("\nLost connection to bolsa. Please press Enter to close\n"));
			cdata->stop = 1;
			return 1;
		}

		WaitForSingleObject(cdata->sharedMemory.hRWMutex, INFINITE);

		if(cdata->nEmpresasToShow < cdata->sharedMemory.sharedMessage->topEmpresas.nEmpresas)
			_tprintf(_T("\nTop %d companies:\n"), cdata->nEmpresasToShow);
		else
			_tprintf(_T("\nTop %d companies:\n"), cdata->sharedMemory.sharedMessage->topEmpresas.nEmpresas);

		for (int i = 0; i < cdata->sharedMemory.sharedMessage->topEmpresas.nEmpresas && i < cdata->nEmpresasToShow; i++) {
			_tprintf(_T("%d - Empresa: %s | Ações Disponivies: %d | Preço Atual: %.2f\n"),
				i+1,
				cdata->sharedMemory.sharedMessage->topEmpresas.empresas[i].nome,
				cdata->sharedMemory.sharedMessage->topEmpresas.empresas[i].acoesDisponives,
				cdata->sharedMemory.sharedMessage->topEmpresas.empresas[i].preco);
		}
		ReleaseMutex(cdata->sharedMemory.hRWMutex);
		ResetEvent(cdata->sharedMemory.CompEvent);
	}
	return 0;
}

DWORD WINAPI showTransanction(LPVOID p)
{
	ControlData* cdata = (ControlData*)p;
	int ranTime;
	HANDLE handles[3] = { cdata->shutdown, cdata->sharedMemory.CloseBoardEvent, cdata->sharedMemory.TransacEvent};
	WaitForSingleObject(cdata->sharedMemory.hRWMutex, INFINITE);
	if (_tcscmp(cdata->sharedMemory.sharedMessage->transacao.nome, _T(" ")) != 0) {
		_tprintf(_T("\nUltima Transação:\n"));

		_tprintf(_T("Empresa: %s | Número de Ações : %d | Preço Total: %.2f\n"),
			cdata->sharedMemory.sharedMessage->transacao.nome,
			cdata->sharedMemory.sharedMessage->transacao.nAcoes,
			cdata->sharedMemory.sharedMessage->transacao.valorTotal);
	}
	ReleaseMutex(cdata->sharedMemory.hRWMutex);


	while (1) {
		DWORD waitResult = WaitForMultipleObjects(3, handles, FALSE, INFINITE);

		if (waitResult == WAIT_OBJECT_0) {
			return 1;
		}
		else if (waitResult == WAIT_OBJECT_0 + 1) {
			cdata->stop = 1;
			return 1;
		}
		WaitForSingleObject(cdata->sharedMemory.hRWMutex, INFINITE);

		_tprintf(_T("\nUltima Transação:\n"));

		_tprintf(_T("Empresa: %s | Número de Ações : %d | Preço Total: %.2f\n"),
			cdata->sharedMemory.sharedMessage->transacao.nome,
			cdata->sharedMemory.sharedMessage->transacao.nAcoes,
			cdata->sharedMemory.sharedMessage->transacao.valorTotal);
		
		ReleaseMutex(cdata->sharedMemory.hRWMutex);
		ResetEvent(cdata->sharedMemory.TransacEvent);
	}
	return 0;
}


int _tmain(int argc, TCHAR* argv[])
{
	ControlData controlData;
	TCHAR command[BUFFER_SIZE];
	HANDLE hThread, hThread2;
	HANDLE alredyRunning;


#ifdef UNICODE
	_setmode(_fileno(stdin), _O_WTEXT);
	_setmode(_fileno(stdout), _O_WTEXT);
	_setmode(_fileno(stderr), _O_WTEXT);
#endif
	controlData.stop = 0;
	alredyRunning = OpenEvent(EVENT_ALL_ACCESS, FALSE, EVENT_RUN);
	// Verifica se o evento não existe
	if (alredyRunning == NULL) {
		if (GetLastError() == ERROR_FILE_NOT_FOUND) {
			_tprintf(_T("Bolsa is not running. Try again later\n"));
		}
		else {
			_tprintf(_T("Failed to open event. Error: %d\n"), GetLastError());
		}
		return -1;
	}

	controlData.nEmpresasToShow = 0;
	if (argc >= 2) {
		if (_stscanf_s(argv[1], _T("%d"), &controlData.nEmpresasToShow) == 1) {
			if (controlData.nEmpresasToShow > 10 || controlData.nEmpresasToShow <= 0) {
				controlData.nEmpresasToShow = 10;
				_tprintf(_T("Second argument is invalid and has been set to default value - 10\n"));
			}
		}
		else {
			controlData.nEmpresasToShow = 10;
			_tprintf(_T("Second argument is invalid and has been set to default value - 10\n"));
		}
	}
	else {
		controlData.nEmpresasToShow = 10;
	}

	if (!initMemAndSync(&controlData))
	{
		_tprintf(TEXT("Error creating/opening shared memory and synchronization mechanisms.\n"));
		exit(1);
	}
	controlData.shutdown = CreateEvent(NULL, TRUE, FALSE, NULL);



	hThread = CreateThread(NULL, 0, topCompanies, &controlData, 0, NULL);
	hThread2 = CreateThread(NULL, 0, showTransanction, &controlData, 0, NULL);

	_tprintf(TEXT("Type in 'exit' to leave.\n"));
	do {
		_getts_s(command, 100);
	} while (_tcscmp(command, TEXT("exit")) != 0 && controlData.stop == 0);

	SetEvent(controlData.shutdown);


	WaitForSingleObject(hThread, INFINITE); 
	WaitForSingleObject(hThread2, INFINITE);
	UnmapViewOfFile(controlData.sharedMemory.sharedMessage);
	CloseHandle(controlData.sharedMemory.hMapFile);
	CloseHandle(controlData.sharedMemory.hRWMutex);
	CloseHandle(controlData.sharedMemory.CompEvent);
	CloseHandle(controlData.sharedMemory.TransacEvent);
	CloseHandle(controlData.shutdown);


	_tprintf(_T("FIM"));
}