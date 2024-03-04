package yggdrasil;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

public final class HMAS {
  public static final String PREFIX = "https://purl.org/hmas/";

  public static final IRI contains = createIRI("contains");

  public static IRI createIRI(String fragment) {
    return SimpleValueFactory.getInstance().createIRI(PREFIX + fragment);
  }

  private HMAS() { }
}
