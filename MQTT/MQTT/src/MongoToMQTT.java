import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.eclipse.paho.client.mqttv3.*;
import org.bson.types.ObjectId;

public class MongoToMQTT {
    
    public static void main(String[] args) {
        ObjectId objectId = null;
        long timestampMiliSeconds = 0;
        // Configuração do MongoDB
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase database = mongoClient.getDatabase("ExperienciaRatos");
        
        // Coleções MongoDB
        MongoCollection<Document> collection1 = database.getCollection("medicoesTemperatura");
        MongoCollection<Document> collection2 = database.getCollection("medicoesPassagem");

        // Configuração do MQTT
        String broker = "tcp://broker.mqtt-dashboard.com:1883";
        String clientId = "JavaMongoToMQTT";
        try {
            MqttClient mqttClient = new MqttClient(broker, clientId);
            mqttClient.connect();
            
            Document query = new Document("_id", new Document("$gt", timestampMiliSeconds));
            // Extração e publicação dos dados da coleção 1
            for (Document doc : collection1.find()) {
                String message = doc.toJson();
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                System.out.println(message);
                mqttClient.publish("pisid_grupo2_joaosilva_temperatura", mqttMessage);
         
            }
            
            // Extração e publicação dos dados da coleção 2
            for (Document doc : collection2.find()) {
                String message = doc.toJson();
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                mqttClient.publish("pisid_grupo2_joaosilva_passagem", mqttMessage);

                String[] splitedMessage = message.trim().split(",");
                String values = splitedMessage[0].split(" : ")[2];
                objectId = new ObjectId(values.substring(0, values.length()-2));
                timestampMiliSeconds = objectId.getTimestamp() * 1000;
                System.out.println(message);
            }
            
            mqttClient.disconnect();
            mongoClient.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

   
    
}