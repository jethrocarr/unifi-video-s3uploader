package unificonnect;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

import com.mongodb.client.result.UpdateResult;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpRequestExecutor;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;


import javax.naming.ConfigurationException;
import javax.xml.bind.ValidationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Process the specified event.
 */
@Service
@EnableAsync
public class EventProcessService {

    private final String apiKey = System.getenv("UNIFI_API_KEY");
    private static final Logger logger = Logger.getLogger("EventProcessService");


    /**
     * Download the video with the specific event ID from the Unifi API
     *
     * @param eventId
     * @return
     */
    public byte[] downloadVideo(String eventId) {

        byte[] videoContents = null;


        try {
            String endpointUrl = "http://localhost:7080/api/2.0/recording/" + eventId + "/download?apiKey=" + apiKey;

            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpGet = new HttpGet(endpointUrl);

            HttpResponse response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();

            InputStream videoDownload = entity.getContent();

            videoContents = IOUtils.toByteArray(videoDownload);

        } catch (java.io.IOException e) {
            e.printStackTrace();
            logger.info("An unexpected issue occurred whilst attempting to download the video from Unifi API.");
        }

        return videoContents;
    }

    /**
     * Lock the recording for the video with the specified ID.
     *
     * This requires making a call to MongoDB.
     * TODO: Currently we open and then close a connection to MongoDB, this is a pretty inefficient way of operating.
     */
    public void lockRecording(String eventId) {
        logger.info("Locking recording: " + eventId);


        MongoClient mongoClient = new MongoClient("localhost", 7441);
        MongoDatabase avDatabase = mongoClient.getDatabase("av");
        MongoCollection<Document> eventCollection = avDatabase.getCollection("event");

        UpdateResult result = eventCollection.updateOne(
                eq("_id", new ObjectId(eventId)),
                set("locked", true)
            );

        /*
         This doesn't work, since Unifi NVR software is still using Mongo 2.4 :'(

        if (result.getModifiedCount() == 1) {
            logger.info("Recording updated.");
        } else {
            logger.log(Level.SEVERE, "Unable to update recording");
        }
        */

        mongoClient.close();
    }


    /**
     * Send the video for the specified event ID up to Detectatron.
     *
     * @param eventId
     * @throws InterruptedException
     */
    @Async
    public void uploadAsync(String eventId) throws InterruptedException
    {
        logger.info("Processing event: " + eventId);


        // Download video and push it up to Detectatron
        try {

            String endpointUrl = "http://detectatron.jethrocarr.com:8080/tag/video";

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(endpointUrl);

            ByteArrayBody uploadFilePart = new ByteArrayBody (downloadVideo(eventId), eventId + ".mp4");

            MultipartEntity reqEntity = new MultipartEntity();
            reqEntity.addPart("file", uploadFilePart);
            httpPost.setEntity(reqEntity);

            HttpResponse response = httpclient.execute(httpPost);

            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode == 200) {
                logger.info("Successful delivery of video");

                // Did we identify keyTags? If so, we should "lock" the recording.

                HttpEntity entity = response.getEntity();
                String detectatronOutput = IOUtils.toString(entity.getContent(), "utf-8");

                logger.log(Level.INFO, "Data received: "+ detectatronOutput);

                Gson gson = new Gson();
                JsonObject jData = new JsonParser().parse(detectatronOutput).getAsJsonObject();
                int numberOfKeyTags = jData.getAsJsonArray("keyTags").size();

                logger.log(Level.INFO, "Found " + numberOfKeyTags + " key tags");


                if (numberOfKeyTags >= 1) {
                    lockRecording(eventId);
                }

            } else {
                logger.log(Level.SEVERE, "Unsuccessful response from Detectatron, HTTP response: " + responseCode);
            }

        } catch (java.io.IOException e) {
            logger.info("An unexpected issue occurred trying to communicate with Detectatron");
        }

    }
}
