/* Initial beliefs and rules */

//Please register here for using the Phantom X Robot: https://app.swaggerhub.com/apis-docs/iomz/leubot/1.2#/user/addUser
api_key("API-KEY").
//env_url("https://yggdrasil.interactions.ics.unisg.ch/environments/61").
env_url("http://localhost:8080/environments/env1").

//Check the default, lower and upper limits of the PhantomX joint parameters: https://github.com/Interactions-HSG/leubot
sourceAngle(512). // ~180 degrees angle
targetAngle(256). // ~90 degrees angle

/* Initial goals */

!start.

/* Plans */

+!start : env_url(Url) <-
  .print("hello world.");
  makeArtifact("notification-server", "yggdrasil.NotificationServerArtifact", ["localhost", 8081], _);
  start;
  !load_environment("myenv", Url).
  // .wait(2000);
  // !moveBlock.

+count("3") : true <-
  .print("3... go!");
  !moveBlock.

+count(Value) : true <-
  .print(Value, "...").

+!moveBlock : true <-
//  makeArtifact("armRobot", "tools.ThingArtifact", [Url, true], ArtId);
//  .print("Robot arm artifact created!");
//  joinWorkspace("102",_);
//  focusWhenAvailable("leubot1");
  .print("Set API token");
  !setAuthentication;
  !deliver.

+!setAuthentication : api_key(Token) <-
  setAPIKey(Token)[artifact_name("leubot1")].

+!deliver : sourceAngle(Source) & targetAngle(Target) <-
  .print("Set base to " , Source);
  invokeAction("https://ci.mines-stetienne.fr/kg/ontology#SetBase", ["https://www.w3.org/2019/wot/json-schema#IntegerSchema"], [Source])[artifact_name("leubot1")];
  !interval;
  .print("Set gripper to 512");
  invokeAction("https://ci.mines-stetienne.fr/kg/ontology#SetGripper", ["https://www.w3.org/2019/wot/json-schema#IntegerSchema"], [512])[artifact_name("leubot1")];
  !interval;
  .print("Set wrist angle to 390");
  invokeAction("https://ci.mines-stetienne.fr/kg/ontology#SetWristAngle", ["https://www.w3.org/2019/wot/json-schema#IntegerSchema"], [390])[artifact_name("leubot1")];
  !interval;
  .print("Set shoulder to 510");
  invokeAction("https://ci.mines-stetienne.fr/kg/ontology#SetShoulder", ["https://www.w3.org/2019/wot/json-schema#IntegerSchema"], [510])[artifact_name("leubot1")];
  !interval;
  .print("Set gripper to 400");
  invokeAction("https://ci.mines-stetienne.fr/kg/ontology#SetGripper", ["https://www.w3.org/2019/wot/json-schema#IntegerSchema"], [400])[artifact_name("leubot1")];
  !interval;
  .print("Set shoulder to 400");
  invokeAction("https://ci.mines-stetienne.fr/kg/ontology#SetShoulder", ["https://www.w3.org/2019/wot/json-schema#IntegerSchema"], [400])[artifact_name("leubot1")];
  !interval;
  .print("Set base to " , Target);
  invokeAction("https://ci.mines-stetienne.fr/kg/ontology#SetBase", ["https://www.w3.org/2019/wot/json-schema#IntegerSchema"], [Target])[artifact_name("leubot1")];
  !interval;
  .print("Set shoulder to 510");
  invokeAction("https://ci.mines-stetienne.fr/kg/ontology#SetShoulder", ["https://www.w3.org/2019/wot/json-schema#IntegerSchema"], [510])[artifact_name("leubot1")];
  !interval;
  .print("Set gripper to 512");
  invokeAction("https://ci.mines-stetienne.fr/kg/ontology#SetGripper", ["https://www.w3.org/2019/wot/json-schema#IntegerSchema"], [512])[artifact_name("leubot1")];
  !interval;
  .print("Set gripper to 400");
  invokeAction("https://ci.mines-stetienne.fr/kg/ontology#SetShoulder", ["https://www.w3.org/2019/wot/json-schema#IntegerSchema"], [400])[artifact_name("leubot1")];
  !interval;
  invokeAction("https://ci.mines-stetienne.fr/kg/ontology#Reset", [])[artifact_name("leubot1")];

  readProperty("https://ci.mines-stetienne.fr/kg/ontology#Posture", Types , Values)[artifact_name("leubot1")];
  .print(Values).

+!interval : true <- .wait(3000).

{ include("inc/hypermedia.asl") }
{ include("$jacamoJar/templates/common-cartago.asl") }
{ include("$jacamoJar/templates/common-moise.asl") }
