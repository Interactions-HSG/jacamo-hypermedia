package yggdrasil;

import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.impl.TupleQueryResultBuilder;
import org.eclipse.rdf4j.query.resultio.QueryResultParser;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLParser;

import cartago.LINK;
import wot.ThingArtifact;

/**
 * This class is similar to {@link yggdrasil.ContainerArtifact}, but uses RDFSub instead of WebSub
 * to receive notifications of changes in membership triples.
 * 
 * A hypermedia artifact that can contain other artifacts. The containment relation is given by
 * {@code eve:contains}. Contained artifacts are exposed as observable properties using by default
 * the Jason functor "member" or one that is passed as an argument during artifact initialization.
 * 
 * Contributors:
 * - Andrei Ciortea (author), Interactions-HSG, University of St. Gallen
 *
 */
public class RDFContainerArtifact extends ThingArtifact {
  private String containmentFunctor;
  
  @Override
  public void init(String url) {
    init(url, "member");
  }
  
  public void init(String url, String containmentProp, boolean dryRun) {
    init(url, containmentProp);
    this.dryRun = dryRun;
  }
  
  public void init(String url, String containmentProp) {
    super.init(url);
    
    this.containmentFunctor = containmentProp;
  }
  
  @LINK
  @Override
  public void onNotification(Notification notification) {
    try {
      TupleQueryResultBuilder builder = new TupleQueryResultBuilder();
      
      QueryResultParser parser = new SPARQLResultsXMLParser();
      parser.setQueryResultHandler(builder);
      parser.parseQueryResult(IOUtils.toInputStream(notification.getMessage(),
          Charset.defaultCharset()));
      
      TupleQueryResult result = builder.getQueryResult();
      
      for (BindingSet solution : result) {
        
        String memberIRI = solution.getValue("member").stringValue();
        String memberName = solution.getValue("memberName").stringValue();
        
        if (getObsPropertyByTemplate(containmentFunctor, memberIRI, memberName) == null) {
          // TODO: expose semantic types as well?
          this.defineObsProperty(containmentFunctor, memberIRI, memberName);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
