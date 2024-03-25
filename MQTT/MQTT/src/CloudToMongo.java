import org.bson.Document;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.util.JSON;

import javax.print.Doc;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Random;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CloudToMongo implements MqttCallback {
    private MqttClient mqttClient;
    private static MongoClient mongoClient;
    private static MongoDatabase db;
    private static JTextArea documentLabel = new JTextArea("\n");
    private static  DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String CONFIG_FILE = "CloudToMongo.ini";

    //IMPORTANTE: na implementeação final, arredondar os dados
    public static void main(String[] args) {
        createWindow();
        try {
            Properties p = loadConfig();
            new CloudToMongo().connectMongo(p); // Connect to MongoDB first
            String cloudTopic = p.getProperty("cloud_topic");
            String[] topics = cloudTopic.split(",");
            for (String topic : topics) {
                new CloudToMongo().connectCloud(topic.trim());
            }
        } catch (IOException e) {
            System.out.println("Error reading configuration file: " + e.getMessage());
            JOptionPane.showMessageDialog(null, "Error reading the configuration file.", "CloudToMongo", JOptionPane.ERROR_MESSAGE);
        }
    }


    private static Properties loadConfig() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
            properties.load(fis);
        }
        return properties;
    }

    private static void createWindow() {
        JFrame frame = new JFrame("Cloud to Mongo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JLabel textLabel = new JLabel("Data from broker:", SwingConstants.CENTER);
        textLabel.setPreferredSize(new Dimension(600, 30));
        JScrollPane scroll = new JScrollPane(documentLabel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scroll.setPreferredSize(new Dimension(600, 200));
        JButton b1 = new JButton("Stop the program");
        frame.getContentPane().add(textLabel, BorderLayout.PAGE_START);
        frame.getContentPane().add(scroll, BorderLayout.CENTER);
        frame.getContentPane().add(b1, BorderLayout.PAGE_END);
        frame.setLocationRelativeTo(null);
        frame.pack();
        frame.setVisible(true);
        b1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                System.exit(0);
            }
        });
    }

    public void connectCloud(String cloudTopic) {
        try {
            int i = new Random().nextInt(100000);
            mqttClient = new MqttClient("tcp://broker.mqtt-dashboard.com:1883", "CloudToMongo_" + String.valueOf(i) + "_" + cloudTopic);
            mqttClient.connect();
            mqttClient.setCallback(this);
            mqttClient.subscribe(cloudTopic);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void connectMongo(Properties p) {
        String mongoURI = "mongodb://";
        String mongoAddress = p.getProperty("mongo_address");
        String mongoUser = p.getProperty("mongo_user");
        String mongoPassword = p.getProperty("mongo_password");
        String mongoReplica = p.getProperty("mongo_replica");
        String mongoAuthentication = p.getProperty("mongo_authentication");
        String mongoDatabase = p.getProperty("mongo_database");
        String mongoCollection = p.getProperty("mongo_collection");

        mongoURI += mongoAuthentication.equals("true") ? (mongoUser + ":" + mongoPassword + "@") : "";
        mongoURI += mongoAddress;
        mongoURI += mongoReplica.equals("false") ? "" : ("/?replicaSet=" + mongoReplica);
        mongoURI += mongoAuthentication.equals("true") ? "/?authSource=admin" : "";

        MongoClient mongoClient = new MongoClient(new MongoClientURI(mongoURI));
        db = mongoClient.getDatabase(mongoDatabase);
        MongoCollection<Document> bookmarks = db.getCollection("bookmarksObjectID");
        bookmarks.drop();
        Document bookmarkTemperatura = new Document("collectionName", "medicoesTemperatura").append("lastProcessedId", null);
        bookmarks.insertOne(bookmarkTemperatura);
        Document bookmarkPassagem = new Document("collectionName", "medicoesPassagem").append("lastProcessedId", null);
        bookmarks.insertOne(bookmarkPassagem);
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        MongoCollection<Document> collection;
        Document document_json; 
        String payloadAsString = new String(message.getPayload());
        if (topic.equals("pisid_mazemov")) {
            collection = db.getCollection("medicoesPassagem");
        } else if (topic.equals("pisid_mazetemp")) {
            collection = db.getCollection("medicoesTemperatura");
        } else {
            System.out.println("Unknown topic: " + topic);
            return;
        }

        if(message.toString().contains("@") 
        ||message.toString().contains("&")
        ||message.toString().contains("#") 
        ||message.toString().contains("!") 
        ||message.toString().contains("^")
        ||message.toString().contains("(")
        ||message.toString().contains(")")
        ||message.toString().contains("_")
        ||message.toString().contains("+")
        ||message.toString().contains("=")
        ||message.toString().contains("?")
        ||message.toString().contains("%")
        ||message.toString().contains("*")) { 
    
            String[] mensagem = payloadAsString.split(",");
            String mensagemAnomala = mensagem[0]+ ", Leitura: null ," + mensagem[2];
            String nullMessage = payloadAsString.replace(payloadAsString, mensagemAnomala);
            document_json = Document.parse(nullMessage);

        } else if (!message.toString().contains(LocalDate.now().toString())) {
            
            String[] split_time = new String(message.getPayload()).split(",");
            String[] split_time2 = new String(message.getPayload()).split(" "); 
            System.out.println(split_time2[1]+split_time[1]);
            
            LocalDateTime myDateObj = LocalDateTime.now();
            String formattedDate = myDateObj.format(myFormatObj);
            String date_error = payloadAsString.replace(split_time2[1], "'"+formattedDate.toString());
            String date_error2 = date_error.replace(split_time2[2],"',");
            System.out.println(date_error2);
            document_json = Document.parse(date_error2);

        } else {
            document_json = Document.parse(payloadAsString);
        }
        collection.insertOne(document_json);
        documentLabel.append(message.toString() + "\n");
    }

    @Override
    public void connectionLost(Throwable cause) {
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }

    public void SendToMQTT(String message,String broker,String clientId){
       
    }
}
