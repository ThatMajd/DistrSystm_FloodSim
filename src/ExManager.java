import java.net.SocketException;
import java.util.*;
import java.io.*;
import java.util.*;
public class ExManager {
    private String path;
    private Integer num_of_nodes;
    private List<Node> nodes;
    // DELETE THIS
    // public static int num_threads = 0;


    public ExManager(String path){
        this.path = path;
        this.nodes = new ArrayList<Node>();
    }

    public Node get_node(int id) {
        for (Node node : nodes) {
            if (node.id == id) {
                return node;
            }
        }
        return null;
    }
    public int getNum_of_nodes() {
        /*
        @returns number of nodes in graph
         */
        return this.num_of_nodes;
    }

    public void update_edge(int id1, int id2, double weight){
        /*
        Symmetrically updates the edge's weight
         */
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

    public void read_txt() throws FileNotFoundException {
        /*
        Start building the graph from the path provided
         */
        File file = new File(this.path);
        Scanner scanner = new Scanner(file);

        // reading the first line of the file which is the number of nodes
        String line = scanner.nextLine();
        String[] line_items = line.split(" ");
        this.num_of_nodes = Integer.parseInt(line_items[0]);

        // reading the rest of the file until 'stop' and initiating the nodes
        while (scanner.hasNextLine()) {
            line = scanner.nextLine();
            if (line.contains("stop")) {
                break;
            }

            line_items = line.split(" ");
            //System.out.println(line);
            Integer id = Integer.parseInt(line_items[0]);
            Node node = new Node(line, this.num_of_nodes);
            nodes.add(node);
        }
    }

    public void terminate(){
        /*
        Safely kills all Sockets
         */
        for (Node node: this.nodes){
            node.end();
        }
    }

    public void start() {
        // Initiate Link State Routing
        for (Node node: this.nodes){
            node.init();
        }
        for (Node node: this.nodes){
            // wait until node is listening
            while (!node.is_listening()){}
            //System.out.println(node.id + "listening");
        }
        for (Node node: this.nodes){
            node.run();
        }

        for (Node node: this.nodes){
            while(node.num_msgs() != num_of_nodes){
                System.out.print("");
            }
            System.out.print("");
        }

        for (Node node: this.nodes){
            try{node.join();}
            catch (InterruptedException e){
                e.printStackTrace();
            }
        }

    }
}