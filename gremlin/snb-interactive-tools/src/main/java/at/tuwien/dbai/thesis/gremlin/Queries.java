package at.tuwien.dbai.thesis.gremlin;

import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery1Result;
import net.ellitron.ldbcsnbimpls.interactive.titan.TitanDbConnectionState;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.apache.tinkerpop.gremlin.process.traversal.Order.decr;
import static org.apache.tinkerpop.gremlin.process.traversal.Order.incr;
import static org.apache.tinkerpop.gremlin.process.traversal.P.*;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;

/**
 * @author Martin Klampfer, 01526110
 */
public class Queries {
  static Logger log = LoggerFactory.getLogger(Queries.class);
  private static GraphTraversalSource g;

  public static void main(String[] args) {
    TitanDbConnectionState connectionState = new TitanDbConnectionState(new HashMap<>()); //use default properties
    Graph graph = connectionState.getClient();
    g = graph.traversal();

    log.info("\t\tStarting Gremlin queries");

    query1();
    query2();
    query3();
    query4();
    query5();
    query6();
    query7();
    query8();
    query9();
    query9_full();
    //query10(); //commented out as it changes the graph
    //query11(); //commented out as it changes the graph
    query12();
    query13();

    log.info("\t\tQueries finished, closing connection...");
    try {
      graph.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
    log.info("\t\tConnection closed.");
  }


  /* ###################################################
                  Structure Independent
  ######################################################  */

  /**
   * Query 1 - custom query
   * Select a person by their first and last name.
   */
  private static void query1() {
    String firstname = "Mahinda";
    String lastname = "Perera";
    Map<String, Object> result = g.V().has("person", "firstName", firstname).has("lastName", lastname).
        valueMap().next();
    log.info("Result of Query 1: " + result);
  }

  /**
   * Query 2 (IS4)
   * Given a Message, retrieve its content and creation date.
   */
  private static void query2() {
    //Map<String, Object> values = g.V().has("post", "iid", "post:343597390421").
    Map<String, Object> values = g.V().has(T.label, P.within("post", "comment")).has("iid", "post:343597390421").
        valueMap("iid", "creationDate", "content", "imageFile").
        next();
    List<String> result = new LinkedList<>();
    result.add(((List<String>) values.get("iid")).get(0));
    result.add(((List<String>) values.get("creationDate")).get(0));
    if (((List<String>) values.get("content")).get(0).equals(""))
      result.add(((List<String>) values.get("imageFile")).get(0));
    else
      result.add(((List<String>) values.get("content")).get(0));
    log.info("Result of Query 2 (IS4): " + result);
  }

  /**
   * Query 3 - custom query
   * Calculate average number of spoken languages.
   */
  private static void query3() {
    //Double mean = g.V().hasLabel("person").values("speaks").count().mean().next();

    //for a single person we can count the number of spoken languages:
    //g.V().has('iid','person:933').properties('speaks').count()
    //however I did not manage to get it for multiple persons, neither with properties, nor with group().by().by()
    //Object result = g.V().hasLabel("person").limit(10).properties("speaks").count(Scope.local).mean().next();

    //another approach that looks promising but does only calculate local speaks-size
    /*
    AtomicLong overall = new AtomicLong(0L);
    AtomicLong entries = new AtomicLong(0L);
    Map<Object, Object> ab = g.V().hasLabel("person").group().by("iid").by(properties("speaks").fold()).by(count(Scope.local)).next();
    ab.forEach((key, val) -> {
      overall.addAndGet((long) val);
      entries.getAndIncrement();
    });

    //again another approach (not working):
    // GraphTraversal<Vertex, Map<Object, Object>> tr1 = g.V().hasLabel("person").limit(10).group().by(T.id).by(properties("speaks").fold()).by(count(Scope.local));//.unfold().select("count");
    //that gives us a list of iid<->sumSpeaks but I did not manage to project to sumSpeaks
     */

    //again another possibility (working), now simply counting:
    /*
    long sumLangs = g.V().hasLabel("person").properties("speaks").count().next();
    long sumPersons = g.V().hasLabel("person").count().next();
    double res2 = sumLangs * 1.0 / sumPersons;
     */

    //working version, relies heavily on java computation
    Map<String, Object> map;
    long overall = 0L;
    int entries = 0;

    //this just limits the calculation to 100 persons, as it runs quite long for all
    //GraphTraversal<Vertex, Map<String, Object>> tr = g.V().hasLabel("person").limit(100).valueMap("speaks");
    //this is the final query:
    GraphTraversal<Vertex, Map<String, Object>> tr = g.V().hasLabel("person").valueMap("speaks");

    //now sum up and
    while (tr.hasNext()) {
      map = tr.next();
      overall += ((List<Object>) map.get("speaks")).size();
      entries++;
    }
    double result = overall * 1.0 / entries;
    log.info("Result of Query 3 (avg spoken languages): " + result);
  }


  /* ###################################################
                    Pattern Matching
  ######################################################  */

  /**
   * Query 4 (IS1)
   * Given a start Person, retrieve their first name, last name, birthday, IP address, browser, and city
   * of residence.
   */
  private static void query4() {
    String personId = "person:933";
    g.V().has("person", "iid", personId).as("person").out("isLocatedIn").as("city").
        select("person", "city").by(valueMap()).by("iid");

    //another version, this time using match():
    g.V().match(
        __.as("person").has("person", "iid", personId).out("isLocatedIn").as("city")
    ).select("person", "city").by(valueMap()).by("iid");
  }

  /**
   * Query 5 (IS3)
   * Given a start Person, retrieve all of their friends, and the date at which they became friends.
   */
  private static void query5() {
    String personId = "person:933";
    g.V().has("person", "iid", personId).
        outE("knows").order().by("creationDate", decr).as("knows").
        otherV().as("a", "b", "c").
        select("a", "b", "c", "knows").by("iid").by("firstName").by("lastName").by("creationDate");
  }


  /**
   * Query 6 (IS7)
   * Given a Message, retrieve the (1-hop) Comments that reply to it.
   * In addition, return a boolean flag knows indicating if the author of the reply (replyAuthor) knows
   * the author of the original message (messageAuthor). If author is same as original author, return
   * False for knows flag.
   */
  private static void query6() {
    String messageId = "post:618475290624";
    GraphTraversal<Vertex, Map<String, Object>> t = g.V().match(
        __.as("me").hasLabel("post").has("iid", messageId).out("hasCreator").values("iid").as("cr"),
        __.as("me").in("replyOf").hasLabel("comment").as("co"),
        __.as("co").out("hasCreator").hasLabel("person").as("replyAuthor"),
        __.as("replyAuthor").out("knows").hasLabel("person").values("iid").fold().as("friends")
    ).select("cr", "me", "co", "replyAuthor", "friends");
    while (t.hasNext()) {
      Map<String, Object> map = t.next();
      //extract comment id
      Vertex co = (Vertex) map.get("co");
      String commentId = co.<String>property("iid").value();
      //check here whether "cr" is in "friends":
      boolean knows;
      List<String> friends = (List<String>) map.get("friends");
      String crId = (String) map.get("cr");
      knows = friends.contains(crId);
      String result = knows ? "The two creators know each other!" : "The two creators do NOT know each other.";
      log.info("Comment with id {} -> {}", commentId, result);
    }
    log.info("Query 6 finished");
  }




  /* ###################################################
                    Path Queries
  ######################################################  */

  /**
   * Query 7 (IC13)
   * Given two Persons, find the shortest path between these two Persons in the
   * subgraph induced by the Knows relationships. Return the length of this
   * path. -1 should be returned if no path is found, and 0 should be returned
   * if the start person is the same as the end person.
   */
  //TODO Attention: this query cannot deal with the case that the two persons are not connected. In that case, it runs until it times out.
  private static void query7() {
    String person1Id = "person:933";
    String person2Id = "person:102";

    if (person1Id.equals(person2Id)) {
      log.info("Path length: " + 0 + ", as the two parameters are equal.");
    } else {
      /*
      //get the shortest path between them:
      Path p = g.V().has("person", "iid", person1Id).
          repeat(out("knows").simplePath()).until(has("person", "iid", person2Id)).
          path().by("iid").limit(1).next();
      log.info(p.toString());
      */

      //get just the length:
      long length = g.V().has("person", "iid", person1Id).
          repeat(out("knows").simplePath()).until(has("person", "iid", person2Id)).
          path().count(Scope.local).next();
      length = length - 1; //this is needed as length contains the number of nodes on that path as we count them
      log.info("Path length: " + length);
    }
  }

  /**
   * Query 8 (IC1)
   * Given two Persons, find the shortest path between these two Persons in the subgraph induced by
   * the knows relationships. Return the length of this path:
   * • −1: no path found
   * • 0: start person = end person
   * • > 0: path found (start person 6= end person)
   */
  /*TODO Attention: this query does not select the distance between person and the person with personId.
    Furthermore, if a person has no work or study related information, this query simply ignores that person.
    This could however be achieved by splitting the query into multiple queries where one selects the person's
    information and location, another one potential work related and another one study related info.
   */
  private static void query8() {
    String personId = "person:933";
    String firstName = "Karl";
    GraphTraversal<Vertex, Map<String, Object>> tr = g.V().has("person", "iid", personId).
        repeat(out("knows")).times(3).    //the argument of times is "maxLoops"->result also includes persons on distance 1 or 2
        dedup().has("firstName", firstName).as("p").
        match(
            __.as("p").out("isLocatedIn").as("locationCity"),
            //__.as("p").coalesce(out("workAt").out("isLocatedIn").path(), __.path()).as("companyPath"), //tryout with optional path, however did not work as expected
            __.as("p").out("workAt").as("company").out("isLocatedIn").as("companyCountry"),
            __.as("p").out("studyAt").as("university").out("isLocatedIn").as("universityCity")
            //).select("p", "locationCity", "companyPath", "university", "universityCity"); //for tryout with optional path
        ).select("p", "locationCity", "company", "companyCountry", "university", "universityCity").by(valueMap());
    while (tr.hasNext()) {
      Map<String, Object> map = tr.next();
      log.info(map.toString());
    }
    log.info("Query 8 finished.");
  }

  /**
   * Query 8 (IC1) - implementation from Jonathan Ellithorpe (jde@cs.stanford.edu) as a comparison to the simple version
   * from me (given above).
   * Note however that my query does not aggregate all expected values and should only be used as a simple example.
   */
  private static void query8_ellithorpe() {
    String personId = "person:933";
    String firstName = "Karl";
    int resultLimit = 10;

    List<Long> distList = new ArrayList<>(resultLimit);
    List<Vertex> matchList = new ArrayList<>(resultLimit);

    g.withSideEffect("x", matchList).withSideEffect("d", distList)
        .V().has("iid", personId)
        .aggregate("done").out("knows")
        .where(without("done")).dedup().fold().sideEffect(
        unfold().has("firstName", firstName).order()
            .by("lastName", incr).by(id(), incr).limit(resultLimit)
            .as("person")
            .select("x").by(count(Scope.local)).is(lt(resultLimit))
            .store("x").by(select("person"))
    ).filter(select("x").count(Scope.local).is(lt(resultLimit))
        .store("d")).unfold().aggregate("done").out("knows")
        .where(without("done")).dedup().fold().sideEffect(
        unfold().has("firstName", firstName).order()
            .by("lastName", incr).by(id(), incr).limit(resultLimit)
            .as("person")
            .select("x").by(count(Scope.local)).is(lt(resultLimit))
            .store("x").by(select("person"))
    ).filter(select("x").count(Scope.local).is(lt(resultLimit))
        .store("d")).unfold().aggregate("done").out("knows")
        .where(without("done")).dedup().fold().sideEffect(
        unfold().has("firstName", firstName).order()
            .by("lastName", incr).by(id(), incr).limit(resultLimit)
            .as("person")
            .select("x").by(count(Scope.local)).is(lt(resultLimit))
            .store("x").by(select("person"))
    ).select("x").count(Scope.local)
        .store("d").iterate();

    List<String> matchListIds = new ArrayList<>(matchList.size());
    matchList.forEach((v) -> {
      matchListIds.add(v.<String>property("iid").value());
    });

    Map<Vertex, Map<String, List<String>>> propertiesMap =
        new HashMap<>(matchList.size());
    g.V().has("iid", within(matchListIds)).as("person")
        .<List<String>>valueMap().as("props")
        .select("person", "props")
        .forEachRemaining(map -> {
          propertiesMap.put((Vertex) map.get("person"),
              (Map<String, List<String>>) map.get("props"));
        });

    Map<Vertex, String> placeNameMap = new HashMap<>(matchList.size());
    g.V().has("iid", within(matchListIds)).as("person")
        .out("isLocatedIn")
        .<String>values("name")
        .as("placeName")
        .select("person", "placeName")
        .forEachRemaining(map -> {
          placeNameMap.put((Vertex) map.get("person"),
              (String) map.get("placeName"));
        });

    Map<Vertex, List<List<Object>>> universityInfoMap =
        new HashMap<>(matchList.size());
    g.V().has("iid", within(matchListIds)).as("person")
        .outE("studyAt").as("classYear")
        .inV().as("universityName")
        .out("isLocatedIn").as("cityName")
        .select("person", "universityName", "classYear", "cityName")
        .by().by("name").by("classYear").by("name")
        .forEachRemaining(map -> {
          Vertex v = (Vertex) map.get("person");
          List<Object> tuple = new ArrayList<>(3);
          tuple.add(map.get("universityName"));
          tuple.add(Integer.decode((String) map.get("classYear")));
          tuple.add(map.get("cityName"));
          if (universityInfoMap.containsKey(v)) {
            universityInfoMap.get(v).add(tuple);
          } else {
            List<List<Object>> tupleList = new ArrayList<>();
            tupleList.add(tuple);
            universityInfoMap.put(v, tupleList);
          }
        });

    Map<Vertex, List<List<Object>>> companyInfoMap =
        new HashMap<>(matchList.size());
    g.V().has("iid", within(matchListIds)).as("person")
        .outE("workAt").as("workFrom")
        .inV().as("companyName")
        .out("isLocatedIn").as("cityName")
        .select("person", "companyName", "workFrom", "cityName")
        .by().by("name").by("workFrom").by("name")
        .forEachRemaining(map -> {
          Vertex v = (Vertex) map.get("person");
          List<Object> tuple = new ArrayList<>(3);
          tuple.add(map.get("companyName"));
          tuple.add(Integer.decode((String) map.get("workFrom")));
          tuple.add(map.get("cityName"));
          if (companyInfoMap.containsKey(v)) {
            companyInfoMap.get(v).add(tuple);
          } else {
            List<List<Object>> tupleList = new ArrayList<>();
            tupleList.add(tuple);
            companyInfoMap.put(v, tupleList);
          }
        });

    List<LdbcQuery1Result> result = new ArrayList<>();

    for (int i = 0; i < matchList.size(); i++) {
      Vertex match = matchList.get(i);
      int distance = (i < distList.get(0)) ? 1
          : (i < distList.get(1)) ? 2 : 3;
      Map<String, List<String>> properties = propertiesMap.get(match);
      List<String> emails = properties.get("email");
      if (emails == null) {
        emails = new ArrayList<>();
      }
      List<String> languages = properties.get("language");
      if (languages == null) {
        languages = new ArrayList<>();
      }
      String placeName = placeNameMap.get(match);
      List<List<Object>> universityInfo = universityInfoMap.get(match);
      if (universityInfo == null) {
        universityInfo = new ArrayList<>();
      }
      List<List<Object>> companyInfo = companyInfoMap.get(match);
      if (companyInfo == null) {
        companyInfo = new ArrayList<>();
      }
      result.add(new LdbcQuery1Result(
          getSNBId(match),
          properties.get("lastName").get(0),
          distance,
          Long.decode(properties.get("birthday").get(0)),
          Long.decode(properties.get("creationDate").get(0)),
          properties.get("gender").get(0),
          properties.get("browserUsed").get(0),
          properties.get("locationIP").get(0),
          emails,
          languages,
          placeName,
          universityInfo,
          companyInfo));
    }
  }

  /**
   * Helper method to extract the iid of a vertex, by Jonathan Ellithorpe (jde@cs.stanford.edu).
   * The iid is split and only the number-part is returned, e.g.: "person:933" -> return: 933L
   *
   * @param v the vertex
   * @return the iid of the vertex limited to the number part
   */
  private static Long getSNBId(Vertex v) {
    return Long.decode(v.<String>property("iid").value().split(":")[1]);
  }

  /**
   * Query 9 (IS2)
   * Given a start Person, retrieve the last 10 Messages created by that user. For each Message, return that
   * Message, the original Post in its conversation (post), and the author of that Post (originalPoster).
   * If any of the Messages is a Post, then the original Post (post) will be the same Message, i.e. that
   * Message will appear twice in that result.
   */
  /*Note that this query cannot deal with the special case that one of the 10 most recent messages is a post.
    This however can be achieved by splitting it into multiple queries as it is done in the next method.
    */
  private static void query9() {
    String personId = "person:933";

    GraphTraversal<Vertex, Map<String, Object>> tr = g.V().has("iid", personId).
        in("hasCreator").as("message").order().by("creationDate", decr).
        repeat(out("replyOf").simplePath()).until(hasLabel("post")).as("post").
        out("hasCreator").as("op").
        select("message", "post", "op").by(valueMap()).by("iid").by(valueMap("iid", "firstName", "lastName")).
        limit(10);
    while (tr.hasNext()) {
      Map<String, Object> map = tr.next();
      log.info(map.toString());
    }
    log.info("Query 9 finished.");
  }

  /**
   * Version of query 9 that is in line with the specification of the query in the LDBC SNB.
   */
  private static void query9_full() {
    String personId = "person:933";
    StringBuilder sb;

    GraphTraversal<Vertex, Object> tr = g.V().has("iid", personId).
        in("hasCreator").as("message").order().by("creationDate", decr).
        select("message").by("iid").limit(10);
    while (tr.hasNext()) {  //iterate over 10 most recent messages
      String messageIid = (String) tr.next();
      sb = new StringBuilder(messageIid + " | ");
      String[] parts = messageIid.split(":");
      if (parts[0].equals("post")) {    //post, gather infos
        Map<String, Object> map = g.V().has("iid", messageIid).as("p1", "p2", "p3").
            out("hasCreator").as("op1", "op2", "op3").
            select("p1", "p2", "p3", "op1", "op2", "op3").
            by("content").by("imageFile").by("creationDate").
            by("iid").by("firstName").by("lastName").
            next();
        String content = (String) map.get("p1");
        String imageFile = (String) map.get("p2");
        sb.append(content.equals("") ? imageFile : content).append(", "); //either imageFile or content
        sb.append(map.get("p3").toString()).append(" | ");

        sb.append(messageIid).append(", "); //again messageIid as this is already a post
        sb.append(map.get("op1").toString()).append(", ");
        sb.append(map.get("op2").toString()).append(", ");
        sb.append(map.get("op3").toString());
      } else {  //comment, move to post
        Map<String, Object> map = g.V().has("iid", messageIid).as("m1", "m2").
            repeat(out("replyOf").simplePath()).until(hasLabel("post")).as("post").
            out("hasCreator").as("op1", "op2", "op3").
            select("m1", "m2", "post", "op1", "op2", "op3").
            by("content").by("creationDate").
            by("iid").
            by("iid").by("firstName").by("lastName").
            next();
        for (Object s : map.values())
          sb.append(s.toString()).append(", ");
      }
      log.info(sb.toString());
    }
    log.info("Query 9 finished.");
  }



  /* ###################################################
                    DML Queries
  ######################################################  */

  /**
   * Query 10 (INS1, IU1) - inspired by the version of Jonathan Ellithorpe (jde@cs.stanford.edu)
   * Add a Person node, connected to the network by 4 possible edge types.
   */
  private static void query10() {
    String cityId = "place:1121";
    String[] tagIds = new String[]{"tag:9831", "tag:2327"};

    String[] univIds = new String[]{"organisation:5672"};
    int[] univYears = new int[]{1999};

    String[] workIds = new String[]{"organisation:925", "organisation:267"};
    int[] workYears = new int[]{2005, 2009};

    List<Object> personKeyValues = getPropertyList();

    // Add person
    Vertex person = g.addV(personKeyValues.toArray()).next();   //need to pass an array

    //examples on how to add and drop properties: via reference and in traversal
    /*
    person.property("ab", "test");
    person = g.V(person.id()).property("cd", "test1").next();
    g.tx().commit();
    person = g.V(person.id()).next();
    person.property("ab").remove();
    g.tx().commit();
    person = g.V(person.id()).next();
     */

    // Add edge to place
    Vertex place = g.V().has("iid", cityId).next();
    person.addEdge("isLocatedIn", place);

    // Add edges to tags
    for (String tagId : tagIds) {
      Vertex tagV = g.V().has("iid", tagId).next();
      person.addEdge("hasInterest", tagV);
    }

    // Add edges to universities
    List<Object> studiedAtKeyValues = new ArrayList<>(2);
    for (int i = 0; i < univIds.length; i++) {
      studiedAtKeyValues.clear();
      studiedAtKeyValues.add("classYear");
      studiedAtKeyValues.add(String.valueOf(univYears[i]));
      Vertex orgV = g.V().has("iid", univIds[i]).next();
      person.addEdge("studyAt", orgV, studiedAtKeyValues.toArray());
    }

    // Add edges to companies
    List<Object> workedAtKeyValues = new ArrayList<>(2);
    for (int i = 0; i < workIds.length; i++) {
      workedAtKeyValues.clear();
      workedAtKeyValues.add("workFrom");
      workedAtKeyValues.add(String.valueOf(workYears[i]));
      Vertex orgV = g.V().has("iid", workIds[i]).next();
      person.addEdge("workAt", orgV, workedAtKeyValues.toArray());
    }
    log.info("Added person with id {}", person.id());
  }

  /**
   * Helper method for query 10 that creates a list containing all properties and the label for the person to be inserted.
   *
   * @return the created list
   */
  private static List<Object> getPropertyList() {
    String personId = "person:1234567890", personFirstName = "John", personLastName = "Doe", gender = "male",
        locationIp = "123.456.789.012", browserUsed = "Firefox";
    String[] languages = new String[]{"en", "de"};
    String[] emails = new String[]{"john@doe.com"};
    Date birthday = new Date();

    // Build key value properties array
    List<Object> personKeyValues = new ArrayList<>(18 + 2 * languages.length + 2 * emails.length);
    personKeyValues.add("iid");
    personKeyValues.add(personId);
    personKeyValues.add(T.label);
    personKeyValues.add("person");
    personKeyValues.add("firstName");
    personKeyValues.add(personFirstName);
    personKeyValues.add("lastName");
    personKeyValues.add(personLastName);
    personKeyValues.add("gender");
    personKeyValues.add(gender);
    personKeyValues.add("birthday");
    personKeyValues.add(String.valueOf(birthday.getTime()));
    personKeyValues.add("creationDate");
    personKeyValues.add(String.valueOf(birthday.getTime())); //use also birthday
    personKeyValues.add("locationIP");
    personKeyValues.add(locationIp);
    personKeyValues.add("browserUsed");
    personKeyValues.add(browserUsed);
    for (String language : languages) {
      personKeyValues.add("language");
      personKeyValues.add(language);
    }
    for (String email : emails) {
      personKeyValues.add("email");
      personKeyValues.add(email);
    }
    return personKeyValues;
  }


  /**
   * Query 11 (DEL7)
   * Remove a Comment node and its edges (isLocatedIn, likes, hasCreator, hasTag). In addition, remove
   * all replies to the Comment connected by replyOf and their edges.
   */
  private static void query11() {
    String commentId = "comment:549756047855";
    g.V().has("iid", commentId).
        union(
            __(),   //identity function
            repeat(in("replyOf")).emit()  //emit all nodes on the way, not only leaves
        ).drop();
  }

  /**
   * Query 12 - custom query
   * Update the year (classYear) a given person graduated at a given university.
   */
  private static void query12() {
    String personId = "person:933";
    String univId = "organisation:2643";
    g.V().has("iid", personId).outE("studyAt").as("e").otherV().has("iid", univId).
        select("e").property("classYear", "2001");
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
  private static void query13() {
    //Not doable in Gremlin as there is no schema in TinkerPop.
    //However, TinkerPop enabled providers may require the specification of a schema even prior to inserting data.
  }
}
