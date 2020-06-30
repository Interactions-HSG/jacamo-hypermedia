/* Initial beliefs and rules */

api_key("test-token").
td_url("https://raw.githubusercontent.com/Interactions-HSG/wot-td-java/feature/http-client/samples/forkliftRobot.ttl").

/* Initial goals */

!start.

/* Plans */

+!start : td_url(Url) <- 
  .print("hello world.");
  // To also execute the requests, remove the second init parameter (dryRun flag).
  // When dryRun is set to true, the requests are printed (but not executed).
  makeArtifact("forkliftRobot", "tools.ThingArtifact", [Url, true], ArtId);
  .print("Artifact created!");
  // Write property of boolean type
  .print("Writing property: http://example.org/Status");
  writeProperty("http://example.org/Status", [true])[artifact_name("forkliftRobot")];
  // Read property of boolean type
  .print("Reading boolean property: http://example.org/Status");
  readProperty("http://example.org/Status", StatusValue)[artifact_name("forkliftRobot")];
  .println("Read value (if dry run, then <no-value>): ", StatusValue);
  // Read property of array type
  .print("Reading array property: http://example.org/Position");
  readProperty("http://example.org/Position", PositionValue)[artifact_name("forkliftRobot")];
  .println("Read value (if dry run, then <no-value>): ", PositionValue);
  // Read property of object type
  .print("Reading object property: http://example.org/LastCarry");
  readProperty("http://example.org/LastCarry", LastCarryTags, LastCarryValue)[artifact_name("forkliftRobot")];
  .println("Read value (if dry run, then <no-value>): ", LastCarryTags, ", ", LastCarryValue);
  // Invoke action with tagged nested lists (i.e., ObjectSchema payload)
  .print("Invoking action with object schema payload: http://example.org/CarryFromTo");
  invokeAction("http://example.org/CarryFromTo", 
    ["http://example.org/SourcePosition", "http://example.org/TargetPosition"],
    [[30, 50, 70], [30, 60, 70]]
  )[artifact_name("forkliftRobot")];
  // Send an authenticated request
  .print("Setting test API token");
  ?api_key(Token);
  setAPIKey(Token)[artifact_name("forkliftRobot")];
  // Invoke action with nested lists (i.e., ArraySchema payload)
  .print("Invoking action with array schema payload: http://example.org/MoveTo");
  invokeAction("http://example.org/MoveTo", [30, 60, 70])[artifact_name("forkliftRobot")].

{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
