import java.io.*;
import java.net.*;
import java.util.*;

public class Node extends Thread{

    private Integer num_of_nodes;
    private Map<Integer, List<Integer>> routingTable;
    public Integer id;
    private Map<Integer, Double> neighbors;
    private Map<Integer, List<Integer>> ports;
    // ports is of the form (neighbor_id) -> (send_port, listen_port)
    private Map<Integer, Integer> seqCounter;
    private Map<Integer, String> msgs;
    private List<Integer> dont_send;
    private int msgs_to_send;
    private int curr_listening;


    // TODO
    // each node should have a Routing table, that has an entry for all the vertices

    // TODO:
    // when a node sends a msg = <msg, source, msgs_to_be_sent_by_source>
    // each node has a map containing all the vertices, #msgs_received, #msgs_to_be_sent
    //

    public static <K, V> void printMap(Map<K, V> map) {
        for (Map.Entry<K, V> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }
    public Node(String line, Integer num_of_nodes){
        /*
        Initilizes node as specified, starts listening
         */
        this.ports = new HashMap<>();
        this.neighbors = new HashMap<>();
        this.seqCounter = new HashMap<>();
        this.msgs = new HashMap<>();
        this.dont_send = new ArrayList<>();
        this.msgs_to_send = 0;
        this.curr_listening = 0;
        this.num_of_nodes = num_of_nodes;
        parseLine(line);
        try {
            this.receiveMessages();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void start() {
        super.start();
        // flood
        try {
            send();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public void update_weight(Integer neighbor, Double new_weight){
        this.msgs_to_send++;
        this.neighbors.put(neighbor, new_weight);
    }
    public void send() throws IOException {
        /*
        Testing function to check whether the communication works
         */
        String msg = "from" + this.id;
        flood(this.id, msg, 0);
    }
    public void reset_msgs_to_send(){
        System.out.println("Restting msgs");
        this.msgs_to_send = 0;
    }

    public void handleMsg(Integer source, String msg){
        /*
        TODO:
        This function handles incoming data coming from other nodes in order to construct
        a local copy of the entire graph
         */
        this.msgs.put(source, msg);
    }
    public void read_msgs(){
        while (this.num_of_nodes != this.msgs.size()){}
        for (String msg : this.msgs.values()){
            System.out.println(this.id + "--" + msg);
        }
        System.out.println();
    }
    private void flood(Integer source, String msg, Integer seq) throws IOException {
        seq++; // incrementing the seq number of the msg for a new broadcast
        String final_msg = source+"/"+msg+"/"+seq;
        for (Integer neighbor: this.neighbors.keySet()){
            if (!this.dont_send.contains(neighbor)) {
                // If neighbor isn't in don't send list
                sendMessage(final_msg, neighbor);
            }
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
        // System.out.println("Sending");
        try {
            // TODO:
            // check another terminating condition
            int port = this.ports.get(receiver).get(0);
            // System.out.println(this.id + " sending on " + port);
            Socket socket = new Socket(InetAddress.getByName("localhost"), port);
            OutputStream out = socket.getOutputStream();
            out.write(msg.getBytes());
            socket.close();
        } catch (java.net.SocketException e) {
            System.out.println(this.id + " cant connect to " + receiver + this.ports.get(receiver).get(0));

        }
    }
    public Boolean is_listening(){
        return this.curr_listening == this.neighbors.size();
    }
    private void not_listening(){
        for (Integer node : this.neighbors.keySet()){
            String msg = this.id + "/" + "out";
            try {
                sendMessage(msg, node);
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void receiveMessages() throws IOException{
        /*
        Listens on all the neighbor's listening ports
         */

        for (Integer neighbor: this.neighbors.keySet()){
            Thread thread = new Thread(() -> {
                int port = this.ports.get(neighbor).get(1);
                try {
                    System.out.println(this.id + " listening on " + port);
                    ServerSocket serverSocket = new ServerSocket(port);
                    while (true) {
                        // TODO:
                        // Figure out a way to check if a node is listening
                        this.curr_listening++;
                        Socket socket = serverSocket.accept();
                        InputStream in = socket.getInputStream();
                        byte[] message = new byte[1024];
                        in.read(message);
                        String msg = new String(message);

                        String[] parts = msg.trim().split("/");

                        int source = Integer.parseInt(parts[0]);
                        String orig_msg = parts[1];

                        if (parts.length == 2){
                            this.dont_send.add(source);
                            socket.close();
                            continue;
                        }

                        int seq = Integer.parseInt(parts[2]);

                        if (!this.seqCounter.containsKey(source)){
                            this.seqCounter.put(source, 0);
                        }
                        if (seq > this.seqCounter.get(source)) {
                            this.seqCounter.put(source, seq);

                            flood(source, orig_msg, seq);
                            if (this.msgs.size() == this.num_of_nodes){
                                System.out.println(this.id+" stopped listening on port "+ port);
                                not_listening();
                                socket.close();
                                break;
                            }
                        }

                        socket.close();
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