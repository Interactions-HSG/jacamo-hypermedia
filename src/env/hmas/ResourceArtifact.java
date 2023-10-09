package hmas;

import cartago.Artifact;
import cartago.ObsProperty;
import cartago.OPERATION;
import ch.unisg.ics.interactions.hmas.interaction.io.ArtifactProfileGraphReader;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Ability;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.ArtifactProfile;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Context;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.Signifier;
import ch.unisg.ics.interactions.hmas.interaction.vocabularies.SHACL;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ResourceArtifact extends Artifact {

  protected ArtifactProfile profile;
  protected Set<Signifier> signifiers;

  public void init(String location) {

    try {

      // Retrieve the resource profile (here, from a local location)
      String absoluteLocation = getAbsoluteLocation(location);
      this.profile = ArtifactProfileGraphReader.readFromFile(absoluteLocation);

      // Retrieve the exposed signifiers
      this.signifiers = profile.getExposedSignifiers();

      // Handle the exposed signifiers to inform the SRM
      handleExposedSignifiers();

    } catch (IOException | URISyntaxException e) {
      failed(e.getMessage());
    }
  }

  /**
   * CArtAgO operation for invoking an action on a Resource artifact based on its exposed signifiers.
   * For example invokeAction("https://saref.etsi.org/core/ToggleCommand", ["https://saref.etsi.org/core/OnOffState"], [0])
   * @param actionTag Either an IRI that identifies the action type, or the action's name.
   * @param payloadTags A list of IRIs or object property names (used for object schema payloads).
   * @param payload The payload to be issued when invoking the action.
   */
  @OPERATION
  public void invokeAction(String actionTag, Object[] payloadTags, Object[] payload) {
    System.out.println("Invoking action: " + actionTag + " with payload: " + Arrays.toString(payload));
  }


  private void informSRM(Set<String> actionTypes, List<String> recommendedContext, List<String> recommendedAbilities) {

    // Handle the signifier, e.g. to create observable properties of the preferred structure
    // The following is provided only as an initial example, and creates observable properties of the form
    // signifier(https://saref.etsi.org/core/ToggleCommand, ["room(empty)"],
    // ["http://example.org/processes/BinaryStateHandler", "http://example.org/bdi/BDIReasoner"])
    // However, my understanding is that no two observable properties should have the same name. Therefore,
    // the following is not viable.
    for (String actionType : actionTypes) {
      Object[] contextArray = recommendedContext.toArray(new Object[recommendedContext.size()]);
      Object[] abilitiesArray = recommendedAbilities.toArray(new Object[recommendedAbilities.size()]);
      ObsProperty pr = defineObsProperty("signifier", actionType, contextArray, abilitiesArray);
      log("- New observable property: " + Arrays.toString(pr.getValues()));
    }
  }

  private void handleExposedSignifiers() {

    // Iterate over all the signifiers to inform the SRM
    for (Signifier signifier : signifiers) {

      // This is not needed, but here we print the signifier identifier for logging purposes
      if (signifier.getIRIAsString().isPresent()) {
        log("Handling signifier: " + signifier.getIRIAsString().get());
      }

      // Retrieve the action types
      Set<String> actionTypes = signifier.getActionSpecification().getRequiredSemanticTypes();
      log("- Semantic types of the action: " + actionTypes.toString());

      // Retrieve the recommended context
      Set<Context> recommendedContexts = signifier.getRecommendedContexts();

      // Retrieve the recommended abilities
      Set<Ability> recommendedAbilities = signifier.getRecommendedAbilities();

      // If the signifier is designed for BDI agents, then handle the recommended context
      // The if-statement is not necessary, but here we use it for safety
      List<String> recommendedBDIContexts = new ArrayList<>();
      // Check for BDI support
      boolean BDISupport = recommendedAbilities
        .stream()
        .anyMatch(ability -> ability.getSemanticTypes().contains("http://example.org/bdi/BDIReasoner"));
      // Handle the context if it is compatible with BDI agents
      if (BDISupport) {
        recommendedBDIContexts = extractRecommendedBeliefs(recommendedContexts);
      }
      log("- Recommended contexts: " + recommendedBDIContexts);


      // Handle the abilities by retrieve all of their types
      List<String> recommendedAbilityTypes = recommendedAbilities.stream()
        .flatMap(ability -> ability.getSemanticTypes().stream())
        .collect(Collectors.toList());
      log("- Recommended abilities: " + recommendedAbilityTypes);

      // Inform the SRM about the exposed signifiers
      informSRM(actionTypes, recommendedBDIContexts, recommendedAbilityTypes);

    }
  }

  private List<String> extractRecommendedBeliefs(Set<Context> recommendedContexts) {

    // The list that will be filled with recommended contexts for BDI agents
    List<String> recommendedBeliefs = new ArrayList<>();

    // An HMAS Context is defined as a SHACL NodeShape, therefore it will be treated as such
    // When a signifier recommends the BDI Ability, we assume that the context imposes constraints
    // on the beliefs of the agent through the property http://example.org/bdi/hasBelief
    SimpleValueFactory valueFactory = SimpleValueFactory.getInstance();
    IRI hasBeliefProperty = valueFactory.createIRI("http://example.org/bdi/hasBelief");

    for (Context context : recommendedContexts) {

      // Retrieve the RDF model of the SHACL NodeShape
      Model contextModel = context.getModel();

      // Retrieve the belief specifications
      List<Resource> beliefSpecs = contextModel.filter(null, SHACL.PATH, hasBeliefProperty)
        .stream()
        .map(Statement::getSubject)
        .collect(Collectors.toList());

      // For each belief specification, retrieve its content (e.g. "room(empty)") and
      // add it to the list of recommended beliefs
      for (Resource beliefSpec : beliefSpecs) {

        // Retrieve the belief content from the belief specification
        Optional<String> beliefContent = contextModel.filter(beliefSpec, SHACL.HAS_VALUE, null)
          .stream()
          .findFirst()
          .map(statement -> statement.getObject().stringValue());

        // If the belief content is present, add it to the list of recommended beliefs
        if (beliefContent.isPresent()) {
          recommendedBeliefs.add(beliefContent.get());
        }

      }

    }

    // Return the recommended contexts for BDI agents
    return recommendedBeliefs;
  }

  String getAbsoluteLocation(String location) throws URISyntaxException {
    URI profileResource = ResourceArtifact.class.getClassLoader().getResource(location).toURI();
    return Paths.get(profileResource).toFile().getPath();
  }
}
