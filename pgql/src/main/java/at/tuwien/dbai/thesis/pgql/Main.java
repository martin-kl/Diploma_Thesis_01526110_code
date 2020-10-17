package at.tuwien.dbai.thesis.pgql;

import oracle.pgql.lang.PgqlException;
import oracle.pgql.lang.Pgql;
import oracle.pgql.lang.PgqlResult;

/**
 * Created by Martin Klampfer, 01526110, for the Diploma thesis at the TU Vienna in 2020.
 */

public class Main {

  public static void main(String[] args) throws PgqlException {

    try (Pgql pgql = new Pgql()) {

      /* ###################################################
                      Structure Independent
      ######################################################  */

      // First of my queries (Structure independent, select person by first and last name)
      String firstName, lastName;
      firstName = "John";
      lastName = "Doe";
      PgqlResult q1 = pgql.parse("SELECT n FROM MATCH (n:Person) WHERE n.firstName=" + firstName + " AND n.lastName = " + lastName);
      System.out.println(q1.getStatement());
      System.out.println("\n\n");



      // Second of my queries - IS4 - select message by id
      long messageId = 1234L;
      PgqlResult q2 = pgql.parse("SELECT m.creationDate AS messageCreationDate, " +
              // "CASE exists(m.content) " +
              "CASE (m.content IS NOT NULL) " +
                "WHEN true THEN m.content " +
                "ELSE m.imageFile " +
              "END AS messageContent " +
              "FROM MATCH (m:Message) WHERE m.id=" + messageId);
      System.out.println("\t\tQuery 2 (IS4):\n" + q2.getStatement());
      System.out.println("\n\n");


      // Third query (Structure independent, avg spoken languages)
      PgqlResult q3 = pgql.parse("SELECT AVG(p.speaks) FROM MATCH (p:Person)");
      //ATTENTION: that query does not work in reality as "speaks" is a list of strings and according to the
      //.xlsx file and the spec, PGQL cannot store a list/array



      /* ###################################################
                        Pattern Matching
      ######################################################  */

      //4th query - IS1 - select person
      long personId = 933L;
      PgqlResult q4 = pgql.parse("SELECT p.firstName, p.lastName, p.birthday, p.locationIP, c.id AS cityId, p.gender, p.creationDate " +
              "FROM MATCH (p:person) -[:isLocatedIn]-> (c) WHERE p.id = " + personId);
              //"FROM MATCH (p:person) -[:isLocatedIn]-> (c:City) WHERE p.id = " + personId);
      System.out.println("\t\tQuery 4 (IS1):\n" + q4.getStatement());
      System.out.println("\n\n");



      //5th query - IS3 - select friends
      PgqlResult q5 = pgql.parse("SELECT f.id AS personId, f.firstName AS firstName, f.lastName AS lastName, e.creationDate AS friendshipCreationDate " +
              "FROM MATCH (p:Person) -[e:knows]-> (f) WHERE p.id = " + personId + "" +
              "ORDER BY friendshipCreationDate DESC, personId ASC");
      //f can only be a person
      System.out.println("\t\tQuery 5 (IS3):\n" + q5.getStatement());
      System.out.println("\n\n");


      //6th query - IS7 - find comments
      PgqlResult q6 = pgql.parse("SELECT c.id AS commentId, c.content AS commentContent, c.creationDate AS commentCreationDate, p2.id AS replyAuthorId, p2.firstName AS replyAuthorFirstName, p2.lastName AS replyAuthorLastName" +
              ", CASE (k IS NOT NULL) WHEN true THEN true ELSE false END AS replyAuthorKnowsOriginalMessageAuthor" +
              " FROM MATCH (p1:Person) <-[:hasCreator]- (m:Message) <-[r:reply_of]- (c:Comment) -[:hasCreator]-> (p2:Person)" +
              ", MATCH (p1) -[k:knows]-? (p2)"+       //I think the question mark goes there, not after "k", see also: https://pgql-lang.org/spec/1.3/#group-variables
              " WHERE m.id = " + messageId);
      System.out.println("\t\tQuery 6 (IS7):\n" + q6.getStatement());
      System.out.println("\n\n");





      /* ###################################################
                        Path Queries
      ######################################################  */

      //Query 7 - IC13 - find shortest path between two persons
      long person1Id = 932L;
      long person2Id = 933L;
      PgqlResult q7 = pgql.parse("SELECT COUNT(e) AS shortestPathLength" + //TODO this does not yet work if there is no such path - it should return -1 in that case...
              " FROM MATCH SHORTEST ((p1:Person) -[e:knows]-* (p2:Person))" +
              " WHERE p1.id = " + person1Id + " AND p2.id = " + person2Id);
      System.out.println("\t\tQuery 7 (IC13):\n" + q7.getStatement());
      System.out.println("\n\n");



              //Query 8 - IC1 - friends (knows*1..3) with a certain name
      String firstNameQ8 = "Mary";
      //ATTENTION: this query does NOT WORK
      PgqlResult q8 = pgql.parse("SELECT f.id AS friendId, f.lastName AS friendLastName, COUNT(e) AS distanceFromPerson, f.birthday as friendBirthday" + //attention: for the real query, quite some attributes are still missing here
              ", c.name as friendCityName" +
              ", ARRAY_AGG(u.name) AS friendUniversities" +    //ATTENTION: PGQL lacks the ability to generate a list of the values we need from co
              //" FROM MATCH (p:Person) -[e:knows]-{1,3} (f:Person)" + //TODO "-{1,3}" kills the query, so it does not run here... but this is the same as in https://pgql-lang.org/spec/1.3/#group-variables ?!?!?! [error in this parser ?!]
              ", MATCH (f) -/:isLocatedIn/-> (c:City)" +
              ", MATCH (f) -/:workAt?/-> (co:Company) -/:isLocatedIn/-> (co:City)" +
              ", MATCH (f) -/:studyAt?/-> (u:University) " + // -/:isLocatedIn/-> (cu:City)" +
              " WHERE p.id = " + personId + " AND f.firstName = " + firstNameQ8 +
              " ORDER BY distanceFromPerson ASC, friendLastName ASC, friendId ASC");
      System.out.println("\t\tQuery 8 (IC1):\n" + q8.getStatement());
      System.out.println("\n\n");





      //Query 9 - IS2 - last 10 messages
      PgqlResult q9 = pgql.parse("SELECT m.id AS messageId, CASE (m.content IS NOT NULL) WHEN true THEN m.content ELSE m.imageFile END AS messageContent, m.creationDate AS messageCreationDate" +
              ", po.id as originalPostId, op.id as originalPostAuthorId, op.firstName AS originalPostAuthorFirstName, op.lastName AS originalPostAuthorLastName" +
              " FROM MATCH (p:Person) <-[e:hasCreator]- (m:Message) -[:replyOf]-> (po:Post) -[:hasCreator]-> (op:Person) " +
              //" FROM MATCH (p:Person) <-[e:hasCreator]- (m:Message) -[:replyOf]->* (po:Post) -[:hasCreator]-> (op:Person) " + //TODO the star for variable length is again not supported apparently, so the above version is NOT correct but it should be a correct query
              " WHERE p.id = " + person1Id);
      System.out.println("\t\tQuery 9 (IS2):\n" + q9.getStatement());
      System.out.println("\n\n");





      /* ###################################################
                        DML Queries
      ######################################################  */

      //Query 10 - II1 - insert a new person
      String personFirstName = "Mary", personLastName = "Ann", gender = "F", birthday="1990-01-01", creationDate="2020-10-01 09:00:00",
              locationIP = "123.4.5.6", browserUsed = "Firefox", speaks = "German; English", email = "m.a@mail.com";
      long cityId = 123456L;
      long[] tagIds = {12L, 34L}; //ATTENTION: PGQL does NOT support something like UNWIND (Cypher) that would allow us to iterate over a list -> NOT DOABLE
      long tagId = 12L; //therefore, use a single ID s.t. query compiles
      long workId = 34L; //therefore, use a single ID s.t. query compiles
      int workFrom = 2010;

      //note that I added single ticks on every String such that they keep the capitalization and that's also what is done in the pgql Spec.
      PgqlResult q10 = pgql.parse("INSERT " +
              "VERTEX p LABELS (Person)" +
                " PROPERTIES (p.id="+personId+", p.firstName='"+personFirstName+"', p.lastName='"+personLastName+"', p.gender='"+gender+"',p.birthday=DATE '"+birthday+"'" + //note the DATE support
                ", p.creationDate=TIMESTAMP '"+creationDate+"', p.locationIp='"+locationIP+"', p.browserUsed='"+browserUsed+"', p.speaks='"+speaks+"', p.email='"+email+"')" +
              ", EDGE e BETWEEN p AND c LABELS (isLocatedIn)" +
              ", EDGE ew BETWEEN p AND co LABELS (workAt) PROPERTIES (ew.workFrom="+workFrom+")" +
              " FROM MATCH (c:City)" +
              ", MATCH (co:Company)"+
              " WHERE c.id="+cityId+" AND co.id="+workId+")");
      System.out.println("\t\tQuery 10 (II1):\n" + q10.getStatement());
      System.out.println("\n\n");



      //Query 11 - ID7 - delete a comment together with its edges & replying comments

      //NOTE that PGQL automatically removes all incoming + outgoing edges on node removal
      long commentId = 4567L;
      PgqlResult q11 = pgql.parse("DELETE c, r" +
              " FROM MATCH (c:Comment) <-/:replyOf*/- (r:Comment)"+ //NOTE that the star here has to be inside (next to the label) on Reachability queries whereas it is outside on "normal" queries -> ex: <-[:replyOf]-*
              " WHERE c.id = " + commentId);
      System.out.println("\t\tQuery 11 (ID7):\n" + q11.getStatement());
      System.out.println("\n\n");


      //Query 12 - custom Update query
      //TODO


      /* ###################################################
                        DDL Query
      ######################################################  */

      //Query 13 - example of a schema for the location
      PgqlResult q12 = pgql.parse("CREATE PROPERTY GRAPH social_network " +
              "VERTEX TABLES (" +
                "Persons LABEL Person PROPERTIES (person_id, firstName, lastName), " +
                "Cities LABEL City PROPERTIES ARE ALL COLUMNS" + //also possible
              ") " +
              "EDGE TABLES (" +
                "LivingIn " + // table name
                  "SOURCE KEY (person_id) REFERENCES Persons " +
                  "DESTINATION Cities " + //take foreign key automatically
                  "LABEL isLocatedIn NO PROPERTIES" +
              ")");
      System.out.println("\t\tQuery 12 (schema):\n" + q12.getStatement());
      System.out.println("\n\n");
    }
  }
}
