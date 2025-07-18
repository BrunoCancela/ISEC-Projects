#include <windows.h>
#include <tchar.h>
#include <fcntl.h>
#include <stdio.h>
#include <io.h>
#include <time.h>
#include "cliente.h"

HANDLE ConnectToServer() {
    HANDLE hPipe;
    while (1) {
        hPipe = CreateFile(
            PIPE_BOLSA,
            GENERIC_READ | GENERIC_WRITE,
            0 | FILE_SHARE_READ | FILE_SHARE_WRITE,
            NULL,
            OPEN_EXISTING,
            FILE_FLAG_OVERLAPPED | 0,
            NULL);

        //dwMode = PIPE_READMODE_MESSAGE
        //SetNamedPIpeHandleState()
        if (hPipe != INVALID_HANDLE_VALUE)
            break;

        if (GetLastError() != ERROR_PIPE_BUSY) {
            _tprintf(_T("Could not open pipe. GLE=%d\n"), GetLastError());
            return INVALID_HANDLE_VALUE;
        }

        // All pipe instances are busy, so wait for 20 seconds.
        if (!WaitNamedPipe(PIPE_BOLSA, 20000)) {
            _tprintf(_T("Could not open pipe: 20 second wait timed out.\n"));
            return INVALID_HANDLE_VALUE;
        }
        DWORD dwMode = PIPE_READMODE_MESSAGE;
        DWORD fSuccess = SetNamedPipeHandleState(
            hPipe,
            dwMode,
            NULL,
            NULL
        );
        if (!fSuccess) {
            return -1;
        }
    }
    return hPipe;
}


DWORD WINAPI ReadFromServer(LPVOID lpParam) {
    ControlData* cd = (ControlData*)lpParam;
    HANDLE hPipe = cd->hPipe;
    Mensagens buffer;
    DWORD bytesRead;
    OVERLAPPED overlapped = { 0 };
    overlapped.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);

    while (1) {
        if (!ReadFile(hPipe, &buffer, sizeof(Mensagens), &bytesRead, &overlapped)) {
            if (GetLastError() == ERROR_IO_PENDING) {
                WaitForSingleObject(overlapped.hEvent, INFINITE);
                if (!GetOverlappedResult(hPipe, &overlapped, &bytesRead, FALSE)) {
                    _tprintf(_T("Read error: %d\n"), GetLastError());
                    _tprintf(_T("\nLost connection to bolsa. Please press Enter to close\n"));
                    cd->bolsaClosed = 1;
                    break;
                }
            }
            else {
                _tprintf(_T("Read error: %d\n"), GetLastError());
                _tprintf(_T("\nLost connection to bolsa. Please press Enter to close\n"));
                cd->bolsaClosed = 1;
                break;
            }
        }

        if (bytesRead > 0) {
            if (buffer.tipo == 1) {
                if (_tcscmp(buffer.mensagem, _T("LOGGED")) == 0) {
                    _tprintf(_T("You have been successfully logged in.\n"));
                    cd->loggedIn = 1;
                }
                else if (_tcscmp(buffer.mensagem, _T("EXIT")) == 0) {
                    _tprintf(_T("EXIT\n"));
                    break;
                }else{
                    _tprintf(_T("%s\n"), buffer.mensagem);
                }
            }else if(buffer.tipo == 2){
                _tprintf(_T("%s"), buffer.mensagem);
                for (int i = 0; i < buffer.nEmpresas; i++) {
                    _tprintf(_T("Empresa: %s | Ações Disponivies: %d | Preço Atual: %.2f\n"), buffer.empresas[i].nome, buffer.empresas[i].acoesDisponives, buffer.empresas[i].preco);
                }
            }
            else if (buffer.tipo == 3) {
                cd->bolsaClosed = 1;
                _tprintf(_T("\nYou have been disconnected from bolsa. Please press Enter to close.\n"));
                break;
            }
        }
        memset(&buffer, 0, sizeof(Mensagens));
    }
    CloseHandle(hPipe);
    CloseHandle(overlapped.hEvent);
    return 0;   
}

void WriteToServer(HANDLE hPipe, const TCHAR* message) {
    DWORD bytesWritten;
    OVERLAPPED overlapped = { 0 };
    overlapped.hEvent = CreateEvent(NULL, TRUE, FALSE, NULL);

    // Note: Using TCHAR, the size must consider TCHAR is potentially a wchar_t
    DWORD messageBytes = (_tcslen(message) + 1) * sizeof(TCHAR); // +1 for null terminator

    if (!WriteFile(hPipe, message, messageBytes, &bytesWritten, &overlapped)) {
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
    WaitForSingleObject(overlapped.hEvent, INFINITE);
    CloseHandle(overlapped.hEvent);
}

int HandleLogin(ControlData* cd, TCHAR* command) {
    TCHAR userName[100];
    TCHAR password[100];
    if (_stscanf_s(command, _T("%99s %99s"), userName, 100, password, 100) == 2) {
        WriteToServer(cd->hPipe, command);
    }
    else {
        _tprintf(_T("Invalid parameters. Usage: login <user-name> <password>\n"));
    }
}

int HandleBuy(ControlData* cd, TCHAR* command) {
    TCHAR nomeEmpresa[100];
    int nAcoes;
    if (_stscanf_s(command, _T("%99s %d"), nomeEmpresa, 100, &nAcoes) == 2) {
        _stprintf_s(command, BUFFER_SIZE, _T("buy %s %d"), nomeEmpresa, nAcoes);
        WriteToServer(cd->hPipe, command);
        ZeroMemory(command, sizeof(command));

    }
    else {
        _tprintf(_T("Invalid parameters. Usage: buy <company-name> <share-number>\n"));
    }
}

int HandleSell(ControlData* cd, TCHAR* command) {
    TCHAR nomeEmpresa[100];
    int nAcoes;
    if (_stscanf_s(command, _T("%99s %d"), nomeEmpresa, 100, &nAcoes) == 2) {
        _stprintf_s(command, BUFFER_SIZE, _T("sell %s %d"), nomeEmpresa, nAcoes);
        WriteToServer(cd->hPipe, command);
        ZeroMemory(command, sizeof(command));

    }
    else {
        _tprintf(_T("Invalid parameters. Usage: buy <company-name> <share-number>\n"));
    }
}

int ProcessCommand(ControlData* cd, TCHAR* command) {
    if (_tcsncmp(command, _T("login "), 6) == 0) {
        if (cd->loggedIn == 0) {
            HandleLogin(cd, command + 6);
            return 1;
        }
        else {
            _tprintf(_T("\nYou are currently logged.\n"));
        }
    }
    else if (_tcscmp(command, _T("listc")) == 0) {
        if (cd->loggedIn == 1) {
            WriteToServer(cd->hPipe, command);
            return 1;
        }
        else {
            _tprintf(_T("\nYou are not currently logged in to make this command.\n"));
        }
        
    }
    else if (_tcsncmp(command, _T("buy "), 4) == 0) {
        if (cd->loggedIn == 1) {
            HandleBuy(cd, command + 4);
            return 1;
        }
        else {
            _tprintf(_T("\nYou are not currently logged in to make this command.\n"));
        }
    }
    else if (_tcsncmp(command, _T("sell "), 5) == 0) {
        if (cd->loggedIn == 1) {
            HandleSell(cd, command + 4);
            return 1;
        }
        else {
            _tprintf(_T("You are not currently logged in to make this command.\n"));
        }
    }
    else if (_tcscmp(command, _T("balance")) == 0) {
        if (cd->loggedIn == 1) {
            WriteToServer(cd->hPipe, command);
            return 1;
        }
        else {
            _tprintf(_T("\nYou are not currently logged in to make this command.\n"));
        }
    }
    else if (_tcscmp(command, _T("exit")) == 0) {
        WriteToServer(cd->hPipe, command);
        return 0;
    }
    else {
        _tprintf(_T("Unknown command.\n"));
        return 1;
    }
}

int _tmain(int argc, TCHAR* argv[])
{
	ControlData controlData;
	TCHAR command[BUFFER_SIZE];
	HANDLE hThread;
	HANDLE alredyRunning;
	DWORD written;
    TCHAR messageBuffer[BUFFER_SIZE];



#ifdef UNICODE
	_setmode(_fileno(stdin), _O_WTEXT);
	_setmode(_fileno(stdout), _O_WTEXT);
	_setmode(_fileno(stderr), _O_WTEXT);
#endif

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

    controlData.loggedIn = 0;
    controlData.bolsaClosed = 0;


    controlData.hSemaphore = OpenSemaphore(SEMAPHORE_ALL_ACCESS, FALSE, SEMAPHORE_NAME);
    if (controlData.hSemaphore == NULL) {
        _tprintf(_T("OpenSemaphore error: %d\n"), GetLastError());
        return -1;
    }
    _tprintf(_T("Waiting to connect to bolsa\n"));
    WaitForSingleObject(controlData.hSemaphore, INFINITE);



    controlData.hPipe = ConnectToServer();
	if (controlData.hPipe == INVALID_HANDLE_VALUE) {
		return 1;
	}

	_tprintf(_T("Connected to bolsa. You can now read from and write to the bolsa.\n"));

	HANDLE hThreadRead = CreateThread(NULL, 0, ReadFromServer, &controlData, 0, NULL);

    int running = 1;
    _tprintf(_T("Enter messages to send to the bolsa. Type 'exit' to quit:\n"));

    do {
        ZeroMemory(messageBuffer, sizeof(messageBuffer));
        _getts_s(messageBuffer, BUFFER_SIZE);
        if (controlData.bolsaClosed == 0) {
            running = ProcessCommand(&controlData, messageBuffer);

            if (_tcscmp(messageBuffer, _T("exit\n")) == 0) {
                break;
            }
        }
        ZeroMemory(messageBuffer, sizeof(messageBuffer));

        // Optional: Clear the buffer if needed
    } while (running && controlData.bolsaClosed == 0);

	WaitForSingleObject(hThreadRead, INFINITE);
    CloseHandle(controlData.hSemaphore);
	CloseHandle(hThreadRead);
	CloseHandle(controlData.hPipe);

	return 0;
}