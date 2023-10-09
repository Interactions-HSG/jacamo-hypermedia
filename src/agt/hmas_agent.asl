/* Contexts */
room(empty).

/* Abilities */
ability("http://example.org/bdi/BDIReasoner").
ability("http://example.org/processes/BinaryStateHandler").

/* Initial goals */
!start.

/* Initial plans */
+!start
  :
    true
  <-
    makeArtifact("Lightbulb1", "hmas.ResourceArtifact", ["lightbulb1-profile.ttl"], Id1);
    focusWhenAvailable("Lightbulb1");
    +id("Lightbulb1", Id1);

    makeArtifact("Lightbulb2", "hmas.ResourceArtifact", ["lightbulb2-profile.ttl"], Id2);
    focusWhenAvailable("Lightbulb2");
    +id("Lightbulb2", Id2);
    !reduceIlluminance;
    .

/*
Try making the plans artifact-agnostic, play with different actions
*/
+!reduceIlluminance
  :
    id("Lightbulb1", Id1)
  <-
    invokeAction("https://saref.etsi.org/core/ToggleCommand", ["https://www.w3.org/2019/wot/json-schema#BooleanSchema"], [false])[artifact_id(Id1), artifact_name(Lightbulb1)];
    .print("Lightbulb1 turned off");
    .

+!reduceIlluminance
  :
    id("Lightbulb2", Id2)
  <-
    invokeAction("https://saref.etsi.org/core/ToggleCommand", ["https://www.w3.org/2019/wot/json-schema#BooleanSchema"], [false])[artifact_id(Id2), artifact_name(Lightbulb2)];
    .print("Lightbulb2 turned off");
    .

+!reduceIlluminance
  :
    true
  <-
    .print("can't perform action");
    .


{ include("inc/hypermedia.asl") }
{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }