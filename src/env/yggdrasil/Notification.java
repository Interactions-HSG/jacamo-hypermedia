package yggdrasil;

public class Notification {
  private String entityIRI;
  private String message;
  
  public Notification(String entityIRI, String message) {
    this.entityIRI = entityIRI;
    this.message = message;
  }
  
  public String getEntityIRI() {
    return entityIRI;
  }
  
  public String getMessage() {
    return message;
  }
}
