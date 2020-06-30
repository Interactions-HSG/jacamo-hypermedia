/* Mirror a hypermedia environment on the local CArtAgO node */

+!load_environment(EnvName, EnvUrl) : true <-
  .print("Hello world of include!");
  makeArtifact(EnvName, "yggdrasil.EnvironmentArtifact", [EnvUrl], ArtId);
  getWorkspaceIRIs(WorkspaceIRIs)[artifact_name(EnvName)];
  !buildWorkspaces(WorkspaceIRIs).

/* Mirror hypermedia workspaces as local CArtAgO workspaces */

+!buildWorkspaces([]) : true .

+!buildWorkspaces([WorkspaceIRI | T]) : true <- //web_environment_artifact_id(WebEnvArtID) <-
  .print("Creating workspace: ", WorkspaceIRI);
  getEntityDetails(WorkspaceIRI, WorkspaceName, WebSubHubIRI, ArtifactIRIs);
  .print("[Workspace: ", WorkspaceIRI, "] Name: ", WorkspaceName, ", available artifacts: ", ArtifactIRIs);
  createWorkspace(WorkspaceName);
  joinWorkspace(WorkspaceName, WorkspaceArtId);
  // Create a hypermedia WorkspaceArtifact for this workspace.
  // Used for some operations (e.g., create artifact). 
  makeArtifact(WorkspaceName, "wot.ThingArtifact", [WorkspaceIRI], WkspArtId);
//  +artifact_details(WorkspaceIRI, WorkspaceName, ArtifactIRIs, WorkspaceArtId);
//  registerArtifactForNotifications(WorkspaceIRI, WebEnvArtID, WebSubHubIRI);
  !buildArtifacts(WorkspaceName, ArtifactIRIs);
  !buildWorkspaces(T).

/* Mirror hypermedia artifacts in local CArtAgO workspaces */

+!buildArtifacts(WorkspaceName, []) : true .

+!buildArtifacts(WorkspaceName, [ArtifactIRI | T]) : true <-
  getEntityDetails(ArtifactIRI, ArtifactName, WebSubHubIRI, _);
  .print("[Artifact: ", ArtifactIRI, "] Name: ", ArtifactName, ", WebSub IRI: ", WebSubHubIRI);
  !makeArtifact(WorkspaceName, ArtifactIRI, ArtifactName,WebSubHubIRI);
  !buildArtifacts(WorkspaceName, T).

+!makeArtifact(WorkspaceName, ArtifactIRI, ArtifactName, WebSubHubIRI)
  : .ground([WorkspaceName, ArtifactIRI, ArtifactName, WebSubHubIRI])
  <-
  .print("Got a thing artifact with a WebSubIRI!");
  !createThingArtifact(WorkspaceName, ArtifactName, ArtifactIRI, ArtID).
//  registerArtifactForNotifications(ArtifactIRI, ArtID, WebSubHubIRI);
//  .print("Subscribed artifact ", ArtifactName, " for notifications!").

+!makeArtifact(WorkspaceName, ArtifactIRI, ArtifactName, WebSubHubIRI)
  : .ground([WorkspaceName, ArtifactIRI, ArtifactName])
  <-
  .print("Got a thing artifact without a WebSubIRI!");
  !createThingArtifact(WorkspaceName, ArtifactName, ArtifactIRI, ArtID).

+!createThingArtifact(WorkspaceName, ArtifactName, ArtifactIRI, ArtID) : true <-
  makeArtifact(ArtifactName, "wot.ThingArtifact", [ArtifactIRI], ArtID);
  +artifact_details(ArtifactIRI, ArtifactName, ArtID).
