package ch.unisg.ics.interactions.jacamo.artifacts.wot;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.hc.core5.http.ParseException;
import org.junit.Before;
import org.junit.Test;

import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.schemas.ArraySchema;
import ch.unisg.ics.interactions.wot.td.schemas.BooleanSchema;
import ch.unisg.ics.interactions.wot.td.schemas.IntegerSchema;
import ch.unisg.ics.interactions.wot.td.schemas.NumberSchema;
import ch.unisg.ics.interactions.wot.td.schemas.ObjectSchema;
import ch.unisg.ics.interactions.wot.td.schemas.StringSchema;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;

public class ThingArtifactTest {
  private static final String PREFIX = "http://example.org/";
  private ThingArtifact artifact;
  private TDHttpRequest request;

  @Before
  public void init() {
    artifact = new ThingArtifact();

    request = new TDHttpRequest(new Form.Builder("http://example.org")
          .addOperationType(TD.invokeAction)
          .build(),
        TD.invokeAction);
  }

  @Test
  public void testBooleanPayload() throws ParseException, IOException {
    request = artifact.setPrimitivePayload(request, new BooleanSchema.Builder().build(), true);
    String payload = request.getPayloadAsString();
    assertTrue(new Gson().fromJson(payload, Boolean.class).booleanValue());
  }

  @Test
  public void testStringPayload() throws ParseException, IOException {
    request = artifact.setPrimitivePayload(request, new StringSchema.Builder().build(), "blabla");
    String payload = request.getPayloadAsString();
    assertEquals("blabla", new Gson().fromJson(payload, String.class));
  }

  @Test
  public void testIntegerPayload() throws ParseException, IOException {
    request = artifact.setPrimitivePayload(request, new IntegerSchema.Builder().build(), (Integer) 42);
    String payload = request.getPayloadAsString();
    assertEquals((Integer) 42, new Gson().fromJson(payload, Integer.class));
  }

  @Test
  public void testNumberPayload() throws ParseException, IOException {
    request = artifact.setPrimitivePayload(request, new NumberSchema.Builder().build(), 0.05);
    String payload = request.getPayloadAsString();
    assertEquals(0.05, new Gson().fromJson(payload, Double.class), 0.001);
  }

  @Test
  public void testObjectPayload() throws ParseException, IOException {
    Object[] tags = new Object[] { PREFIX + "FirstName", PREFIX + "LastName", PREFIX + "Age" };
    Object[] params = new Object[] { "John", "Doe", 42 };

    request = artifact.setObjectPayload(request, new ObjectSchema.Builder()
        .addProperty("first_name", new StringSchema.Builder()
            .addSemanticType(PREFIX + "FirstName")
            .build())
        .addProperty("last_name", new StringSchema.Builder()
            .addSemanticType(PREFIX + "LastName")
            .build())
        .addProperty("age", new IntegerSchema.Builder()
            .addSemanticType(PREFIX + "Age")
            .build())
        .build(), tags, params);

    String payload = request.getPayloadAsString();
    JsonObject object = JsonParser.parseString(payload).getAsJsonObject();

    assertEquals("John", object.get("first_name").getAsString());
    assertEquals("Doe", object.get("last_name").getAsString());
    assertEquals(42, object.get("age").getAsInt());
  }

  @Test
  public void testArrayPayload() throws ParseException, IOException {
    Object[] params = new Object[] { "John", "Doe", 42, true };

    request = artifact.setArrayPayload(request, new ArraySchema.Builder().build(), params);

    String payload = request.getPayloadAsString();
    JsonArray array = JsonParser.parseString(payload).getAsJsonArray();

    assertEquals("John", array.get(0).getAsString());
    assertEquals("Doe", array.get(1).getAsString());
    assertEquals(42, array.get(2).getAsInt());
  }

  @Test
  public void testSimpleListToArray() {
    List<Object> list = new ArrayList<Object>();
    list.add("bla");
    list.add(false);
    list.add(2.5);
    list.add(12);

    Object[] array = artifact.nestedListsToArrays(list);
    assertEquals("bla", array[0]);
    assertFalse((boolean) array[1]);
    assertEquals(2.5, (double) array[2], 0.01);
    assertEquals(12, array[3]);
  }

  @Test
  public void testNestedListToArray() {
    List<Object> level2 = new ArrayList<Object>();
    level2.add("1");
    level2.add(2);

    List<Object> level1 = new ArrayList<Object>();
    level1.add(3.5);
    level1.add(level2);

    Object[] array = artifact.nestedListsToArrays(level1);
    assertEquals(3.5, (double) array[0], 0.01);
    assertTrue(array[1] instanceof Object[]);
    assertEquals("1", ((Object[]) array[1])[0]);
    assertEquals(2, ((Object[]) array[1])[1]);
  }
}
