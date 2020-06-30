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

public class EnvironmentArtifact extends ThingArtifact {
  private IRI envIRI;
  private Model graph;
  private ValueFactory rdf;
  
  private List<String> workspaces;
  
  @Override
  public void init(String url) {
    super.init(url);
    
    this.rdf = SimpleValueFactory.getInstance();
    this.workspaces = new ArrayList<String>();
    
    if (td.getThingURI().isPresent() && td.getGraph().isPresent()) {
      this.envIRI = rdf.createIRI(td.getThingURI().get());
      this.graph = td.getGraph().get();
    } else {
      failed("Could not read RDF graph for environment: " + url);
    }
  }
  
  @OPERATION
  public void getWorkspaceIRIs(OpFeedbackParam<String[]> workspaceIRIs) {    
    workspaces = getMembers(graph, envIRI);
    
    String[] workspaceIRIArray = new String[workspaces.size()];
    workspaceIRIs.set((String[]) workspaces.toArray(workspaceIRIArray));
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
  
  private List<String> getMembers(Model model, Resource subject) {
    return Models.objectIRIs(model.filter(subject, EVE.contains, null))
        .stream().map(iri -> iri.stringValue()).collect(Collectors.toList());
  }
  
}
