/* 
 * Mirroring of a hypermedia environment on the local CArtAgO node
 */

+!load_environment(EnvName, EnvUrl) : true <-
  makeArtifact(EnvName, "yggdrasil.RDFContainerArtifact", [EnvUrl, "workspace"], ArtId);
  focusWhenAvailable(EnvName);
  !subEnvironmentMembers(EnvUrl, ArtId).

+!subEnvironmentMembers(EnvUrl, ArtID) : true <-
  ?rdfsub(HubIRI, TopicIRI)[artifact_name(ArtID,_)];
  registerEnvironmentForNotifications(TopicIRI, EnvUrl, ArtID, HubIRI).

/* Mirror hypermedia workspaces as local CArtAgO workspaces */

+workspace(WorkspaceIRI, WorkspaceName) : true <-
  .print("Creating workspace: ", WorkspaceIRI);
  .print("[Workspace: ", WorkspaceIRI, "] Name: ", WorkspaceName);
  createWorkspace(WorkspaceName);
  joinWorkspace(WorkspaceName, WorkspaceArtId);
  // Create a hypermedia WorkspaceArtifact for this workspace.
  // Used for some operations (e.g., create artifact). 
  makeArtifact(WorkspaceName, "yggdrasil.RDFContainerArtifact", [WorkspaceIRI, "artifact"], WkspArtId);
  focusWhenAvailable(WorkspaceName);
  !subWorkspaceMembers(WorkspaceIRI, WkspArtId).

+!subWorkspaceMembers(WorkspaceIRI, ArtID) : true <-
  ?rdfsub(HubIRI, TopicIRI)[artifact_name(ArtID,_)];
  registerWorkspaceForNotifications(TopicIRI, WorkspaceIRI, ArtID, HubIRI).

/* Mirror hypermedia artifacts in local CArtAgO workspaces */

+artifact(ArtifactIRI, ArtifactName) : true <-
  .print("[Artifact: ", ArtifactIRI, "] Name: ", ArtifactName);
  makeArtifact(ArtifactName, "wot.ThingArtifact", [ArtifactIRI], ArtID);
  focusWhenAvailable(ArtifactName);
  !registerForWebSub(ArtifactName, ArtID).

+!registerForWebSub(ArtifactName, ArtID) : true <-
  ?websub(HubIRI, TopicIRI)[artifact_name(ArtID,_)];
  registerArtifactForNotifications(TopicIRI, ArtID, HubIRI).
