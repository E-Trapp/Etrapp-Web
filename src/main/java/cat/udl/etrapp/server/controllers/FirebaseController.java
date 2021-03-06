package cat.udl.etrapp.server.controllers;

import cat.udl.etrapp.server.daos.UsersDAO;
import cat.udl.etrapp.server.models.EventComment;
import cat.udl.etrapp.server.models.EventMessage;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import org.json.JSONObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FirebaseController {

    private static final String dbUrl = "https://e-trapp.firebaseio.com/";
    private static final String fcmUrl = "https://fcm.googleapis.com/v1/projects/e-trapp/messages:send HTTP/1.1";
    private static FirebaseController instance;

    private FirebaseController() {
    }

    public static synchronized FirebaseController getInstance() {
        if (instance == null) instance = new FirebaseController();
        return instance;
    }

    public void writeMessage(long eventId, EventMessage eventMessage) {
        Client client = ClientBuilder.newClient();
        Map<String, Object> data = eventMessage.asMap();
        Map<String, String> timestamp = new HashMap<>();
        timestamp.put(".sv", "timestamp");
        data.put("timestamp", timestamp);
        try {
            client.target(dbUrl + "eventMessages/event" + eventId + ".json")
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                    .async()
                    .post(Entity.entity(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendNotification(long id) {
        JSONObject container = new JSONObject();
        JSONObject message = new JSONObject();
        JSONObject notification = new JSONObject();
        notification.put("body", "testBody");
        notification.put("title", "testTitle");
        message.put("token", UsersDAO.getInstance().getNotificationTokenById(id));
        message.put("notification", notification);
        container.put("message", message);

        Client client = ClientBuilder.newClient();
        try {
            client.target(fcmUrl)
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                    .async()
                    .post(Entity.entity(message, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getAccessToken() throws IOException {
        GoogleCredential googleCredential = GoogleCredential
                .fromStream(new FileInputStream("e-trapp-firebase-adminsdk-mvgsb-fa9653cc9f.json"))
                .createScoped(
                        Arrays.asList(
                                "https://www.googleapis.com/auth/firebase.messaging",
                                "https://www.googleapis.com/auth/firebase.database",
                                "https://www.googleapis.com/auth/userinfo.email"
                        )
                );
        googleCredential.refreshToken();
        return googleCredential.getAccessToken();
    }

    public void writeComment(long eventId, EventComment eventMessage) {
        Client client = ClientBuilder.newClient();
        Map<String, Object> data = eventMessage.asMap();
        Map<String, String> timestamp = new HashMap<>();
        timestamp.put(".sv", "timestamp");
        data.put("timestamp", timestamp);
        try {
            client.target(dbUrl + "eventComments/event" + eventId + ".json")
                    .request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                    .async()
                    .post(Entity.entity(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
