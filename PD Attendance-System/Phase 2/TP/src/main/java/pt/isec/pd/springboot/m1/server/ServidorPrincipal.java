package pt.isec.pd.springboot.m1.server;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.isec.pd.springboot.m1.database.DBConnection;
import pt.isec.pd.springboot.m1.database.SQLiteDBSetup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Service
public class ServidorPrincipal implements DatabaseVersionUpdateListener, DatabaseTransferInterface{
    @Value("${servidor.principal.tcp.port}")
    private int tcpPort;

    @Value("${servidor.principal.db.path}")
    private String dbPath;

    @Value("${servidor.principal.rmi.service.name}")
    private String rmiServiceName;

    @Value("${servidor.principal.rmi.port}")
    private int rmiPort;
    private Connection dbConnection;
    private int dbVersion = 0;
    private static final String MULTICAST_ADDRESS = "230.44.44.44";
    private static final int MULTICAST_PORT = 4444;

    private Thread heartbeatThread;
    private boolean running = true;
    private Thread serverThread;

    @PostConstruct
    public void init() {
        // code to initialize the fields
    }


//    public ServidorPrincipal(int tcpPort, String dbPath, String rmiServiceName, int rmiPort) {
//        //8080 database.db serverp 1099
//        this.tcpPort = tcpPort;
//        this.dbPath = dbPath;
//        this.rmiServiceName = rmiServiceName;
//        this.rmiPort = rmiPort;
//    }

    public void iniciar() {
        if (isServerRunning() || isRMIRegistryRunning()) {
            System.err.println("Another server instance is already running.");
            return;
        }
        inicializarBaseDeDados();
        iniciarRMIServico();
        iniciarServidorTCP();
        iniciarHeartbeat();
    }

    private boolean isServerRunning() {
        try (Socket socket = new Socket(InetAddress.getByName("localhost"), tcpPort)) {
            // If this code is reached, it means a server is running on tcpPort
            return true;
        } catch (IOException e) {
            // Exception is expected if no server is running
            return false;
        }
    }

    private boolean isRMIRegistryRunning() {
        try {
            Registry registry = LocateRegistry.getRegistry(rmiPort);
            registry.lookup(rmiServiceName);
            return true; // If this line is reached, the RMI service is already bound
        } catch (Exception e) {
            // Exception is expected if RMI service is not already bound
            return false;
        }
    }

    private void inicializarBaseDeDados() {
        try {
            File dbFile = new File(dbPath);
            if (!dbFile.exists()) {
                // Database file doesn't exist, initialize it
                SQLiteDBSetup.setup(dbPath);
                System.out.println("Base de dados SQLite criada.");
            }
            String url = "jdbc:sqlite:" + dbPath;
            dbConnection = DBConnection.connect(url);
            dbVersion = ClientHandler.getCurrentVersion(dbConnection);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void iniciarRMIServico() {
        try {
            ServidorPrincipal serviceImpl = this;
            DatabaseTransferInterface stub = (DatabaseTransferInterface) UnicastRemoteObject.exportObject(serviceImpl, 0);
            Registry registry = LocateRegistry.createRegistry(rmiPort);
            registry.rebind(rmiServiceName, stub);
            System.out.println("Serviço RMI " + rmiServiceName + " iniciado no porto " + rmiPort);
        } catch (Exception e) {
            System.err.println("Erro ao iniciar o serviço RMI: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void iniciarServidorTCP() {
        serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
                System.out.println("TCP Server iniciado no porto " + serverSocket.getLocalPort() + " ...");
                while (running) {
                    Socket clientSocket = serverSocket.accept(); // Accept client connection
                    new ClientHandler(clientSocket, dbConnection, this).start(); // Handle client in a new thread
                }
            } catch (IOException e) {
                System.out.println("Erro ao iniciar servidor TCP: " + e.getMessage());
            }
        });

        serverThread.start();
    }
    private void iniciarHeartbeat() {
        heartbeatThread = new Thread(() -> {
            while (running) {
                enviarHeartbeat();
                try {
                    Thread.sleep(10000); // Sleep for 10 seconds
                } catch (InterruptedException e) {
                    System.err.println("Heartbeat thread interrupted: " + e.getMessage());
                }
            }
        });

        heartbeatThread.start();
    }

    private void enviarHeartbeat() {
        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            String message = rmiPort + ";" + rmiServiceName + ";" + dbVersion;
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, group, MULTICAST_PORT);
            socket.send(packet);
            System.out.println("send heartbeat - DB version:"+dbVersion);
        } catch (IOException e) {
            System.err.println("Erro ao enviar heartbeat: " + e.getMessage());
        }
    }
    public void updateDatabaseVersion() {
        dbVersion++; // Increment DB version and send a heartbeat immediately
        enviarHeartbeat();
    }

    // Call this method to stop the heartbeat thread
    public void stopHeartbeat() {
        running = false;
        heartbeatThread.interrupt();
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Erro: numero de argumentos invalido <portoTCP> <caminhoDB> <nomeServicoRMI> <portoRMI>");
            return;
        }
        int tcpPort = Integer.parseInt(args[0]);
        String dbPath = args[1];
        String rmiServiceName = args[2];
        int rmiPort = Integer.parseInt(args[3]);

//        ServidorPrincipal servidor = new ServidorPrincipal(tcpPort, dbPath, rmiServiceName, rmiPort);
//        servidor.iniciar();
    }
    static int getCurrentVersion(Connection connAux) throws SQLException {
        String sql = "SELECT MAX(VersionNumber) FROM versao;";
        try (PreparedStatement pstmt = connAux.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            } else {
                return 0; // Default to 0 if no version found
            }
        }
    }
    private static void insertNewVersion(Connection connAux, int newVersion) throws SQLException {
        String sql = "INSERT INTO versao (VersionNumber) VALUES (?);";
        try (PreparedStatement pstmt = connAux.prepareStatement(sql)) {
            pstmt.setInt(1, newVersion);
            pstmt.executeUpdate();
        }
    }

    @Override
    public void onDatabaseUpdate() throws SQLException {
        int currentVersion = getCurrentVersion(dbConnection);
        int newVersion = currentVersion + 1;
        insertNewVersion(dbConnection,newVersion);
        updateDatabaseVersion();
    }

    @Override
    public byte[] getDatabase() throws RemoteException {
        File dbFile = new File(this.dbPath); // dbPath should be the path to your database file

        if (!dbFile.exists()) {
            throw new RemoteException("Database file not found at " + dbPath);
        }

        try (FileInputStream fis = new FileInputStream(dbFile)) {
            byte[] data = new byte[(int) dbFile.length()];
            fis.read(data);
            return data;
        } catch (IOException e) {
            throw new RemoteException("Error reading database file: " + e.getMessage(), e);
        }
    }
}