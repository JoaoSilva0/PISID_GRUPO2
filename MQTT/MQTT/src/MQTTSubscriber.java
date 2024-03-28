import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MQTTSubscriber {

    private static WriteMySql writeMySql;

    public MQTTSubscriber(WriteMySql writeMySql) {
        this.writeMySql = writeMySql;
    }

    public static void main(String[] args) {
        MQTTSubscriber mqttSubscriber = new MQTTSubscriber(new WriteMySql());
        String broker = "tcp://broker.mqtt-dashboard.com:1883";
        String clientId = "JavaMQTTSubscriber";
        MemoryPersistence persistence = new MemoryPersistence();
        try {
            writeMySql.connectDatabase_to();
           // WriteMySql.writeConfiguration();
            MqttClient mqttClient = new MqttClient(broker, clientId, persistence);
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.out.println("Conexão perdida com o broker MQTT." + cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println("Mensagem recebida:");
                    System.out.println("   Tópico: " + topic);
                    System.out.println("   Mensagem: " + new String(message.getPayload()));
                    writeMySql.WriteToMySQL(new String(message.getPayload()), topic);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });

            mqttClient.connect();
            System.out.println("Conectado ao broker MQTT.");

            // Inscreva-se em tópicos de interesse
            String[] topics = {"pisid_grupo2_temperatura", "pisid_grupo2_passagem"};
            int[] qos = {1, 1}; // QoS (Quality of Service) 1
            mqttClient.subscribe(topics, qos);
            System.out.println("Inscrito nos tópicos MQTT.");

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}
