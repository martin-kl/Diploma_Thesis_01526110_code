/* 
 * Copyright (C) 2015-2016 Stanford University
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.ellitron.ldbcsnbimpls.interactive.arangodb;

import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoCursor;
import com.arangodb.entity.BaseDocument;
import com.arangodb.model.AqlQueryOptions;
import com.arangodb.util.MapBuilder;

import com.ldbc.driver.control.LoggingService;
import com.ldbc.driver.Db;
import com.ldbc.driver.DbConnectionState;
import com.ldbc.driver.DbException;
import com.ldbc.driver.OperationHandler;
import com.ldbc.driver.ResultReporter;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcNoResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery1;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery10;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery10Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery11;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery11Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery12;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery12Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery13;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery13Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery14;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery14Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery1Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery2;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery2Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery3;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery3Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery4;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery4Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery5;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery5Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery6;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery6Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery7;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery7Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery8;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery8Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery9;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery9Result;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery1PersonProfile;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery1PersonProfileResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery2PersonPosts;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery2PersonPostsResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery3PersonFriends;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery3PersonFriendsResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery4MessageContent;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery4MessageContentResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery5MessageCreator;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery5MessageCreatorResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery6MessageForum;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery6MessageForumResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery7MessageReplies;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcShortQuery7MessageRepliesResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate1AddPerson;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate1AddPerson.Organization;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate2AddPostLike;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate3AddCommentLike;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate4AddForum;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate5AddForumMembership;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate6AddPost;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate7AddComment;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate8AddFriendship;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * An implementation of the LDBC SNB interactive workload[1] for ArangoDB. Queries
 * are executed against a running ArangoDB cluster. Configuration parameters for
 * this implementation (that are supplied via the LDBC driver) are listed
 * below.
 * 
 * Configuration Parameters:
 * 
 * host - IP address of an ArangoDB server (default: 127.0.0.1).
 * port - Port of the ArangoDB server (default: 8529).
 * graphName - Name of the graph to use (default: default).
 * 
 * References:
 * [1]: Prat, Arnau (UPC) and Boncz, Peter (VUA) and Larriba, Josep Lluís (UPC)
 * and Angles, Renzo (TALCA) and Averbuch, Alex (NEO) and Erling, Orri (OGL)
 * and Gubichev, Andrey (TUM) and Spasić, Mirko (OGL) and Pham, Minh-Duc (VUA)
 * and Martínez, Norbert (SPARSITY). "LDBC Social Network Benchmark (SNB) -
 * v0.2.2 First Public Draft Release". http://www.ldbcouncil.org/.
 *
 * @author Jonathan Ellithorpe (jde@cs.stanford.edu)
 */
public class ArangoDb extends Db {

  private DbConnectionState connectionState = null;

  @Override
  protected DbConnectionState getConnectionState() throws DbException {
    return connectionState;
  }

  @Override
  protected void onClose() throws IOException {
    connectionState.close();
  }

  @Override
  protected void onInit(Map<String, String> properties,
      LoggingService loggingService) throws DbException {

    connectionState = new ArangoDbConnectionState(properties);

    /*
     * Register operation handlers with the benchmark.
     */
    registerOperationHandler(LdbcQuery1.class,
        LdbcQuery1Handler.class);
    registerOperationHandler(LdbcQuery2.class,
        LdbcQuery2Handler.class);
    registerOperationHandler(LdbcQuery3.class,
        LdbcQuery3Handler.class);
    registerOperationHandler(LdbcQuery4.class,
        LdbcQuery4Handler.class);
    registerOperationHandler(LdbcQuery5.class,
        LdbcQuery5Handler.class);
    registerOperationHandler(LdbcQuery6.class,
        LdbcQuery6Handler.class);
    registerOperationHandler(LdbcQuery7.class,
        LdbcQuery7Handler.class);
    registerOperationHandler(LdbcQuery8.class,
        LdbcQuery8Handler.class);
    registerOperationHandler(LdbcQuery9.class,
        LdbcQuery9Handler.class);
    registerOperationHandler(LdbcQuery10.class,
        LdbcQuery10Handler.class);
    registerOperationHandler(LdbcQuery11.class,
        LdbcQuery11Handler.class);
    registerOperationHandler(LdbcQuery12.class,
        LdbcQuery12Handler.class);
    registerOperationHandler(LdbcQuery13.class,
        LdbcQuery13Handler.class);
    registerOperationHandler(LdbcQuery14.class,
        LdbcQuery14Handler.class);

    registerOperationHandler(LdbcShortQuery1PersonProfile.class,
        LdbcShortQuery1PersonProfileHandler.class);
    registerOperationHandler(LdbcShortQuery2PersonPosts.class,
        LdbcShortQuery2PersonPostsHandler.class);
    registerOperationHandler(LdbcShortQuery3PersonFriends.class,
        LdbcShortQuery3PersonFriendsHandler.class);
    registerOperationHandler(LdbcShortQuery4MessageContent.class,
        LdbcShortQuery4MessageContentHandler.class);
    registerOperationHandler(LdbcShortQuery5MessageCreator.class,
        LdbcShortQuery5MessageCreatorHandler.class);
    registerOperationHandler(LdbcShortQuery6MessageForum.class,
        LdbcShortQuery6MessageForumHandler.class);
    registerOperationHandler(LdbcShortQuery7MessageReplies.class,
        LdbcShortQuery7MessageRepliesHandler.class);

    registerOperationHandler(LdbcUpdate1AddPerson.class,
        LdbcUpdate1AddPersonHandler.class);
    registerOperationHandler(LdbcUpdate2AddPostLike.class,
        LdbcUpdate2AddPostLikeHandler.class);
    registerOperationHandler(LdbcUpdate3AddCommentLike.class,
        LdbcUpdate3AddCommentLikeHandler.class);
    registerOperationHandler(LdbcUpdate4AddForum.class,
        LdbcUpdate4AddForumHandler.class);
    registerOperationHandler(LdbcUpdate5AddForumMembership.class,
        LdbcUpdate5AddForumMembershipHandler.class);
    registerOperationHandler(LdbcUpdate6AddPost.class,
        LdbcUpdate6AddPostHandler.class);
    registerOperationHandler(LdbcUpdate7AddComment.class,
        LdbcUpdate7AddCommentHandler.class);
    registerOperationHandler(LdbcUpdate8AddFriendship.class,
        LdbcUpdate8AddFriendshipHandler.class);
  }

  /**
   * ------------------------------------------------------------------------
   * Complex Queries
   * ------------------------------------------------------------------------
   */
  /**
   * Given a start Person, find up to 20 Persons with a given first name that
   * the start Person is connected to (excluding start Person) by at most 3
   * steps via Knows relationships. Return Persons, including summaries of the
   * Persons workplaces and places of study. Sort results ascending by their
   * distance from the start Person, for Persons within the same distance sort
   * ascending by their last name, and for Persons with same last name
   * ascending by their identifier.[1]
   */
  public static class LdbcQuery1Handler
      implements OperationHandler<LdbcQuery1, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcQuery1Handler.class);

    @Override
    public void executeOperation(LdbcQuery1 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      String statement = "";

      // Execute the query and get the results.
      List<LdbcQuery1Result> resultList = new ArrayList<>();

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * Given a start Person, find (most recent) Posts and Comments from all of
   * that Person’s friends, that were created before (and including) a given
   * date. Return the top 20 Posts/Comments, and the Person that created each
   * of them. Sort results descending by creation date, and then ascending by
   * Post identifier.[1]
   */
  public static class LdbcQuery2Handler
      implements OperationHandler<LdbcQuery2, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcQuery2Handler.class);

    @Override
    public void executeOperation(LdbcQuery2 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      String statement = "";

      // Execute the query and get the results.
      List<LdbcQuery2Result> resultList = new ArrayList<>();

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * Given a start Person, find Persons that are their friends and friends of
   * friends (excluding start Person) that have made Posts/Comments in both of
   * the given Countries, X and Y, within a given period. Only Persons that are
   * foreign to Countries X and Y are considered, that is Persons whose
   * Location is not Country X or Country Y. Return top 20 Persons, and their
   * Post/Comment counts, in the given countries and period. Sort results
   * descending by total number of Posts/Comments, and then ascending by Person
   * identifier.[1]
   */
  public static class LdbcQuery3Handler
      implements OperationHandler<LdbcQuery3, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcQuery3Handler.class);

    @Override
    public void executeOperation(LdbcQuery3 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      long periodStart = operation.startDate().getTime();
      long periodEnd = periodStart
          + ((long) operation.durationDays()) * 24l * 60l * 60l * 1000l;

      String statement = "";

      // Execute the query and get the results.
      List<LdbcQuery3Result> resultList = new ArrayList<>();

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * Given a start Person, find Tags that are attached to Posts that were
   * created by that Person’s friends. Only include Tags that were attached to
   * friends’ Posts created within a given time interval, and that were never
   * attached to friends’ Posts created before this interval. Return top 10
   * Tags, and the count of Posts, which were created within the given time
   * interval, that this Tag was attached to. Sort results descending by Post
   * count, and then ascending by Tag name.[1]
   */
  public static class LdbcQuery4Handler
      implements OperationHandler<LdbcQuery4, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcQuery4Handler.class);

    @Override
    public void executeOperation(LdbcQuery4 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      long periodStart = operation.startDate().getTime();
      long periodEnd = periodStart
          + ((long) operation.durationDays()) * 24l * 60l * 60l * 1000l;

      String statement = "";

      // Execute the query and get the results.
      List<LdbcQuery4Result> resultList = new ArrayList<>();

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * Given a start Person, find the Forums which that Person’s friends and
   * friends of friends (excluding start Person) became Members of after a
   * given date. Return top 20 Forums, and the number of Posts in each Forum
   * that was Created by any of these Persons. For each Forum consider only
   * those Persons which joined that particular Forum after the given date.
   * Sort results descending by the count of Posts, and then ascending by Forum
   * identifier.[1]
   */
  public static class LdbcQuery5Handler
      implements OperationHandler<LdbcQuery5, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcQuery5Handler.class);

    @Override
    public void executeOperation(LdbcQuery5 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      String statement = "";

      // Execute the query and get the results.
      List<LdbcQuery5Result> resultList = new ArrayList<>();

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * Given a start Person and some Tag, find the other Tags that occur together
   * with this Tag on Posts that were created by start Person’s friends and
   * friends of friends (excluding start Person). Return top 10 Tags, and the
   * count of Posts that were created by these Persons, which contain both this
   * Tag and the given Tag. Sort results descending by count, and then
   * ascending by Tag name.[1]
   */
  public static class LdbcQuery6Handler
      implements OperationHandler<LdbcQuery6, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcQuery6Handler.class);

    @Override
    public void executeOperation(LdbcQuery6 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      String statement = "";

      // Execute the query and get the results.
      List<LdbcQuery6Result> resultList = new ArrayList<>();

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * Given a start Person, find (most recent) Likes on any of start Person’s
   * Posts/Comments. Return top 20 Persons that Liked any of start Person’s
   * Posts/Comments, the Post/Comment they liked most recently, creation date
   * of that Like, and the latency (in minutes) between creation of
   * Post/Comment and Like. Additionally, return a flag indicating whether the
   * liker is a friend of start Person. In the case that a Person Liked
   * multiple Posts/Comments at the same time, return the Post/Comment with
   * lowest identifier. Sort results descending by creation time of Like, then
   * ascending by Person identifier of liker.[1]
   */
  public static class LdbcQuery7Handler
      implements OperationHandler<LdbcQuery7, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcQuery7Handler.class);

    @Override
    public void executeOperation(LdbcQuery7 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      String statement = "";

      // Execute the query and get the results.
      List<LdbcQuery7Result> resultList = new ArrayList<>();

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * Given a start Person, find (most recent) Comments that are replies to
   * Posts/Comments of the start Person. Only consider immediate (1-hop)
   * replies, not the transitive (multi-hop) case. Return the top 20 reply
   * Comments, and the Person that created each reply Comment. Sort results
   * descending by creation date of reply Comment, and then ascending by
   * identifier of reply Comment.[1]
   */
  public static class LdbcQuery8Handler
      implements OperationHandler<LdbcQuery8, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcQuery8Handler.class);

    @Override
    public void executeOperation(LdbcQuery8 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      String statement = "";

      // Execute the query and get the results.
      List<LdbcQuery8Result> resultList = new ArrayList<>();

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * Given a start Person, find the (most recent) Posts/Comments created by
   * that Person’s friends or friends of friends (excluding start Person). Only
   * consider the Posts/Comments created before a given date (excluding that
   * date). Return the top 20 Posts/Comments, and the Person that created each
   * of those Posts/Comments. Sort results descending by creation date of
   * Post/Comment, and then ascending by Post/Comment identifier.[1]
   */
  public static class LdbcQuery9Handler
      implements OperationHandler<LdbcQuery9, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcQuery9Handler.class);

    @Override
    public void executeOperation(LdbcQuery9 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      String statement = "";

      // Execute the query and get the results.
      List<LdbcQuery9Result> resultList = new ArrayList<>();

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * Given a start Person, find that Person’s friends of friends (excluding
   * start Person, and immediate friends), who were born on or after the 21st
   * of a given month (in any year) and before the 22nd of the following month.
   * Calculate the similarity between each of these Persons and start Person,
   * where similarity for any Person is defined as follows:
   * <ul>
   * <li>common = number of Posts created by that Person, such that the Post
   * has a Tag that start Person is Interested in</li>
   * <li>uncommon = number of Posts created by that Person, such that the Post
   * has no Tag that start Person is Interested in</li>
   * <li>similarity = common - uncommon</li>
   * </ul>
   * Return top 10 Persons, their Place, and their similarity score. Sort
   * results descending by similarity score, and then ascending by Person
   * identifier.[1]
   */
  public static class LdbcQuery10Handler
      implements OperationHandler<LdbcQuery10, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcQuery10Handler.class);

    @Override
    public void executeOperation(LdbcQuery10 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      String statement = "";

      // Execute the query and get the results.
      List<LdbcQuery10Result> resultList = new ArrayList<>();

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * Given a start Person, find that Person’s friends and friends of friends
   * (excluding start Person) who started Working in some Company in a given
   * Country, before a given date (year). Return top 10 Persons, the Company
   * they worked at, and the year they started working at that Company. Sort
   * results ascending by the start date, then ascending by Person identifier,
   * and lastly by Organization name descending.[1]
   */
  public static class LdbcQuery11Handler
      implements OperationHandler<LdbcQuery11, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcQuery11Handler.class);

    @Override
    public void executeOperation(LdbcQuery11 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      String statement = "";

      // Execute the query and get the results.
      List<LdbcQuery11Result> resultList = new ArrayList<>();

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * Given a start Person, find the Comments that this Person’s friends made in
   * reply to Posts, considering only those Comments that are immediate (1-hop)
   * replies to Posts, not the transitive (multi-hop) case. Only consider Posts
   * with a Tag in a given TagClass or in a descendent of that TagClass. Count
   * the number of these reply Comments, and collect the Tags (with valid tag
   * class) that were attached to the Posts they replied to. Return top 20
   * Persons with at least one reply, the reply count, and the collection of
   * Tags. Sort results descending by Comment count, and then ascending by
   * Person identifier.[1]
   */
  public static class LdbcQuery12Handler
      implements OperationHandler<LdbcQuery12, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcQuery12Handler.class);

    @Override
    public void executeOperation(LdbcQuery12 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      String statement = "";

      // Execute the query and get the results.
      List<LdbcQuery12Result> resultList = new ArrayList<>();

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * Given two Persons, find the shortest path between these two Persons in the
   * subgraph induced by the Knows relationships. Return the length of this
   * path. -1 should be returned if no path is found, and 0 should be returned
   * if the start person is the same as the end person.[1]
   */
  public static class LdbcQuery13Handler
      implements OperationHandler<LdbcQuery13, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcQuery13Handler.class);

    @Override
    public void executeOperation(LdbcQuery13 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      String statement = "";

      // Execute the query and get the results.
      resultReporter.report(0, null, operation);
    }
  }

  /**
   * Given two Persons, find all (unweighted) shortest paths between these two
   * Persons, in the subgraph induced by the Knows relationship. Then, for each
   * path calculate a weight. The nodes in the path are Persons, and the weight
   * of a path is the sum of weights between every pair of consecutive Person
   * nodes in the path. The weight for a pair of Persons is calculated such
   * that every reply (by one of the Persons) to a Post (by the other Person)
   * contributes 1.0, and every reply (by ones of the Persons) to a Comment (by
   * the other Person) contributes 0.5. Return all the paths with shortest
   * length, and their weights. Sort results descending by path weight. The
   * order of paths with the same weight is unspecified.[1]
   */
  public static class LdbcQuery14Handler
      implements OperationHandler<LdbcQuery14, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcQuery14Handler.class);

    @Override
    public void executeOperation(LdbcQuery14 operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      String statement = "";

      // Execute the query and get the results.
      List<LdbcQuery14Result> resultList = new ArrayList<>();

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * ------------------------------------------------------------------------
   * Short Queries
   * ------------------------------------------------------------------------
   */
  /**
   * Given a start Person, retrieve their first name, last name, birthday, IP
   * address, browser, and city of residence.[1]
   */
  public static class LdbcShortQuery1PersonProfileHandler implements
      OperationHandler<LdbcShortQuery1PersonProfile, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcShortQuery1PersonProfileHandler.class);

    @Override
    public void executeOperation(LdbcShortQuery1PersonProfile operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase(); 
      String statement = 
					"WITH Place"
					+ " FOR p IN Person"
          + " FILTER p._key == @personId"
          + "   FOR c IN 1..1 OUTBOUND p isLocatedIn"
          + " RETURN {"
          + "   firstName: p.firstName,"
          + "   lastName: p.lastName,"
          + "   birthday: p.birthday,"
          + "   locationIP: p.locationIP,"
          + "   browserUsed: p.browserUsed,"
          + "   cityId: c._key,"
          + "   gender: p.gender,"
          + "   creationDate: p.creationDate"
          + "  }";

      ArangoCursor<BaseDocument> cursor = db.query(
          statement,
					new MapBuilder()
              .put("personId", String.valueOf(operation.personId()))
              .get(),
					new AqlQueryOptions(),
					BaseDocument.class
				);

      if (cursor.hasNext()) {
        BaseDocument doc = cursor.next();

        resultReporter.report(0, 
            new LdbcShortQuery1PersonProfileResult(
                (String)doc.getAttribute("firstName"),
                (String)doc.getAttribute("lastName"),
                (Long)doc.getAttribute("birthday"),
                (String)doc.getAttribute("locationIP"),
                (String)doc.getAttribute("browserUsed"),
                Long.decode((String)doc.getAttribute("cityId")),
                (String)doc.getAttribute("gender"),
                (Long)doc.getAttribute("creationDate")),
              operation);
      } else {
        resultReporter.report(0, null, operation);
      }
    }
  }

  /**
   * Given a start Person, retrieve the last 10 Messages (Posts or Comments)
   * created by that user. For each message, return that message, the original
   * post in its conversation, and the author of that post. If any of the
   * Messages is a Post, then the original Post will be the same Message, i.e.,
   * that Message will appear twice in that result. Order results descending by
   * message creation date, then descending by message identifier.[1]
   */
  public static class LdbcShortQuery2PersonPostsHandler implements
      OperationHandler<LdbcShortQuery2PersonPosts, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcShortQuery2PersonPostsHandler.class);

    @Override
    public void executeOperation(LdbcShortQuery2PersonPosts operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();
      String statement =
					"WITH Message"
					+ " FOR person IN Person"
          + " FILTER person._key == @personId"
          + "   FOR message IN 1..1 INBOUND person hasCreator"
          + "     SORT message.creationDate DESC, TO_NUMBER(message._key) DESC"
          + "     LIMIT @limit"
          + "     FOR originalPost IN 0..1024 OUTBOUND message replyOf"
          + "       FILTER originalPost.type == \"Post\""
          + "         FOR originalPostAuthor IN 1..1 OUTBOUND originalPost hasCreator"
          + " RETURN {"
          + "   messageId: message._key,"
          + "   messageContent: message.content,"
          + "   messageImageFile: message.imageFile,"
          + "   messageCreationDate: message.creationDate,"
          + "   originalPostId: originalPost._key,"
          + "   originalPostAuthorId: originalPostAuthor._key,"
          + "   originalPostAuthorFirstName: originalPostAuthor.firstName,"
          + "   originalPostAuthorLastName: originalPostAuthor.lastName"
          + "  }";

      ArangoCursor<BaseDocument> cursor = db.query(
          statement,
					new MapBuilder()
              .put("personId", String.valueOf(operation.personId()))
              .put("limit", new Integer(operation.limit()))
              .get(),
					new AqlQueryOptions(),
					BaseDocument.class
				);

      List<LdbcShortQuery2PersonPostsResult> resultList = new ArrayList<>();

      while (cursor.hasNext()) {
        BaseDocument doc = cursor.next();

        String content = (String)doc.getAttribute("messageContent");
        if (content == null) {
          content = (String)doc.getAttribute("messageImageFile");
        }

        resultList.add(new LdbcShortQuery2PersonPostsResult(
            Long.valueOf((String)doc.getAttribute("messageId")),
            content,
            (Long)doc.getAttribute("messageCreationDate"),
            Long.valueOf((String)doc.getAttribute("originalPostId")),
            Long.valueOf((String)doc.getAttribute("originalPostAuthorId")),
            (String)doc.getAttribute("originalPostAuthorFirstName"),
            (String)doc.getAttribute("originalPostAuthorLastName")));
      }

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * Given a start Person, retrieve all of their friends, and the date at which
   * they became friends. Order results descending by friendship creation date,
   * then ascending by friend identifier.[1]
   */
  public static class LdbcShortQuery3PersonFriendsHandler implements
      OperationHandler<LdbcShortQuery3PersonFriends, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcShortQuery3PersonFriendsHandler.class);

    @Override
    public void executeOperation(LdbcShortQuery3PersonFriends operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();
      String statement = "WITH Person"
                         + " FOR person IN Person"
                         + " FILTER person._key == @personId"
                         + "   FOR friend, knows_edge IN 1..1 OUTBOUND person knows"
                         + "     SORT knows_edge.creationDate DESC, TO_NUMBER(friend._key) ASC"
                         + " RETURN {"
                         + "   friendId: friend._key,"
                         + "   firstName: friend.firstName,"
                         + "   lastName: friend.lastName,"
                         + "   friendshipCreationDate: knows_edge.creationDate"
                         + " }";

      ArangoCursor<BaseDocument> cursor = db.query(
          statement,
					new MapBuilder()
              .put("personId", String.valueOf(operation.personId()))
              .get(),
					new AqlQueryOptions(),
					BaseDocument.class
				);

      List<LdbcShortQuery3PersonFriendsResult> resultList = new ArrayList<>();

      while (cursor.hasNext()) {
        BaseDocument doc = cursor.next();

        resultList.add(new LdbcShortQuery3PersonFriendsResult(
            Long.valueOf((String)doc.getAttribute("friendId")),
            (String)doc.getAttribute("firstName"),
            (String)doc.getAttribute("lastName"),
            (Long)doc.getAttribute("friendshipCreationDate")));
      }

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * Given a Message (Post or Comment), retrieve its content and creation
   * date.[1]
   */
  public static class LdbcShortQuery4MessageContentHandler implements
      OperationHandler<LdbcShortQuery4MessageContent, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcShortQuery4MessageContentHandler.class);

    @Override
    public void executeOperation(LdbcShortQuery4MessageContent operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();
      String statement = "WITH Message"
                         + " FOR message IN Message"
                         + " FILTER message._key == @messageId"
                         + " RETURN {"
                         + "   messageContent: message.content,"
                         + "   messageImageFile: message.imageFile,"
                         + "   messageCreationDate: message.creationDate"
                         + "  }";
      
      ArangoCursor<BaseDocument> cursor = db.query(
          statement,
					new MapBuilder()
              .put("messageId", String.valueOf(operation.messageId()))
              .get(),
					new AqlQueryOptions(),
					BaseDocument.class
				);

      if (cursor.hasNext()) {
        BaseDocument doc = cursor.next();

        String content = (String)doc.getAttribute("messageContent");
        if (content == null) {
          content = (String)doc.getAttribute("messageImageFile");
        }

        resultReporter.report(
            0, 
            new LdbcShortQuery4MessageContentResult(
                content,
                (Long)doc.getAttribute("messageCreationDate")), 
            operation);
      } else {
        resultReporter.report(0, null, operation);
      }
    }
  }

  /**
   * Given a Message (Post or Comment), retrieve its author.[1]
   */
  public static class LdbcShortQuery5MessageCreatorHandler implements
      OperationHandler<LdbcShortQuery5MessageCreator, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcShortQuery5MessageCreatorHandler.class);

    @Override
    public void executeOperation(LdbcShortQuery5MessageCreator operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();
      String statement = "WITH Message, Person"
                         + " FOR message IN Message"
                         + " FILTER message._key == @messageId"
                         + "   FOR author IN 1..1 OUTBOUND message hasCreator"
                         + " RETURN {"
                         + "   authorId: author._key,"
                         + "   firstName: author.firstName,"
                         + "   lastName: author.lastName"
                         + "  }";
      
      ArangoCursor<BaseDocument> cursor = db.query(
          statement,
					new MapBuilder()
              .put("messageId", String.valueOf(operation.messageId()))
              .get(),
					new AqlQueryOptions(),
					BaseDocument.class
				);

      if (cursor.hasNext()) {
        BaseDocument doc = cursor.next();

        resultReporter.report(
            0, 
            new LdbcShortQuery5MessageCreatorResult(
                Long.valueOf((String)doc.getAttribute("authorId")),
                (String)doc.getAttribute("firstName"),
                (String)doc.getAttribute("lastName")), 
            operation);
      } else {
        resultReporter.report(0, null, operation);
      }
    }
  }

  /**
   * Given a Message (Post or Comment), retrieve the Forum that contains it and
   * the Person that moderates that forum. Since comments are not directly
   * contained in forums, for comments, return the forum containing the
   * original post in the thread which the comment is replying to.[1]
   */
  public static class LdbcShortQuery6MessageForumHandler implements
      OperationHandler<LdbcShortQuery6MessageForum, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcShortQuery6MessageForumHandler.class);

    @Override
    public void executeOperation(LdbcShortQuery6MessageForum operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();
      String statement = "WITH Message, Forum, Person"
                         + "  FOR message IN Message"
                         + "    FILTER message._key == @messageId"
                         + "    FOR originalPost IN 0..1024 OUTBOUND message replyOf"
                         + "      FILTER originalPost.type == \"Post\""
                         + "      FOR forum IN 1..1 INBOUND originalPost containerOf"
                         + "        FOR moderator IN 1..1 OUTBOUND forum hasModerator"
                         + "  RETURN {"
                         + "    forumId: forum._key,"
                         + "    forumTitle: forum.title,"
                         + "    moderatorId: moderator._key,"
                         + "    moderatorFirstName: moderator.firstName,"
                         + "    moderatorLastName: moderator.lastName"
                         + "  }";

      ArangoCursor<BaseDocument> cursor = db.query(
          statement,
					new MapBuilder()
              .put("messageId", String.valueOf(operation.messageId()))
              .get(),
					new AqlQueryOptions(),
					BaseDocument.class
				);

      if (cursor.hasNext()) {
        BaseDocument doc = cursor.next();

        resultReporter.report(
            0, 
            new LdbcShortQuery6MessageForumResult(
                Long.valueOf((String)doc.getAttribute("forumId")),
                (String)doc.getAttribute("forumTitle"),
                Long.valueOf((String)doc.getAttribute("moderatorId")),
                (String)doc.getAttribute("moderatorFirstName"),
                (String)doc.getAttribute("moderatorLastName")),
            operation);
      } else {
        resultReporter.report(0, null, operation);
      }
    }
  }

  /**
   * Given a Message (Post or Comment), retrieve the (1-hop) Comments that
   * reply to it. In addition, return a boolean flag indicating if the author
   * of the reply knows the author of the original message. If author is same
   * as original author, return false for "knows" flag. Order results
   * descending by creation date, then ascending by author identifier.[1]
   */
  public static class LdbcShortQuery7MessageRepliesHandler implements
      OperationHandler<LdbcShortQuery7MessageReplies, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcShortQuery7MessageRepliesHandler.class);

    @Override
    public void executeOperation(LdbcShortQuery7MessageReplies operation,
        DbConnectionState dbConnectionState,
        ResultReporter resultReporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();
      String statement = "WITH Message, Person"
                         + " LET authorFriends = ("
                         + "   FOR message IN Message"
                         + "     FILTER message._key == @messageId"
                         + "     FOR author IN 1..1 OUTBOUND message hasCreator"
                         + "       FOR authorFriend IN 1..1 OUTBOUND author knows"
                         + "   RETURN authorFriend"
                         + " )"
                         + " "
                         + " FOR message IN Message"
                         + "   FILTER message._key == @messageId"
                         + "   FOR reply IN 1..1 INBOUND message replyOf"
                         + "     FOR replyAuthor IN 1..1 OUTBOUND reply hasCreator"
                         + " SORT reply.creationDate DESC, TO_NUMBER(replyAuthor._key) ASC"
                         + " RETURN {"
                         + "   replyId: reply._key,"
                         + "   replyContent: reply.content,"
                         + "   replyCreationDate: reply.creationDate,"
                         + "   replyAuthorId: replyAuthor._key,"
                         + "   replyAuthorFirstName: replyAuthor.firstName,"
                         + "   replyAuthorLastName: replyAuthor.lastName,"
                         + "   replyAuthorIsFriend: replyAuthor._key IN authorFriends[*]._key"
                         + " }";

      ArangoCursor<BaseDocument> cursor = db.query(
          statement,
					new MapBuilder()
              .put("messageId", String.valueOf(operation.messageId()))
              .get(),
					new AqlQueryOptions(),
					BaseDocument.class
				);

      List<LdbcShortQuery7MessageRepliesResult> resultList = new ArrayList<>();

      while (cursor.hasNext()) {
        BaseDocument doc = cursor.next();

        System.out.println(doc);
        resultList.add(new LdbcShortQuery7MessageRepliesResult(
            Long.valueOf((String)doc.getAttribute("replyId")),
            (String)doc.getAttribute("replyContent"),
            (Long)doc.getAttribute("replyCreationDate"),
            Long.valueOf((String)doc.getAttribute("replyAuthorId")),
            (String)doc.getAttribute("replyAuthorFirstName"),
            (String)doc.getAttribute("replyAuthorLastName"),
            (Boolean)doc.getAttribute("replyAuthorIsFriend")));
      }

      resultReporter.report(0, resultList, operation);
    }
  }

  /**
   * ------------------------------------------------------------------------
   * Update Queries
   * ------------------------------------------------------------------------
   */
  /**
   * Add a Person to the social network. [1]
   * 
   * This query involves creating many relationships of different types.
   * This is currently done using multiple cypher queries, but it may be
   * possible to combine them in some way to amortize per query overhead and
   * thus increase performance.
   */
  public static class LdbcUpdate1AddPersonHandler implements
      OperationHandler<LdbcUpdate1AddPerson, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcUpdate1AddPersonHandler.class);
    private final Calendar calendar;

    public LdbcUpdate1AddPersonHandler() {
      this.calendar = new GregorianCalendar();
    }

    @Override
    public void executeOperation(LdbcUpdate1AddPerson operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      StringBuilder stmtBldr = new StringBuilder();
      stmtBldr.append("INSERT {"
                      + " _key: @personId,"
                      + " firstName: @firstName,"
                      + " lastName: @lastName,"
                      + " gender: @gender,"
                      + " birthday: @birthday,"
                      + " birthday_day: @birthday_day,"
                      + " birthday_month: @birthday_month,"
                      + " creationDate: @creationDate,"
                      + " locationIP: @locationIP,"
                      + " browserUsed: @browserUsed,"
                      + " email: @email,"
                      + " speaks: @speaks"
                      + " } INTO Person\n");

      // Use Calendar to figure out birthday_day and birthday_month from birthday    
      Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
      cal.setTimeInMillis(operation.birthday().getTime());

      MapBuilder paramBldr =  new MapBuilder()
          .put("personId", String.valueOf(operation.personId()))
          .put("firstName", operation.personFirstName())
          .put("lastName", operation.personLastName())
          .put("gender", operation.gender())
          .put("birthday", new Long(operation.birthday().getTime()))
          .put("birthday_day", new Integer(cal.get(Calendar.DAY_OF_MONTH)))
          .put("birthday_month", new Integer(cal.get(Calendar.MONTH) + 1))
          .put("creationDate", new Long(operation.creationDate().getTime()))
          .put("locationIP", operation.locationIp())
          .put("browserUsed", operation.browserUsed())
          .put("email", operation.emails().toString())
          .put("speaks", operation.languages().toString());

      // hasInterest edges.
      stmtBldr.append("LET hasInterestEdges = [");
      if (operation.tagIds().size() > 0) {
        for (int i = 0; i < operation.tagIds().size(); i++) {
          Long tagId = operation.tagIds().get(i);
          stmtBldr.append(String.format(
              "{_from: \"Person/%d\", _to: \"Tag/%d\"}", operation.personId(), tagId));
          if (i != operation.tagIds().size() - 1)
            stmtBldr.append(", ");
        }
      }
      stmtBldr.append("]\n");
      stmtBldr.append("FOR hasInterestEdge IN hasInterestEdges INSERT hasInterestEdge INTO hasInterest\n");

      // studyAt edges.
      stmtBldr.append("LET studyAtEdges = [");
      if (operation.studyAt().size() > 0) {
        for (int i = 0; i < operation.studyAt().size(); i++) {
          Organization org = operation.studyAt().get(i);
          stmtBldr.append(String.format(
              "{_from: \"Person/%d\", _to: \"Organisation/%d\", classYear: %d}", 
              operation.personId(), 
              org.organizationId(),
              org.year()));
          if (i != operation.studyAt().size() - 1)
            stmtBldr.append(", ");
        }
      }
      stmtBldr.append("]\n");
      stmtBldr.append("FOR studyAtEdge IN studyAtEdges INSERT studyAtEdge INTO studyAt\n");

      // workAt edges.
      stmtBldr.append("LET workAtEdges = [");
      if (operation.workAt().size() > 0) {
        for (int i = 0; i < operation.workAt().size(); i++) {
          Organization org = operation.workAt().get(i);
          stmtBldr.append(String.format(
              "{_from: \"Person/%d\", _to: \"Organisation/%d\", workFrom: %d}", 
              operation.personId(), 
              org.organizationId(),
              org.year()));
          if (i != operation.workAt().size() - 1)
            stmtBldr.append(", ");
        }
      }
      stmtBldr.append("]\n");
      stmtBldr.append("FOR workAtEdge IN workAtEdges INSERT workAtEdge INTO workAt\n");

      ArangoCursor<BaseDocument> cursor = db.query(
          stmtBldr.toString(),
          paramBldr.get(),
					new AqlQueryOptions(),
					BaseDocument.class
				);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }

  /**
   * Add a Like to a Post of the social network.[1]
   */
  public static class LdbcUpdate2AddPostLikeHandler implements
      OperationHandler<LdbcUpdate2AddPostLike, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcUpdate2AddPostLikeHandler.class);

    @Override
    public void executeOperation(LdbcUpdate2AddPostLike operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();
      String statement = String.format(
          "INSERT {_from: \"Person/%d\", _to: \"Message/%d\", creationDate: %d} INTO likes",
          operation.personId(),
          operation.postId(),
          operation.creationDate().getTime());
      
      ArangoCursor<BaseDocument> cursor = db.query(
          statement,
          null,
					new AqlQueryOptions(),
					BaseDocument.class
				);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }

  /**
   * Add a Like to a Comment of the social network.[1]
   */
  public static class LdbcUpdate3AddCommentLikeHandler implements
      OperationHandler<LdbcUpdate3AddCommentLike, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcUpdate3AddCommentLikeHandler.class);

    @Override
    public void executeOperation(LdbcUpdate3AddCommentLike operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();
      String statement = String.format(
          "INSERT {_from: \"Person/%d\", _to: \"Message/%d\", creationDate: %d} INTO likes",
          operation.personId(),
          operation.commentId(),
          operation.creationDate().getTime());
      
      ArangoCursor<BaseDocument> cursor = db.query(
          statement,
          null,
					new AqlQueryOptions(),
					BaseDocument.class
				);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }

  /**
   * Add a Forum to the social network.[1]
   */
  public static class LdbcUpdate4AddForumHandler implements
      OperationHandler<LdbcUpdate4AddForum, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcUpdate4AddForumHandler.class);

    @Override
    public void executeOperation(LdbcUpdate4AddForum operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();
      StringBuilder stmtBldr = new StringBuilder();
      stmtBldr.append(String.format(
          "INSERT {_key: \"%d\", title: \"%s\", creationDate: %d} INTO Forum\n",
          operation.forumId(),
          operation.forumTitle(),
          operation.creationDate().getTime()));
      
      stmtBldr.append("LET hasTagEdges = [");
      if (operation.tagIds().size() > 0) {
        for (int i = 0; i < operation.tagIds().size(); i++) {
          Long tagId = operation.tagIds().get(i);
          stmtBldr.append(String.format(
              "{_from: \"Forum/%d\", _to: \"Tag/%d\"}", operation.forumId(), tagId));
          if (i != operation.tagIds().size() - 1)
            stmtBldr.append(", ");
        }
      }
      stmtBldr.append("]\n");
      stmtBldr.append("FOR hasTagEdge IN hasTagEdges INSERT hasTagEdge INTO hasTag\n");
      
      stmtBldr.append(String.format(
          "INSERT {_from: \"Forum/%d\", _to: \"Person/%d\"} INTO hasModerator\n",
          operation.forumId(),
          operation.moderatorPersonId()));
      
      ArangoCursor<BaseDocument> cursor = db.query(
          stmtBldr.toString(),
          null,
					new AqlQueryOptions(),
					BaseDocument.class
				);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }

  /**
   * Add a Forum membership to the social network.[1]
   */
  public static class LdbcUpdate5AddForumMembershipHandler implements
      OperationHandler<LdbcUpdate5AddForumMembership, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcUpdate5AddForumMembershipHandler.class);

    @Override
    public void executeOperation(LdbcUpdate5AddForumMembership operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();
      String statement = String.format(
          "INSERT {_from: \"Forum/%d\", _to: \"Person/%d\", joinDate: %d} INTO hasMember",
          operation.forumId(),
          operation.personId(),
          operation.joinDate().getTime());
      
      ArangoCursor<BaseDocument> cursor = db.query(
          statement,
          null,
					new AqlQueryOptions(),
					BaseDocument.class
				);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }

  /**
   * Add a Post to the social network.[1]
   */
  public static class LdbcUpdate6AddPostHandler implements
      OperationHandler<LdbcUpdate6AddPost, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcUpdate6AddPostHandler.class);

    @Override
    public void executeOperation(LdbcUpdate6AddPost operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      StringBuilder stmtBldr = new StringBuilder();
      stmtBldr.append("INSERT {"
                      + " _key: @messageId,"
                      + " creationDate: @creationDate,"
                      + " browserUsed: @browserUsed,"
                      + " locationIP: @locationIP,"
                      + " length: @length,"
                      + " language: @language,");
      if (operation.imageFile().length() > 0) {
        stmtBldr.append(" imageFile: @imageFile } INTO Message\n");
      } else {
        stmtBldr.append(" content: @content } INTO Message\n");
      }

      MapBuilder paramBldr =  new MapBuilder()
          .put("messageId", String.valueOf(operation.postId()))
          .put("creationDate", operation.creationDate().getTime())
          .put("browserUsed", operation.browserUsed())
          .put("locationIP", operation.locationIp())
          .put("length", operation.length())
          .put("language", operation.language());
      if (operation.imageFile().length() > 0) {
        paramBldr.put("imageFile", operation.imageFile());
      } else {
        paramBldr.put("content", operation.content());
      }

      // hasTag edges.
      stmtBldr.append("LET hasTagEdges = [");
      if (operation.tagIds().size() > 0) {
        for (int i = 0; i < operation.tagIds().size(); i++) {
          Long tagId = operation.tagIds().get(i);
          stmtBldr.append(String.format(
              "{_from: \"Message/%d\", _to: \"Tag/%d\"}", operation.postId(), tagId));
          if (i != operation.tagIds().size() - 1)
            stmtBldr.append(", ");
        }
      }
      stmtBldr.append("]\n");
      stmtBldr.append("FOR hasTagEdge IN hasTagEdges INSERT hasTagEdge INTO hasTag\n");

      // Author
      stmtBldr.append(String.format(
          "INSERT {_from: \"Message/%d\", _to: \"Person/%d\"} INTO hasCreator\n",
          operation.postId(),
          operation.authorPersonId()));

      // Forum
      stmtBldr.append(String.format(
          "INSERT {_from: \"Forum/%d\", _to: \"Message/%d\"} INTO containerOf\n",
          operation.forumId(),
          operation.postId()));

      // Country
      stmtBldr.append(String.format(
          "INSERT {_from: \"Message/%d\", _to: \"Place/%d\"} INTO isLocatedIn\n",
          operation.postId(),
          operation.countryId()));

      ArangoCursor<BaseDocument> cursor = db.query(
          stmtBldr.toString(),
          paramBldr.get(),
					new AqlQueryOptions(),
					BaseDocument.class
				);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }

  /**
   * Add a Comment replying to a Post/Comment to the social network.[1]
   */
  public static class LdbcUpdate7AddCommentHandler implements
      OperationHandler<LdbcUpdate7AddComment, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcUpdate7AddCommentHandler.class);

    @Override
    public void executeOperation(LdbcUpdate7AddComment operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();

      StringBuilder stmtBldr = new StringBuilder();
      stmtBldr.append("INSERT {"
                      + " _key: @messageId,"
                      + " creationDate: @creationDate,"
                      + " browserUsed: @browserUsed,"
                      + " locationIP: @locationIP,"
                      + " length: @length,"
                      + " content: @content } INTO Message\n");

      MapBuilder paramBldr =  new MapBuilder()
          .put("messageId", String.valueOf(operation.commentId()))
          .put("creationDate", operation.creationDate().getTime())
          .put("browserUsed", operation.browserUsed())
          .put("locationIP", operation.locationIp())
          .put("length", operation.length())
          .put("content", operation.content());

      // hasTag edges.
      stmtBldr.append("LET hasTagEdges = [");
      if (operation.tagIds().size() > 0) {
        for (int i = 0; i < operation.tagIds().size(); i++) {
          Long tagId = operation.tagIds().get(i);
          stmtBldr.append(String.format(
              "{_from: \"Message/%d\", _to: \"Tag/%d\"}", operation.commentId(), tagId));
          if (i != operation.tagIds().size() - 1)
            stmtBldr.append(", ");
        }
      }
      stmtBldr.append("]\n");
      stmtBldr.append("FOR hasTagEdge IN hasTagEdges INSERT hasTagEdge INTO hasTag\n");

      // Author
      stmtBldr.append(String.format(
          "INSERT {_from: \"Message/%d\", _to: \"Person/%d\"} INTO hasCreator\n",
          operation.commentId(),
          operation.authorPersonId()));

      // Replying to another message
      Long replyOfId;
      if (operation.replyToCommentId() != -1)
        replyOfId = operation.replyToCommentId();
      else
        replyOfId = operation.replyToPostId();

      stmtBldr.append(String.format(
          "INSERT {_from: \"Message/%d\", _to: \"Message/%d\"} INTO replyOf\n",
          operation.commentId(),
          replyOfId));

      // Country
      stmtBldr.append(String.format(
          "INSERT {_from: \"Message/%d\", _to: \"Place/%d\"} INTO isLocatedIn\n",
          operation.commentId(),
          operation.countryId()));

      ArangoCursor<BaseDocument> cursor = db.query(
          stmtBldr.toString(),
          paramBldr.get(),
					new AqlQueryOptions(),
					BaseDocument.class
				);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }

  /**
   * Add a friendship relation to the social network.[1]
   */
  public static class LdbcUpdate8AddFriendshipHandler implements
      OperationHandler<LdbcUpdate8AddFriendship, DbConnectionState> {

    private static final Logger logger =
        LoggerFactory.getLogger(LdbcUpdate8AddFriendshipHandler.class);

    @Override
    public void executeOperation(LdbcUpdate8AddFriendship operation,
        DbConnectionState dbConnectionState,
        ResultReporter reporter) throws DbException {

      ArangoDatabase db = ((ArangoDbConnectionState) dbConnectionState).getDatabase();
      
      String statement = String.format(
          "INSERT {_from: \"Person/%d\", _to: \"Person/%d\", creationDate: %d} INTO knows",
          operation.person1Id(),
          operation.person2Id(),
          operation.creationDate().getTime());
      
      ArangoCursor<BaseDocument> cursor = db.query(
          statement,
          null,
					new AqlQueryOptions(),
					BaseDocument.class
				);

      reporter.report(0, LdbcNoResult.INSTANCE, operation);
    }
  }
}
