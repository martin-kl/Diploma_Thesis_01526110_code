// Cypher Queries - from DEV version of LDBC impl github

// ###################################
// 		 Structure Independent
// ###################################

// Query 1 => custom Query
:param firstName => 'Mahinda';
:param lastName => 'Perera';
//note on Neo4j Browser: have to activate multiple statement support to send both :param in one "Enter"
MATCH (p:Person {firstName:$firstName, lastName:$lastName})
RETURN p

// ---------------------------------------------

// IS4
MATCH (m:Message {id:$messageId})
RETURN
  m.creationDate as messageCreationDate,
  CASE exists(m.content)
    WHEN true THEN m.content
    ELSE m.imageFile
  END AS messageContent

// ---------------------------------------------

// Query 3 => custom Query
MATCH (p:Person)
RETURN AVG( SIZE( p.speaks ) )

	//more complicated version: (possibly include both of them to show unwind?)
MATCH (p:Person)
UNWIND p.speaks AS lang
WITH p.id AS id, COUNT(lang) AS nrLangs
RETURN AVG(nrLangs) AS avgLanguages
	//we need the WITH to count the langs for every person before we calculate the AVG
	// and the p.id in the WITH as otherwise it would count over all speaks of ALL persons !!





// ###################################
// 			Pattern Matching
// ###################################

// IS1
MATCH (n:Person {id:$personId})-[:IS_LOCATED_IN]->(p:Place)
RETURN
  n.firstName AS firstName,
  n.lastName AS lastName,
  n.birthday AS birthday,
  n.locationIP AS locationIP,
  n.browserUsed AS browserUsed,
  p.id AS cityId,
  n.gender AS gender,
  n.creationDate AS creationDate

// ---------------------------------------------

// IS3
MATCH (n:Person {id:$personId})-[r:KNOWS]-(friend)
RETURN
  friend.id AS personId,
  friend.firstName AS firstName,
  friend.lastName AS lastName,
  r.creationDate AS friendshipCreationDate
ORDER BY friendshipCreationDate DESC, toInteger(personId) ASC

// ---------------------------------------------

// IS7
MATCH (m:Message {id:$messageId})<-[:REPLY_OF]-(c:Comment)-[:HAS_CREATOR]->(p:Person)
OPTIONAL MATCH (m)-[:HAS_CREATOR]->(a:Person)-[r:KNOWS]-(p)
RETURN
  c.id AS commentId,
  c.content AS commentContent,
  c.creationDate AS commentCreationDate,
  p.id AS replyAuthorId,
  p.firstName AS replyAuthorFirstName,
  p.lastName AS replyAuthorLastName,
  CASE r
    WHEN null THEN false
    ELSE true
  END AS replyAuthorKnowsOriginalMessageAuthor
ORDER BY commentCreationDate DESC, replyAuthorId





// ###################################
// 			Path Queries
// ###################################

// IC13
MATCH (person1:Person {id:$person1Id}), (person2:Person {id:$person2Id})
OPTIONAL MATCH path = shortestPath((person1)-[:KNOWS*]-(person2))
RETURN
CASE path IS NULL
  WHEN true THEN -1
  ELSE length(path)
END AS shortestPathLength;

// ---------------------------------------------

// IC1
MATCH p=shortestPath((person:Person {id:$personId})-[path:KNOWS*1..3]-(friend:Person {firstName: $firstName}))
WHERE person <> friend
WITH friend, length(p) AS distance
  ORDER BY distance ASC, friend.lastName ASC, toInteger(friend.id) ASC
  LIMIT 20
MATCH (friend)-[:IS_LOCATED_IN]->(friendCity:Place)
OPTIONAL MATCH (friend)-[studyAt:STUDY_AT]->(uni:Organisation)-[:IS_LOCATED_IN]->(uniCity:Place)
WITH
  friend,
  collect(
    CASE uni.name
      WHEN null THEN null
      ELSE [uni.name, studyAt.classYear, uniCity.name]
	END
  ) AS unis,
  friendCity,
  distance
OPTIONAL MATCH (friend)-[workAt:WORK_AT]->(company:Organisation)-[:IS_LOCATED_IN]->(companyCountry:Place)
WITH
  friend,
  collect(
    CASE company.name
      WHEN null THEN null
      ELSE [company.name, workAt.workFrom, companyCountry.name]
    END
  ) AS companies,
  unis,
  friendCity,
  distance
RETURN
  friend.id AS friendId,
  friend.lastName AS friendLastName,
  distance AS distanceFromPerson,
  friend.birthday AS friendBirthday,
  friend.creationDate AS friendCreationDate,
  friend.gender AS friendGender,
  friend.browserUsed AS friendBrowserUsed,
  friend.locationIP AS friendLocationIp,
  friend.email AS friendEmails,
  friend.speaks AS friendLanguages,
  friendCity.name AS friendCityName,
  unis AS friendUniversities,
  companies AS friendCompanies
ORDER BY distanceFromPerson ASC, friendLastName ASC, toInteger(friendId) ASC
LIMIT 20

// ---------------------------------------------

// IS2
MATCH (:Person {id:$personId})<-[:HAS_CREATOR]-(m:Message)-[:REPLY_OF*0..]->(p:Post)
MATCH (p)-[:HAS_CREATOR]->(c)
RETURN
  m.id as messageId,
  CASE exists(m.content)
    WHEN true THEN m.content
    ELSE m.imageFile
  END AS messageContent,
  m.creationDate AS messageCreationDate,
  p.id AS originalPostId,
  c.id AS originalPostAuthorId,
  c.firstName as originalPostAuthorFirstName,
  c.lastName as originalPostAuthorLastName
ORDER BY messageCreationDate DESC
LIMIT 10





// ###################################
// 			DML Queries
// ###################################

// II1 (IU1)
MATCH (c:City {id:$cityId})
CREATE (p:Person {id: $personId, firstName: $personFirstName, lastName: $personLastName, gender: $gender, birthday: $birthday, creationDate: $creationDate, locationIP: $locationIP, browserUsed: $browserUsed, speaks: $languages, emails: $emails})-[:IS_LOCATED_IN]->(c)
WITH p, count(*) AS dummy1
UNWIND $tagIds AS tagId
    MATCH (t:Tag {id: tagId})
    CREATE (p)-[:HAS_INTEREST]->(t)
WITH p, count(*) AS dummy2
UNWIND $studyAt AS s
    MATCH (u:Organisation {id: s[0]})
    CREATE (p)-[:STUDY_AT {classYear: s[1]}]->(u)
WITH p, count(*) AS dummy3
UNWIND $workAt AS w
    MATCH (comp:Organisation {id: w[0]})
    CREATE (p)-[:WORKS_AT {workFrom: w[1]}]->(comp)


// ---------------------------------------------

// ID7
MATCH (comment:Comment {id: $commentId})<-[:REPLY_OF*]-(replies:Comment)
DETACH DELETE comment, replies

// ---------------------------------------------

// Query 12 => custom update query
//TODO 





// ###################################
// 			DDL Query
// ###################################

// Query 13 => custom Query
// not doable in cypher as there is no schema