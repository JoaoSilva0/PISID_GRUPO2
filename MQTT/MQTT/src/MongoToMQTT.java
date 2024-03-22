import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.eclipse.paho.client.mqttv3.*;
import org.bson.types.ObjectId;

public class MongoToMQTT {
    
    public static ObjectId convertString(String message) {
        String[] splitedMessage = message.trim().split(",");
        String values = splitedMessage[0].split(": ")[2];
        String cleanedString = values.substring(1, values.length()-2);
        ObjectId objectId = new ObjectId(cleanedString);
        return objectId;
        //return objectId.getTimestamp() * 1000;
    }

    public static void main(String[] args) throws InterruptedException {
        long timestampMiliSeconds = 0;
        ObjectId objectId = null;
        // Configuração do MongoDB
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase database = mongoClient.getDatabase("ExperienciaRatos");
        
        // Coleções MongoDB
        MongoCollection<Document> collection1 = database.getCollection("medicoesTemperatura");
        MongoCollection<Document> collection2 = database.getCollection("medicoesPassagem");

        // Configuração do MQTT
        String broker = "tcp://broker.mqtt-dashboard.com:1883";
        String clientId = "JavaMongoToMQTT";
        while(true) {
            try {
                MqttClient mqttClient = new MqttClient(broker, clientId);
                mqttClient.connect();
                Document query = new Document();
                if (objectId != null) {
                    query.append("_id", new Document("$gt", objectId));
                }
                System.out.println(query);
                // Extração e publicação dos dados da coleção 1
                for (Document doc : collection1.find(query)) {
                    String message = doc.toJson();
                    MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                    mqttClient.publish("pisid_grupo2_joaosilva_temperatura", mqttMessage);
                    objectId = convertString(message);
                }
                
                /*                // Extração e publicação dos dados da coleção 2
                for (Document doc : collection2.find(query)) {
                    String message = doc.toJson();
                    MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                    mqttClient.publish("pisid_grupo2_joaosilva_passagem", mqttMessage);
    
                   
                }
                 */

                //mqttClient.disconnect();
                //mongoClient.close();
            } catch (MqttException e) {
                e.printStackTrace();
            }
            Thread.sleep(2000);
        }
        
    }

   
    
}