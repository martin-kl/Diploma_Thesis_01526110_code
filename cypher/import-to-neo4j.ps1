#$env:NEO4J_HOME\bin\neo4j-admin.ps1 import --into $env:NEO4J_DB_DIR `
#D:\University\diploma_thesis\neo4j-community-4.1.0\bin\neo4j-admin.ps1 import --into $env:NEO4J_DB_DIR `

D:\University\diploma_thesis\neo4j-community-4.1.0\bin\neo4j-admin.ps1 import `
  --id-type=INTEGER `
  --nodes=Place="$env:NEO4J_DATA_DIR\static\place$env:POSTFIX" `
  --nodes=Organisation="$env:NEO4J_DATA_DIR\static\organisation$env:POSTFIX" `
  --nodes=TagClass="$env:NEO4J_DATA_DIR\static\tagclass$env:POSTFIX" `
  --nodes=Tag="$env:NEO4J_DATA_DIR\static\tag$env:POSTFIX" `
  --nodes=Message:Comment="$env:NEO4J_DATA_DIR\dynamic\comment$env:POSTFIX" `
  --nodes=Forum="$env:NEO4J_DATA_DIR\dynamic\forum$env:POSTFIX" `
  --nodes=Person="$env:NEO4J_DATA_DIR\dynamic\person$env:POSTFIX" `
  --nodes=Message:Post="$env:NEO4J_DATA_DIR\dynamic\post$env:POSTFIX" `
  --relationships=IS_PART_OF="$env:NEO4J_DATA_DIR\static\place_isPartOf_place$env:POSTFIX" `
  --relationships=IS_SUBCLASS_OF="$env:NEO4J_DATA_DIR\static\tagclass_isSubclassOf_tagclass$env:POSTFIX" `
  --relationships=IS_LOCATED_IN="$env:NEO4J_DATA_DIR\static\organisation_isLocatedIn_place$env:POSTFIX" `
  --relationships=HAS_TYPE="$env:NEO4J_DATA_DIR\static\tag_hasType_tagclass$env:POSTFIX" `
  --relationships=HAS_CREATOR="$env:NEO4J_DATA_DIR\dynamic\comment_hasCreator_person$env:POSTFIX" `
  --relationships=IS_LOCATED_IN="$env:NEO4J_DATA_DIR\dynamic\comment_isLocatedIn_place$env:POSTFIX" `
  --relationships=REPLY_OF="$env:NEO4J_DATA_DIR\dynamic\comment_replyOf_comment$env:POSTFIX" `
  --relationships=REPLY_OF="$env:NEO4J_DATA_DIR\dynamic\comment_replyOf_post$env:POSTFIX" `
  --relationships=CONTAINER_OF="$env:NEO4J_DATA_DIR\dynamic\forum_containerOf_post$env:POSTFIX" `
  --relationships=HAS_MEMBER="$env:NEO4J_DATA_DIR\dynamic\forum_hasMember_person$env:POSTFIX" `
  --relationships=HAS_MODERATOR="$env:NEO4J_DATA_DIR\dynamic\forum_hasModerator_person$env:POSTFIX" `
  --relationships=HAS_TAG="$env:NEO4J_DATA_DIR\dynamic\forum_hasTag_tag$env:POSTFIX" `
  --relationships=HAS_INTEREST="$env:NEO4J_DATA_DIR\dynamic\person_hasInterest_tag$env:POSTFIX" `
  --relationships=IS_LOCATED_IN="$env:NEO4J_DATA_DIR\dynamic\person_isLocatedIn_place$env:POSTFIX" `
  --relationships=KNOWS="$env:NEO4J_DATA_DIR\dynamic\person_knows_person$env:POSTFIX" `
  --relationships=LIKES="$env:NEO4J_DATA_DIR\dynamic\person_likes_comment$env:POSTFIX" `
  --relationships=LIKES="$env:NEO4J_DATA_DIR\dynamic\person_likes_post$env:POSTFIX" `
  --relationships=HAS_CREATOR="$env:NEO4J_DATA_DIR\dynamic\post_hasCreator_person$env:POSTFIX" `
  --relationships=HAS_TAG="$env:NEO4J_DATA_DIR\dynamic\comment_hasTag_tag$env:POSTFIX" `
  --relationships=HAS_TAG="$env:NEO4J_DATA_DIR\dynamic\post_hasTag_tag$env:POSTFIX" `
  --relationships=IS_LOCATED_IN="$env:NEO4J_DATA_DIR\dynamic\post_isLocatedIn_place$env:POSTFIX" `
  --relationships=STUDY_AT="$env:NEO4J_DATA_DIR\dynamic\person_studyAt_organisation$env:POSTFIX" `
  --relationships=WORK_AT="$env:NEO4J_DATA_DIR\dynamic\person_workAt_organisation$env:POSTFIX" `
  --delimiter '|'
