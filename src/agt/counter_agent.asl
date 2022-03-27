/* Initial beliefs and rules */

env_url("http://localhost:8085/environments/env2").

/* Initial goals */

!start.

/* Plans */

+!start : env_url(Url) <-
  .print("hello world.");
  makeArtifact("notification-server", "ch.unisg.ics.interactions.jacamo.artifacts.yggdrasil.NotificationServerArtifact", ["localhost", 8082], _);
  start;
  !load_environment("myenv", Url);
  focusWhenAvailable("wksp2");
  .print("Creating counter...");
  invokeAction("http://w3id.org/eve#MakeArtifact",
    ["http://w3id.org/eve#ArtifactClass", "http://w3id.org/eve#ArtifactName"],
    ["http://example.org/Counter", "c2"]
  )[artifact_name("wksp2")];
  focusWhenAvailable("c2");
  !countTo(3).

+!countTo(0) : true .

+!countTo(X) : true <-
  .print(X, "...");
  invokeAction("http://example.org/Increment", [])[artifact_name("c2")];
  .wait(1000);
  !countTo(X - 1).

{ include("inc/hypermedia.asl") }
{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }

// uncomment the include below to have an agent compliant with its organisation
//{ include("$moiseJar/asl/org-obedient.asl") }
