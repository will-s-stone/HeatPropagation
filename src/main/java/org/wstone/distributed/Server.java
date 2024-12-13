package org.wstone.distributed;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    String HOST = "localhost";
    int PORT = 6966;
    ServerSocket ss;

    void listen() throws IOException, InterruptedException {
        /*
         * the premise I am going for here is that I will loop and send
         * the map each iteration
         */
        try{
            ss = new ServerSocket(PORT);
            Socket socket = ss.accept(); // blocking
            System.out.println("Connection from " + socket);

            InputStream is = socket.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is);

            ConcurrentHashMap<String, Double> map = (ConcurrentHashMap<String, Double>) ois.readObject();
            System.out.println("Map -->" + map.get(Arrays.toString(new int[]{0, 0})));

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            Thread.sleep(100);
            ss.close();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        Server c = new Server();
        c.listen();
    }
}
