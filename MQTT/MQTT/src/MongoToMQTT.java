import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.paho.client.mqttv3.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class MongoToMQTT {
    static ObjectId objectId1 = null; // Resetting objectId for each iteration
    static ObjectId objectId2 = null;
    
    public static ObjectId convertString(String message) {
        String[] splitedMessage = message.trim().split(",");
        String values = splitedMessage[0].split(": ")[2];
        String cleanedString = values.substring(1, values.length()-2);
        return new ObjectId(cleanedString);
    }

    public static void main(String[] args) throws InterruptedException {
        // MongoDB Configuration
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase database = mongoClient.getDatabase("ExperienciaRatos");
        
        // MongoDB Collections
        MongoCollection<Document> collection1 = database.getCollection("medicoesTemperatura");
        MongoCollection<Document> collection2 = database.getCollection("medicoesPassagem");

        // MQTT Configuration
        String broker = "tcp://broker.mqtt-dashboard.com:1883";
        String clientId = "JavaMongoToMQTT";

        // Executor service for managing threads
        while(true) {
            try {
                ExecutorService executor = Executors.newFixedThreadPool(2);

                MqttClient mqttClient = new MqttClient(broker, clientId);
                mqttClient.connect();
                
                
                // Submitting tasks to the executor service for concurrent execution
                Future<ObjectId> future1 = executor.submit(() -> extractAndPublish(collection1, objectId1, mqttClient, "pisid_grupo2_joaosilva_temperatura"));
                Future<ObjectId> future2 = executor.submit(() -> extractAndPublish(collection2, objectId2, mqttClient, "pisid_grupo2_joaosilva_passagem"));

                objectId1 = future1.get(); // Get the returned objectId from the future
                objectId2 = future2.get();
                
                // Wait for all threads to finish execution before moving to the next iteration
                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                
                // Disconnect MQTT client
                mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            // Delay between iterations
            Thread.sleep(5000);
        }
    }
    
    // Method to extract documents from collection, publish to MQTT, and update objectId
    private static ObjectId extractAndPublish(MongoCollection<Document> collection, ObjectId objectId, MqttClient mqttClient, String topic) {
        Document query = new Document();
        if (objectId != null) {
            query.append("_id", new Document("$gt", objectId));
            System.out.println(query);
        }
        
        for (Document doc : collection.find(query)) {
            String message = doc.toJson();
            System.out.println(message);
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            try {
                mqttClient.publish(topic, mqttMessage);
            } catch (MqttException e) {
                e.printStackTrace();
            }
            objectId = doc.getObjectId("_id");
        }
        return objectId;
    }
}

