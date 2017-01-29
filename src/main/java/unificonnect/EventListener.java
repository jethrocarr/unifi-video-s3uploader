package unificonnect;

import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.BasicBSONObject;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens to events from the Unifi Video platform.
 */

@Component
public class EventListener implements CommandLineRunner {

    private static final Logger logger = Logger.getLogger("EventListener");

    @Override
    public void run (String... args) throws Exception {
        // DO STUFF!
        logger.log(Level.INFO, "Doing stuff!");

        //
        MongoClient mongoClient = new MongoClient();

        MongoCollection coll = mongoClient.getDatabase("local").getCollection("oplog.rs");

        DBCursor cur = coll.find().sort(BasicDBObjectBuilder.start("$natural", 1).get()).addOption(Bytes.QUERYOPTION_TAILABLE | Bytes.QUERYOPTION_AWAITDATA);


        // Keep thread alive
        try {
            CountDownLatch latch = new CountDownLatch(1);
            latch.await();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "An error occurred while latch was waiting.", e);
        }
    }
}
