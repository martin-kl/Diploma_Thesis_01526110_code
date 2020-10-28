package net.ellitron.ldbcsnbimpls.interactive.tools;

import net.ellitron.ldbcsnbimpls.interactive.titan.TitanDbConnectionState;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.T;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.count;

public class CustomQuery {
  static Logger log = LoggerFactory.getLogger(CustomQuery.class);
  private static GraphTraversalSource g;

  public static void main(String[] args) {
    TitanDbConnectionState connectionState = new TitanDbConnectionState(new HashMap<>()); //use default properties
    Graph graph = connectionState.getClient();
    g = graph.traversal();

    log.info("\t\tStarting Gremlin queries");

    //siq_selection1();
    //siq_selection2();
    siq_sum();

    log.info("\t\tDone");
  }



  /**
   * Query siq_selection1 (custom query) - select a person by their first and last name.
   * Attention: depending on the data and computing power, this query might take a while.
   */
  private static void siq_selection1() {
    String firstname = "Mahinda";
    String lastname = "Perera";
    Map<String, Object> result = g.V().has("person", "firstName", firstname).has("lastName", lastname).
        valueMap().next();
    log.info("Result of Query siq_selection1: " + result);
  }

  /**
   * Query siq_selection2 (IS4) - select the content of a message.
   */
  private static void siq_selection2() {
    Map<String, Object> values = g.V().has("post", "iid", "post:343597390421").
        valueMap("iid", "creationDate", "content", "imageFile").
        next();
    List<String> result = new LinkedList<>();
    result.add(((List<String>) values.get("iid")).get(0));
    result.add(((List<String>) values.get("creationDate")).get(0));
    if (((List<String>) values.get("content")).get(0).equals(""))
      result.add(((List<String>) values.get("imageFile")).get(0));
    else
      result.add(((List<String>) values.get("content")).get(0));
    log.info("Result of Query siq_selection2 (IS4): " + result);
  }

  /**
   * Query siq_summ (custom query) - calculate average number of spoken languages.
   */
  private static void siq_sum() {
    //Double mean = g.V().hasLabel("person").values("speaks").count().mean().next();

    //for a single person:
    //g.V().has('iid','person:933').properties('speaks').count()
    //Object tmp = g.V().has("person", "iid", "person:933").valueMap().next();
    Object result = g.V().has("person", "iid", "person:933").limit(5).properties("speaks").next();
    log.info("Result of Query siq_summ: " + result);
  }
}

