import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.eclipse.paho.client.mqttv3.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MongoToMQTT {
   
    public static void main(String[] args) throws InterruptedException {
        // MongoDB Configuration
        MongoClient mongoClient = new MongoClient("localhost", 27017);
        MongoDatabase database = mongoClient.getDatabase("ExperienciaRatos");
        
        // MongoDB Collections
        MongoCollection<Document> collection1 = database.getCollection("medicoesTemperatura");
        MongoCollection<Document> collection2 = database.getCollection("medicoesPassagem");
        MongoCollection<Document> bookmarks = database.getCollection("bookmarksObjectID");


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
                executor.submit(() -> extractAndPublish(collection1, bookmarks, mqttClient, "pisid_grupo2_temperatura", "medicoesTemperatura"));
                executor.submit(() -> extractAndPublish(collection2, bookmarks, mqttClient, "pisid_grupo2_passagem", "medicoesPassagem"));
                
                // Wait for all threads to finish execution before moving to the next iteration
                executor.shutdown();
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                
                // Disconnect MQTT client
                mqttClient.disconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
            
            // Delay between iterations
            Thread.sleep(1000);
        }
    }
    
    // Method to extract documents from collection, publish to MQTT, and update bookmarks
    private static void extractAndPublish(MongoCollection<Document> collection, MongoCollection<Document> bookmarks, MqttClient mqttClient, String topic, String collectionName) {
        Document bookmarkQuery = new Document("collectionName", collectionName);
        Document bookmarkDocument = bookmarks.find(bookmarkQuery).first();
        ObjectId lastProcessedId = bookmarkDocument != null ? bookmarkDocument.getObjectId("lastProcessedId") : null;
    
        Document query = new Document();
        if (lastProcessedId != null) {
            query.append("_id", new Document("$gt", lastProcessedId));
        }
        
        for (Document doc : collection.find(query)) {
            String message = doc.toJson();
            System.out.println(message);
            MqttMessage mqttMessage = new MqttMessage(message.getBytes());
            try {
                mqttClient.publish(topic, mqttMessage);
                lastProcessedId = doc.getObjectId("_id");
                Document bookmarkUpdate = new Document("$set", new Document("lastProcessedId", lastProcessedId));
                bookmarks.updateOne(bookmarkQuery, bookmarkUpdate, new UpdateOptions().upsert(true));
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }
}

