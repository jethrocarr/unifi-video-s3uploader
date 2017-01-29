package unificonnect;


import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.BSON;
import org.bson.BasicBSONObject;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.xml.bind.ValidationException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gt;



/**
 * Listens to events from the Unifi Video platform. This implementation is a right hack, basically we poll every 1
 * second for any events since our last attempt. Ideally we'd have a subscription based approach rather than this ugly
 * polling, but it's what we have to play with currently with MongoDB not having triggers.
 *
 * One alternative solution might be to use the oplog and check for updates from that, but it's possibly a curse worse
 * than the disease...
 *
 * We also need much better hardening around this code, so that unexpected issues (eg connection dropping to MongoDB)
 * are handled as well as possible.
 */

@Component
public class EventListener implements CommandLineRunner {

    private static final Logger logger = Logger.getLogger("EventListener");

    @Autowired
    private EventProcessService myEventProcessService;

    @Override
    public void run (String... args) throws Exception {

        try {
            // Connect to the MongoDB instance that has been setup by Unifi Video
            // TODO: Warning: hard coded MongoDB settings
            logger.log(Level.INFO, "Connecting to MongoDB...");

            MongoClient mongoClient = new MongoClient("localhost", 7441);
            MongoDatabase avDatabase = mongoClient.getDatabase("av");
            MongoCollection<Document> eventCollection = avDatabase.getCollection("event");

            // Set the oldest record to find. We use the startup time of this application
            Date searchTime = new Date();
            long timestamp = searchTime.getTime(); // note: intentionally using milliseconds.

            while (true) {
                logger.log(Level.INFO, "Searching for new events from: " + String.valueOf(timestamp));

                // Select all records since the timestamp (if any)
                //db.getCollection('event').find({ inProgress: false, startTime: { $gt: 1485669687649 } }).sort( { startTime: 1} )

                MongoCursor<Document> cursor = eventCollection.find(
                        and(
                                eq("inProgress", false),
                                gt("startTime", timestamp))
                )
                        .sort(
                                new BasicDBObject("startTime", 1)
                        ).iterator();
                try {
                    while (cursor.hasNext()) {

                        Document currentEvent = cursor.next();
                        System.out.println(currentEvent.toJson());

                        // Send to the async processing job to run in the background.
                        myEventProcessService.uploadAsync(currentEvent.getObjectId("_id").toString());

                        // Set the timestamp to the last processed record.
                        timestamp = currentEvent.getLong("startTime");
                    }

                } finally {
                    cursor.close();
                }

                // Let's not loop too quickly.
                Thread.sleep(1000);
            }

        } catch (java.lang.InterruptedException e) {
            e.printStackTrace();
            throw new ValidationException("Process terminated unexpectedly");
        }

    }

}
