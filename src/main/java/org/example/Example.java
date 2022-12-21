package org.example;

class Example {
    public static void main(String[] args) {
        var kafkaClient = new KafkaClient();
        var generator = UUIDGenerator.builder().listener(i -> kafkaClient.publishMessage("uuid-audit-log",
                i.toString())).build();
        var id1 = generator.generate();
        var id2 = generator.generate();
        assert !id1.equals(id2);
    }

    static class KafkaClient {
        void publishMessage(String topic, String message) {
            System.out.printf("Sent %s to topic %s%n", message, topic);
        }

    }
}
