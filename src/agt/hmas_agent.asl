!start.

+!start : true
  <- makeArtifact("lamp", "hmas.ResourceArtifact", ["lamp-profile.ttl"], ArtId);
  focus(ArtId).

+signifier(ActionType, RecContext, RecAbilities) <-
  .print(ActionType) ;
  .print(RecContext) ;
  .print(RecAbilities) .
