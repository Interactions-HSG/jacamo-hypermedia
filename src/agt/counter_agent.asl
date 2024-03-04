/* Initial beliefs and rules */

entry_url("http://localhost:8080/workspaces/61").

/* Initial goals */

!start.

/* Plans */

+!start : entry_url(Url) <-
  .print("hello world.");
  makeArtifact("notification-server", "yggdrasil.NotificationServerArtifact", ["localhost", 8082], _);
  start;
  !load_environment("61", Url);
  .print("Environment loaded...");
  .print("Creating counter...");
  invokeAction("makeArtifact",
    ["artifactClass", "artifactName"],
    ["http://example.org/Counter", "c2"]
  )[artifact_name("103")];
  .wait(2000);
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
