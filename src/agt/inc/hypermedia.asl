/* 
 * Mirroring of a hypermedia environment on the local CArtAgO node
 */

+!load_environment(EnvName, EnvUrl) : true <-
  .print("Loading environment (entry point): ", EnvUrl);
  makeArtifact(EnvName, "yggdrasil.RDFContainerArtifact", [EnvUrl, "workspace"], ArtId);
  focusWhenAvailable(EnvName);
  !subEnvironmentMembers(EnvUrl, ArtId).

+!subEnvironmentMembers(EnvUrl, ArtID) : true <-
  ?rdfsub(HubIRI, TopicIRI)[artifact_name(ArtID,_)];
  registerEnvironmentForNotifications(TopicIRI, EnvUrl, ArtID, HubIRI).

/* Mirror hypermedia workspaces as local CArtAgO workspaces */

+workspace(WorkspaceIRI, WorkspaceName) : true <-
  .print("Discovered workspace (name: ", WorkspaceName ,"): ", WorkspaceIRI);
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
  .print("Discovered artifact (name: ", ArtifactName ,"): ", ArtifactIRI);
  makeArtifact(ArtifactName, "wot.ThingArtifact", [ArtifactIRI], ArtID);
  focusWhenAvailable(ArtifactName);
  !registerForWebSub(ArtifactName, ArtID).

+!registerForWebSub(ArtifactName, ArtID) : true <-
  ?websub(HubIRI, TopicIRI)[artifact_name(ArtID,_)];
  registerArtifactForNotifications(TopicIRI, ArtID, HubIRI).
