package yggdrasil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Models;

import cartago.LINK;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import wot.ThingArtifact;

/**
 * A hypermedia artifact that can contain other artifacts. The containment relation is given by
 * {@code eve:contains}. Contained artifacts are exposed as observable properties using by default
 * the Jason functor "member" or one that is passed as an argument during artifact initialization.
 * 
 * Contributors:
 * - Andrei Ciortea (author), Interactions-HSG, University of St. Gallen
 *
 */
public class ContainerArtifact extends ThingArtifact {
  private IRI containerIRI;
  private Model graph;
  private ValueFactory rdf;
  
  private String containmentFunctor;
  private List<String> members;
  
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
    
    this.rdf = SimpleValueFactory.getInstance();
    this.containmentFunctor = containmentProp;
    this.members = new ArrayList<String>();
    
    exposeWorkspaceProps();
  }
  
  @LINK
  @Override
  public void onNotification(Notification notification) {
    try {
      this.td = TDGraphReader.readFromString(TDFormat.RDF_TURTLE, notification.getMessage());
      exposeWorkspaceProps();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  // TODO: remove / update existing obs props
  private void exposeWorkspaceProps() {
    if (td.getThingURI().isPresent() && td.getGraph().isPresent()) {
      this.containerIRI = rdf.createIRI(td.getThingURI().get());
      this.graph = td.getGraph().get();
      
      this.members = getMembers(graph, containerIRI);
      
      for (String memberIRI : members) {
        MemberMetadata data = new MemberMetadata(memberIRI);
        
        log("checking obs property for: " + memberIRI);
        
        if (getObsPropertyByTemplate(containmentFunctor, memberIRI, data.containerName) == null) {
          log("not found, exposing property");
          // TODO: expose semantic types as well?
          this.defineObsProperty(containmentFunctor, memberIRI, data.containerName);
        }
      }
    } else {
      failed("Could not read RDF graph for container: " + td.getThingURI());
    }
  }
  
  private List<String> getMembers(Model model, Resource subject) {
    return Models.objectIRIs(model.filter(subject, EVE.contains, null))
        .stream().map(iri -> iri.stringValue()).collect(Collectors.toList());
  }
  
  class MemberMetadata {
    String containerIRI;
    String containerName;
    List<String> memberIRIs;
    
    MemberMetadata(String iri) {
      this.containerIRI = iri;
      
      try {
        ThingDescription td = TDGraphReader.readFromURL(TDFormat.RDF_TURTLE, containerIRI);
        
        containerName = td.getTitle();
        memberIRIs = getMembers(td.getGraph().get(), rdf.createIRI(containerIRI));
      } catch (IOException | NoSuchElementException e) {
        failed(e.getMessage());
      }
    }
  }
  
}
