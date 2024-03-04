/*
 * Mirroring of a hypermedia environment on the local CArtAgO node
 */

+!load_environment(EnvName, EnvUrl) : true <-
  .print("Loading environment (entry point): ", EnvUrl);
  makeArtifact(EnvName, "yggdrasil.ContainerArtifact", [EnvUrl, "workspace"], ArtId);
  focus(ArtId);
  .print("Created artifact, registering for notifications");
  !registerForWebSub(EnvName, ArtId).

/* Mirror hypermedia workspaces as local CArtAgO workspaces */

@workspace_discovery[atomic]
+workspace(WorkspaceIRI, WorkspaceName) : true <-
  .print("Discovered workspace (name: ", WorkspaceName ,"): ", WorkspaceIRI);
  createWorkspace(WorkspaceName);
  joinWorkspace(WorkspaceName, WorkspaceArtId);
  .term2string(WorkspaceNameTerm, WorkspaceName);
  ?joined(WorkspaceNameTerm, WorkspaceId);
  // Create a hypermedia WorkspaceArtifact for this workspace.
  // Used for some operations (e.g., create artifact).
  makeArtifact(WorkspaceName, "yggdrasil.ContainerArtifact", [WorkspaceIRI, "artifact"], WkspArtId)[wid(WorkspaceId)];
  focus(WkspArtId);
  !registerForWebSub(WorkspaceName, WkspArtId).

/* Mirror hypermedia artifacts in local CArtAgO workspaces */

+artifact(ArtifactIRI, ArtifactName)[workspace(_,WorkspaceName,_)] : true <-
  .print("Discovered artifact ", ArtifactName ," in workspace ", WorkspaceName, ": ", ArtifactIRI);
  makeArtifact(ArtifactName, "wot.ThingArtifact", [ArtifactIRI], ArtID);
  focus(ArtID);
  !registerForWebSub(ArtifactName, ArtID);
  .term2string(WorkspaceName, WorkspaceNameStr);
  ?workspace(WorkspaceIRI, WorkspaceNameStr);
  registerArtifactForFocus(WorkspaceIRI, ArtifactIRI, ArtID, ArtifactName).

+!registerForWebSub(ArtifactName, ArtID) : true <-
  ?websub(HubIRI, TopicIRI)[artifact_id(ArtID)];
  registerArtifactForWebSub(TopicIRI, ArtID, HubIRI).

-!registerForWebSub(ArtifactName, ArtID) : true <-
  .print("WebSub not available for artifact: ", ArtifactName).
