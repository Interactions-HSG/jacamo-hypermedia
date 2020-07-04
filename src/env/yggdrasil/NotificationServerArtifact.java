package yggdrasil;

import java.io.IOException;
import java.util.AbstractQueue;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.INTERNAL_OPERATION;
import cartago.OPERATION;

public class NotificationServerArtifact extends Artifact {
  private Map<String,ArtifactId> artifactRegistry;
  private AbstractQueue<Notification> notifications;
  
  private Server server;
  private boolean httpServerRunning;
  
  public static final int NOTIFICATION_DELIVERY_DELAY = 100;
  
  void init(int port) {
    server = new Server(port);
    server.setHandler(new NotificationHandler());
    
    artifactRegistry = new Hashtable<String,ArtifactId>();
    notifications = new ConcurrentLinkedQueue<Notification>();
  }
  
  @OPERATION
  void registerArtifactForNotifications(String artifactIRI, ArtifactId artifactId, String hubIRI) {
    artifactRegistry.put(artifactIRI, artifactId);
    sendSubscribeRequest(hubIRI, artifactIRI);
  }
  
  @OPERATION
  void start() {
    try {
      httpServerRunning = true;
      
      execInternalOp("deliverNotifications");
      
      server.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  @OPERATION
  void stop() {
    try {
      server.stop();
      httpServerRunning = false;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  @INTERNAL_OPERATION
  void deliverNotifications() {
    while (httpServerRunning) {
      while (!notifications.isEmpty()) {
        Notification n = notifications.poll();
        ArtifactId artifactId = artifactRegistry.get(n.getEntityIRI());
        
        try {
          
          execLinkedOp(artifactId, "onNotification", n);
          
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      
      await_time(NOTIFICATION_DELIVERY_DELAY);
    }
  }
  
  private void sendSubscribeRequest(String hubIRI, String artifactIRI) {
    HttpClient client = new HttpClient();
    try {
      client.start();
      
      // TODO: construct the callback IRI dynamically
      ContentResponse response = client.POST(hubIRI)
          .content(new StringContentProvider("{"
              + "\"hub.mode\" : \"subscribe\","
              + "\"hub.topic\" : \"" + artifactIRI + "\","
              + "\"hub.callback\" : \"http://localhost:8081/notifications/\""
              + "}"), "application/json")
          .send();
      
      if (response.getStatus() != HttpStatus.SC_OK) {
        log("Request failed: " + response.getStatus());
      }
      
      client.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  class NotificationHandler extends AbstractHandler {
    
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, 
        HttpServletResponse response) throws IOException, ServletException {

      String artifactIRI = null;
      Enumeration<String> linkHeadersEnum = baseRequest.getHeaders("Link");
      
      while (linkHeadersEnum.hasMoreElements()) {
        String value = linkHeadersEnum.nextElement();
        
        if (value.endsWith("rel=\"self\"")) {
          artifactIRI = value.substring(1, value.indexOf('>'));
        }
      }
      
      if (artifactIRI == null) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("text/plain");
        response.getWriter().println("Link headers are missing! See the W3C WebSub Recommendation for details.");
      } else {
          /** Note: the following code (commented out) will occasionally throw an IllegalMonitorStateException, 
          hence the need for an intermediary buffer. **/
//        ArtifactId artifactId = artifactRegistry.get(artifactIRI);
//        
//        if (artifactId != null) {
//          String notification = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
//          
//          try {
//            execLinkedOp(artifactId, "onNotification", new Notification(artifactIRI, notification));
//          } catch (OperationException e) {
//            log(e.getMessage());
//            e.printStackTrace();
//          }
//          
//          response.setStatus(HttpServletResponse.SC_OK);
//        } else {
//          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
//        }
        
        if (artifactRegistry.containsKey(artifactIRI)) {
          String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
          
          notifications.add(new Notification(artifactIRI, payload));
          
          response.setStatus(HttpServletResponse.SC_OK);
        } else {
          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
      }
      
      baseRequest.setHandled(true);
    }
  }
	
}

