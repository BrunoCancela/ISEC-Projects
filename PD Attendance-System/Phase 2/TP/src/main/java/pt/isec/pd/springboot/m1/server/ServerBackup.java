package pt.isec.pd.springboot.m1.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.Naming;
import java.util.Timer;
import java.util.TimerTask;

public class ServerBackup {
    private static final String MULTICAST_ADDRESS = "230.44.44.44";
    private static final int MULTICAST_PORT = 4444;
    private String backupDirectory;
    private int localDbVersion = -1; // Local version of the database
    private Timer heartbeatTimer = new Timer();
    private String rmiServiceName;
    private int rmiPort;

    public ServerBackup(String backupDirectory) {
        this.backupDirectory = backupDirectory;
    }

    public void start() {
        resetHeartbeatTimer();
        listenForHeartbeats();
    }

    private void listenForHeartbeats() {
        try (MulticastSocket socket = new MulticastSocket(MULTICAST_PORT)) {
            InetAddress group = InetAddress.getByName(MULTICAST_ADDRESS);
            socket.joinGroup(group);

            while (true) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                processHeartbeat(message);
            }
        } catch (IOException e) {
            System.err.println("Error listening for multicast heartbeats: " + e.getMessage());
        }
    }

    private void processHeartbeat(String message) {
        String[] parts = message.split(";");
        if (parts.length < 3) return;

        try {
            rmiPort = Integer.parseInt(parts[0].trim());
            rmiServiceName = parts[1].trim();
            int serverDbVersion = Integer.parseInt(parts[2].trim());
            resetHeartbeatTimer();
            System.out.println("localDbVersion"+ localDbVersion);
            if(localDbVersion == -1){
                fetchDatabase();
                localDbVersion = serverDbVersion;
            }else {
                if (serverDbVersion == localDbVersion || serverDbVersion-1 == localDbVersion) {
                    fetchDatabase();
                    localDbVersion = serverDbVersion;
                } else {
                    System.out.println("DB version change");
                    System.exit(1);
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Error parsing heartbeat message: " + e.getMessage());
        }
    }


    private void resetHeartbeatTimer() {
        heartbeatTimer.cancel();
        heartbeatTimer = new Timer();
        heartbeatTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.err.println("No heartbeat detected. Shutting down...");
                System.exit(0);
            }
        }, 30000); // 30 seconds
    }

    private void fetchDatabase() {
        try {
            String rmiUrl = "rmi://localhost:" + rmiPort + "/" + rmiServiceName;

            DatabaseTransferInterface remoteService = (DatabaseTransferInterface) Naming.lookup(rmiUrl);

            byte[] dbData = remoteService.getDatabase();

            File backupDir = new File(backupDirectory);
            if (!backupDir.exists()) {
                if (!backupDir.mkdirs()) {
                    System.err.println("Failed to create backup directory: " + backupDirectory);
                    return;
                }
            }

            File backupFile = new File(backupDir, "backup.db");
            try (FileOutputStream fos = new FileOutputStream(backupFile)) {
                fos.write(dbData);
            }
            System.out.println("Database successfully fetched and updated.");
        } catch (Exception e) {
            System.err.println("Error fetching database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean isDirectoryEmpty(String directoryPath) {
        File directory = new File(directoryPath);

        if (!directory.exists()) {
            return true; // Consider non-existent directory as empty
        }

        if (directory.isDirectory()) {
            String[] files = directory.list();
            return files == null || files.length == 0;
        }

        return false; // Return false if it is a file and not a directory
    }


    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: ServerBackup <Backup Directory>");
            System.exit(1);
        }
        boolean isEmpty = isDirectoryEmpty(args[0]);

        if (!isEmpty) {
            System.out.println("Directory is not empty. Terminating process.");
            System.exit(1); // Terminate the process
        }
        ServerBackup backupServer = new ServerBackup(args[0]);
        backupServer.start();
    }
}