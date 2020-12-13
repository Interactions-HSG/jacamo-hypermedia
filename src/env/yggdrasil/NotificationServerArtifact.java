package yggdrasil;

import java.io.IOException;
import java.util.AbstractQueue;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpStatus;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import cartago.Artifact;
import cartago.ArtifactId;
import cartago.INTERNAL_OPERATION;
import cartago.OPERATION;

public class NotificationServerArtifact extends Artifact {
  private final static String WEBSUB_CALLBACK_PATH = "/notifications/websub";
  private final static String RDFSUB_CALLBACK_PATH = "/notifications/rdfsub";
  
  // Maps artifact IRIs to CArtAgO artifact ids
  private Map<String,ArtifactId> artifactRegistry;
  // Maps subscription IRIs to artifact IRIs
  private Map<String,String> subscriptionArtifactRegistry;
  // Queue that holds notifications waiting to be dispatched
  private AbstractQueue<Notification> notifications;
  
  private String callbackUri;
  
  private Server server;
  private boolean httpServerRunning;
  
  public static final int NOTIFICATION_DELIVERY_DELAY = 100;
  
  void init(String host, Integer port) {
    artifactRegistry = new Hashtable<String,ArtifactId>();
    subscriptionArtifactRegistry = new Hashtable<String,String>();
    notifications = new ConcurrentLinkedQueue<Notification>();
    
    StringBuilder callbackBuilder = new StringBuilder("http://").append(host);
    
    if (port != null) {
      callbackBuilder.append(":")
          .append(Integer.valueOf(port));
    }
    
    callbackUri = callbackBuilder.toString();
    
    server = new Server(port);
    
    ContextHandler webSubContext = new ContextHandler(WEBSUB_CALLBACK_PATH);
    webSubContext.setHandler(new WebSubNotificationHandler());
    
    ContextHandler rdfSubContext = new ContextHandler(RDFSUB_CALLBACK_PATH);
    rdfSubContext.setHandler(new RDFSubNotificationHandler());
    
    ContextHandlerCollection contexts = new ContextHandlerCollection();
    contexts.setHandlers(new Handler[] { webSubContext, rdfSubContext });
    server.setHandler(contexts);
  }
  
  @OPERATION
  void registerArtifactForNotifications(String artifactIRI, ArtifactId artifactId, String hubIRI) {
    artifactRegistry.put(artifactIRI, artifactId);
    sendWebSubSubscribeRequest(hubIRI, artifactIRI);
  }
  
  @OPERATION
  void registerEnvironmentForNotifications(String topicIRI, String environmentIRI, 
      ArtifactId artifactId, String hubIRI) {
    artifactRegistry.put(environmentIRI, artifactId);
    subscribeToMemberPattern(hubIRI, topicIRI, environmentIRI, 
        "http://w3id.org/eve#EnvironmentArtifact");
  }
  
  @OPERATION
  void registerWorkspaceForNotifications(String topicIRI, String workspaceIRI, 
      ArtifactId artifactId, String hubIRI) {
    artifactRegistry.put(workspaceIRI, artifactId);
    subscribeToMemberPattern(hubIRI, topicIRI, workspaceIRI, "http://w3id.org/eve#WorkspaceArtifact");
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
  
  private void sendWebSubSubscribeRequest(String hubIRI, String artifactIRI) {
    HttpClient client = new HttpClient();
    try {
      client.start();
      
      ContentResponse response = client.POST(hubIRI)
          .content(new StringContentProvider("{"
              + "\"hub.mode\" : \"subscribe\","
              + "\"hub.topic\" : \"" + artifactIRI + "\","
              + "\"hub.callback\" : \"" + callbackUri + WEBSUB_CALLBACK_PATH + "/\""
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
  
  private void subscribeToMemberPattern(String hubIRI, String topicIRI, String artifactIRI, 
      String artifactClass) {
    HttpClient client = new HttpClient();
    try {
      client.start();
      
      ContentResponse response = client.POST(hubIRI)
          .content(new StringContentProvider("<> a us:Subscription ;\n" + 
              "us:callback <" + callbackUri + RDFSUB_CALLBACK_PATH + "/> ;\n" + 
              "us:trigger <http://localhost:1080/trigger-members> ;\n" + 
              "us:query \"select ?member ?memberName from <" + topicIRI + "> where { "
              + "<" + artifactIRI + "> a <" + artifactClass + "> ; "
              + "<http://w3id.org/eve#contains> ?member . "
              + "?member <http://purl.org/dc/terms/title> ?memberName . "
              + "}\" ."), "text/turtle")
          .send();
      
      if (response.getStatus() == HttpStatus.SC_ACCEPTED) {
        String subscriptionIRI = response.getHeaders().get("Location");
        subscriptionArtifactRegistry.put(subscriptionIRI, artifactIRI);
      } else {
        log("Request failed: " + response.getStatus());
      }
      
      client.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  class RDFSubNotificationHandler extends AbstractHandler {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, 
        HttpServletResponse response) throws IOException, ServletException {
      
      if (baseRequest.getMethod().equals("GET")) {
        response.setStatus(HttpServletResponse.SC_NO_CONTENT);
      } else if (baseRequest.getMethod().equals("POST")) {
        Optional<String> subscriptionIRI = extractLink(baseRequest, "self");
        
        if (!subscriptionIRI.isPresent()) {
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
          String artifactIRI = subscriptionArtifactRegistry.get(subscriptionIRI.get());
          
          if (artifactIRI != null && artifactRegistry.containsKey(artifactIRI)) {
            String payload = request.getReader().lines()
                .collect(Collectors.joining(System.lineSeparator()));
            notifications.add(new Notification(artifactIRI, payload));
            
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
          } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
          }
        }
      } else {
        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
      }
      
      baseRequest.setHandled(true);
    }
  }
  
  class WebSubNotificationHandler extends AbstractHandler {
    
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, 
        HttpServletResponse response) throws IOException, ServletException {

      Optional<String> artifactIRI = extractLink(baseRequest, "self");
      
      if (!artifactIRI.isPresent()) {
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        response.setContentType("text/plain");
        response.getWriter().println("Link headers are missing! See the W3C WebSub Recommendation "
            + "for details.");
      } else {
          /** Note: the following code (commented out) will occasionally throw an 
          IllegalMonitorStateException, hence the need for an intermediary buffer. **/
//        ArtifactId artifactId = artifactRegistry.get(artifactIRI);
//        
//        if (artifactId != null) {
//          String notification = request.getReader().lines()
//                .collect(Collectors.joining(System.lineSeparator()));
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
        
        if (artifactRegistry.containsKey(artifactIRI.get())) {
          String payload = request.getReader().lines()
              .collect(Collectors.joining(System.lineSeparator()));
          
          notifications.add(new Notification(artifactIRI.get(), payload));
          
          response.setStatus(HttpServletResponse.SC_OK);
        } else {
          response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
      }
      
      baseRequest.setHandled(true);
    }
  }
  
  private static Optional<String> extractLink(Request request, String rel) {
    Enumeration<String> linkHeadersEnum = request.getHeaders("Link");
    
    while (linkHeadersEnum.hasMoreElements()) {
      String value = linkHeadersEnum.nextElement();
      
      if (value.endsWith("rel=\""+ rel +"\"")) {
        return Optional.of(value.substring(1, value.indexOf('>')));
      }
    }
    
    return Optional.empty();
  }
}

