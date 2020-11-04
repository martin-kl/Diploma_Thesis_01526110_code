package at.tuwien.dbai.thesis.pgql;

import oracle.pgql.lang.PgqlException;
import oracle.pgql.lang.Pgql;
import oracle.pgql.lang.PgqlResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Martin Klampfer, 01526110, for the Diploma thesis at the TU Vienna in 2020.
 */

public class Queries {

  private static Pgql pgql;
  private static Logger log;

  private static final long personId = 123L;
  private static final long messageId = 456L;

  public static void main(String[] args) throws PgqlException {
    pgql = new Pgql();
    log = LoggerFactory.getLogger(Queries.class);

    log.info("Starting Gremlin queries");

    query_examples();
    /*
    query1();
    query2();
    query3();
    query4();
    query5();
    query6();
    query7();
    query8();
    query9();
    query10();
    query11();
    query12();
    query13();
     */

    log.info("Queries finished.");
  }
  /* ###################################################
            Queries from the introductory part
  ######################################################  */

  private static void query_examples() throws PgqlException {
    PgqlResult q1 = pgql.parse("SELECT p.firstName, p.lastName FROM MATCH (p:Person) WHERE p.id = " + personId);
    log.info("\t\tQuery Example 1:\n{}\n\n", q1.getStatement());

    PgqlResult q2 = pgql.parse("SELECT id(p), labels(p) AS lbl, p FROM MATCH (p:Person) LIMIT 5");
    log.info("\t\tQuery Example 2:\n{}\n\n", q2.getStatement());
  }

  /* ###################################################
                  Structure Independent
  ######################################################  */

  /**
   * Query 1 - custom query
   * Select a person by their first and last name.
   */
  private static void query1() throws PgqlException {
    String firstName = "John", lastName = "Doe";

    PgqlResult q1 = pgql.parse("SELECT n FROM MATCH (n:Person) WHERE n.firstName=" + firstName + " AND n.lastName = " + lastName);

    log.info("\t\tQuery 1:\n{}\n\n", q1.getStatement());
  }


  /**
   * Query 2 (IS4)
   * Given a Message, retrieve its content and creation date.
   */
  private static void query2() throws PgqlException {
    PgqlResult q2 = pgql.parse("SELECT m.creationDate AS messageCreationDate, " +
        "CASE (m.content IS NOT NULL) " +
          "WHEN true THEN m.content " +
          "ELSE m.imageFile " +
        "END AS messageContent " +
        "FROM MATCH (m:Message) WHERE m.id=" + messageId);

    log.info("Query 2 (IS4):\n{}\n\n", q2.getStatement());
  }


  /**
   * Query 3 - custom query
   * Calculate average number of spoken languages.
   */
  private static void query3() throws PgqlException {
    //ATTENTION: that query does not work like this as "speaks" is a list of strings and according to the
    //.xlsx file and the spec, PGQL cannot store a list/array -> so this is only as a schematic

    PgqlResult q3 = pgql.parse("SELECT AVG(p.speaks) FROM MATCH (p:Person)");
    log.info("Query 3:\n{}\n\n", q3.getStatement());
  }



  /* ###################################################
                    Pattern Matching
  ######################################################  */

  /**
   * Query 4 (IS1)
   * Given a start Person, retrieve their first name, last name, birthday, IP address, browser, and city
   * of residence.
   */
  private static void query4() throws PgqlException {
    PgqlResult q4 = pgql.parse("SELECT p.firstName, p.lastName, p.birthday, p.locationIP, c.id AS cityId, p.gender, p.creationDate " +
        "FROM MATCH (p:person) -[:isLocatedIn]-> (c:City) WHERE p.id = " + personId);

    log.info("Query 4 (IS1):\n{}\n\n", q4.getStatement());
  }


  /**
   * Query 5 (IS3)
   * Given a start Person, retrieve all of their friends, and the date at which they became friends.
   */
  private static void query5() throws PgqlException {
    PgqlResult q5 = pgql.parse("SELECT f.id AS personId, f.firstName AS firstName, f.lastName AS lastName, e.creationDate AS friendshipCreationDate " +
        "FROM MATCH (p:Person) -[e:knows]-> (f) WHERE p.id = " + personId + " " + //f can only be a person
        "ORDER BY friendshipCreationDate DESC, personId ASC");

    log.info("Query 5 (IS3):\n{}\n\n", q5.getStatement());
  }


  /**
   * Query 6 (IS7)
   * Given a Message, retrieve the (1-hop) Comments that reply to it.
   * In addition, return a boolean flag knows indicating if the author of the reply (replyAuthor) knows
   * the author of the original message (messageAuthor). If author is same as original author, return
   * False for knows flag.
   */
  private static void query6() throws PgqlException {
    PgqlResult q6 = pgql.parse("SELECT c.id AS commentId, c.content AS commentContent, c.creationDate AS commentCreationDate, p2.id AS replyAuthorId, p2.firstName AS replyAuthorFirstName, p2.lastName AS replyAuthorLastName, " +
        "CASE (k IS NOT NULL) WHEN true THEN true ELSE false END AS replyAuthorKnowsOriginalMessageAuthor " +
        "FROM MATCH (p1:Person) <-[:hasCreator]- (m:Message) <-[r:reply_of]- (c:Comment) -[:hasCreator]-> (p2:Person), " +
        "MATCH (p1) -[k:knows]-? (p2) " +  //I think the question mark goes there, not after "k", see also: https://pgql-lang.org/spec/1.3/#group-variables
        "WHERE m.id = " + messageId);

    log.info("Query 6 (IS7):\n{}\n\n", q6.getStatement());
  }





  /* ###################################################
                    Path Queries
  ######################################################  */

  /**
   * Query 7 (IC13)
   * Given two Persons, find the shortest path between these two Persons in the subgraph induced by
   * the knows relationships. Return the length of this path:
   * • −1: no path found
   * • 0: start person = end person
   * • > 0: path found (start person 6= end person)
   */
  private static void query7() throws PgqlException {
    long person2Id = 234L;

    //I'm not sure if e is null in case there is no path
    PgqlResult q7 = pgql.parse("SELECT CASE e IS NULL WHEN true THEN -1 ELSE COUNT(e) END AS shortestPathLength " +
        "FROM MATCH SHORTEST ( (p1:Person) -[e:knows]-* (p2:Person) ) " +
        "WHERE p1.id = " + personId + " AND p2.id = " + person2Id);
    log.info("Query 7 (IC13):\n{}\n\n", q7.getStatement());
  }


  /**
   * Query 8 (IC1)
   * Given a start Person, find Persons with a given first name (firstName) that the start Person is connected
   * to (excluding start Person) by at most 3 steps via the knows relationships. Return Persons,
   * including the distance (1..3), summaries of the Persons workplaces and places of study.
   */
  private static void query8() throws PgqlException {
    String firstNameQ8 = "Mary";

    PgqlResult q8 = pgql.parse("SELECT f.id AS friendId, f.lastName AS friendLastName, COUNT(e) AS distanceFromPerson, f.birthday as friendBirthday" +
        ", f.creationDate as friendCreationDate, f.gender as friendGender, f.browserUsed as friendBrowserUsed, f.locationIp as friendLocationIp" +
        ", f.email as friendEmails, f.speaks as friendLanguages,  c.name as friendCityName" +
        //PGQL lacks the ability to generate a list of the values, we can only aggregate one attribute at a time into a list/array:
        ", ARRAY_AGG(u.name) AS friendUniversityNames, ARRAY_AGG(es.classYear) AS friendUniversityYears, ARRAY_AGG(uc.name) AS friendUniversityCities" +
        ", ARRAY_AGG(co.name) AS friendCompanyNames, ARRAY_AGG(ew.workFrom) AS friendCompanyTimes, ARRAY_AGG(coc.name) AS friendCompanyCountries " +
        "FROM MATCH SHORTEST ( (p:Person) -/e:knows/-{1,3} (f:Person) )" +
        ", MATCH (f) -/:isLocatedIn/-> (c:City)" +
        ", MATCH (f) -/es:studyAt?/-> (u:University) -/:isLocatedIn/-> (uc:City)" +
        ", MATCH (f) -/ew:workAt?/-> (co:Company) -/:isLocatedIn/-> (coc:Country)" +
        "WHERE p.id = " + personId + " AND f.firstName = " + firstNameQ8 + " " +
        "ORDER BY distanceFromPerson ASC, friendLastName ASC, friendId ASC");

    log.info("Query 8 (IC1):\n{}\n\n", q8.getStatement());
  }


  /**
   * Query 9 (IS2)
   * Given a start Person, retrieve the last 10 Messages created by that user. For each Message, return that
   * Message, the original Post in its conversation (post), and the author of that Post (originalPoster).
   * If any of the Messages is a Post, then the original Post (post) will be the same Message, i.e. that
   * Message will appear twice in that result.
   */
  private static void query9() throws PgqlException {
    PgqlResult q9 = pgql.parse("SELECT m.id AS messageId, CASE (m.content IS NOT NULL) WHEN true THEN m.content ELSE m.imageFile END AS messageContent, m.creationDate AS messageCreationDate, " +
        "po.id as originalPostId, op.id as originalPostAuthorId, op.firstName AS originalPostAuthorFirstName, op.lastName AS originalPostAuthorLastName " +
        "FROM MATCH (p:Person) <-[e:hasCreator]- (m:Message) -/:replyOf*/-> (po:Post) -[:hasCreator]-> (op:Person) " + //we have to use reachability semantics, otherwise the query does not compile (i.e. -[:replyOf]->* does not work)
        "WHERE p.id = " + personId + " " +
        "ORDER BY messageCreationDate DESC LIMIT 10");
    // -/xxx/- denotes reachability whereas -[xxx]- denotes a normal query

    log.info("Query 9 (IS2):\n{}\n\n", q9.getStatement());
  }





  /* ###################################################
                    DML Queries
  ######################################################  */

  /**
   * Query 10 (INS1, IU1)
   * Add a Person node, connected to the network by 4 possible edge types.
   */
  private static void query10() throws PgqlException {
    //Query 10 - II1 - insert a new person
    String personFirstName = "Mary", personLastName = "Ann", gender = "F", birthday = "1990-01-01", creationDate = "2020-10-01 09:00:00",
        locationIP = "123.4.5.6", browserUsed = "Firefox", speaks = "German; English", email = "m.a@mail.com";
    long cityId = 123456L;
    //long[] tagIds = {12L, 34L}; //TODO ATTENTION: PGQL does NOT support something like UNWIND (Cypher) that would allow us to iterate over a list -> NOT DOABLE
    long tagId = 12L; //therefore, use a single ID s.t. query compiles
    long workId = 34L; //therefore, use a single ID s.t. query compiles
    int workFrom = 2010;

    //note that I added single ticks on every String such that they keep the capitalization and that's also what is done in the pgql Spec.
    PgqlResult q10 = pgql.parse("INSERT VERTEX p LABELS (Person) " +
        "PROPERTIES (p.id=" + personId + ", p.firstName='" + personFirstName + "', p.lastName='" + personLastName + "', p.gender='" + gender + "',p.birthday=DATE '" + birthday + "', " + //note the DATE support
          "p.creationDate=TIMESTAMP '" + creationDate + "', p.locationIp='" + locationIP + "', p.browserUsed='" + browserUsed + "', p.speaks='" + speaks + "', p.email='" + email + "'), " +
        "EDGE e BETWEEN p AND c LABELS (isLocatedIn), " +
        "EDGE ew BETWEEN p AND co LABELS (workAt) PROPERTIES (ew.workFrom=" + workFrom + "), " +
        "EDGE et BETWEEN p AND t LABELS (hasInterest) " +
        "FROM MATCH (c:City), MATCH (co:Company), MATCH (t:Tag) " +
        "WHERE c.id=" + cityId + " AND co.id=" + workId + " AND t.id=" + tagId);

    log.info("Query 10 (INS1):\n{}\n\n", q10.getStatement());
  }


  /**
   * Query 11 (DEL7)
   * Remove a Comment node and its edges (isLocatedIn, likes, hasCreator, hasTag). In addition, remove
   * all replies to the Comment connected by replyOf and their edges.
   */
  //note that PGQL automatically removes all incoming + outgoing edges on node removal
  private static void query11() throws PgqlException {
    long commentId = 789L;

    PgqlResult q11 = pgql.parse("DELETE c, r" +
        " FROM MATCH (c:Comment) <-/:replyOf*/- (r:Comment)" + //NOTE that the star here has to be inside (next to the label) on Reachability queries whereas it is outside on "normal" queries: <-[:replyOf]-*
        " WHERE c.id = " + commentId);

    log.info("Query 11 (DEL7):\n{}\n\n", q11.getStatement());
  }


  /**
   * Query 12 - custom query
   * Update the year (classYear) a given person graduated at a given university.
   */
  private static void query12() throws PgqlException {
    //TODO implement
    PgqlResult q12 = pgql.parse("");

    log.info("Query 12 (DEL7):\n{}\n\n", q12.getStatement());
  }


  /* ###################################################
                    DDL Query
  ######################################################  */

  /**
   * Query 13 - custom query
   * Add a street entity to the data model and change the relationship
   * Person[0..*]-isLocatedIn->[1]City to
   * Person[0..*]-isLocatedIn->[1]Street[0..*]-isLocatedIn->[1]City
   */
  private static void query13() throws PgqlException {
    /*This query creates the structure that we want on a new graph, we cannot however change an existing structure
      only drop the old one and create the whole graph structure again. */
    PgqlResult q13 = pgql.parse("CREATE PROPERTY GRAPH social_network " +
        "VERTEX TABLES (" +
          "Persons LABEL Person PROPERTIES (person_id, firstName, lastName), " + //exemplary properties
          "Streets LABEL Street PROPERTIES (streets_id, name, length), " +
          "Cities LABEL City PROPERTIES ARE ALL COLUMNS" +  //also possible
        ") " +
        "EDGE TABLES (" +
          "LivingAt " + // table name
            "SOURCE KEY (person_id) REFERENCES Persons " +
            "DESTINATION Streets " + //take foreign key automatically
            "LABEL isLocatedIn PROPERTIES (houseNumber)" +
          "IsLocatedIn " + //table name, street is located in city
            "SOURCE KEY (streets_id) REFERENCES Streets " +
            "DESTINATION Cities " +
            "LABEL isLocatedIn NO PROPERTIES" +
        ")");

    log.info("Query 13 (schema):\n{}\n\n", q13.getStatement());
  }
}
