import java.util.*;
import java.io.*;
public class ExManager {
    private String path;
    private Integer num_of_nodes;
    private List<Node> nodes;


    public ExManager(String path){
        this.path = path;
        this.nodes = new ArrayList<Node>();
    }

    public Node getNode(int id) {
        for (Node node : nodes) {
            if (node.id == id) {
                return node;
            }
        }
        return null;
    }
    public int getNum_of_nodes() {
        return this.num_of_nodes;
    }

    public void update_edge(int id1, int id2, double weight){
        Node from = null;
        Node to = null;
        for (Node node : nodes) {
            if (node.id == id1) {
                from = node;
            }
            if (node.id == id2) {
                to = node;
            }
        }
        assert from != null;
        assert id1 != id2;
        assert to != null;
        from.update_weight(to.id, weight);
        to.update_weight(from.id, weight);
    }

    public void read_txt() throws FileNotFoundException{
        File file = new File(this.path);
        Scanner scanner = new Scanner(file);

        // reading the first line of the file which is the number of nodes
        String line = scanner.nextLine();
        String[] line_items = line.split(" ");
        this.num_of_nodes = Integer.parseInt(line_items[0]);

        // reading the rest of the file until 'stop' and initiating the nodes
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            if (line.contains("stop")){
                break;
            }

            line_items = line.split(" ");
            System.out.println(line);
            Integer id = Integer.parseInt(line_items[0]);
            Node node = new Node(line, this.num_of_nodes);
            nodes.add(node);
        }
        for (Node node : this.nodes) {
            try {
                node.join();
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }
        // DELETE THIS

//        try {
//            for (Node n : this.nodes){
//                n.receiveMessages();
//            }
//        } catch (IOException e){
//            e.printStackTrace();
//        }
//        try{
//            for (Node n : this.nodes){
//                n.send();
//            }
//        } catch (IOException e){
//            e.printStackTrace();
//        }
//
//        for (Node n : this.nodes){
//            n.read_msgs();
//        }
    }

    public void start(){
        // your code here
        for (Node node : this.nodes){
            while (!node.is_listening());
        }
        System.out.println("All nodes are listening");
        for (Node node : this.nodes){
            node.start();
        }
        for (Node node : this.nodes) {
            try {
                node.join();
            } catch (InterruptedException e){
                e.printStackTrace();
            }
        }

        for (Node node : this.nodes){
            node.read_msgs();
        }
    }
}
