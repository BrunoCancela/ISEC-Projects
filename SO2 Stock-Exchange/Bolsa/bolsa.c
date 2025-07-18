#include <windows.h>
#include <tchar.h>
#include <fcntl.h>
#include <stdio.h>
#include <io.h>
#include <time.h>
#include "bolsa.h"

Empresa empresaDefault = { .acoes = 0, .acoesDisponives = 0, .preco = 0, .nome = {_T(" ")}};

void initReg(ControlData* cd) {
	cd->nClientes = 0;
	cd->nEmpresas = 0;
	cd->maxClientes = 0;
	cd->pause = 0;
	
	LSTATUS res;
	HKEY chave = 0;
	DWORD estado;


	//Registry
	res = RegCreateKeyExW(HKEY_CURRENT_USER,
		REG_KEY,
		0,
		NULL,
		REG_OPTION_NON_VOLATILE,
		KEY_ALL_ACCESS,
		NULL,
		&chave,
		&estado);

	if (res == ERROR_SUCCESS) {
		if (estado == REG_OPENED_EXISTING_KEY) {
			_tprintf(_T("Chave já criada\n"));
		}
		else if (estado == REG_CREATED_NEW_KEY) {
			_tprintf(_T("Chave criada com sucesso\n"));
		}
	}

	if (chave != 0) {
		DWORD par_tipo, tam = sizeof(REG_NAME);
		res = RegQueryValueEx(chave,
			REG_NAME,
			NULL,
			&par_tipo,
			NULL,
			NULL);

		if (res == ERROR_SUCCESS && par_tipo == REG_DWORD) {
			res = RegQueryValueEx(chave,
				REG_NAME,
				NULL,
				&par_tipo,
				(const BYTE*)&cd->maxClientes,
				&tam);
			if (res == ERROR_SUCCESS)
				_tprintf(_T("MaxClients: %d\n"), cd->maxClientes);
		}else {
			cd->maxClientes = MAX_CLIENTES_CON;
			res = RegSetValueEx(chave,
				REG_NAME,
				0,
				REG_DWORD,
				(const BYTE*)&cd->maxClientes,
				sizeof(cd->maxClientes));
			if (res == ERROR_SUCCESS)
				_tprintf(_T("\nSUCESSO\n"));
			else
				_tprintf(_T("\nERRO\n"));
		}
	}
}

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
		SHAREDMEMSIZE); 

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

	_tprintf(_T("\n%d\n"), cdata->maxClientes);
	cdata->hSemaphore = CreateSemaphore(NULL, cdata->maxClientes, cdata->maxClientes, SEMAPHORE_NAME);
	if (cdata->hSemaphore == NULL) {
		UnmapViewOfFile(cdata->sharedMemory.sharedMessage);
		CloseHandle(cdata->sharedMemory.hMapFile);
		CloseHandle(cdata->sharedMemory.hRWMutex);
		CloseHandle(cdata->sharedMemory.CompEvent);
		CloseHandle(cdata->sharedMemory.TransacEvent);
		CloseHandle(cdata->sharedMemory.CloseBoardEvent);
		return FALSE;
	}

	cdata->sharedMemory.sharedMessage->topEmpresas.nEmpresas = 0;
	for (int i = 0; i < MAX_TOP_EMPRESAS; i++) {
		cdata->sharedMemory.sharedMessage->topEmpresas.empresas[i] = empresaDefault;
	}

	_tcscpy_s(cdata->sharedMemory.sharedMessage->transacao.nome, BUFFER_SIZE, _T(" "));
	cdata->sharedMemory.sharedMessage->transacao.valorTotal = 0.0;
	cdata->sharedMemory.sharedMessage->transacao.nAcoes = 0;



	return TRUE;
}

BOOL loadUsers(ControlData* cdata, TCHAR* usersFile) {
	HANDLE hFile = CreateFile(
		usersFile,
		GENERIC_READ,
		FILE_SHARE_READ,
		NULL,
		OPEN_EXISTING,
		FILE_ATTRIBUTE_NORMAL,
		NULL);

	if (hFile == INVALID_HANDLE_VALUE) {
		_tprintf(_T("ERROR - %lu\n"), GetLastError());
		return FALSE;
	}

	HANDLE hFileMapping = CreateFileMapping(
		hFile,
		NULL,
		PAGE_READONLY,
		0,
		0,  // Map the entire file
		NULL);

	if (hFileMapping == NULL) {
		_tprintf(_T("CreateFileMapping failed with %lu\n"), GetLastError());
		CloseHandle(hFile);
		return FALSE;
	}

	char* pData = (char*)MapViewOfFile(
		hFileMapping,
		FILE_MAP_READ,
		0,
		0,
		0);  // Map the entire file

	if (pData == NULL) {
		_tprintf(_T("MapViewOfFile failed with %lu\n"), GetLastError());
		CloseHandle(hFileMapping);
		CloseHandle(hFile);
		return FALSE;
	}

	TCHAR line[BUFFER_SIZE*3];
	int j = 0;
	for (int i = 0; pData[i] != '\0'; i++) {
		if (j < BUFFER_SIZE - 1) {
			line[j] = pData[i];
			j++;
		}

		if (pData[i+1] == '\n' || pData[i+1] == '\0' || j >= BUFFER_SIZE*3 - 1) {
			line[j] = '\0'; 

			_tprintf(_T("%s"), line);

			if (_stscanf_s(line, _T("%255s %255s %f"),
				cdata->clientes[cdata->nClientes].nome, BUFFER_SIZE,
				cdata->clientes[cdata->nClientes].password, BUFFER_SIZE,
				&cdata->clientes[cdata->nClientes].saldo) == 3) {
				cdata->clientes[cdata->nClientes].estado = 0;
				cdata->clientes[cdata->nClientes].nCarteiraEmpresas = 0;
				for (int k = 0; k < 5; k++) {
					cdata->clientes[cdata->nClientes].carteira[k].empresa = NULL;
					cdata->clientes[cdata->nClientes].carteira[k].nAcoesCompradas = 0;
					cdata->clientes[cdata->nClientes].carteira[k].nAcoesParaVenda = 0;
				}
				cdata->nClientes++;
			}
			j = 0; 
		}
		if (cdata->nClientes == MAX_CLIENTES) {
			break;
		}
	}

	UnmapViewOfFile(pData);
	CloseHandle(hFileMapping);
	CloseHandle(hFile);

	_tprintf(_T("\nClient List %d:\n"), cdata->nClientes);
	for (int i = 0; i < cdata->nClientes; i++) {
		_tprintf(_T("Name: %s, Password: %s, Balance: %.2f\n"),
			cdata->clientes[i].nome,
			cdata->clientes[i].password,
			cdata->clientes[i].saldo);
	}



	return TRUE;
}

void companiesAndTransactions(ControlData* cdata) {
	WaitForSingleObject(cdata->sharedMemory.hRWMutex, INFINITE);
	cdata->sharedMemory.sharedMessage->topEmpresas.nEmpresas = 0;
	for (int i = 0; i < cdata->nEmpresas && i < 10; i++) {
		cdata->sharedMemory.sharedMessage->topEmpresas.empresas[i] = cdata->empresas[i];
		cdata->sharedMemory.sharedMessage->topEmpresas.nEmpresas++;
	}
	ReleaseMutex(cdata->sharedMemory.hRWMutex);
	SetEvent(cdata->sharedMemory.CompEvent);
}

void checkTop10(ControlData* cdata) {
	int i;
	for (i = 0; i < MAX_TOP_EMPRESAS; i++) {
		if (_tcscmp(cdata->empresas[i].nome, cdata->sharedMemory.sharedMessage->topEmpresas.empresas[i].nome) != 0) {
			companiesAndTransactions(cdata);
			break;
		}
	}
}

void sortCompanies(ControlData* cdata) {
	int i, j;
	Empresa temp;
	for (i = 0; i < cdata->nEmpresas - 1; i++) {
		for (j = 0; j < cdata->nEmpresas - i - 1; j++) {
			if (cdata->empresas[j].preco < cdata->empresas[j + 1].preco) {
				temp = cdata->empresas[j];
				cdata->empresas[j] = cdata->empresas[j + 1];
				cdata->empresas[j + 1] = temp;
			}
		}
	}
	checkTop10(cdata);
}

void HandleAddCompany(ControlData* cd, const TCHAR* params) {
	TCHAR companyName[BUFFER_SIZE];
	int numShares;
	float sharePrice;

	if (_stscanf_s(params, _T("%255s %d %f"), companyName, BUFFER_SIZE, &numShares, &sharePrice) == 3) {
		if (cd->nEmpresas < MAX_EMPRESAS) {
			int exists = 1;
			for (int i = 0; i < cd->nEmpresas; i++) {
				if (_tcscmp(companyName, cd->empresas[i].nome) == 0) {
					exists = 0;
					break;
				}
			}
			if (exists) {
				_tcscpy_s(cd->empresas[cd->nEmpresas].nome, BUFFER_SIZE, companyName);
				cd->empresas[cd->nEmpresas].acoesDisponives = numShares;
				cd->empresas[cd->nEmpresas].acoes = numShares;
				cd->empresas[cd->nEmpresas].preco = sharePrice;
				cd->nEmpresas++;
				_tprintf(_T("Companie %s has been added successfully\n"), companyName);
				sortCompanies(cd);

			}
			else {
				_tprintf(_T("Companie %s already exists\n"), companyName);
			}
		}
		else {
			_tprintf(_T("Already reach the max of companies\n"));
		}
	}
	else {
		_tprintf(_T("Invalid parameters. Usage: addc <company-name> <number-of-shares> <share-price>\n"));
	}
}

BOOL loadCompanies(ControlData* cdata) {
	HANDLE hFile = CreateFile(
		lpCompaniesFileName,
		GENERIC_READ,
		FILE_SHARE_READ,
		NULL,
		OPEN_EXISTING,
		FILE_ATTRIBUTE_NORMAL,
		NULL);

	if (hFile == INVALID_HANDLE_VALUE) {
		_tprintf(_T("ERROR - %lu\n"), GetLastError());
		return FALSE;
	}

	HANDLE hFileMapping = CreateFileMapping(
		hFile,
		NULL,
		PAGE_READONLY,
		0,
		0,  // Map the entire file
		NULL);

	if (hFileMapping == NULL) {
		_tprintf(_T("CreateFileMapping failed with %lu\n"), GetLastError());
		CloseHandle(hFile);
		return FALSE;
	}

	char* pData = (char*)MapViewOfFile(
		hFileMapping,
		FILE_MAP_READ,
		0,
		0,
		0);  // Map the entire file

	if (pData == NULL) {
		_tprintf(_T("MapViewOfFile failed with %lu\n"), GetLastError());
		CloseHandle(hFileMapping);
		CloseHandle(hFile);
		return FALSE;
	}


	TCHAR line[BUFFER_SIZE * 3];
	int j = 0;
	for (int i = 0; pData[i] != '\0'; i++) {
		if (j < BUFFER_SIZE - 1) {
			line[j] = pData[i];
			j++;
		}
		if (pData[i + 1] == '\n' || pData[i + 1] == '\0' || j >= BUFFER_SIZE * 3 - 1) {

			line[j] = '\0';
			HandleAddCompany(cdata, line);
			j = 0;
		}
	}

	UnmapViewOfFile(pData);
	CloseHandle(hFileMapping);
	CloseHandle(hFile);

	return TRUE;
}

void WriteToClient(HANDLE hPipe, const Mensagens* message) {
	DWORD bytesWritten;
	OVERLAPPED overlapped = { 0 };
	overlapped.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);

	//DWORD messageBytes = (_tcslen(message) + 1) * sizeof(message);

	if (!WriteFile(hPipe, message, sizeof(Mensagens), &bytesWritten, &overlapped)) {
		if (GetLastError() == ERROR_IO_PENDING) {
			WaitForSingleObject(overlapped.hEvent, INFINITE);
			if (!GetOverlappedResult(hPipe, &overlapped, &bytesWritten, FALSE)) {
				_tprintf(_T("Write error: %d\n"), GetLastError());
			}
		}
		else {
			_tprintf(_T("Write error: %d\n"), GetLastError());
		}
	}
	CloseHandle(overlapped.hEvent);
}

void HandleListCompanies(ControlData* cd) {
	_tprintf(_T("Companies: \n"));
	for (int i = 0; i < cd->nEmpresas; i++) {
		_tprintf(_T("Empresa: %s | Ações Disponivies: %d | Preço Atual: %.2f\n"), cd->empresas[i].nome, cd->empresas[i].acoesDisponives, cd->empresas[i].preco);
	}
}

void HandleSetStockPrice(ControlData* cd, const TCHAR* params) {
	TCHAR companyName[BUFFER_SIZE];
	float newPrice;

	if (_stscanf_s(params, _T("%255s %f"), companyName, BUFFER_SIZE, &newPrice) == 2) {
		int exists = 0;
		int i = 0;
		for (i = 0; i < cd->nEmpresas; i++) {
			if (_tcscmp(companyName, cd->empresas[i].nome) == 0) {
				exists = 1;
				break;
			}
		}
		if (exists) {
			cd->empresas[i].preco = newPrice;
			_tprintf(_T("The share price of the companie \"%s\" has been been altered to %.2f\n"),companyName, newPrice);
			checkTop10(cd);
			Mensagens novoValor;
			novoValor.tipo = 1;
			_stprintf_s(novoValor.mensagem, BUFFER_SIZE, _T("Empresa: %s, novo valor: %.2f"), cd->empresas[i].nome, cd->empresas[i].preco);
			Broadcast(cd, novoValor);
		}
		else {
			_tprintf(_T("The companie \"%s\" doesnt exist\n"), companyName);
		}
	}
	else {
		_tprintf(_T("Invalid parameters. Usage: stock <company-name> <new-price>\n"));
	}
}


void HandleListUsers(ControlData* cd) {
	_tprintf(_T("Users: \n"));
	for (int i = 0; i < cd->nClientes; i++) {
		_tprintf(_T("Username: %s | Balance: %.2f | "), cd->clientes[i].nome, cd->clientes[i].saldo);

		if (cd->clientes[i].estado == 1) {
			_tprintf(_T("ONLINE\n"));
		}
		else {
			_tprintf(_T("OFFLINE\n"));
		}
	}
}

DWORD WINAPI ThreadPause(LPVOID lpParam) {
	ControlData* cd = (ControlData*)lpParam;
	int seconds;
	WaitForSingleObject(cd->hMutexBolsa, INFINITE);
	seconds = cd->pause;
	ReleaseMutex(cd->hMutexBolsa);
	_tprintf(_T("Seconds must be more than 0\n"));
	seconds = seconds * 1000;
	Sleep(seconds);
	_tprintf(_T("Seconds must be more than 0\n"));

	WaitForSingleObject(cd->hMutexBolsa, INFINITE);
	cd->pause = 0;
	ReleaseMutex(cd->hMutexBolsa);

}

void HandlePauseOperations(ControlData* cd, const TCHAR* params) {
    int seconds;

    if (_stscanf_s(params, _T("%d"), &seconds) == 1) {
		if (seconds <= 0) {
			_tprintf(_T("Seconds must be more than 0\n"));
			return;
		}
		WaitForSingleObject(cd->hMutexBolsa, INFINITE);
		if (cd->pause != 0) {        
			_tprintf(_T("There is already a pausing operations running\n"));
		}
		else {
			cd->pause = seconds;
			_tprintf(_T("Pausing operations for %d seconds\n"), seconds);
			HANDLE hThreadRead = CreateThread(NULL, 0, ThreadPause, cd, 0, NULL);
			if (hThreadRead == NULL) {
				_tprintf(_T("Create pause thread failed with %d.\n"), GetLastError());
			}
		}
		
		ReleaseMutex(cd->hMutexBolsa);

    } else {
        _tprintf(_T("Invalid parameters. Usage: pause <seconds>\n"));
    }
}

void HandleClosePlatform(ControlData* cd) {
	_tprintf(_T("Closing platform and notifying all clients...\n"));
	Mensagens mensagem;
	mensagem.tipo = 3;
	_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("CLOSE"));
	for (int i = 0; i < MAX_CLIENTES; i++) {
		if (cd->pipeClientes[i] != INVALID_HANDLE_VALUE) {
			_tprintf(_T("%d\n"), i);
			WriteToClient(cd->pipeClientes[i], &mensagem);
		}
	}
}

int ProcessCommand(ControlData* cd, TCHAR* command) {
	if (_tcscmp(command, _T("loadc")) == 0) {
		WaitForSingleObject(cd->hMutexBolsa, INFINITE);
		loadCompanies(cd);
		ReleaseMutex(cd->hMutexBolsa);
		return 1;
	}
	else if (_tcsncmp(command, _T("addc "), 5) == 0) {
		WaitForSingleObject(cd->hMutexBolsa, INFINITE);
		HandleAddCompany(cd, command + 5);
		ReleaseMutex(cd->hMutexBolsa);
		return 1;
	}
	else if (_tcscmp(command, _T("listc")) == 0) {
		WaitForSingleObject(cd->hMutexBolsa, INFINITE);
		HandleListCompanies(cd);
		ReleaseMutex(cd->hMutexBolsa);
		return 1;
	}
	else if (_tcsncmp(command, _T("stock "), 6) == 0) {
		WaitForSingleObject(cd->hMutexBolsa, INFINITE);
		HandleSetStockPrice(cd, command + 6);
		ReleaseMutex(cd->hMutexBolsa);
		return 1;
	}
	else if (_tcscmp(command, _T("users")) == 0) {
		WaitForSingleObject(cd->hMutexBolsa, INFINITE);
		HandleListUsers(cd);
		ReleaseMutex(cd->hMutexBolsa);
		return 1;
	}
	else if (_tcsncmp(command, _T("pause "), 6) == 0) {
		HandlePauseOperations(cd, command + 6);
		return 1;
	}
	else if (_tcscmp(command, _T("close")) == 0) {
		WaitForSingleObject(cd->hMutexBolsa, INFINITE);
		HandleClosePlatform(cd);
		ReleaseMutex(cd->hMutexBolsa);
		return 0;
	}
	else {
		_tprintf(_T("Unknown command.\n"));
		return 1;
	}
}

void ProcessClientCommand(LPTSTR buffer, ControlData* cd, Cliente* currentClient, HANDLE hPipe) {
	Mensagens mensagem;
	Mensagens novoValor;
	novoValor.tipo = 0;

	if (_tcscmp(buffer, _T("listc")) == 0) {
		mensagem.tipo = 2;
		mensagem.nEmpresas = cd->nEmpresas;
		memcpy(mensagem.empresas, cd->empresas, sizeof(cd->empresas));
		_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("Empresas:\n"));
	}
	else if (_tcscmp(buffer, _T("balance")) == 0) {
		mensagem.tipo = 1;
		_stprintf_s(mensagem.mensagem, BUFFER_SIZE, _T("Balance: %.2f"), currentClient->saldo);
	}
	else if (_tcsncmp(buffer, _T("sell "), 5) == 0) {
		int indexOfCompanieInWallet = -1;
		TCHAR nomeEmpresa[100];
		int nAcoes;
		mensagem.tipo = 1;
		if (cd->pause == 0) {

			if (_stscanf_s(buffer + 5, _T("%99s %d"), nomeEmpresa, 100, &nAcoes) == 2) {
				for (int j = 0; j < 5; j++) {
					if (currentClient->carteira[j].empresa != NULL &&
						_tcscmp(nomeEmpresa, currentClient->carteira[j].empresa->nome) == 0) {
						indexOfCompanieInWallet = j;
						break;
					}
				}

				if (indexOfCompanieInWallet != -1) {
					if (currentClient->carteira[indexOfCompanieInWallet].nAcoesCompradas < nAcoes) {
						_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("You don't have enough shares of this company\n"));
					}
					else {
						currentClient->carteira[indexOfCompanieInWallet].empresa->acoesDisponives += nAcoes;
						currentClient->carteira[indexOfCompanieInWallet].nAcoesCompradas -= nAcoes;
						currentClient->carteira[indexOfCompanieInWallet].nAcoesParaVenda += nAcoes;
						_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("You successfully put your shares to sell\n"));
					}
				}
				else {
					_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("You don't have shares of this company\n"));
				}
			}
		}
		else {
			_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("Cant execute this command because is on pause\n"));
		}
	}
	else if (_tcsncmp(buffer, _T("buy "), 4) == 0) {
		int reachedMaxOfCompanies = 0;
		int indexOfCompanieInWallet = -1;
		TCHAR nomeEmpresa[100];
		int nAcoes;
		mensagem.tipo = 1;

		if (cd->pause == 0) {
			if (_stscanf_s(buffer + 4, _T("%99s %d"), nomeEmpresa, 100, &nAcoes) == 2) {
				if (currentClient->nCarteiraEmpresas >= 5) {
					reachedMaxOfCompanies = 1;
					for (int j = 0; j < 5; j++) {
						if (currentClient->carteira[j].empresa != NULL &&
							_tcscmp(nomeEmpresa, currentClient->carteira[j].empresa->nome) == 0) {
							reachedMaxOfCompanies = 0;
							indexOfCompanieInWallet = j;
							break;
						}
					}
				}

				if (reachedMaxOfCompanies) {
					_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("Already reached the number of companies"));
				}
				else {
					int exists = 0;
					int i = 0;
					for (i = 0; i < cd->nEmpresas; i++) {
						if (_tcscmp(nomeEmpresa, cd->empresas[i].nome) == 0) {
							exists = 1;
							break;
						}
					}
					if (exists) {
						if (cd->empresas[i].acoesDisponives < nAcoes) {
							_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("There aren't enough shares to buy\n"));
						}
						else {
							if ((cd->empresas[i].preco * nAcoes) > currentClient->saldo) {
								_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("Not enough money to buy this amount of shares\n"));
							}
							else {
								_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("Successfully bought the shares\n"));

								WaitForSingleObject(cd->sharedMemory.hRWMutex, INFINITE);
								_tcscpy_s(cd->sharedMemory.sharedMessage->transacao.nome, BUFFER_SIZE, cd->empresas[i].nome);
								cd->sharedMemory.sharedMessage->transacao.valorTotal = (nAcoes * cd->empresas[i].preco);
								cd->sharedMemory.sharedMessage->transacao.nAcoes = nAcoes;

								ReleaseMutex(cd->sharedMemory.hRWMutex);
								SetEvent(cd->sharedMemory.TransacEvent);
								if (indexOfCompanieInWallet == -1) {
									int j;
									for (j = 0; j < 5; j++) {
										if (currentClient->carteira[j].empresa == NULL) {
											break;
										}
									}
									indexOfCompanieInWallet = j;
									currentClient->nCarteiraEmpresas++;
									currentClient->carteira[j].empresa = &cd->empresas[i];
								}

								if (cd->empresas[i].acoes >= nAcoes) {
									currentClient->saldo -= cd->empresas[i].preco * nAcoes;
									currentClient->carteira[indexOfCompanieInWallet].nAcoesCompradas += nAcoes;
									cd->empresas[i].acoesDisponives -= nAcoes;
									cd->empresas[i].acoes -= nAcoes;
									cd->empresas[i].preco += ((cd->empresas[i].preco * nAcoes) / 10);
									checkTop10(cd);
									novoValor.tipo = 1;
									_stprintf_s(novoValor.mensagem, BUFFER_SIZE, _T("Empresa: %s | novo valor: %.2f"), cd->empresas[i].nome, cd->empresas[i].preco);
								}
								else {
									int acoesCompradasDaEmpresa = 0;
									int acoesCompradasDeClientes = 0;
									int nAcoesAComprarInicialmente = nAcoes;
									acoesCompradasDaEmpresa = cd->empresas[i].acoes;
									nAcoes -= cd->empresas[i].acoes;
									acoesCompradasDeClientes = nAcoes;
									cd->empresas[i].acoes = 0;
									cd->empresas[i].acoesDisponives -= nAcoes;
									for (int x = 0; x < cd->nClientes; x++) {
										if (&cd->clientes[x] != currentClient && cd->clientes[x].nCarteiraEmpresas != 0) {
											for (int y = 0; y < 5; y++) {
												if (cd->clientes[x].carteira[y].empresa != NULL &&
													_tcscmp(nomeEmpresa, cd->clientes[x].carteira[y].empresa->nome) == 0 &&
													cd->clientes[x].carteira[y].nAcoesParaVenda > 0) {
													if (cd->clientes[x].carteira[y].nAcoesParaVenda >= nAcoes) {
														cd->clientes[x].carteira[y].nAcoesParaVenda -= nAcoes;
														cd->clientes[x].saldo += cd->clientes[x].carteira[y].empresa->preco * nAcoes;
														currentClient->carteira[indexOfCompanieInWallet].nAcoesCompradas += nAcoes;
														nAcoes = 0;
														if (cd->clientes[x].carteira[y].nAcoesParaVenda == 0) {
															if (cd->clientes[x].carteira[y].nAcoesCompradas == 0) {
																cd->clientes[x].carteira[y].empresa = NULL;
																cd->clientes[x].nCarteiraEmpresas--;
															}
														}
													}
													else {
														nAcoes -= cd->empresas[i].acoes;
														currentClient->carteira[indexOfCompanieInWallet].nAcoesCompradas += nAcoes;
														cd->clientes[x].saldo += cd->clientes[x].carteira[y].empresa->preco * cd->clientes[x].carteira[y].nAcoesParaVenda;
														cd->clientes[x].carteira[y].nAcoesParaVenda = 0;
														if (cd->clientes[x].carteira[y].nAcoesCompradas == 0) {
															cd->clientes[x].carteira[y].empresa = NULL;
															cd->clientes[x].nCarteiraEmpresas--;
														}
													}
												}
											}
										}
										if (nAcoes <= 0) {
											break;
										}
									}


									currentClient->saldo -= cd->empresas[i].preco * nAcoesAComprarInicialmente;

									int random = rand() % 2;
									_tprintf(_T("%d"), random);
									if (random == 1) {
										cd->empresas[i].preco += ((cd->empresas[i].preco * nAcoesAComprarInicialmente) / 10);
									}
									else {
										cd->empresas[i].preco -= ((cd->empresas[i].preco * nAcoesAComprarInicialmente) / 10);
										if (cd->empresas[i].preco <= 0) {
											cd->empresas[i].preco = 0.1;
										}
									}
									checkTop10(cd);
									novoValor.tipo = 1;
									_stprintf_s(novoValor.mensagem, BUFFER_SIZE, _T("Empresa: %s | novo valor: %.2f"), cd->empresas[i].nome, cd->empresas[i].preco);
								}
							}
						}
					}
					else {
						_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("The company doesn't exist\n"));
					}
				}
			}
		}else{
			_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("Cant execute this command because is on pause\n"));
		}
	}
	else {
		_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("Invalid command\n"));
	}

	WriteToClient(hPipe, &mensagem);

	if (novoValor.tipo == 1) {
		Broadcast(cd, novoValor);
	}
}

void Broadcast(ControlData* cd, Mensagens mensagem) {
	for (int i = 0; i < MAX_CLIENTES; i++) {
		if (cd->pipeClientes[i] != INVALID_HANDLE_VALUE) {
			WriteToClient(cd->pipeClientes[i], &mensagem);
		}
	}
}

DWORD WINAPI ReadFromClient(LPVOID lpParam) {
	ThreadClient* aux = (ThreadClient*)lpParam;
	HANDLE hPipe = aux->hPipe;
	ControlData* cd = aux->controlData;
	TCHAR buffer[BUFFER_SIZE];
	DWORD bytesRead;
	OVERLAPPED overlapped = { 0 };
	Cliente* currentClient = NULL;
	HANDLE sinalEvento = CreateEvent(NULL, TRUE, FALSE, NULL);

	while (1) {
		ZeroMemory(buffer, sizeof(buffer));
		ResetEvent(overlapped.hEvent);
		overlapped.hEvent = sinalEvento;

		if (!ReadFile(hPipe, buffer, BUFFER_SIZE * sizeof(TCHAR), &bytesRead, &overlapped)) {
			if (GetLastError() == ERROR_IO_PENDING) {
				HANDLE handles[2] = { cd->CloseEvent,overlapped.hEvent };
				DWORD waitResult = WaitForMultipleObjects(2, handles, FALSE, INFINITE);
				if (waitResult == WAIT_OBJECT_0) {
					break;
				}
				if (!GetOverlappedResult(hPipe, &overlapped, &bytesRead, FALSE)) {
					_tprintf(_T("Read error: %d\n"), GetLastError());
					_tprintf(_T("Lost connection to the client\n"));
					break;
				}
			}
			else {
				_tprintf(_T("Read error: %d\n"), GetLastError());
				_tprintf(_T("Lost connection to the client\n"));

				break;
			}
		}

		if (bytesRead > 0) {
			buffer[bytesRead / sizeof(TCHAR)] = _T('\0');
			_tprintf(_T("Received from client: %s\n"), buffer);
		}

		if (_tcscmp(buffer, _T("exit")) == 0) {
			Mensagens mensagem;
			mensagem.tipo = 1;
			_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("EXIT"));
			WriteToClient(hPipe, &mensagem);
			break;
		}

		if (currentClient == NULL) {
			int logIn = 0;
			TCHAR userName[BUFFER_SIZE];
			TCHAR password[BUFFER_SIZE];
			ZeroMemory(userName, sizeof(userName));
			ZeroMemory(password, sizeof(password));

			_stscanf_s(buffer, _T("%99s %99s"), userName, (unsigned)_countof(userName), password, (unsigned)_countof(password));
			WaitForSingleObject(cd->hMutexBolsa, INFINITE);
			for (int i = 0; i < cd->nClientes; i++) {
				if (_tcscmp(cd->clientes[i].nome, userName) == 0 && _tcscmp(cd->clientes[i].password, password) == 0 && cd->clientes[i].estado == 0) {
					cd->clientes[i].estado = 1;
					currentClient = &cd->clientes[i];
					logIn = 1;
					break;
				}
			}
			ReleaseMutex(cd->hMutexBolsa);
			if (logIn) {
				_tprintf(_T("User %s logged in\n"), userName);
				ZeroMemory(buffer, sizeof(buffer));
				Mensagens mensagem;
				mensagem.tipo = 1;
				_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("LOGGED"));
				WriteToClient(hPipe, &mensagem);
			}
			else {
				Mensagens mensagem;
				mensagem.tipo = 1;
				_tcscpy_s(mensagem.mensagem, BUFFER_SIZE, _T("Username wrong or password wrong or client current online\n"));
				WriteToClient(hPipe, &mensagem);
			}
		}
		else {
			WaitForSingleObject(cd->hMutexBolsa, INFINITE);
			ProcessClientCommand(buffer, cd, currentClient, hPipe);
			ReleaseMutex(cd->hMutexBolsa);
			ZeroMemory(buffer, sizeof(buffer));
		}
	}
	for (int i = 0; i < MAX_CLIENTES; i++) {
		if (cd->pipeClientes[i] == hPipe) {
			cd->pipeClientes[i] = INVALID_HANDLE_VALUE;
			break;
		}
	}
	CloseHandle(overlapped.hEvent);
	if(currentClient != NULL)
	currentClient->estado = 0;
	ReleaseSemaphore(cd->hSemaphore, 1, NULL);
	DisconnectNamedPipe(hPipe);
	CloseHandle(hPipe);
	return 0;
}

DWORD WINAPI CreatePipeAndHandleClients(LPVOID lpParam) {
	ControlData* cd = (ControlData*)lpParam;
	HANDLE hPipe, hThreadRead;
	OVERLAPPED overlapped = { 0 };
	HANDLE hThreads[MAX_CLIENTES] = { NULL }; // Array to store thread handles
	int threadCount = 0;

	while (1) {
		hPipe = CreateNamedPipe(
			PIPE_BOLSA,
			PIPE_ACCESS_DUPLEX | FILE_FLAG_OVERLAPPED,
			PIPE_TYPE_MESSAGE | PIPE_READMODE_MESSAGE | PIPE_WAIT,
			PIPE_UNLIMITED_INSTANCES,
			BUFFER_PIPE,
			BUFFER_PIPE,
			5000,
			NULL);

		if (hPipe == INVALID_HANDLE_VALUE) {
			_tprintf(_T("CreateNamedPipe failed with %d.\n"), GetLastError());
			continue;
		}

		overlapped.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);
		if (overlapped.hEvent == NULL) {
			_tprintf(_T("CreateEvent failed with %d.\n"), GetLastError());
			CloseHandle(hPipe);
			continue;
		}

		BOOL connectResult = ConnectNamedPipe(hPipe, &overlapped);
		DWORD error = GetLastError();

		if (!connectResult && error != ERROR_IO_PENDING) {
			if (error == ERROR_PIPE_CONNECTED) {
				_tprintf(_T("Client connected immediately.\n"));
				// Handle client connection immediately
			}
			else {
				_tprintf(_T("ConnectNamedPipe failed with %d.\n"), error);
				CloseHandle(overlapped.hEvent);
				CloseHandle(hPipe);
				continue;
			}
		}

		if (error == ERROR_IO_PENDING) {
			HANDLE handles[2] = { cd->CloseEvent, overlapped.hEvent };
			DWORD waitResult = WaitForMultipleObjects(2, handles, FALSE, INFINITE);
			if (waitResult == WAIT_OBJECT_0) {
				_tprintf(_T("Close event signaled, shutting down.\n"));
				CloseHandle(overlapped.hEvent);
				CloseHandle(hPipe);
				break;
			}
			else if (waitResult == WAIT_OBJECT_0 + 1) {
				_tprintf(_T("Client connected. Starting read thread.\n"));
			}
			else {
				_tprintf(_T("WaitForMultipleObjects failed with %d.\n"), GetLastError());
				CloseHandle(overlapped.hEvent);
				CloseHandle(hPipe);
				continue;
			}
		}

		int i;
		for (i = 0; i < MAX_CLIENTES; i++) {
			if (cd->pipeClientes[i] == INVALID_HANDLE_VALUE) {
				_tprintf(_T("Assigned to slot %d\n"), i);
				break;
			}
		}

		if (i == MAX_CLIENTES) {
			_tprintf(_T("No available slots for new client.\n"));
			CloseHandle(overlapped.hEvent);
			CloseHandle(hPipe);
			continue;
		}

		cd->pipeClientes[i] = hPipe;
		ThreadClient aux;
		aux.hPipe = cd->pipeClientes[i];
		aux.controlData = cd;

		hThreadRead = CreateThread(NULL, 0, ReadFromClient, &aux, 0, NULL);
		if (hThreadRead == NULL) {
			_tprintf(_T("CreateThread failed with %d.\n"), GetLastError());
			CloseHandle(overlapped.hEvent);
			CloseHandle(hPipe);
			cd->pipeClientes[i] = INVALID_HANDLE_VALUE;
			continue;
		}

		hThreads[threadCount++] = hThreadRead;

		CloseHandle(overlapped.hEvent);
	}

	WaitForMultipleObjects(threadCount, hThreads, TRUE, INFINITE);

	for (int j = 0; j < threadCount; j++) {
		CloseHandle(hThreads[j]);
	}

	return 0;
}

int _tmain(int argc, TCHAR* argv[])
{
	ControlData controlData;
	TCHAR command[BUFFER_SIZE];
	HANDLE hThreadServer;
	TCHAR* usersFile;


#ifdef UNICODE
	_setmode(_fileno(stdin), _O_WTEXT);
	_setmode(_fileno(stdout), _O_WTEXT);
	_setmode(_fileno(stderr), _O_WTEXT);
#endif

	if (argc >= 2) {
		TCHAR* arg = argv[1];
		BOOL endsWithTxt = FALSE;
		int len = 0;

		while (arg[len] != '\0') {
			len++;
		}

		if (len > 4 && arg[len - 4] == _T('.') && arg[len - 3] == _T('t') && arg[len - 2] == _T('x') && arg[len - 1] == _T('t')) {
			endsWithTxt = TRUE;
		}

		if (endsWithTxt) {
			usersFile = argv[1];
		}
		else {
			usersFile = _T("users.txt");
		}
	}
	else {
		usersFile = _T("users.txt");
	}

	controlData.shutdown = 0;
	for (int i = 0; i < MAX_CLIENTES; i++) {
		controlData.pipeClientes[i] = INVALID_HANDLE_VALUE;
	}

	controlData.alredyRunning = CreateEvent(NULL, TRUE, FALSE, EVENT_RUN);
	if (GetLastError() == ERROR_ALREADY_EXISTS) {
		_tprintf(_T("\nThere is already a bolsa program running"));
		return -1;
	}

	controlData.CloseEvent = CreateEvent(NULL, TRUE, FALSE, CLOSE_EVENT);
	if (controlData.CloseEvent == NULL) {
		_tprintf(_T("\nCreateEvent close event failed"));
		return -1; 
	}

	srand((unsigned)time(NULL));


	controlData.hMutexBolsa = CreateMutex(NULL,
		FALSE,
		MUTEX_BOLSA);

	if (controlData.hMutexBolsa == NULL)
	{
		_tprintf(TEXT("Error: CreateMutex (%d)\n"), GetLastError());
		return -1;
	}

	initReg(&controlData);
	if (!initMemAndSync(&controlData))
	{
		_tprintf(TEXT("Error creating/opening shared memory and synchronization mechanisms.\n"));
		exit(1);
	}

	if (!loadUsers(&controlData, usersFile)) {
		_tprintf(TEXT("Error loading users.\n"));
		exit(1);
	}

	hThreadServer = CreateThread(NULL, 0, CreatePipeAndHandleClients, &controlData, 0, NULL);

	_tprintf(TEXT("Type in 'close' to leave.\n"));
	int running = 1;
	do {
		_getts_s(command, BUFFER_SIZE);
		running = ProcessCommand(&controlData, command);
	} while (running);

	SetEvent(controlData.CloseEvent);
	SetEvent(controlData.sharedMemory.CloseBoardEvent);

	controlData.shutdown = 1; //altera a flag para que a thread termine

	WaitForSingleObject(hThreadServer, INFINITE);

	CloseHandle(hThreadServer);
	UnmapViewOfFile(controlData.sharedMemory.sharedMessage);
	CloseHandle(controlData.sharedMemory.hMapFile);
	CloseHandle(controlData.sharedMemory.hRWMutex);
	CloseHandle(controlData.sharedMemory.CompEvent);
	CloseHandle(controlData.sharedMemory.TransacEvent);
	CloseHandle(controlData.sharedMemory.CloseBoardEvent);
	CloseHandle(controlData.hSemaphore);
	CloseHandle(controlData.CloseEvent);
	CloseHandle(controlData.alredyRunning);

	_tprintf(_T("FIM"));
}