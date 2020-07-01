/* 
 * Mirroring of a hypermedia environment on the local CArtAgO node
 */

+!load_environment(EnvName, EnvUrl) : true <-
  makeArtifact(EnvName, "yggdrasil.ContainerArtifact", [EnvUrl, "workspace"], ArtId);
  focusWhenAvailable(EnvName).

/* Mirror hypermedia workspaces as local CArtAgO workspaces */

+workspace(WorkspaceIRI, WorkspaceName) : true <-
  .print("Creating workspace: ", WorkspaceIRI);
  .print("[Workspace: ", WorkspaceIRI, "] Name: ", WorkspaceName);
  createWorkspace(WorkspaceName);
  joinWorkspace(WorkspaceName, WorkspaceArtId);
  // Create a hypermedia WorkspaceArtifact for this workspace.
  // Used for some operations (e.g., create artifact). 
  makeArtifact(WorkspaceName, "yggdrasil.ContainerArtifact", [WorkspaceIRI, "artifact"], WkspArtId);
  focusWhenAvailable(WorkspaceName).

/* Mirror hypermedia artifacts in local CArtAgO workspaces */

+artifact(ArtifactIRI, ArtifactName) : true <-
  .print("[Artifact: ", ArtifactIRI, "] Name: ", ArtifactName);
  makeArtifact(ArtifactName, "wot.ThingArtifact", [ArtifactIRI], ArtID);
  focusWhenAvailable(ArtifactName).
