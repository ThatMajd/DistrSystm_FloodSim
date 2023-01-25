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
    private Map<Integer, Socket> sendingSockets;
    public Map<Integer, ServerSocket> receivingSockets;
    private Map<Integer, Socket> clientSockets;
    private List<Integer> curr_listening;

    public ExManager manager;


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
        this.curr_listening = new ArrayList<>();
        this.sendingSockets = new HashMap<>();
        this.receivingSockets = new HashMap<>();
        this.clientSockets = new HashMap<>();


        parseLine(line);
        receiveMessages();
    }

    public void print_graph(){

        float[][] adjacency_table = new float[num_of_nodes][num_of_nodes];
        for (int i=0; i<num_of_nodes;i++){
            for (int j=0; j<num_of_nodes;j++){
                adjacency_table[i][j] = -1;
            }
        }
        for (int j = 0; j < this.num_of_nodes; j++) {

            String j_msgs = this.msgs.get(j+1);

            String[] parts = j_msgs.split(",");

            String[] neighbor_and_val;
            for (int k = 0; k < parts.length; k++){
                parts[k] = parts[k].replace("{","");
                parts[k] = parts[k].replace("}","");
                parts[k] = parts[k].replace(" ","");
                neighbor_and_val = parts[k].split(",");
                for (int l = 0; l < neighbor_and_val.length; l++){
                    String[] neighbor_and_val_parts = neighbor_and_val[l].split("=");
                    int neighbor = Integer.parseInt(neighbor_and_val_parts[0]);
                    float value = Float.parseFloat(neighbor_and_val_parts[1]);
                    adjacency_table[neighbor-1][j] = value;
                }
            }
        }
        print_adjacency_table(adjacency_table);

    }

    public void print_adjacency_table(float[][] adjacency_table){
        for (int i=0; i<num_of_nodes;i++){
            for (int j=0; j<num_of_nodes;j++){
                System.out.print(adjacency_table[i][j]);
                if (j != num_of_nodes-1){
                    System.out.print(", ");
                }
            }
            System.out.println("");
        }
    }
    public void init(){
        this.stop_listening = false;
        this.msgs = new HashMap<>();
    }

    public int get_num_neighs(){
        return this.neighbors.size();
    }

    @Override
    public void run() {
        assert false;
        super.run();
        send();
    }
    public int num_msgs(){
        return this.msgs.size();
    }
    private void send(){
        String msg = this.neighbors.toString();//"from" + this.id;
        handleMessage(this.id+"/"+msg+"@");
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


    private synchronized void handleMessage(String msg){
        // msg is of the form (msg, source)
        if (msg.equals("")){
            return;
        }
        for (String s: msg.split("@")) {
            String[] parts = s.split("/");
            Integer source = Integer.parseInt(parts[0]);
            String org_msg = parts[1];
            //System.out.println(this.id + "-" + s + " and sending it to " + this.neighbors.keySet());
            if (!this.msgs.containsKey(source)) {
                this.msgs.put(source, org_msg);
                for (Integer neighbor : this.neighbors.keySet()) {
                    // send msg
                    sendMessage(s + "@", neighbor);
                }
            }
        }
    }

    private void sendMessage(String msg, Integer receiver){
        if (!this.sendingSockets.containsKey(receiver)){
            try {
                int send_port = this.ports.get(receiver).get(0);
                Socket client = new Socket(InetAddress.getByName("localhost"), send_port);
                this.sendingSockets.put(receiver, client);
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
        while (!stop_listening) {
            try {
                Socket client = this.sendingSockets.get(receiver);
                OutputStream out = client.getOutputStream();
                out.write((msg).getBytes());
                out.flush();
                break;
            } catch (ConnectException e) {
                System.out.println("Problem with host/port or host is busy");
                e.printStackTrace();
                try {
                    sleep(2500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
    public Boolean is_listening(){
        return this.receivingSockets.keySet().size() == this.neighbors.size();
    }

    public void end(){
        this.stop_listening = true;
        for (Socket s: this.sendingSockets.values()){
            try {
                s.getOutputStream().close();
                s.close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
        for (Integer n: this.receivingSockets.keySet()){
            try {
                this.clientSockets.get(n).getInputStream().close();
                this.clientSockets.get(n).close();
                this.receivingSockets.get(n).close();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public synchronized void add_server(Integer neigh, ServerSocket s){
        this.receivingSockets.put(neigh, s);
    }
    public synchronized void add_client(Integer neigh, Socket s){
        this.clientSockets.put(neigh, s);
    }

    public void receiveMessages(){
        for (Integer neighbor: this.neighbors.keySet()){
            Thread thread = new Thread(() -> {
                try {
                    //System.out.println("Node "+this.id+" is listening on port " + listen_port);
                    ServerSocket serverSocket = new ServerSocket(this.ports.get(neighbor).get(1));
                    //serverSocket.setSoTimeout(5000);
                    Socket socket = null;
                    StringBuilder f_msg = new StringBuilder();
                    add_server(neighbor, serverSocket);
                    try {
                        socket = serverSocket.accept();
                        add_client(neighbor, socket);

                    } catch (SocketTimeoutException e){
                        System.out.println("Timed out");
                    }
                    try {
                        while (!stop_listening) {
                            InputStream in = socket.getInputStream();
                            byte[] msg_bits = new byte[4029];
                            in.read(msg_bits);
                            String msg = new String(msg_bits).trim();
                            int i;
                            if (!msg.equals("")) {
                                f_msg.append(msg);
                                i = f_msg.toString().lastIndexOf('@');
                                if (i != -1) {
                                    handleMessage(f_msg.substring(0, i));
                                    f_msg = new StringBuilder(f_msg.substring(i + 1));
                                }
                            }
                        }
                    } catch (SocketException ignored){}

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            thread.start();
        }
    }

}