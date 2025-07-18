package pt.isec.pd.springboot.m1.client.model;

import pt.isec.pd.springboot.m1.commands.ComandoStruct;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientCon {
    private String serverAddress;
    private int serverPort;
    private Socket socket;
    private ObjectInputStream oin;
    private ObjectOutputStream oout;
    private BufferedReader input;
    private PrintWriter output;
    public static final int MAX_SIZE = 4000;
    public static final int TIMEOUT = 10;

    public ClientCon(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;

    }


    public boolean conectar() {
        try {
            socket = new Socket(InetAddress.getByName(serverAddress), serverPort);
            // Set a reasonable timeout value or consider removing the timeout if not needed
            // socket.setSoTimeout(TIMEOUT * 1000);

            oout = new ObjectOutputStream(socket.getOutputStream());
            oout.flush(); // Flush the header for the ObjectOutputStream
            oin = new ObjectInputStream(socket.getInputStream());

            // Now it's safe to start listening for notifications

            return true;
        } catch (Exception e) {
            System.out.println("Ocorreu um erro ao conectar ao socket:\n\t" + e);
            return false;
        }
    }


    public boolean enviarCredenciais(String email, String senha) {
        try {
            output.println(email);
            output.println(senha);
            String resposta = input.readLine();
            return resposta != null && resposta.equals("OK");
        } catch (SocketTimeoutException e) {
            System.err.println("Timeout: O servidor não respondeu em 10 segundos.");
            return false;
        } catch (IOException e) {
            System.err.println("Erro ao enviar credenciais: " + e.getMessage());
            return false;
        }
    }

    public String enviarComando(ComandoStruct comando) {
        try {
            oout.reset();
            oout.writeObject(comando);
            oout.flush();

            String response = (String) oin.readObject();
            if (response != null) {
                return response;
            } else {
                return "Sem resposta do servidor.";
            }
        } catch (IOException | ClassNotFoundException e) {
            return "Erro ao enviar comando: " + e.getMessage();
        }
    }
    public void desconectar() {
        try {
            // Verifica se o socket não é nulo e se está aberto antes de tentar fechar
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Conexão com o servidor encerrada.");
            }
        } catch (IOException e) {
            System.err.println("Erro ao fechar a conexão com o servidor: " + e.getMessage());
        }
    }
}