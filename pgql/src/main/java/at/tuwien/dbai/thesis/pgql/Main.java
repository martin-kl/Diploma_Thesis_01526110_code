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
              ", MATCH (p1) -[k:knows]-? (p2)"+ //TODO I'm not sure about this here, this or -[k?:knows]-
              //", MATCH (p1) -[k?:knows]- (p2)"+ //that is also specified but what does that even mean?
              " WHERE m.id = " + messageId);
      System.out.println("\t\tQuery 6 (IS7):\n" + q6.getStatement());
      System.out.println("\n\n");





      /* ###################################################
                        Path Queries
      ######################################################  */

      //7th query - IC13 - find shortest path between two persons
      long person1Id = 932L;
      long person2Id = 933L;
      PgqlResult q7 = pgql.parse("SELECT COUNT(e) AS shortestPathLength" + //TODO this does not yet work if there is no such path - it should return -1 in that case...
              " FROM MATCH SHORTEST ((p1:Person) -[e:knows]-* (p2:Person))" +
              " WHERE p1.id = " + person1Id + " AND p2.id = " + person2Id);
      System.out.println("\t\tQuery 7 (IC13):\n" + q7.getStatement());
      System.out.println("\n\n");



    }
  }
}
