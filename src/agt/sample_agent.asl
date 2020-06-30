/* Initial beliefs and rules */

td_url("http://yggdrasil.andreiciortea.ro/environments/env1").

/* Initial goals */

!start.

/* Plans */

+!start : td_url(Url) <-
  .print("hello world.");
  !load_environment("myenv", Url).

{ include("inc/hypermedia.asl") }
{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }

// uncomment the include below to have an agent compliant with its organisation
//{ include("$moiseJar/asl/org-obedient.asl") }
