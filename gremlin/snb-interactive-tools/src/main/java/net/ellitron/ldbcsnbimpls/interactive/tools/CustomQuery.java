package net.ellitron.ldbcsnbimpls.interactive.tools;

import net.ellitron.ldbcsnbimpls.interactive.titan.TitanDbConnectionState;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.apache.tinkerpop.gremlin.process.traversal.Order.decr;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.*;

public class CustomQuery {
  static Logger log = LoggerFactory.getLogger(CustomQuery.class);
  private static GraphTraversalSource g;

  public static void main(String[] args) {
    TitanDbConnectionState connectionState = new TitanDbConnectionState(new HashMap<>()); //use default properties
    Graph graph = connectionState.getClient();
    g = graph.traversal();

    log.info("\t\tStarting Gremlin queries");

    /*
    query1();
    query2();
    query3();
    query4();
    query5();
    query6();
     */

    log.info("\t\tDone");
  }


  /**
   * ####################################################
   *        Queries from the Analysis Sections
   * ####################################################
   */

  /**
   * #####################################
   *     Structure independent queries
   * #####################################
   */

  /**
   * Query 1 (custom query) - select a person by their first and last name.
   * Attention: depending on the data and computing power, this query might take a while.
   */
  private static void query1() {
    String firstname = "Mahinda";
    String lastname = "Perera";
    Map<String, Object> result = g.V().has("person", "firstName", firstname).has("lastName", lastname).
        valueMap().next();
    log.info("Result of Query siq_selection1: " + result);
  }

  /**
   * Query 2 (IS4) - select the content of a message.
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
   * Query 3 (custom query) - calculate average number of spoken languages.
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

  /**
   * #####################################
   *       Pattern Matching queries
   * #####################################
   */

  private static void query4() {
    g.V().has("person", "iid", "person:933").as("person").out("isLocatedIn").as("city").
        select("person", "city").by(valueMap()).by("iid");

    //another version, this time using match():
    g.V().match(
        __.as("person").has("person", "iid", "person:933").out("isLocatedIn").as("city")
    ).select("person", "city").by(valueMap()).by("iid");
  }

  private static void query5() {
    g.V().has("person", "iid", "person:933").outE("knows").order().by("creationDate", decr).as("knows").otherV().as("a", "b", "c").select("a", "b", "c", "knows").by("iid").by("firstName").by("lastName").by("creationDate");
  }

  //IS7
  private static void query6() {
    String messageId = "post:618475290624";
    GraphTraversal<Vertex, Map<String, Object>> t = g.V().match(
        __.as("me").hasLabel("post").has("iid", messageId).out("hasCreator").as("cr"),
        __.as("me").in("replyOf").hasLabel("comment").as("co"),
        __.as("co").out("hasCreator").hasLabel("person").as("replyAuthor"),
        __.as("replyAuthor").out("knows").hasLabel("person").fold().as("friends")
    ).select("me", "co", "replyAuthor", "friends");
    while(t.hasNext()) {
      Map<String, Object> map = t.next();
      //we could check here whether "cr" is in "friends"
      log.info(map.toString());
    }
  }
}

