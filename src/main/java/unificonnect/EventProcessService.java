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
import org.springframework.beans.factory.annotation.Autowired;
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
    private final String endpointDetectatron = System.getenv("ENDPOINT_DETECTATRON");

    private static final Logger logger = Logger.getLogger("EventProcessService");

    @Autowired
    S3UploadService myS3UploadService;

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
            int responsecode = response.getStatusLine().getStatusCode();

            if (responsecode == 200) {
                HttpEntity entity = response.getEntity();

                InputStream videoDownload = entity.getContent();

                videoContents = IOUtils.toByteArray(videoDownload);
            } else {
                logger.warning("Unexpected response code from Unifi API: "+ responsecode);
                throw new RuntimeException("Unable to download video from API");
            }

        } catch (java.io.IOException e) {
            e.printStackTrace();
            logger.warning("An unexpected issue occurred whilst attempting to download the video from Unifi API.");
            throw new RuntimeException("Unable to download video from API");
        }

        return videoContents;
    }


    /**
     * Download the specified recording event from UniFi Video & Upload to S3.
     *
     * @param eventId
     * @throws InterruptedException
     */
    @Async
    public void uploadAsync(String eventId) throws InterruptedException
    {
        logger.info("Processing event: " + eventId);
        myS3UploadService.uploader(eventId + ".mp4", downloadVideo(eventId), "");
    }
}
