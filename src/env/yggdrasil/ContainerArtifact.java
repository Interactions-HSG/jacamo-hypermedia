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

import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import wot.ThingArtifact;

public class ContainerArtifact extends ThingArtifact {
  private IRI containerIRI;
  private Model graph;
  private ValueFactory rdf;
  
  private String containmentProp;
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
    this.containmentProp = containmentProp;
    this.members = new ArrayList<String>();
    
    if (td.getThingURI().isPresent() && td.getGraph().isPresent()) {
      this.containerIRI = rdf.createIRI(td.getThingURI().get());
      this.graph = td.getGraph().get();
      
      this.members = getMembers(graph, containerIRI);
      exposeWorkspaceProps();
    } else {
      failed("Could not read RDF graph for environment: " + url);
    }
  }
  
  @OPERATION
  public void getWorkspaceIRIs(OpFeedbackParam<String[]> workspaceIRIs) {    
    members = getMembers(graph, containerIRI);
    
    String[] workspaceIRIArray = new String[members.size()];
    workspaceIRIs.set((String[]) members.toArray(workspaceIRIArray));
  }
  
  @OPERATION
  public void getEntityDetails(String entityIRI, OpFeedbackParam<String> name, 
      OpFeedbackParam<String> webSubHubIRI, OpFeedbackParam<String[]> memberIRIs) {
    
    try {
      ThingDescription td = TDGraphReader.readFromURL(TDFormat.RDF_TURTLE, entityIRI);
      
      name.set(td.getTitle());      
      List<String> artifacts = getMembers(td.getGraph().get(), rdf.createIRI(entityIRI));
      
      String[] artifactIRIArray = new String[artifacts.size()];
      memberIRIs.set((String[]) artifacts.toArray(artifactIRIArray));
    } catch (IOException | NoSuchElementException e) {
      failed(e.getMessage());
    }
  }
  
  private void exposeWorkspaceProps() {
    for (String memberIRI : members) {
      EntityMetadata data = new EntityMetadata(memberIRI);
      
      // TODO: expose semantic types as well?
      this.defineObsProperty(containmentProp, memberIRI, data.workspaceName);
    }
  }
  
  private List<String> getMembers(Model model, Resource subject) {
    return Models.objectIRIs(model.filter(subject, EVE.contains, null))
        .stream().map(iri -> iri.stringValue()).collect(Collectors.toList());
  }
  
  class EntityMetadata {
    String entityIRI;
    String workspaceName;
    List<String> memberIRIs;
    
    EntityMetadata(String iri) {
      this.entityIRI = iri;
      
      try {
        ThingDescription td = TDGraphReader.readFromURL(TDFormat.RDF_TURTLE, entityIRI);
        
        workspaceName = td.getTitle();
        memberIRIs = getMembers(td.getGraph().get(), rdf.createIRI(entityIRI));
      } catch (IOException | NoSuchElementException e) {
        failed(e.getMessage());
      }
    }
  }
  
}
