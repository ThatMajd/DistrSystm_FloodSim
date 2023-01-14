import java.io.*;
import java.net.*;
import java.util.*;

public class Node {

    private Map<Integer, List<Integer>> routingTable;
    public Integer id;
    private Map<Integer, Double> neighbors;
    private Map<Integer, List<Integer>> ports;
    // ports is of the form (neighbor_id) -> (send_port, listen_port)


    // TODO
    // each node should have a Routing table, that has an entry for all the vertices

    public static <K, V> void printMap(Map<K, V> map) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }
    public Node(String line){
        this.ports = new HashMap<>();
        this.neighbors = new HashMap<>();
        parseLine(line);
    }

    private void test(){
        for (Integer neighbor: this.neighbors.keySet()){
            int listen_port = this.ports.get(neighbor).get(1);
        }
    }
    private void flooding_seq() throws IOException {
        for (Integer neighbor: this.neighbors.keySet()){
            sendMessage("a", neighbor);
        }
    }

    private void parseLine(String line){
        String[] parts = line.split(" ");
        this.id = Integer.parseInt(parts[0]);
        for (int i = 1; i < parts.length; i += 4) {
            int neighbor = Integer.parseInt(parts[i]);
            double weight = Double.parseDouble(parts[i + 1]);
            int send_port = Integer.parseInt(parts[i + 2]);
            int listen_port = Integer.parseInt(parts[i + 3]);

            if (!neighbors.containsKey(neighbor)) {
                neighbors.put(neighbor, weight);
                ports.put(neighbor, new ArrayList<>());
            }
            ports.get(neighbor).add(send_port);
            ports.get(neighbor).add(listen_port);
        }
    }
    private void sendMessage(String msg, Integer receiver) throws IOException {
        /*
        This function sends a message to another node
        @param msg that message to be sent
        @param receiver the id of the receiver
        @returns None
         */
        try {
            Socket socket = new Socket("localhost", this.ports.get(receiver).get(0));
            OutputStream out = socket.getOutputStream();
            out.write((this.id+"/"+msg+"/1").getBytes());
            socket.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    private void handleMessage(String message){
        String[] parts = message.split("/");
        int sender = Integer.parseInt(parts[0]);
        String orig_msg = parts[1];
        int seq = Integer.parseInt(parts[2]);
    }
    public void receiveMessage() throws IOException{
        /*
        receives a message by listening on all ports
         */
        for (Integer neighbor: this.neighbors.keySet()){
            Thread thread = new Thread(() -> {
                int port = this.ports.get(neighbor).get(1);
                try {
                    ServerSocket serverSocket = new ServerSocket(port);
                    while (true) {
                        Socket socket = serverSocket.accept();
                        InputStream in = socket.getInputStream();
                        byte[] message = new byte[1024];
                        in.read(message);
                        handleMessage(new String(message));
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        }
    }
}