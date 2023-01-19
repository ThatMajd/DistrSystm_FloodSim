import java.io.*;
import java.net.*;
import java.util.*;

public class Node extends Thread{

    private Integer num_of_nodes;
    public Integer id;
    private Map<Integer, Double> neighbors;
    private Map<Integer, List<Integer>> ports;
    // ports is of the form (neighbor_id) -> (send_port, listen_port)
    private Map<Integer, Integer> seqCounter;
    private Map<Integer, String> msgs;
    private Boolean stop_listening;
    private Integer curr_listening;


    // TODO
    // each node should have a Routing table, that has an entry for all the vertices

    public static <K, V> void printMap(Map<K, V> map) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }
    public Node(String line, Integer num_of_nodes){
        this.ports = new HashMap<>();
        this.neighbors = new HashMap<>();
        this.seqCounter = new HashMap<>();
        this.msgs = new HashMap<>();
        this.num_of_nodes = num_of_nodes;
        this.stop_listening = false;
        this.curr_listening = 0;
        parseLine(line);
        receiveMessages();
    }

    @Override
    public synchronized void start() {
        assert false;
        super.start();
        send();

    }
    public int num_msgs(){
        return this.msgs.size();
    }
    private void send(){
        String msg = "from" + this.id;
        handleMessage(this.id+"/"+msg);
    }
    public void read_msgs(){
        assert !is_listening();
        System.out.println(this.id + " " + (this.msgs.size() == this.num_of_nodes));
    }

    public void update_weight(Integer neighbor, Double new_weight){
        this.neighbors.put(neighbor, new_weight);
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

    private void flood(String msg){
        for (Integer neighbor: this.neighbors.keySet()){
            // send msg
            sendMessage(msg, neighbor);
        }
    }

    private synchronized void handleMessage(String msg){
        // msg is of the form (msg, source)
        String[] parts = msg.split("/");
        Integer source = Integer.parseInt(parts[0]);
        String org_msg = parts[1];
        if (!this.msgs.containsKey(source)){
            this.msgs.put(source, org_msg);
            flood(msg);
        }
    }
    private void sendMessage(String msg, Integer receiver){
        int send_port = this.ports.get(receiver).get(0);
        assert is_listening();
        //System.out.println("Node "+this.id+" is sending on port " + send_port + " to " + receiver + msg);
        while (true)
            try {
                Socket client = new Socket(InetAddress.getByName("localhost"), send_port);
                OutputStream out = client.getOutputStream();
                out.write(msg.getBytes());
                client.close();
                break;
            }
            catch (ConnectException e) {
                System.out.println("Problem with host/port or host is busy");
            }
            catch (IOException e){
                e.printStackTrace();
            }
    }
    public Boolean is_listening(){
        return this.curr_listening == this.neighbors.size();
    }
    private synchronized void incrementListening(){
        this.curr_listening++;
    }
    private synchronized void decrementListening(){
        this.curr_listening--;
    }
    public void stop_receiving(){
        this.stop_listening = true;
    }

    public void receiveMessages(){
        for (Integer neighbor: this.neighbors.keySet()){
            int listen_port = this.ports.get(neighbor).get(1);
                Thread thread = new Thread(() -> {
                    try {
                        //System.out.println("Node "+this.id+" is listening on port " + listen_port);
                        ServerSocket serverSocket = new ServerSocket(listen_port);
                        serverSocket.setSoTimeout(5000);
                        while (!this.stop_listening){
                            try {
                                incrementListening();
                                Socket socket = serverSocket.accept();
                                InputStream in = socket.getInputStream();
                                byte[] message = new byte[1024];
                                in.read(message);
                                String msg = new String(message);
                                //System.out.println(this.id + " got "+ msg.trim());
                                handleMessage(msg.trim());
                                socket.close();
                            } catch (SocketTimeoutException e){
                                decrementListening();
                            }
                        }
                        serverSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                thread.start();
            }
    }

}