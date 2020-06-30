package yggdrasil;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public final class EVE {
  public static final String PREFIX = "http://w3id.org/eve#";
  
  public static final IRI contains = createIRI("contains");
  
  public static IRI createIRI(String fragment) {
    return SimpleValueFactory.getInstance().createIRI(PREFIX + fragment);
  }
  
  private EVE() { }
}
