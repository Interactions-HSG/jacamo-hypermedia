/* Initial beliefs and rules */

api_key("test-token").
robot_td("http://yggdrasil.interactions.ics.unisg.ch/environments/61/workspaces/hackathon-wksp/artifacts/leubot").

/* Initial goals */

!start.

/* Plans */

+!start : robot_td(Url) & api_key(Token) <-
  makeArtifact("armRobot", "wot.ThingArtifact", [Url, true], ArtId);
  setAPIKey(Token)[artifact_name("armRobot")];
  !openGripper.

+!openGripper : true <-
  invokeAction("https://ci.mines-stetienne.fr/kg/ontology#SetGripper", ["https://www.w3.org/2019/wot/json-schema#IntegerSchema"], [512])[artifact_name("leubot1")].

