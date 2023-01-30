/* Initial beliefs and rules */

art_url("https://raw.githubusercontent.com/Interactions-HSG/example-tds/main/tds/img-annotator.ttl").
img_url("https://i.imgur.com/5bGzZi7.jpg").


/* Initial goals */

!start.

/* Plans */

+!start : art_url(Url) <-
  .print("hello world.");
  makeArtifact("imgAnnotator", "wot.ThingArtifact", [Url], ArtId);
  !annotate_img .

+!annotate_img: img_url(Url) <-
  .print("Annotating image.");
  invokeAction("http://example.org/tackling-online-disinformation/ontology#AnnotateImage", ["http://purl.org/dc/dcmitype/StillImage"], [Url], ["http://purl.org/dc/dcmitype/Text"], Text)[artifact_name("imgAnnotator")];
  .print(Text) .


{ include("inc/hypermedia.asl") }
{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }

// uncomment the include below to have an agent compliant with its organisation
//{ include("$moiseJar/asl/org-obedient.asl") }
