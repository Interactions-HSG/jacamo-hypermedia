package wot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.Header;

import cartago.Artifact;
import cartago.LINK;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.affordances.PropertyAffordance;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.security.APIKeySecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import ch.unisg.ics.interactions.wot.td.vocabularies.WoTSec;
import yggdrasil.Notification;

/**
 * A CArtAgO artifact that can interpret a W3C WoT Thing Description (TD) and exposes the affordances 
 * of the described Thing to agents. The artifact uses the hypermedia controls provided in the TD to
 * compose and issue HTTP requests for the exposed affordances.
 * 
 * Contributors:
 * - Andrei Ciortea (author), Interactions-HSG, University of St. Gallen
 *
 */
public class ThingArtifact extends Artifact {
  protected ThingDescription td;
  protected boolean dryRun;
  private Optional<String> apiKey;
  
  /**
   * Method called by CArtAgO to initialize the artifact. The W3C WoT Thing Description (TD) used by
   * this artifact is retrieved and parsed during initialization. 
   * 
   * @param url A URL that dereferences to a W3C WoT Thing Description.
   */
  public void init(String url) {
    try {
     td = TDGraphReader.readFromURL(TDFormat.RDF_TURTLE, url);
     
     for (SecurityScheme scheme : td.getSecuritySchemes()) {
       defineObsProperty("securityScheme", scheme.getSchemaType());
     }
     
     registerForWebSub(url);
    } catch (IOException e) {
      failed(e.getMessage());
    }
    
    this.apiKey = Optional.empty();
    this.dryRun = false;
  }
  
  /**
   * Method called by CArtAgO to initialize the artifact. The W3C WoT Thing Description (TD) used by
   * this artifact is retrieved and parsed during initialization.
   * 
   * @param url A URL that dereferences to a W3C WoT Thing Description.
   * @param dryRun When set to true, the requests are logged, but not executed.
   */
  public void init(String url, boolean dryRun) {
    init(url);
    this.dryRun = dryRun;
  }
  
  /**
   * CArtAgO operation for reading a property of a Thing using a semantic model of the Thing.
   * 
   * @param semanticType An IRI that identifies the property type.
   * @param output The read value. Can be a list of one or more primitives, or a nested list of
   * primitives or arbitrary depth.
   */
  @OPERATION
  public void readProperty(String semanticType, OpFeedbackParam<Object[]> output) {
    readProperty(semanticType, Optional.empty(), output);
  }
  
  /**
   * CArtAgO operation for reading a property of a Thing using a semantic model of the Thing.
   * 
   * @param semanticType An IRI that identifies the property type.
   * @param tags A list of IRIs, used if the property is an object schema.
   * @param output The read value. Can be a list of one or more primitives, or a nested list of
   * primitives or arbitrary depth.
   */
  @OPERATION
  public void readProperty(String semanticType, OpFeedbackParam<Object[]> tags, 
      OpFeedbackParam<Object[]> output) {
    readProperty(semanticType, Optional.of(tags), output);
  }

  /**
   * CArtAgO operation for writing a property of a Thing using a semantic model of the Thing.
   * 
   * @param semanticType An IRI that identifies the property type.
   * @param payload The payload to be issued when writing the property.
   */
  @OPERATION
  public void writeProperty(String semanticType, Object[] payload) {
    writeProperty(semanticType, new Object[0], payload);
  }
  
  /**
   * CArtAgO operation for writing a property of a Thing using a semantic model of the Thing.
   * 
   * @param semanticType An IRI that identifies the property type.
   * @param tags A list of IRIs that identify parameters sent in the payload. Used for object schemas.
   * @param payload The payload to be issued when writing the property.
   */
  @OPERATION
  public void writeProperty(String semanticType, Object[] tags, Object[] payload) {
    validateParameters(tags, payload);
    if (payload.length == 0) {
      failed("The payload used when writing a property cannot be empty.");
    }
    
    PropertyAffordance property = getFirstPropertyOrFail(semanticType);
    Optional<TDHttpResponse> response = executePropertyRequest(property, TD.writeProperty, tags, 
        payload);
    
    if (response.isPresent() && !requestSucceeded(response.get().getStatusCode())) {
      failed("Status code: " + response.get().getStatusCode());
    }
  }
  
  /**
   * CArtAgO operation for invoking an action on a Thing using a semantic model of the Thing.
   * 
   * @param semanticType An IRI that identifies the action type.
   * @param payload The payload to be issued when invoking the action.
   */
  @OPERATION
  public void invokeAction(String semanticType, Object[] payload) {
    invokeAction(semanticType, new Object[0], payload);
  }
  
  /**
   * CArtAgO operation for invoking an action on a Thing using a semantic model of the Thing.
   * 
   * @param semanticType An IRI that identifies the action type.
   * @param tags A list of IRIs that identify parameters sent in the payload. Used for object schemas.
   * @param payload The payload to be issued when invoking the action.
   */
  @OPERATION
  public void invokeAction(String semanticType, Object[] tags, Object[] payload) {
    validateParameters(tags, payload);
    
    Optional<ActionAffordance> action = td.getFirstActionBySemanticType(semanticType);
    
    if (action.isPresent()) {
      Optional<Form> form = action.get().getFirstForm();
      
      if (!form.isPresent()) {
        // Should not happen (an exception will be raised by the TD library first)
        failed("Invalid TD: the invoked action does not have a valid form.");
      }
      
      Optional<DataSchema> inputSchema = action.get().getInputSchema();
      if (!inputSchema.isPresent() && payload.length > 0) {
        failed("This type of action does not take any input: " + semanticType);
      }
      
      Optional<TDHttpResponse> response = executeRequest(TD.invokeAction, form.get(), inputSchema, 
          tags, payload);
      
      if (response.isPresent() && !requestSucceeded(response.get().getStatusCode())) {
        failed("Status code: " + response.get().getStatusCode());
      }
    } else {
      failed("Unknown action: " + semanticType);
    }
  }
  
  /**
   * CArtAgO operation that sets an authentication token (used with APIKeySecurityScheme).
   * 
   * @param token The authentication token.
   */
  @OPERATION
  public void setAPIKey(String token) {
    if (token != null && !token.isEmpty()) {
      this.apiKey = Optional.of(token);
    }
  }
  
  /* Set a primitive payload. */
  TDHttpRequest setPrimitivePayload(TDHttpRequest request, DataSchema schema, Object payload) {
    try {
      if (payload instanceof Boolean) {
        // Matches to TD BooleanSchema
        request.setPrimitivePayload(schema, (boolean) payload);
      } else if (payload instanceof Byte || payload instanceof Integer || payload instanceof Long) {
        // Matches to TD IntegerSchema
        request.setPrimitivePayload(schema, Long.valueOf(String.valueOf(payload)));
      } else if (payload instanceof Float || payload instanceof Double) {
        // Matches to TD NumberSchema
        request.setPrimitivePayload(schema, Double.valueOf(String.valueOf(payload)));
      } else if (payload instanceof String) {
        // Matches to TD StringSchema
        request.setPrimitivePayload(schema, (String) payload);
      } else {
        failed("Unable to detect the primitive datatype of payload: " 
            + payload.getClass().getCanonicalName());
      }
    } catch (IllegalArgumentException e) {
      failed(e.getMessage());
    }
    
    return request;
  }
  
  /* Set a TD ObjectSchema payload */
  TDHttpRequest setObjectPayload(TDHttpRequest request, DataSchema schema, Object[] tags, 
      Object[] payload) {
    Map<String, Object> requestPayload = new HashMap<String, Object>();
    
    for (int i = 0; i < tags.length; i ++) {
      if (tags[i] instanceof String) {
        requestPayload.put((String) tags[i], payload[i]);
      }
    }
    
    request.setObjectPayload((ObjectSchema) schema, requestPayload);
    
    return request;
  }
  
  /* Set a TD ArraySchema payload */
  TDHttpRequest setArrayPayload(TDHttpRequest request, DataSchema schema, Object[] payload) {
    request.setArrayPayload((ArraySchema) schema, Arrays.asList(payload));
    return request;
  }
  
  @LINK
  public void onNotification(Notification notification) {
    log("The state of this ThingArtifact has changed: " + notification.getMessage());
  }
  
  /* Registers for WebSub to an Yggdrasil node. This is not a generic implementation, but one
   * specific to Yggdrasil. */
  private void registerForWebSub(String url) {
    try {
      Header[] headers = Request.get(url).execute().returnResponse().getHeaders("Link");
      
      // This current implementation is specific to Yggdrasil, not a general implementation
      if (headers.length != 2) {
        return;
      }
      
      Optional<String> hub = Optional.empty();
      Optional<String> topic = Optional.empty();
      
      for (Header h : headers) {
        if (h.getValue().endsWith("rel=\"hub\"")) {
          hub = Optional.of(h.getValue().substring(1, h.getValue().indexOf('>')));
        }
        if (h.getValue().endsWith("rel=\"self\"")) {
          topic = Optional.of(h.getValue().substring(1, h.getValue().indexOf('>')));
        }
      }
      
      if (hub.isPresent() && topic.isPresent()) {
        log("Found WebSub links: " + hub.get() + ", " + topic.get());
        defineObsProperty("websub", hub.get(), topic.get());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  /* Matches the entire 2XX class */
  private boolean requestSucceeded(int statusCode) {
    return statusCode >= 200 && statusCode < 300;
  }
  
  
  private void validateParameters(Object[] tags, Object[] payload) {
    if (tags.length > 0 && tags.length != payload.length) {
      failed("Illegal arguments: the lists of tags and action parameters should have equal length.");
    }
  }
  
  private void readProperty(String semanticType, Optional<OpFeedbackParam<Object[]>> tags, 
      OpFeedbackParam<Object[]> output) {
    PropertyAffordance property = getFirstPropertyOrFail(semanticType);
    Optional<TDHttpResponse> response = executePropertyRequest(property, TD.readProperty, 
        new Object[0], new Object[0]);
    
    if (!dryRun) {
      if (!response.isPresent()) {
        failed("Something went wrong with the read property request.");
      }
      
      if (requestSucceeded(response.get().getStatusCode())) {
        readPayloadWithSchema(response.get(), property.getDataSchema(), tags, output);
      } else {
        failed("Status code: " + response.get().getStatusCode());
      }
    }
  }
  
  private PropertyAffordance getFirstPropertyOrFail(String semanticType) {
    Optional<PropertyAffordance> property = td.getFirstPropertyBySemanticType(semanticType);
    
    if (!property.isPresent()) {
      failed("Unknown property: " + semanticType);
    }
    
    return property.get();
  }
  
  // TODO: Reading payloads of type object currently works with 2 limitations:
  // - only the first semantic tag is retrieved for object properties (one that is not a data schema)
  // - we cannot use nested objects with the current ThingArtifact API (needs a more elaborated
  // JaCa - WoT bridge)
  @SuppressWarnings("unchecked")
  private void readPayloadWithSchema(TDHttpResponse response, DataSchema schema, 
      Optional<OpFeedbackParam<Object[]>> tags, OpFeedbackParam<Object[]> output) {
    
    switch (schema.getDatatype()) {
      case DataSchema.BOOLEAN:
        output.set(new Boolean[] { response.getPayloadAsBoolean() });
        break;
      case DataSchema.STRING:
        output.set(new String[] { response.getPayloadAsString() });
        break;
      case DataSchema.INTEGER:
        output.set(new Integer[] { response.getPayloadAsInteger() });
        break;
      case DataSchema.NUMBER:
        output.set(new Double[] { response.getPayloadAsDouble() });
        break;
      case DataSchema.OBJECT:
        // Only consider this case if the invoked CArtAgO operation was for an object payload
        // (i.e., a list of tags is expected).
        if (tags.isPresent()) {
          Map<String, Object> payload = response.getPayloadAsObject((ObjectSchema) schema);
          List<String> tagList = new ArrayList<String>();
          List<Object> data = new ArrayList<Object>();
          
          for (String tag : payload.keySet()) {
            tagList.add(tag);
            Object value = payload.get(tag);
            if (value instanceof Collection<?>) {
              data.add(nestedListsToArrays((Collection<Object>) value));
            } else {
              data.add(value);
            }
          }
          
          tags.get().set(tagList.toArray());
          output.set(data.toArray());
        }
        break;
      case DataSchema.ARRAY:
        List<Object> payload = response.getPayloadAsArray((ArraySchema) schema);
        output.set(nestedListsToArrays(payload));
        break;
      default:
        break;
    }
  }
  
  @SuppressWarnings("unchecked")
  Object[] nestedListsToArrays(Collection<Object> data) {
    Object[] out = data.toArray();
    
    for (int i = 0; i < out.length; i ++) {
      if (out[i] instanceof Collection<?>) {
        out[i] = nestedListsToArrays((Collection<Object>) out[i]);
      }
    }
    
    return out;
  }
  
  private Optional<TDHttpResponse> executePropertyRequest(PropertyAffordance property, 
    String operationType, Object[] tags, Object[] payload) {
    Optional<Form> form = property.getFirstFormForOperationType(operationType);
    
    if (!form.isPresent()) {
      // Should not happen (an exception will be raised by the TD library first)
      failed("Invalid TD: the property does not have a valid form.");
    }
    
    DataSchema schema = property.getDataSchema();
    
    return executeRequest(operationType, form.get(), Optional.of(schema), tags, payload);
  }
  
  private Optional<TDHttpResponse> executeRequest(String operationType, Form form, 
      Optional<DataSchema> schema, Object[] tags, Object[] payload) {
    if (schema.isPresent() && payload.length > 0) {
      // Request with payload
      if (tags.length > 0) {
        return executeRequestObjectPayload(operationType, form, schema.get(), tags, payload);
      } else if (payload.length == 1 && !(payload[0] instanceof Object[])) {
        return executeRequestPrimitivePayload(operationType, form, schema.get(), payload[0]);
      } else if (payload.length >= 1) {
        return executeRequestArrayPayload(operationType, form, schema.get(), payload);
      } else {
        failed("Could not detect the type of payload (primitive, object, or array).");
        return Optional.empty();
      }
    } else {
      // Request without payload
      TDHttpRequest request = new TDHttpRequest(form, operationType);
      return issueRequest(request);
    }
  }
  
  private Optional<TDHttpResponse> executeRequestPrimitivePayload(String operationType, Form form, 
      DataSchema schema, Object payload) {
    TDHttpRequest request = new TDHttpRequest(form, operationType);
    request = setPrimitivePayload(request, schema, payload);
    
    return issueRequest(request);
  }
  
  private Optional<TDHttpResponse> executeRequestObjectPayload(String operationType, Form form, 
      DataSchema schema, Object[] tags, Object[] payload) {
    if (schema.getDatatype() != DataSchema.OBJECT) {
      failed("TD mismatch: illegal arguments, this affordance uses a data schema of type " 
          + schema.getDatatype());
    }
    
    TDHttpRequest request = new TDHttpRequest(form, operationType);
    request = setObjectPayload(request, schema, tags, payload);
    
    return issueRequest(request);
  }
  
  private Optional<TDHttpResponse> executeRequestArrayPayload(String operationType, Form form, 
      DataSchema schema, Object[] payload) {
    if (schema.getDatatype() != DataSchema.ARRAY) {
      failed("TD mismatch: illegal arguments, this affordance uses a data schema of type " 
          + schema.getDatatype());
    }
    
    TDHttpRequest request = new TDHttpRequest(form, operationType);
    request = setArrayPayload(request, schema, payload);
    
    return issueRequest(request);
  }
  
  private Optional<TDHttpResponse> issueRequest(TDHttpRequest request) {
    Optional<SecurityScheme> scheme = td.getSecuritySchemeByType(WoTSec.APIKeySecurityScheme);
    
    if (scheme.isPresent() && apiKey.isPresent()) {
      request.setAPIKey((APIKeySecurityScheme) scheme.get(), apiKey.get());
    }
    
    if (this.dryRun) {
      log(request.toString());
      return Optional.empty();
    } else {
      log(request.toString());
      try {
        return Optional.of(request.execute());
      } catch (IOException e) {
        failed(e.getMessage());
      }
    }
    
    return Optional.empty();
  }
}
