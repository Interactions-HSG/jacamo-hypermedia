/*
 * Mirroring of a hypermedia environment on the local CArtAgO node
 */

+!load_environment(EnvName, EnvUrl) : true <-
  .print("Loading environment (entry point): ", EnvUrl);
  makeArtifact(EnvName, "yggdrasil.ContainerArtifact", [EnvUrl, "workspace"], ArtId);
  focusWhenAvailable(EnvName);
  !registerForWebSub(EnvName, ArtId).

/* Mirror hypermedia workspaces as local CArtAgO workspaces */

+workspace(WorkspaceIRI, WorkspaceName) : true <-
  .print("Discovered workspace (name: ", WorkspaceName ,"): ", WorkspaceIRI);
  createWorkspace(WorkspaceName);
  joinWorkspace(WorkspaceName, WorkspaceArtId);
  // Create a hypermedia WorkspaceArtifact for this workspace.
  // Used for some operations (e.g., create artifact).
  makeArtifact(WorkspaceName, "yggdrasil.ContainerArtifact", [WorkspaceIRI, "artifact"], WkspArtId);
  focusWhenAvailable(WorkspaceName);
  !registerForWebSub(WorkspaceName, WkspArtId).

/* Mirror hypermedia artifacts in local CArtAgO workspaces */

+artifact(ArtifactIRI, ArtifactName) : true <-
  .print("Discovered artifact (name: ", ArtifactName ,"): ", ArtifactIRI);
  makeArtifact(ArtifactName, "wot.ThingArtifact", [ArtifactIRI], ArtID);
  focusWhenAvailable(ArtifactName);
  !registerForWebSub(ArtifactName, ArtID).

+!registerForWebSub(ArtifactName, ArtID) : true <-
  ?websub(HubIRI, TopicIRI)[artifact_name(ArtID,_)];
  registerArtifactForNotifications(TopicIRI, ArtID, HubIRI).

-!registerForWebSub(ArtifactName, ArtID) : true <-
  .print("WebSub not available for artifact: ", ArtifactName).
