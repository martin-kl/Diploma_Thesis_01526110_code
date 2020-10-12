/* 
 * Copyright (C) 2015-2019 Stanford University
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
package net.ellitron.ldbcsnbimpls.interactive.torcdb2;

import net.ellitron.torcdb2.*;

import com.ldbc.driver.control.LoggingService;
import com.ldbc.driver.Db;
import com.ldbc.driver.DbConnectionState;
import com.ldbc.driver.DbException;
import com.ldbc.driver.OperationHandler;
import com.ldbc.driver.ResultReporter;
import com.ldbc.driver.workloads.ldbc.snb.interactive.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.lang.InterruptedException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An implementation of the LDBC SNB interactive workload[1] for TorcDB2.
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
public class TorcDb2 extends Db {

  private TorcDb2ConnectionState connState;

  // Maximum number of times to try a transaction before giving up.
  private static int MAX_TX_ATTEMPTS = 100;

  public TorcDb2() {
    this.connState = null;
  }

  @Override
  protected void onInit(Map<String, String> props, LoggingService loggingService) 
      throws DbException {
    connState = new TorcDb2ConnectionState(props);

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

  @Override
  protected void onClose() throws IOException {
    connState.close();
  }

  @Override
  protected DbConnectionState getConnectionState() throws DbException {
    return connState;
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

    @Override
    public void executeOperation(final LdbcQuery1 op, DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState)dbConnState;

      if (cState.fakeComplexReads()) {
        List<Long> personIDs = cState.personIDFeed();

        List<LdbcQuery1Result> result = new ArrayList<>(op.limit());
        for (int i = 0; i < op.limit(); i++) {
          int n1 = ThreadLocalRandom.current().nextInt(0, personIDs.size());
          Long pid = personIDs.get(n1);
          result.add(new LdbcQuery1Result(
              pid,
              null,
              0,
              0,
              0,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null));
        }

        resultReporter.report(result.size(), result, op);
        return;
      }

      // Parameters of this query
      final long personId = op.personId();
      final String firstName = op.firstName();
      final int limit = op.limit();

      final UInt128 torcPersonId = new UInt128(TorcEntity.PERSON.idSpace, personId);

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcQuery1 Start");

      List<LdbcQuery1Result> result = new ArrayList<>(limit);

      Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

      // Vertices that match our search criteria.
      List<Vertex> l1_matches = new ArrayList<>();
      List<Vertex> l2_matches = new ArrayList<>();
      List<Vertex> l3_matches = new ArrayList<>();

      Vertex start = new Vertex(torcPersonId, TorcEntity.PERSON.label);
      TraversalResult l1_friends = graph.traverse(start, "knows", Direction.OUT, false, "Person");

      graph.getProperties(vProps, l1_friends);
      for (Vertex v : l1_friends.vSet) {
        if (((String)vProps.get(v).get("firstName")).equals(firstName)) {
          l1_matches.add(v);
        }
      }

      Set<Vertex> seenSet = new HashSet<>();
      seenSet.add(start);
      seenSet.addAll(l1_friends.vSet);

      if (l1_matches.size() < limit) {
        TraversalResult l2_friends = graph.traverse(l1_friends, "knows", Direction.OUT, false, "Person");

        GraphHelper.subtract(l2_friends, seenSet);

        graph.getProperties(vProps, l2_friends);
        for (Vertex v : l2_friends.vSet) {
          if (((String)vProps.get(v).get("firstName")).equals(firstName)) {
            l2_matches.add(v);
          }
        }

        seenSet.addAll(l2_friends.vSet);

        if (l1_matches.size() + l2_matches.size() < limit) {
          TraversalResult l3_friends = graph.traverse(l2_friends, "knows", Direction.OUT, false, "Person");

          GraphHelper.subtract(l3_friends, seenSet);

          graph.getProperties(vProps, l3_friends);
          for (Vertex v : l3_friends.vSet) {
            if (((String)vProps.get(v).get("firstName")).equals(firstName)) {
              l3_matches.add(v);
            }
          }
        }
      }

      // Sort the matches ascending by lastname and then ascending by
      // identifier for a given distance.
      Comparator<Vertex> c = new Comparator<Vertex>() {
            public int compare(Vertex v1, Vertex v2) {
              String v1LastName = ((String)vProps.get(v1).get("lastName"));
              String v2LastName = ((String)vProps.get(v2).get("lastName"));
              
              if (v1LastName.compareTo(v2LastName) != 0) {
                return v1LastName.compareTo(v2LastName);
              } else {
                Long v1Id = v1.id().getLowerLong();
                Long v2Id = v2.id().getLowerLong();
                return v1Id.compareTo(v2Id);
              }
            }
          };

      Collections.sort(l1_matches, c);
      Collections.sort(l2_matches, c);
      Collections.sort(l3_matches, c);
      
      List<Vertex> matches = new ArrayList<>();
      matches.addAll(l1_matches);
      matches.addAll(l2_matches);
      matches.addAll(l3_matches);

      matches = matches.subList(0, Math.min(matches.size(), limit));

      TraversalResult match_place = graph.traverse(matches, "isLocatedIn", Direction.OUT, false, "Place");
      TraversalResult match_universities = graph.traverse(matches, "studyAt", Direction.OUT, true, "Organisation");
      TraversalResult match_companies = graph.traverse(matches, "workAt", Direction.OUT, true, "Organisation");
      TraversalResult university_place = graph.traverse(match_universities, "isLocatedIn", Direction.OUT, false, "Place");
      TraversalResult company_place = graph.traverse(match_companies, "isLocatedIn", Direction.OUT, false, "Place");

      graph.getProperties(vProps, match_place, match_universities, match_companies, university_place, company_place);

      for (int j = 0; j < matches.size(); j++) {
        Vertex f = matches.get(j);
        int distance;
        if (j < l1_matches.size())
          distance = 1;
        else if (j < l1_matches.size() + l2_matches.size())
          distance = 2;
        else
          distance = 3;

        List<Vertex> universities = match_universities.vMap.get(f);
        List<Map<Object, Object>> uniProps = match_universities.pMap.get(f);
        List<List<Object>> universityInfo = new ArrayList<>();
        if (universities != null) {
          for (int i = 0; i < universities.size(); i++) {
            Vertex university = universities.get(i);
            Map<Object, Object> props = uniProps.get(i);

            List<Object> info = new ArrayList<>(3);
            info.add(vProps.get(university).get("name"));
            info.add(props.get("classYear"));
            info.add(vProps.get(university_place.vMap.get(university).get(0)).get("name"));
            universityInfo.add(info);
          }
        }

        List<Vertex> companies = match_companies.vMap.get(f);
        List<Map<Object, Object>> comProps = match_companies.pMap.get(f);
        List<List<Object>> companyInfo = new ArrayList<>();
        if (companies != null) {
          for (int i = 0; i < companies.size(); i++) {
            Vertex company = companies.get(i);
            Map<Object, Object> props = comProps.get(i);

            List<Object> info = new ArrayList<>(3);
            info.add(vProps.get(company).get("name"));
            info.add(props.get("workFrom"));
            info.add(vProps.get(company_place.vMap.get(company).get(0)).get("name"));
            companyInfo.add(info);
          }
        }

        result.add(new LdbcQuery1Result(
            f.id().getLowerLong(), //((UInt128)t.get().get("friendId")).getLowerLong(),
            ((String)vProps.get(f).get("lastName")), //(String)t.get().get("lastName"),
            distance, //((Long)t.get().get("distance")).intValue() - 1,
            ((Long)vProps.get(f).get("birthday")), //Long.valueOf((String)t.get().get("birthday")),
            ((Long)vProps.get(f).get("creationDate")), //Long.valueOf((String)t.get().get("creationDate")),
            ((String)vProps.get(f).get("gender")), //(String)t.get().get("gender"),
            ((String)vProps.get(f).get("browserUsed")), //(String)t.get().get("browserUsed"),
            ((String)vProps.get(f).get("locationIP")), //(String)t.get().get("locationIP"),
            ((List<String>)vProps.get(f).get("email")), //(List<String>)t.get().get("emails"),
            ((List<String>)vProps.get(f).get("language")), //(List<String>)t.get().get("languages"),
            ((String)vProps.get(match_place.vMap.get(f).get(0)).get("name")), //(String)t.get().get("placeName"),
            universityInfo, //(List<List<Object>>)t.get().get("universityInfo"),
            companyInfo)); //(List<List<Object>>)t.get().get("companyInfo")));
      }

      resultReporter.report(result.size(), result, op);
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

    @Override
    public void executeOperation(final LdbcQuery2 op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState)dbConnState;

      if (cState.fakeComplexReads()) {
        List<Long> personIDs = cState.personIDFeed();
        List<Long> messageIDs = cState.messageIDFeed();

        List<LdbcQuery2Result> result = new ArrayList<>(op.limit());
        for (int i = 0; i < op.limit(); i++) {
          int n1 = ThreadLocalRandom.current().nextInt(0, personIDs.size());
          int n2 = ThreadLocalRandom.current().nextInt(0, messageIDs.size());
          Long pid = personIDs.get(n1);
          Long mid = messageIDs.get(n2);
          result.add(new LdbcQuery2Result(
              pid, 
              null,
              null,
              mid,
              null,
              0));
        }

        resultReporter.report(result.size(), result, op);
        return;
      }
      
      // Parameters of this query
      final long personId = op.personId();
      final long maxDate = op.maxDate().getTime();
      final int limit = op.limit();
      
      final UInt128 torcPersonId = new UInt128(TorcEntity.PERSON.idSpace, personId); 

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcQuery2 Start");

      List<LdbcQuery2Result> result = new ArrayList<>(limit);

      Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

      Vertex start = new Vertex(torcPersonId, TorcEntity.PERSON.label);
      TraversalResult friends = graph.traverse(start, "knows", Direction.OUT, false, "Person");

      TraversalResult messages = graph.traverse(friends, "hasCreator", Direction.IN, false, "Post", "Comment");

      graph.getProperties(vProps, messages);
      
      // Sort the Posts and Comments descending by their creation date and ascending by post
      // identifier. Reversed for priority queue.
      Comparator<Vertex> c = new Comparator<Vertex>() {
            public int compare(Vertex v1, Vertex v2) {
              Long v1creationDate = ((Long)vProps.get(v1).get("creationDate"));
              Long v2creationDate = ((Long)vProps.get(v2).get("creationDate"));
              if (v1creationDate.compareTo(v2creationDate) != 0)
                return v1creationDate.compareTo(v2creationDate);
              else {
                Long v1Id = v1.id().getLowerLong();
                Long v2Id = v2.id().getLowerLong();
                return -1*v1Id.compareTo(v2Id);
              }
            }
          };

      PriorityQueue<Vertex> pq = new PriorityQueue(limit, c);
      for (Vertex m : messages.vSet) {
        Long creationDate = (Long)vProps.get(m).get("creationDate");
        
        if (creationDate > maxDate)
          continue;

        if (pq.size() < limit) {
          pq.add(m);
          continue;
        }

        if (creationDate > (Long)vProps.get(pq.peek()).get("creationDate")) {
          pq.add(m);
          pq.poll();
        }
      }

      // Create a list from the priority queue. This list will contain the
      // messages in reverse order.
      List<Vertex> msgList = new ArrayList<>(pq.size());
      while (pq.size() > 0)
        msgList.add(pq.poll());

      // Wish there was a good way to go back and find the authors from what
      // we have already read, but we don't have a great way to do that now,
      // so go and read the authors.
      TraversalResult authors = graph.traverse(msgList, "hasCreator", Direction.OUT, false, "Person");

      graph.getProperties(vProps, authors);

      for (int i = msgList.size()-1; i >= 0; i--) {
        Vertex m = msgList.get(i);
        Vertex f = authors.vMap.get(m).get(0);

        String content = (String)vProps.get(m).get("content");
        if (content.equals(""))
          content = (String)vProps.get(m).get("imageFile");

        result.add(new LdbcQuery2Result(
            f.id().getLowerLong(), //((UInt128)t.get().get("personId")).getLowerLong(),
            ((String)vProps.get(f).get("firstName")), //(String)t.get().get("firstName"), 
            ((String)vProps.get(f).get("lastName")), //(String)t.get().get("lastName"),
            m.id().getLowerLong(), //((UInt128)t.get().get("messageId")).getLowerLong(), 
            content, //(String)t.get().get("content"),
            ((Long)vProps.get(m).get("creationDate")))); //Long.valueOf((String)t.get().get("creationDate"))))
      }

      resultReporter.report(result.size(), result, op);
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

    @Override
    public void executeOperation(final LdbcQuery3 op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState)dbConnState;

      if (cState.fakeComplexReads()) {
        List<Long> personIDs = cState.personIDFeed();
        List<Long> messageIDs = cState.messageIDFeed();

        List<LdbcQuery3Result> result = new ArrayList<>(op.limit());

        for (int i = 0; i < op.limit(); i++) {
          int n1 = ThreadLocalRandom.current().nextInt(0, personIDs.size());
          Long pid = personIDs.get(n1);
          result.add(new LdbcQuery3Result(
              pid,
              null,
              null,
              0,
              0,
              0));
        }

        resultReporter.report(result.size(), result, op);
        return;
      }

      // Parameters of this query
      final long personId = op.personId();
      final String countryXName = op.countryXName();
      final String countryYName = op.countryYName();
      final long startDate = op.startDate().getTime();
      final long durationDays = op.durationDays();
      final int limit = op.limit();

      final long endDate = startDate + (durationDays * 24L * 60L * 60L * 1000L);

      final UInt128 torcPersonId = new UInt128(TorcEntity.PERSON.idSpace, personId);

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcQuery3 Start");

      List<LdbcQuery3Result> result = new ArrayList<>(limit);

      Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

      Vertex start = new Vertex(torcPersonId, TorcEntity.PERSON.label);

      TraversalResult l1_friends = graph.traverse(start, "knows", Direction.OUT, false, "Person");
      TraversalResult l2_friends = graph.traverse(l1_friends, "knows", Direction.OUT, false, "Person");

      Set<Vertex> friends = new HashSet<>(l1_friends.vSet.size() + l2_friends.vSet.size());
      friends.addAll(l1_friends.vSet);
      friends.addAll(l2_friends.vSet);
      friends.remove(start);

      TraversalResult friendCity = graph.traverse(friends, "isLocatedIn", Direction.OUT, false, "Place");
      TraversalResult cityCountry = graph.traverse(friendCity, "isPartOf", Direction.OUT, false, "Place");
      graph.getProperties(vProps, cityCountry);

      // Filter out all friends located in either countryX or countryY.
      friends.removeIf(f -> {
        String placeName = (String)vProps.get(cityCountry.vMap.get(friendCity.vMap.get(f).get(0)).get(0)).get("name");
        return placeName.equals(countryXName) || placeName.equals(countryYName);
      });

      TraversalResult messages = graph.traverse(friends, "hasCreator", Direction.IN, false, "Post", "Comment");
      
      graph.getProperties(vProps, (Collection<Vertex>)messages.vSet, "creationDate");

      // Filter out all messages not in the given time window.
      messages.vSet.removeIf(m -> {
        Long creationDate = (Long)vProps.get(m).get("creationDate");
        return !(startDate <= creationDate && creationDate <= endDate);
      });

      TraversalResult messageLocation = graph.traverse(messages.vSet, "isLocatedIn", Direction.OUT, false, "Place");

      graph.getProperties(vProps, messageLocation.vSet, "name");

      // Filter out all messages not in countryX or countryY.
      messages.vSet.removeIf(m -> {
        String placeName = (String)vProps.get(messageLocation.vMap.get(m).get(0)).get("name");
        return !(placeName.equals(countryXName) || placeName.equals(countryYName));
      });

      // Once we intersect with the filtered messages, only friends with
      // non-zero number of messages will be part of the messages.vMap keyset.
      GraphHelper.intersect(messages, messages.vSet);

      Map<Vertex, Long> friendCountryXMsgCounts = new HashMap<>(messages.vMap.size());
      Map<Vertex, Long> friendCountryYMsgCounts = new HashMap<>(messages.vMap.size()); 
      List<Vertex> friendResults = new ArrayList<>(messages.vMap.size());
      for (Vertex f : messages.vMap.keySet()) {
        List<Vertex> mList = messages.vMap.get(f);
        long countryXCount = 0;
        long countryYCount = 0;
        for (Vertex m : mList) {
          String placeName = (String)vProps.get(messageLocation.vMap.get(m).get(0)).get("name");

          if (placeName.equals(countryXName))
            countryXCount++;

          if (placeName.equals(countryYName))
            countryYCount++;
        }

        if (countryXCount > 0 && countryYCount > 0) {
          friendCountryXMsgCounts.put(f, countryXCount);
          friendCountryYMsgCounts.put(f, countryYCount);
          friendResults.add(f);
        }
      }
      
      // Sort results descending by total number of Posts/Comments, and then ascending by Person
      // identifier.
      Comparator<Vertex> c = new Comparator<Vertex>() {
            public int compare(Vertex v1, Vertex v2) {
              Long v1MsgCount = friendCountryXMsgCounts.get(v1) + friendCountryYMsgCounts.get(v1);
              Long v2MsgCount = friendCountryXMsgCounts.get(v2) + friendCountryYMsgCounts.get(v2);

              if (v1MsgCount.compareTo(v2MsgCount) != 0) {
                // Message count is descending
                return -1*v1MsgCount.compareTo(v2MsgCount);
              } else {
                Long v1Id = v1.id().getLowerLong();
                Long v2Id = v2.id().getLowerLong();
                return v1Id.compareTo(v2Id);
              }
            }
          };

      Collections.sort(friendResults, c);

      // Take top limit
      friendResults = friendResults.subList(0, Math.min(friendResults.size(), limit));

      graph.getProperties(vProps, friendResults);

      for (int i = 0; i < friendResults.size(); i++) {
        Vertex f = friendResults.get(i);

        result.add(new LdbcQuery3Result(
            f.id().getLowerLong(), //((UInt128)((Traverser<Map>)t).get().get("personId")).getLowerLong(),
            (String)vProps.get(f).get("firstName"), //(String)((Traverser<Map>)t).get().get("firstName"), 
            (String)vProps.get(f).get("lastName"), //(String)((Traverser<Map>)t).get().get("lastName"),
            friendCountryXMsgCounts.get(f), //(Long)((Traverser<Map>)t).get().get("countryXCount"),
            friendCountryYMsgCounts.get(f), //(Long)((Traverser<Map>)t).get().get("countryYCount"),
            friendCountryXMsgCounts.get(f) + friendCountryYMsgCounts.get(f))); //(Long)((Traverser<Map>)t).get().get("totalCount")))
      }

      resultReporter.report(result.size(), result, op);
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

    @Override
    public void executeOperation(final LdbcQuery4 op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState)dbConnState;

      if (cState.fakeComplexReads()) {
        List<Long> personIDs = cState.personIDFeed();
        List<Long> messageIDs = cState.messageIDFeed();


        List<LdbcQuery4Result> result = new ArrayList<>(op.limit());

        for (int i = 0; i < op.limit(); i++) {
          result.add(new LdbcQuery4Result(
              null,
              0));
        }

        resultReporter.report(result.size(), result, op);
        return;
      }

      // Parameters of this query
      final long personId = op.personId();
      final long startDate = op.startDate().getTime();
      final long durationDays = op.durationDays();
      final int limit = op.limit();

      final long endDate = startDate + (durationDays * 24L * 60L * 60L * 1000L);

      final UInt128 torcPersonId = new UInt128(TorcEntity.PERSON.idSpace, personId);

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcQuery4 Start");

      List<LdbcQuery4Result> result = new ArrayList<>(limit);

      Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

      Vertex start = new Vertex(torcPersonId, TorcEntity.PERSON.label);

      TraversalResult friends = graph.traverse(start, "knows", Direction.OUT, false, "Person");
      TraversalResult posts = graph.traverse(friends, "hasCreator", Direction.IN, false, "Post");

      graph.getProperties(vProps, posts);

      // Filter out posts that are more recent than endDate. Don't want to do
      // extra work for them.
      posts.vSet.removeIf(p -> {
        Long creationDate = (Long)vProps.get(p).get("creationDate");
        return creationDate > endDate;
      });

      TraversalResult tags = graph.traverse(posts.vSet, "hasTag", Direction.OUT, false, "Tag");

      // Separate out tags before the window and in the window.
      Set<Vertex> tagsWithinWindow = new HashSet<>();
      Set<Vertex> tagsBeforeWindow = new HashSet<>();
      Map<Vertex, Long> tagCounts = new HashMap<>();
      for (Vertex p : tags.vMap.keySet()) {
        Long pCreationDate = (Long)vProps.get(p).get("creationDate");
        if (pCreationDate >= startDate && pCreationDate <= endDate) {
          for (Vertex t : tags.vMap.get(p)) {
            tagsWithinWindow.add(t);
            if (tagCounts.containsKey(t))
              tagCounts.put(t, tagCounts.get(t) + 1);
            else
              tagCounts.put(t, 1L);
          }
        } else if (pCreationDate < startDate) {
          for (Vertex t : tags.vMap.get(p))
            tagsBeforeWindow.add(t);
        }
      }

      tagsWithinWindow.removeAll(tagsBeforeWindow);

      List<Vertex> matchedTags = new ArrayList<>(tagsWithinWindow);

      graph.getProperties(vProps, matchedTags);

      // Sort tags by count
      Comparator<Vertex> c = new Comparator<Vertex>() {
            public int compare(Vertex t1, Vertex t2) {
              Long t1Count = tagCounts.get(t1);
              Long t2Count = tagCounts.get(t2);

              if (t1Count.compareTo(t2Count) != 0) {
                // Tag count sort is descending
                return -1*t1Count.compareTo(t2Count);
              } else {
                String t1Name = (String)vProps.get(t1).get("name");
                String t2Name = (String)vProps.get(t2).get("name");
                return t1Name.compareTo(t2Name);
              }
            }
          };

      Collections.sort(matchedTags, c);

      List<Vertex> topTags = matchedTags.subList(0, Math.min(matchedTags.size(), limit));

      for (int i = 0; i < topTags.size(); i++) {
        Vertex t = topTags.get(i);

        result.add(new LdbcQuery4Result(
              (String)vProps.get(t).get("name"),
              tagCounts.get(t).intValue()));
      }

      resultReporter.report(result.size(), result, op);
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

    @Override
    public void executeOperation(final LdbcQuery5 op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState)dbConnState;

      if (cState.fakeComplexReads()) {
        List<Long> personIDs = cState.personIDFeed();
        List<Long> messageIDs = cState.messageIDFeed();


        List<LdbcQuery5Result> result = new ArrayList<>(op.limit());

        for (int i = 0; i < op.limit(); i++) {
          result.add(new LdbcQuery5Result(
              null,
              0));
        }

        resultReporter.report(result.size(), result, op);
        return;
      }

      // Parameters of this query
      final long personId = op.personId();
      final long minDate = op.minDate().getTime();
      final int limit = op.limit();

      final UInt128 torcPersonId = new UInt128(TorcEntity.PERSON.idSpace, personId);

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcQuery5 Start");

      List<LdbcQuery5Result> result = new ArrayList<>(limit);

      Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

      Vertex start = new Vertex(torcPersonId, TorcEntity.PERSON.label);

      long startTime = System.nanoTime();

      TraversalResult l1_friends = graph.traverse(start, "knows", Direction.OUT, false, "Person");
      TraversalResult l2_friends = graph.traverse(l1_friends, "knows", Direction.OUT, false, "Person");
      
//    System.out.println(String.format("l1 l2 friends time: %d us", (System.nanoTime() - startTime)/1000));
//    startTime = System.nanoTime();

      Set<Vertex> friends = new HashSet<>(l1_friends.vSet.size() + l2_friends.vSet.size());
      friends.addAll(l1_friends.vSet);
      friends.addAll(l2_friends.vSet);
      friends.remove(start);
      
//    System.out.println(String.format("create friends set: %d us", (System.nanoTime() - startTime)/1000));
//    startTime = System.nanoTime();

      TraversalResult friendForums = graph.traverse(friends, "hasMember", Direction.IN, true, "Forum");

//    System.out.println(String.format("friendForums(%d) = graph.traverse(friends(%d), hasMember): %d us", friendForums.vSet.size(), friends.size(), (System.nanoTime() - startTime)/1000));
//    startTime = System.nanoTime();

      // Filter out all edges with joinDate <= minDate
      GraphHelper.removeEdgeIf(friendForums, (v, p) -> { 
        if ((Long)p.get("joinDate") <= minDate)
          return true;
        else 
          return false;
      });

//    System.out.println(String.format("removeEdgeIf time: %d us", (System.nanoTime() - startTime)/1000));
//    startTime = System.nanoTime();

      // Invert the friendForums mapping so we get a list of all the friends
      // that joined a given forum after a certain date.
      Map<Vertex, List<Vertex>> forumFriendsVMap = new HashMap<>(friendForums.vSet.size());
      for (Vertex friend : friendForums.vMap.keySet()) {
        List<Vertex> forums = friendForums.vMap.get(friend);
        for (Vertex forum : forums) {
          if (forumFriendsVMap.containsKey(forum))
            forumFriendsVMap.get(forum).add(friend);
          else {
            List<Vertex> fList = new ArrayList<>();
            fList.add(friend);
            forumFriendsVMap.put(forum, fList);
          }
        }
      }

      TraversalResult forumFriends = new TraversalResult(forumFriendsVMap, null, null);

//    System.out.println(String.format("invert friendForums time: %d us", (System.nanoTime() - startTime)/1000));
//    startTime = System.nanoTime();

      TraversalResult friendPosts = graph.traverse(friendForums.vMap.keySet(), "hasCreator", Direction.IN, false, "Post");

//    System.out.println(String.format("friendPosts(%d) = graph.traverse(friendForums.vMap.keySet()(%d)): %d us", friendPosts.vSet.size(), friendForums.vMap.size(), (System.nanoTime() - startTime)/1000));
//    startTime = System.nanoTime();
      
      TraversalResult forumPosts = graph.traverse(friendForums, "containerOf", Direction.OUT, false, "Post");
      
//    System.out.println(String.format("forumPosts(%d) = graph.traverse(friendForums(%d)): %d us", forumPosts.vSet.size(), friendForums.vSet.size(), (System.nanoTime() - startTime)/1000));
//    startTime = System.nanoTime();
     
      Map<Vertex, Integer> forumFriendPostCounts = new ConcurrentHashMap<>(forumPosts.vMap.size());

      Thread t1 = new Thread(() -> {
        int forumCount = 0;
        for (Vertex forum : friendForums.vSet) {
          if ((forumCount % 2) == 0) {
            int count = 0;
            if (forumPosts.vMap.containsKey(forum)) {
              Set<Vertex> forumPostSet = new HashSet<>(forumPosts.vMap.get(forum));
              for (Vertex friend : forumFriends.vMap.get(forum)) {
                if (friendPosts.vMap.containsKey(friend)) {
                  for (Vertex post : friendPosts.vMap.get(friend)) {
                    if (forumPostSet.contains(post))
                      count++;
                  }
                }
              }
            }

            forumFriendPostCounts.put(forum, count);
          }

          forumCount++;
        }
      });

      Thread t2 = new Thread(() -> {
        int forumCount = 0;
        for (Vertex forum : friendForums.vSet) {
          if ((forumCount % 2) == 1) {
            int count = 0;
            if (forumPosts.vMap.containsKey(forum)) {
              Set<Vertex> forumPostSet = new HashSet<>(forumPosts.vMap.get(forum));
              for (Vertex friend : forumFriends.vMap.get(forum)) {
                if (friendPosts.vMap.containsKey(friend)) {
                  for (Vertex post : friendPosts.vMap.get(friend)) {
                    if (forumPostSet.contains(post))
                      count++;
                  }
                }
              }
            }

            forumFriendPostCounts.put(forum, count);
          }

          forumCount++;
        }
      });

      t1.start();
      t2.start();

      try {
        t1.join();
        t2.join();
      } catch (InterruptedException e) {
        System.out.println("Rudely interrupted");
      }


//    System.out.println(String.format("make forumFriendPostCounts: %d us", (System.nanoTime() - startTime)/1000));
//    startTime = System.nanoTime();
      
      List<Vertex> forums = new ArrayList<>(forumFriendPostCounts.keySet());
      
      // Sort results descending by the count of Posts, and then ascending by Forum  identifier.
      Comparator<Vertex> c = new Comparator<Vertex>() {
            public int compare(Vertex v1, Vertex v2) {
              Integer forum1FriendPostCount = forumFriendPostCounts.get(v1);
              Integer forum2FriendPostCount = forumFriendPostCounts.get(v2);

              if (forum1FriendPostCount.compareTo(forum2FriendPostCount) != 0) {
                // Post count sort is descending
                return -1*forum1FriendPostCount.compareTo(forum2FriendPostCount);
              } else {
                Long v1Id = v1.id().getLowerLong();
                Long v2Id = v2.id().getLowerLong();
                // IDs are ascending
                return v1Id.compareTo(v2Id);
              }
            }
          };

      Collections.sort(forums, c);

//    System.out.println(String.format("sort forums: %d us", (System.nanoTime() - startTime)/1000));
//    startTime = System.nanoTime();
      
      // Take top limit
      forums = forums.subList(0, Math.min(forums.size(), limit));

      graph.getProperties(vProps, forums);

      for (int i = 0; i < forums.size(); i++) {
        Vertex forum = forums.get(i);

        result.add(new LdbcQuery5Result(
            (String)vProps.get(forum).get("title"), 
            forumFriendPostCounts.get(forum)));
      }

//    System.out.println(String.format("generate result: %d us", (System.nanoTime() - startTime)/1000));
      
      resultReporter.report(result.size(), result, op);
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

    @Override
    public void executeOperation(final LdbcQuery6 op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState)dbConnState;

      if (cState.fakeComplexReads()) {
        List<Long> personIDs = cState.personIDFeed();
        List<Long> messageIDs = cState.messageIDFeed();


        List<LdbcQuery6Result> result = new ArrayList<>(op.limit());

        for (int i = 0; i < op.limit(); i++) {
          result.add(new LdbcQuery6Result(
              null,
              0));
        }

        resultReporter.report(result.size(), result, op);
        return;
      }

      // Parameters of this query
      final long personId = op.personId();
      final String tagName = op.tagName();
      final int limit = op.limit();

      final UInt128 torcPersonId = new UInt128(TorcEntity.PERSON.idSpace, personId);

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcQuery6 Start");

      List<LdbcQuery6Result> result = new ArrayList<>(limit);

      Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

      Vertex start = new Vertex(torcPersonId, TorcEntity.PERSON.label);

      TraversalResult l1_friends = graph.traverse(start, "knows", Direction.OUT, false, "Person");
      TraversalResult l2_friends = graph.traverse(l1_friends, "knows", Direction.OUT, false, "Person");

      Set<Vertex> friends = new HashSet<>(l1_friends.vSet.size() + l2_friends.vSet.size());
      friends.addAll(l1_friends.vSet);
      friends.addAll(l2_friends.vSet);
      friends.remove(start);

      TraversalResult posts = graph.traverse(friends, "hasCreator", Direction.IN, false, "Post");
      TraversalResult tags = graph.traverse(posts, "hasTag", Direction.OUT, false, "Tag");

      graph.getProperties(vProps, tags);

      Map<Vertex, Long> coTagCounts = new HashMap<>();
      for (Vertex p : tags.vMap.keySet()) {
        boolean hasTag = false;
        for (Vertex t : tags.vMap.get(p)) {
          if (((String)vProps.get(t).get("name")).equals(tagName)) {
            hasTag = true;
            break;
          }
        }

        if (hasTag) {
          for (Vertex t : tags.vMap.get(p)) {
            if (!((String)vProps.get(t).get("name")).equals(tagName)) {
              if (coTagCounts.containsKey(t)) {
                coTagCounts.put(t, coTagCounts.get(t) + 1);
              } else {
                coTagCounts.put(t, 1L);
              }
            }
          } 
        }
      }

      List<Vertex> coTags = new ArrayList<>(coTagCounts.keySet());

      // Sort tags by count
      Comparator<Vertex> c = new Comparator<Vertex>() {
            public int compare(Vertex t1, Vertex t2) {
              Long t1Count = coTagCounts.get(t1);
              Long t2Count = coTagCounts.get(t2);

              if (t1Count.compareTo(t2Count) != 0) {
                // Tag count sort is descending
                return -1*t1Count.compareTo(t2Count);
              } else {
                String t1Name = (String)vProps.get(t1).get("name");
                String t2Name = (String)vProps.get(t2).get("name");
                return t1Name.compareTo(t2Name);
              }
            }
          };

      Collections.sort(coTags, c);

      List<Vertex> topCoTags = coTags.subList(0, Math.min(coTags.size(), limit));

      for (int i = 0; i < topCoTags.size(); i++) {
        Vertex t = topCoTags.get(i);

        result.add(new LdbcQuery6Result(
              (String)vProps.get(t).get("name"),
              coTagCounts.get(t).intValue()));
      }

      resultReporter.report(result.size(), result, op);
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

    @Override
    public void executeOperation(final LdbcQuery7 op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState)dbConnState;

      if (cState.fakeComplexReads()) {
        List<Long> personIDs = cState.personIDFeed();
        List<Long> messageIDs = cState.messageIDFeed();


        List<LdbcQuery7Result> result = new ArrayList<>(op.limit());

        for (int i = 0; i < op.limit(); i++) {
          int n1 = ThreadLocalRandom.current().nextInt(0, personIDs.size());
          int n2 = ThreadLocalRandom.current().nextInt(0, messageIDs.size());
          Long pid = personIDs.get(n1);
          Long mid = messageIDs.get(n2);
          result.add(new LdbcQuery7Result(
              pid,
              null,
              null,
              0,
              mid,
              null,
              0,
              false));
        }

        resultReporter.report(result.size(), result, op);
        return;
      }
      
      // Parameters of this query
      final long personId = op.personId();
      final int limit = op.limit();
      
      final UInt128 torcPersonId = new UInt128(TorcEntity.PERSON.idSpace, personId);

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcQuery7 Start");

      List<LdbcQuery7Result> result = new ArrayList<>(limit);

      Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

      Vertex start = new Vertex(torcPersonId, TorcEntity.PERSON.label);

      TraversalResult friends = graph.traverse(start, "knows", Direction.OUT, false, "Person");        
      TraversalResult messages = graph.traverse(start, "hasCreator", Direction.IN, false, "Post", "Comment");
      TraversalResult likes = graph.traverse(messages, "likes", Direction.IN, true, "Person");

      Map<Vertex, Long> personMostRecentLikeDate = new HashMap<>();
      Map<Vertex, Vertex> personMostRecentLikeMsg = new HashMap<>();
      Long minLikeDate = Long.MAX_VALUE;
      int numMinLikeDates = 0;
      for (Vertex msg : likes.vMap.keySet()) {
        List<Vertex> likers = likes.vMap.get(msg);
        List<Map<Object, Object>> likeProps = likes.pMap.get(msg);

        for (int i = 0; i < likers.size(); i++) {
          Vertex liker = likers.get(i);
          Long likeDate = (Long)likeProps.get(i).get("creationDate");
          if (personMostRecentLikeDate.containsKey(liker)) {
            // We already have a most recent like date registered for this
            // person. Check if the new like date is more recent and, if so,
            // update the map. Also check if this changes the least recent
            // like date contained in the map.
            Long currLikeDate = personMostRecentLikeDate.get(liker);
            if (currLikeDate < likeDate) {
              personMostRecentLikeDate.put(liker, likeDate);
              personMostRecentLikeMsg.put(liker, msg);
              if (currLikeDate == minLikeDate) {
                if (numMinLikeDates == 1) { 
                  Long newMinLikeDate = Long.MAX_VALUE;
                  for (Long date : personMostRecentLikeDate.values()) {
                    if (date < newMinLikeDate) {
                      newMinLikeDate = date;
                      numMinLikeDates = 1;
                    } else if (date == newMinLikeDate) {
                      numMinLikeDates++;
                    }
                  }
                  minLikeDate = newMinLikeDate;
                } else {
                  numMinLikeDates--;
                }
              }
            } else if (currLikeDate == likeDate) {
              // In this case when a person has liked more than one message at
              // the same time, we are to choose the message that has the
              // lower identifier.
              Vertex currMsg = personMostRecentLikeMsg.get(liker);
              if (msg.id().getLowerLong() < currMsg.id().getLowerLong())
                personMostRecentLikeMsg.put(liker, msg);
            }
          } else if (personMostRecentLikeDate.size() < limit) {
            // If haven't collected enough people yet, and we have someone we
            // haven't seen before here, then automatically insert them into
            // the map.
            personMostRecentLikeDate.put(liker, likeDate);
            personMostRecentLikeMsg.put(liker, msg);
            if (likeDate < minLikeDate) {
              minLikeDate = likeDate;
              numMinLikeDates = 1;
            } else if (likeDate == minLikeDate) {
              numMinLikeDates++;
            }
          } else {
            // The map is full of "limit" entries and we haven't seen this
            // person before. If the likeDate is less recent than our current
            // minimum, then we can reject this entry outright. If the
            // likeDate is equal to our current minimum, then we just keep it,
            // and we'll sort out the minimums by vertex ID in the end to
            // figure out which ones make it into the final result. Otherwise,
            // if the likeDate is more recent than the minimum, then we add
            // it, and check if the number above the minimum has hit our
            // limit... in this case we can cut off the minimums entirely.
            if (likeDate < minLikeDate) {
              continue;
            } else if (likeDate == minLikeDate) {
              personMostRecentLikeDate.put(liker, likeDate);
              personMostRecentLikeMsg.put(liker, msg);
              numMinLikeDates++;
            } else {
              personMostRecentLikeDate.put(liker, likeDate);
              personMostRecentLikeMsg.put(liker, msg);

              if (personMostRecentLikeDate.size() - numMinLikeDates >= limit) {
                Map<Vertex, Long> newPersonMostRecentLikeDate = new HashMap<>();
                Map<Vertex, Vertex> newPersonMostRecentLikeMsg = new HashMap<>();

                Long newMinLikeDate = Long.MAX_VALUE;
                for (Vertex v : personMostRecentLikeDate.keySet()) {
                  Long date = personMostRecentLikeDate.get(v);
                  if (date != minLikeDate) {
                    newPersonMostRecentLikeDate.put(v, date);
                    newPersonMostRecentLikeMsg.put(v, personMostRecentLikeMsg.get(v));

                    if (date < newMinLikeDate) {
                      newMinLikeDate = date;
                      numMinLikeDates = 1;
                    } else if (date == newMinLikeDate) {
                      numMinLikeDates++;
                    }
                  }
                }

                personMostRecentLikeDate = newPersonMostRecentLikeDate;
                personMostRecentLikeMsg = newPersonMostRecentLikeMsg;
                minLikeDate = newMinLikeDate;
              }
            }
          }
        }
      }

      List<Vertex> likersList = new ArrayList<>(personMostRecentLikeDate.keySet());

      // Sort results descending by creation time of Like, then  ascending by Person identifier of
      // liker.
      final Map<Vertex, Long> likeDates = personMostRecentLikeDate;
      Comparator<Vertex> c = new Comparator<Vertex>() {
            public int compare(Vertex v1, Vertex v2) {
              Long v1likeDate = likeDates.get(v1);
              Long v2likeDate = likeDates.get(v2);
              if (v1likeDate.compareTo(v2likeDate) != 0)
                return -1*v1likeDate.compareTo(v2likeDate);
              else {
                Long v1Id = v1.id().getLowerLong();
                Long v2Id = v2.id().getLowerLong();
                return v1Id.compareTo(v2Id);
              }
            }
          };

      Collections.sort(likersList, c);
      
      List<Vertex> topLikers = likersList.subList(0, Math.min(likersList.size(), limit));

      graph.getProperties(vProps, topLikers);

      List<Vertex> msgList = new ArrayList<>(topLikers.size());

      for (Vertex tLiker : topLikers) 
        msgList.add(personMostRecentLikeMsg.get(tLiker));

      graph.getProperties(vProps, msgList);

      for (int i = 0; i < topLikers.size(); i++) {
        Vertex liker = topLikers.get(i);
        Long likeDate = personMostRecentLikeDate.get(liker);
        Vertex msg = personMostRecentLikeMsg.get(liker);

        String content = (String)vProps.get(msg).get("content");
        if (content.equals(""))
          content = (String)vProps.get(msg).get("imageFile");

        Long latencyMinutes = 
          (likeDate - (Long)vProps.get(msg).get("creationDate")) / (1000l * 60l);

        result.add(new LdbcQuery7Result(
            liker.id().getLowerLong(), 
            (String)vProps.get(liker).get("firstName"),
            (String)vProps.get(liker).get("lastName"),
            likeDate,
            msg.id().getLowerLong(),
            content,
            latencyMinutes.intValue(),
            !friends.vSet.contains(liker)));
      }

      resultReporter.report(result.size(), result, op);
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

    @Override
    public void executeOperation(final LdbcQuery8 op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState)dbConnState;

      if (cState.fakeComplexReads()) {
        List<Long> personIDs = cState.personIDFeed();
        List<Long> messageIDs = cState.messageIDFeed();


        List<LdbcQuery8Result> result = new ArrayList<>(op.limit());

        for (int i = 0; i < op.limit(); i++) {
          int n1 = ThreadLocalRandom.current().nextInt(0, personIDs.size());
          int n2 = ThreadLocalRandom.current().nextInt(0, messageIDs.size());
          Long pid = personIDs.get(n1);
          Long mid = messageIDs.get(n2);
          result.add(new LdbcQuery8Result(
              pid,
              null,
              null,
              0,
              mid,
              null));
        }

        resultReporter.report(result.size(), result, op);
        return;
      }
      
      // Parameters of this query
      final long personId = op.personId();
      final int limit = op.limit();
      
      final UInt128 torcPersonId = new UInt128(TorcEntity.PERSON.idSpace, personId);

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcQuery8 Start");

      List<LdbcQuery8Result> result = new ArrayList<>(limit);

      Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

      Vertex start = new Vertex(torcPersonId, TorcEntity.PERSON.label);

      TraversalResult posts = graph.traverse(start, "hasCreator", Direction.IN, false, "Post", "Comment");

      TraversalResult replies = graph.traverse(posts, "replyOf", Direction.IN, false, "Post", "Comment");

      graph.getProperties(vProps, replies.vSet, "creationDate");

      // Sort results descending by creation date of reply Comment, and then ascending by identifier
      // of reply Comment. Reversed for priority queue.
      Comparator<Vertex> c = new Comparator<Vertex>() {
            public int compare(Vertex v1, Vertex v2) {
              Long v1creationDate = ((Long)vProps.get(v1).get("creationDate"));
              Long v2creationDate = ((Long)vProps.get(v2).get("creationDate"));
              if (v1creationDate.compareTo(v2creationDate) != 0)
                return v1creationDate.compareTo(v2creationDate);
              else {
                Long v1Id = v1.id().getLowerLong();
                Long v2Id = v2.id().getLowerLong();
                return -1*v1Id.compareTo(v2Id);
              }
            }
          };

      PriorityQueue<Vertex> pq = new PriorityQueue(limit, c);
      for (Vertex r : replies.vSet) {
        Long creationDate = (Long)vProps.get(r).get("creationDate");
        
        if (pq.size() < limit) {
          pq.add(r);
          continue;
        }

        if (creationDate > (Long)vProps.get(pq.peek()).get("creationDate")) {
          pq.add(r);
          pq.poll();
        }
      }

      // Create a list from the priority queue. This list will contain the
      // messages in reverse order.
      List<Vertex> replyList = new ArrayList<>(pq.size());
      while (pq.size() > 0)
        replyList.add(pq.poll());

      TraversalResult authors = graph.traverse(replyList, "hasCreator", Direction.OUT, false, "Person");

      graph.getProperties(vProps, authors);
      graph.getProperties(vProps, replyList);

      for (int i = replyList.size()-1; i >= 0; i--) {
        Vertex r = replyList.get(i);
        Vertex a = authors.vMap.get(r).get(0);

        String content = (String)vProps.get(r).get("content");
        if (content.equals(""))
          content = (String)vProps.get(r).get("imageFile");

        result.add(new LdbcQuery8Result(
              a.id().getLowerLong(),
              (String)vProps.get(a).get("firstName"),
              (String)vProps.get(a).get("lastName"),
              (Long)vProps.get(r).get("creationDate"),
              r.id().getLowerLong(),
              content));
      }

      resultReporter.report(result.size(), result, op);
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

    @Override
    public void executeOperation(final LdbcQuery9 op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState)dbConnState;

      if (cState.fakeComplexReads()) {
        List<Long> personIDs = cState.personIDFeed();
        List<Long> messageIDs = cState.messageIDFeed();


        List<LdbcQuery9Result> result = new ArrayList<>(op.limit());

        for (int i = 0; i < op.limit(); i++) {
          int n1 = ThreadLocalRandom.current().nextInt(0, personIDs.size());
          int n2 = ThreadLocalRandom.current().nextInt(0, messageIDs.size());
          Long pid = personIDs.get(n1);
          Long mid = messageIDs.get(n2);
          result.add(new LdbcQuery9Result(
              pid,
              null,
              null,
              mid,
              null,
              0));
        }

        resultReporter.report(result.size(), result, op);
        return;
      }

      // Parameters of this query
      final long personId = op.personId();
      final long maxDate = op.maxDate().getTime();
      final int limit = op.limit();
      
      final UInt128 torcPersonId = new UInt128(TorcEntity.PERSON.idSpace, personId);

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcQuery9 Start");

      List<LdbcQuery9Result> result = new ArrayList<>(limit);

      Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

      Vertex start = new Vertex(torcPersonId, TorcEntity.PERSON.label);

      TraversalResult l1_friends = graph.traverse(start, "knows", Direction.OUT, false, "Person");
      TraversalResult l2_friends = graph.traverse(l1_friends, "knows", Direction.OUT, false, "Person");

      Set<Vertex> friends = new HashSet<>(l1_friends.vSet.size() + l2_friends.vSet.size());
      friends.addAll(l1_friends.vSet);
      friends.addAll(l2_friends.vSet);
      friends.remove(start);

      TraversalResult messages = graph.traverse(friends, "hasCreator", Direction.IN, false, "Post", "Comment");
      
      graph.getProperties(vProps, messages.vSet, "creationDate");

      // Sort results descending by creation date of Post/Comment, and then ascending by
      // Post/Comment identifier. Reversed for priority queue.
      Comparator<Vertex> c = new Comparator<Vertex>() {
            public int compare(Vertex v1, Vertex v2) {
              Long v1creationDate = ((Long)vProps.get(v1).get("creationDate"));
              Long v2creationDate = ((Long)vProps.get(v2).get("creationDate"));
              if (v1creationDate.compareTo(v2creationDate) != 0)
                return v1creationDate.compareTo(v2creationDate);
              else {
                Long v1Id = v1.id().getLowerLong();
                Long v2Id = v2.id().getLowerLong();
                return -1*v1Id.compareTo(v2Id);
              }
            }
          };

      PriorityQueue<Vertex> pq = new PriorityQueue(limit, c);
      for (Vertex m : messages.vSet) {
        Long creationDate = (Long)vProps.get(m).get("creationDate");
        
        if (creationDate >= maxDate)
          continue;

        if (pq.size() < limit) {
          pq.add(m);
          continue;
        }

        if (creationDate > (Long)vProps.get(pq.peek()).get("creationDate")) {
          pq.add(m);
          pq.poll();
        }
      }

      // Create a list from the priority queue. This list will contain the
      // messages in reverse order.
      List<Vertex> msgList = new ArrayList<>(pq.size());
      while (pq.size() > 0)
        msgList.add(pq.poll());

      // Wish there was a good way to go back and find the authors from what
      // we have already read, but we don't have a great way to do that now,
      // so go and read the authors.
      TraversalResult authors = graph.traverse(msgList, "hasCreator", Direction.OUT, false, "Person");

      graph.getProperties(vProps, authors);
      graph.getProperties(vProps, msgList);

      for (int i = msgList.size()-1; i >= 0; i--) {
        Vertex m = msgList.get(i);
        Vertex f = authors.vMap.get(m).get(0);

        String content = (String)vProps.get(m).get("content");
        if (content.equals(""))
          content = (String)vProps.get(m).get("imageFile");

        result.add(new LdbcQuery9Result(
            f.id().getLowerLong(), //((UInt128)t.get().get("personId")).getLowerLong(),
            ((String)vProps.get(f).get("firstName")), //(String)t.get().get("firstName"), 
            ((String)vProps.get(f).get("lastName")), //(String)t.get().get("lastName"),
            m.id().getLowerLong(), //((UInt128)t.get().get("messageId")).getLowerLong(), 
            content, //(String)t.get().get("content"),
            ((Long)vProps.get(m).get("creationDate")))); //Long.valueOf((String)t.get().get("creationDate"))))
      }

      resultReporter.report(result.size(), result, op);
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

    @Override
    public void executeOperation(final LdbcQuery10 op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState)dbConnState;

      if (cState.fakeComplexReads()) {
        List<Long> personIDs = cState.personIDFeed();
        List<Long> messageIDs = cState.messageIDFeed();


        List<LdbcQuery10Result> result = new ArrayList<>(op.limit());

        for (int i = 0; i < op.limit(); i++) {
          int n1 = ThreadLocalRandom.current().nextInt(0, personIDs.size());
          Long pid = personIDs.get(n1);
          result.add(new LdbcQuery10Result(
              pid,
              null,
              null,
              0,
              null,
              null));
        }

        resultReporter.report(result.size(), result, op);
        return;
      }
      
      // Parameters of this query
      final long personId = op.personId();
      final int month = op.month() - 1; // make month zero based
      final int limit = op.limit();

      Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));

      final UInt128 torcPersonId = new UInt128(TorcEntity.PERSON.idSpace, personId);

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcQuery10 Start");

      List<LdbcQuery10Result> result = new ArrayList<>(limit);

      Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

      Vertex start = new Vertex(torcPersonId, TorcEntity.PERSON.label);
      TraversalResult l1_friends = graph.traverse(start, "knows", Direction.OUT, false, "Person");
      TraversalResult l2_friends = graph.traverse(l1_friends, "knows", Direction.OUT, false, "Person");

      l2_friends.vSet.removeAll(l1_friends.vSet);
      l2_friends.vSet.remove(start);

      graph.getProperties(vProps, l2_friends.vSet, "birthday"); 

      // Filter by birthday
      l2_friends.vSet.removeIf(f -> {
        calendar.setTimeInMillis((Long)vProps.get(f).get("birthday"));
        int bmonth = calendar.get(Calendar.MONTH); // zero based 
        int bday = calendar.get(Calendar.DAY_OF_MONTH); // starts with 1
        if ((bmonth == month && bday >= 21) || 
            (bmonth == ((month + 1) % 12) && bday < 22)) {
          return false;
        }
        return true;
      });

      TraversalResult posts = graph.traverse(l2_friends.vSet, "hasCreator", Direction.IN, false, "Post");
      TraversalResult tags = graph.traverse(posts, "hasTag", Direction.OUT, false, "Tag");

      TraversalResult interests = graph.traverse(start, "hasInterest", Direction.OUT, false, "Tag");

      // For each l2 friend calculate the similarity score.
      Map<Vertex, Long> similarityScore = new HashMap<>();
      for (Vertex f : l2_friends.vSet) {
        if (posts.vMap.containsKey(f)) {
          long common = 0;
          long uncommon = 0;
          for (Vertex p : posts.vMap.get(f)) {
            if (tags.vMap.containsKey(p)) {
              for (Vertex t : tags.vMap.get(p)) {
                if (interests.vSet.contains(t)) {
                  common++;
                  break;
                }
              }
            }
          }
          uncommon = posts.vMap.get(f).size() - common;
          similarityScore.put(f, new Long(common - uncommon));
        } else {
          similarityScore.put(f, new Long(0L));
        }
      }

      // Sort results descending by similarity score, and then ascending by Person identifier.
      // Reversed for priority queue.
      Comparator<Vertex> c = new Comparator<Vertex>() {
            public int compare(Vertex v1, Vertex v2) {
              Long v1similarityScore = similarityScore.get(v1);
              Long v2similarityScore = similarityScore.get(v2);
              if (v1similarityScore.compareTo(v2similarityScore) != 0)
                return v1similarityScore.compareTo(v2similarityScore);
              else {
                Long v1Id = v1.id().getLowerLong();
                Long v2Id = v2.id().getLowerLong();
                return -1*v1Id.compareTo(v2Id);
              }
            }
          };

      PriorityQueue<Vertex> pq = new PriorityQueue(limit, c);
      for (Vertex f : l2_friends.vSet) {
        Long score = (Long)similarityScore.get(f);
        
        if (pq.size() < limit) {
          pq.add(f);
          continue;
        }

        if (score > similarityScore.get(pq.peek())) {
          pq.add(f);
          pq.poll();
        } else if (score.equals(similarityScore.get(pq.peek())) && 
            f.id().getLowerLong() < pq.peek().id().getLowerLong()) {
          pq.add(f);
          pq.poll();
        }
      }

      // Create a list from the priority queue. This list will contain the
      // results in reverse order.
      List<Vertex> fList = new ArrayList<>(pq.size());
      while (pq.size() > 0)
        fList.add(pq.poll());

      graph.getProperties(vProps, fList);

      TraversalResult locations = graph.traverse(fList, "isLocatedIn", Direction.OUT, false, "Place");

      graph.getProperties(vProps, locations);

      for (int i = fList.size()-1; i >= 0; i--) {
        Vertex f = fList.get(i);

        result.add(new LdbcQuery10Result(
              f.id().getLowerLong(),
              (String)vProps.get(f).get("firstName"),
              (String)vProps.get(f).get("lastName"),
              similarityScore.get(f).intValue(),
              (String)vProps.get(f).get("gender"),
              (String)vProps.get(locations.vMap.get(f).get(0)).get("name")));
      }

      resultReporter.report(result.size(), result, op);
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

    @Override
    public void executeOperation(final LdbcQuery11 op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState)dbConnState;

      if (cState.fakeComplexReads()) {
        List<Long> personIDs = cState.personIDFeed();
        List<Long> messageIDs = cState.messageIDFeed();


        List<LdbcQuery11Result> result = new ArrayList<>(op.limit());

        for (int i = 0; i < op.limit(); i++) {
          int n1 = ThreadLocalRandom.current().nextInt(0, personIDs.size());
          Long pid = personIDs.get(n1);
          result.add(new LdbcQuery11Result(
              pid,
              null,
              null,
              null,
              0));
        }

        resultReporter.report(result.size(), result, op);
        return;
      }
      
      // Parameters of this query
      final long personId = op.personId();
      final String countryName = op.countryName();
      final int workFromYear = op.workFromYear();
      final int limit = op.limit();

      final UInt128 torcPersonId = new UInt128(TorcEntity.PERSON.idSpace, personId);

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcQuery11 Start");

      class ResultTuple {
        public int year;
        public Vertex v;
        public String name;

        public ResultTuple(int year, Vertex v, String name) {
          this.year = year;
          this.v = v;
          this.name = name;
        }
      };

      List<LdbcQuery11Result> result = new ArrayList<>(limit);

      Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

      Vertex start = new Vertex(torcPersonId, TorcEntity.PERSON.label);
      TraversalResult l1_friends = graph.traverse(start, "knows", Direction.OUT, false, "Person");
      TraversalResult l2_friends = graph.traverse(l1_friends, "knows", Direction.OUT, false, "Person");

      Set<Vertex> friends = new HashSet<>(l1_friends.vSet.size() + l2_friends.vSet.size());
      friends.addAll(l1_friends.vSet);
      friends.addAll(l2_friends.vSet);
      friends.remove(start);
      
      TraversalResult company = graph.traverse(friends, "workAt", Direction.OUT, true, "Organisation");

      GraphHelper.removeEdgeIf(company, (v, p) -> { 
        if (((Integer)p.get("workFrom")).compareTo(workFromYear) >= 0)
          return true;
        else 
          return false;
      });

      TraversalResult country = graph.traverse(company, "isLocatedIn", Direction.OUT, false, "Place");

      graph.getProperties(vProps, country.vSet, "name");

      company.vSet.removeIf(c -> {
        return !((String)vProps.get(country.vMap.get(c).get(0)).get("name")).equals(countryName);
      });

      graph.getProperties(vProps, company.vSet, "name");

      // Sort results ascending by the start date, then ascending by Person identifier, and lastly
      // by Organization name descending. Reversed for priority queue.
      Comparator<ResultTuple> comparator = new Comparator<ResultTuple>() {
            public int compare(ResultTuple a, ResultTuple b) {
              Long aId = a.v.id().getLowerLong();
              Long bId = b.v.id().getLowerLong();
              if (a.year != b.year)
                return -1*(a.year - b.year);
              else if (aId.compareTo(bId) != 0)
                return -1*aId.compareTo(bId);
              else
                return a.name.compareTo(b.name);
            }
          };

      PriorityQueue<ResultTuple> pq = new PriorityQueue(limit, comparator);
      for (Vertex f : company.vMap.keySet()) {
        List<Vertex> cList = company.vMap.get(f);
        List<Map<Object, Object>> pList = company.pMap.get(f);

        for (int i = 0; i < cList.size(); i++) {
          Vertex c = cList.get(i);
          Map<Object, Object> p = pList.get(i);

          if (!company.vSet.contains(c))
            continue;
          
          int year = ((Integer)p.get("workFrom")).intValue();
          String name = (String)vProps.get(c).get("name");

          if (pq.size() < limit) {
            pq.add(new ResultTuple(year, f, name));
            continue;
          }

          if (year < pq.peek().year) {
            pq.add(new ResultTuple(year, f, name));
            pq.poll();
          } else if (year == pq.peek().year) {
            if (f.id().getLowerLong() < pq.peek().v.id().getLowerLong()) {
              pq.add(new ResultTuple(year, f, name));
              pq.poll();
            } else if (f.id().getLowerLong() == pq.peek().v.id().getLowerLong()) {
              if (name.compareTo(pq.peek().name) > 0) {
                pq.add(new ResultTuple(year, f, name));
                pq.poll();
              }
            }
          }
        }
      }

      List<ResultTuple> rList = new ArrayList<>(pq.size());
      Set<Vertex> fSet = new HashSet<>(pq.size());
      while (pq.size() > 0) {
        ResultTuple rt = pq.poll();
        rList.add(rt);
        fSet.add(rt.v);
      }
      
      graph.getProperties(vProps, fSet);

      for (int i = rList.size()-1; i >= 0; i--) {
        ResultTuple rt = rList.get(i);

        result.add(new LdbcQuery11Result(
              rt.v.id().getLowerLong(),
              (String)vProps.get(rt.v).get("firstName"),
              (String)vProps.get(rt.v).get("lastName"),
              rt.name,
              rt.year));
      }

      resultReporter.report(result.size(), result, op);
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

    @Override
    public void executeOperation(final LdbcQuery12 op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState)dbConnState;

      if (cState.fakeComplexReads()) {
        List<Long> personIDs = cState.personIDFeed();
        List<Long> messageIDs = cState.messageIDFeed();


        List<LdbcQuery12Result> result = new ArrayList<>(op.limit());

        for (int i = 0; i < op.limit(); i++) {
          int n1 = ThreadLocalRandom.current().nextInt(0, personIDs.size());
          Long pid = personIDs.get(n1);
          result.add(new LdbcQuery12Result(
              pid,
              null,
              null,
              null,
              0));
        }

        resultReporter.report(result.size(), result, op);
        return;
      }

      // Parameters of this query
      final long personId = op.personId();
      final String tagClassName = op.tagClassName();
      final int limit = op.limit();

      final UInt128 torcPersonId = new UInt128(TorcEntity.PERSON.idSpace, personId);

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcQuery12 Start");

      List<LdbcQuery12Result> result = new ArrayList<>(limit);

      Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

      Vertex start = new Vertex(torcPersonId, TorcEntity.PERSON.label);
      TraversalResult startFriends = graph.traverse(start, "knows", Direction.OUT, false, "Person");
      TraversalResult friendComments = graph.traverse(startFriends, "hasCreator", Direction.IN, false, "Comment");
      TraversalResult commentPost = graph.traverse(friendComments, "replyOf", Direction.OUT, false, "Post");
      TraversalResult postTags = graph.traverse(commentPost, "hasTag", Direction.OUT, false, "Tag");
      TraversalResult tagClasses = graph.traverse(postTags, "hasType", Direction.OUT, false, "TagClass");

      // Find all the tags that are of the given type. Here we will comb
      // through the tagClasses and see which tags have the right type. The
      // rest may just be of a subType, so we traverse up the hasType tree for
      // the remaining tags.
      Set<Vertex> matchingTags = new HashSet<>(tagClasses.vMap.size());
      while (!tagClasses.vMap.isEmpty()) {
        graph.getProperties(vProps, tagClasses.vSet, "name");

        tagClasses.vMap.entrySet().removeIf( e -> {
            Vertex tag = (Vertex)e.getKey();
            Vertex tagClass = ((List<Vertex>)e.getValue()).get(0);
            
            if (((String)vProps.get(tagClass).get("name")).equals(tagClassName)) {
              matchingTags.add(tag);
              return true;
            }

            return false;
          });

        if (tagClasses.vMap.isEmpty())
          break;

        TraversalResult superTagClasses = graph.traverse(tagClasses, "hasType", Direction.OUT, false, "TagClass");
        tagClasses = GraphHelper.fuse(tagClasses, superTagClasses, false);
      }

      // We only care about the tags of the given type.
      GraphHelper.intersect(postTags, matchingTags);

      // Create map of comment to the set of all matching tags that were on
      // the post that the comment was in reply to.
      TraversalResult commentTags = GraphHelper.fuse(commentPost, postTags, false);

      // Filter for the comments that have non-zero matching tags.
      GraphHelper.intersect(friendComments, commentTags.vMap.keySet());

      // Create map of friend to the set of all matching tags that were on
      // posts that the friend commented on.
      TraversalResult friendTags = GraphHelper.fuse(friendComments, commentTags, true);

      // Sort in the reverse order from the query result order so that the
      // priority queue's "top" element is the least element.
      Comparator<Vertex> c = new Comparator<Vertex>() {
            public int compare(Vertex v1, Vertex v2) {
              int v1CommentCount = friendComments.vMap.get(v1).size();
              int v2CommentCount = friendComments.vMap.get(v2).size();

              if (v1CommentCount != v2CommentCount)
                return v1CommentCount - v2CommentCount;
              else
                return -1 * v1.id().compareTo(v2.id());
            }
          };

      PriorityQueue<Vertex> pq = new PriorityQueue(limit, c);
      for (Vertex f : friendComments.vMap.keySet()) {
        int commentCount = friendComments.vMap.get(f).size();

        if (pq.size() < limit) {
          pq.add(f);
          continue;
        }

        if (commentCount > friendComments.vMap.get(pq.peek()).size()) {
          pq.add(f);
          pq.poll();
        }
      }

      // Create a list from the priority queue. This list will contain the
      // friends in reverse order.
      List<Vertex> topFriends = new ArrayList<>(pq.size());
      while (pq.size() > 0)
        topFriends.add(pq.poll());

      // Fill in the properties for our results.
      graph.getProperties(vProps, topFriends);
      graph.getProperties(vProps, friendTags.vSet, "name");

      for (int i = topFriends.size()-1; i >= 0; i--) {
        Vertex f = topFriends.get(i);
        List<Vertex> tags = friendTags.vMap.get(f);

        List<String> tagNames = new ArrayList<>(tags.size());
        for (Vertex v : tags)
          tagNames.add(((String)vProps.get(v).get("name")));

        result.add(new LdbcQuery12Result(
            f.id().getLowerLong(),
            ((String)vProps.get(f).get("firstName")),
            ((String)vProps.get(f).get("lastName")),
            tagNames,
            friendComments.vMap.get(f).size()));
      }

      resultReporter.report(result.size(), result, op);
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

    @Override
    public void executeOperation(final LdbcQuery13 op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState)dbConnState;

      if (cState.fakeComplexReads()) {
        List<Long> personIDs = cState.personIDFeed();
        List<Long> messageIDs = cState.messageIDFeed();


        resultReporter.report(1, new LdbcQuery13Result(0), op);
        return;
      }
      
      // Parameters of this query
      final long person1Id = op.person1Id();
      final long person2Id = op.person2Id();

      if (person1Id == person2Id) {
        resultReporter.report(1, new LdbcQuery13Result(0), op);
        return;        
      }

      final UInt128 torcPerson1Id = 
          new UInt128(TorcEntity.PERSON.idSpace, person1Id);
      final UInt128 torcPerson2Id = 
          new UInt128(TorcEntity.PERSON.idSpace, person2Id);

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcQuery13 Start");

      Set<Vertex> start = new HashSet<>();
      start.add(new Vertex(torcPerson1Id, TorcEntity.PERSON.label));

      Set<Vertex> end = new HashSet<>();
      end.add(new Vertex(torcPerson2Id, TorcEntity.PERSON.label));

      TraversalResult startFriends = new TraversalResult(null, null, start);
      TraversalResult endFriends = new TraversalResult(null, null, end);
      Set<Vertex> startSeenSet = new HashSet<>();
      Set<Vertex> endSeenSet = new HashSet<>();
      int n = 1;
      do {
        startFriends = graph.traverse(startFriends, "knows", Direction.OUT, false, "Person");
        
        startFriends.vSet.removeAll(startSeenSet);
        
        // No path to destination vertex.
        if (startFriends.vSet.size() == 0) {
          n = -1;
          break;
        }

        if (!Collections.disjoint(startFriends.vSet, endFriends.vSet))
          break;

        startSeenSet.addAll(startFriends.vSet);

        n++;

        endFriends = graph.traverse(endFriends, "knows", Direction.OUT, false, "Person");

        endFriends.vSet.removeAll(endSeenSet);

        // No path to destination vertex.
        if (endFriends.vSet.size() == 0) {
          n = -1;
          break;
        }

        if (!Collections.disjoint(startFriends.vSet, endFriends.vSet))
          break;

        endSeenSet.addAll(endFriends.vSet);

        n++;
      } while (true);

      resultReporter.report(1, new LdbcQuery13Result(n), op);
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

    @Override
    public void executeOperation(final LdbcQuery14 op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState)dbConnState;

      if (cState.fakeComplexReads()) {
        List<Long> personIDs = cState.personIDFeed();
        List<Long> messageIDs = cState.messageIDFeed();


        List<LdbcQuery14Result> result = new ArrayList<>(1);
        
        List<Long> personIDsInPath = new ArrayList<>(2);
        personIDsInPath.add(op.person1Id());
        personIDsInPath.add(op.person2Id());

        result.add(new LdbcQuery14Result(
            personIDsInPath,
            42.0));

        resultReporter.report(result.size(), result, op);
        return;
      }

      // Define a linked-list datatype for paths of vertices.
      class VertexPath {
        public Vertex v;
        public VertexPath p;

        public VertexPath(Vertex v, VertexPath p) {
          this.v = v;
          this.p = p;
        }

        @Override
        public int hashCode() {
          if (p != null)
            return v.hashCode() ^ p.hashCode();
          else
            return v.hashCode();
        }

        @Override
        public boolean equals(final Object object) {
          if (object instanceof VertexPath) {
            VertexPath other = (VertexPath)object;
            if (p != null)
              return this.v.id().equals(other.v.id()) && this.p.equals(other.p);
            else
              return this.v.id().equals(other.v.id());
          }

          return false;
        }

        @Override
        public String toString() {
          StringBuilder sb = new StringBuilder();
          VertexPath it = this;
          sb.append("[");
          while (it != null) {
            if (it.p != null)
              sb.append(it.v.id().getLowerLong() + ",");
            else
              sb.append(it.v.id().getLowerLong());
            it = it.p;
          }
          sb.append("]");
          return sb.toString();
        }
      };

      // Define a vertex pair map key.
      class VertexPair {
        public Vertex v1;
        public Vertex v2;

        public VertexPair(Vertex v1, Vertex v2) {
          this.v1 = v1;
          this.v2 = v2;
        }

        @Override
        public int hashCode() {
          return v1.hashCode() ^ v2.hashCode();
        }

        @Override
        public boolean equals(final Object object) {
          if (object instanceof VertexPair) {
            VertexPair other = (VertexPair)object;
            return this.v1.id().equals(other.v1.id()) &&
                    this.v2.id().equals(other.v2.id());
          }

          return false;
        }

        @Override
        public String toString() {
          return String.format("(%X,%X)", v1.id().getLowerLong(), v2.id().getLowerLong());
        }
      };

      // Parameters of this query
      final long person1Id = op.person1Id();
      final long person2Id = op.person2Id();

      final UInt128 torcPerson1Id = 
          new UInt128(TorcEntity.PERSON.idSpace, person1Id);
      final UInt128 torcPerson2Id = 
          new UInt128(TorcEntity.PERSON.idSpace, person2Id);

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcQuery14 Start");

      List<LdbcQuery14Result> result = new ArrayList<>();

      Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

      Vertex start = new Vertex(torcPerson1Id, TorcEntity.PERSON.label);
      Vertex end = new Vertex(torcPerson2Id, TorcEntity.PERSON.label);
      
      Set<Vertex> startSet = new HashSet<>();
      startSet.add(start);

      Set<Vertex> endSet = new HashSet<>();
      endSet.add(end);

      TraversalResult startFriends = new TraversalResult(null, null, startSet);
      TraversalResult endFriends = new TraversalResult(null, null, endSet);
      Set<Vertex> startSeenSet = new HashSet<>();
      Set<Vertex> endSeenSet = new HashSet<>();
      List<TraversalResult> startTRList = new ArrayList<>();
      List<TraversalResult> endTRList = new ArrayList<>();
      int hops = 1;
      do {
        startFriends = graph.traverse(startFriends, "knows", Direction.OUT, false, "Person");
        
        startFriends.vSet.removeAll(startSeenSet);
        
        // No path to destination vertex.
        if (startFriends.vSet.size() == 0) {
          hops = -1;
          break;
        }

        startTRList.add(startFriends);

        if (!Collections.disjoint(startFriends.vSet, endFriends.vSet))
          break;

        startSeenSet.addAll(startFriends.vSet);

        hops++;

        endFriends = graph.traverse(endFriends, "knows", Direction.OUT, false, "Person");

        endFriends.vSet.removeAll(endSeenSet);

        // No path to destination vertex.
        if (endFriends.vSet.size() == 0) {
          hops = -1;
          break;
        }

        endTRList.add(endFriends);

        if (!Collections.disjoint(startFriends.vSet, endFriends.vSet))
          break;

        endSeenSet.addAll(endFriends.vSet);

        hops++;
      } while (true);
      
      if (hops != -1) {
        List<VertexPath> shortestPaths = new ArrayList<>();
        if (hops == 1) {
          // Special case where shortest path is of length 1
          shortestPaths.add(new VertexPath(start, new VertexPath(end, null)));
        } else {
          // Filter for paths that lead to the end vertex.
          for (int i = startTRList.size()-1; i >= 0; i--) {
            if (i == startTRList.size()-1)
              GraphHelper.intersect(startTRList.get(i), endTRList.get(endTRList.size()-1).vSet);
            else
              GraphHelper.intersect(startTRList.get(i), startTRList.get(i+1).vMap.keySet());
          }

          for (int i = endTRList.size()-1; i >= 0; i--) {
            if (i == endTRList.size()-1)
              GraphHelper.intersect(endTRList.get(i), startTRList.get(startTRList.size()-1).vSet);
            else
              GraphHelper.intersect(endTRList.get(i), endTRList.get(i+1).vMap.keySet());
          }

          // Path cache stores a list of all of the paths that start from a given vertex. All of the
          // paths in the list for a given vertex X start with vertex X. We then build this cache
          // "backwards" starting from the end of the traversal and working towards the beginning,
          // so that we finish with a list of all the paths starting from the start node. We also do
          // this from the end node too, since we have these two traversal result lists.
          Map<Vertex, List<VertexPath>> startPathCache = new HashMap<>();
          for (int i = startTRList.size()-1; i >= 0; i--) {
            for (Vertex b : startTRList.get(i).vMap.keySet()) {
              List<VertexPath> paths = new ArrayList<>();
              for (Vertex n : startTRList.get(i).vMap.get(b)) {
                if (!startPathCache.containsKey(n)) {
                  List<VertexPath> p = new ArrayList<>();
                  p.add(new VertexPath(n, null));
                  startPathCache.put(n, p);
                }

                for (VertexPath path : startPathCache.get(n)) {
                  paths.add(new VertexPath(b, path));
                }
              }

              startPathCache.put(b, paths);
            }
          }

          List<VertexPath> startPaths = startPathCache.get(start);
          
          Map<Vertex, List<VertexPath>> endPathCache = new HashMap<>();
          for (int i = endTRList.size()-1; i >= 0; i--) {
            for (Vertex b : endTRList.get(i).vMap.keySet()) {
              List<VertexPath> paths = new ArrayList<>();
              for (Vertex n : endTRList.get(i).vMap.get(b)) {
                if (!endPathCache.containsKey(n)) {
                  List<VertexPath> p = new ArrayList<>();
                  p.add(new VertexPath(n, null));
                  endPathCache.put(n, p);
                }

                for (VertexPath path : endPathCache.get(n)) {
                  paths.add(new VertexPath(b, path));
                }
              }

              endPathCache.put(b, paths);
            }
          }

          List<VertexPath> endPaths = endPathCache.get(end);

          // Now we use startPaths and endPaths to construct 'shortestPaths' which is a list of all
          // the shortest paths from start to end. We begin with organizing the end paths by the
          // vertex reached from the end.
          Map<Vertex, List<VertexPath>> invEndPaths = new HashMap<>();
          for (VertexPath p : endPaths) {
            VertexPath it = p;
            while (it.p != null)
              it = it.p;

            if (!invEndPaths.containsKey(it.v)) {
              List<VertexPath> path = new ArrayList<>();
              path.add(p);
              invEndPaths.put(it.v, path);
            } else {
              List<VertexPath> path = invEndPaths.get(it.v);
              path.add(p);
            }
          }

          for (VertexPath sp : startPaths) {
            VertexPath it = sp;
            while (it.p != null)
              it = it.p;

            for (VertexPath ep : invEndPaths.get(it.v)) {
              it = sp;
              VertexPath cpStart = null;
              VertexPath cpEnd = null;
              while (it != null) {
                VertexPath vp = new VertexPath(it.v, null);
                if (cpStart == null) {
                  cpStart = vp;
                  cpEnd = cpStart;
                } else {
                  cpEnd.p = vp;
                  cpEnd = vp;
                }
                it = it.p;
              }

              it = ep;
              VertexPath vp = null;
              while (!it.v.equals(cpEnd.v)) {
                vp = new VertexPath(it.v, vp);
                it = it.p;
              }
              cpEnd.p = vp;

              shortestPaths.add(cpStart);
            }
          }
        }

        // Calculate the path weights.
        Map<VertexPair, Double> pairWeights = new HashMap<>();
        Map<VertexPath, Double> pathWeights = new HashMap<>();
        Map<Vertex, TraversalResult[]> traversalResultCache = new HashMap<>();
        for (int i = 0; i < shortestPaths.size(); i++) {
          VertexPath path = shortestPaths.get(i);
          double pathWeight = 0.0;
          while (path != null) {
            if (path.p != null) {
              VertexPair vpair = new VertexPair(path.v, path.p.v);
              
              if (!pairWeights.containsKey(vpair)) {
                double pairWeight = 0.0;
                
                TraversalResult v1p;
                TraversalResult v1c;
                TraversalResult v1crp;
                TraversalResult v1crc;
                if (traversalResultCache.containsKey(vpair.v1)) {
                  TraversalResult results[] = traversalResultCache.get(vpair.v1);
                  v1p = results[0];
                  v1c = results[1];
                  v1crp = results[2];
                  v1crc = results[3];
                } else {
                  v1p = graph.traverse(vpair.v1, "hasCreator", Direction.IN, false, "Post");
                  v1c = graph.traverse(vpair.v1, "hasCreator", Direction.IN, false, "Comment");
                  v1crp = graph.traverse(v1c, "replyOf", Direction.OUT, false, "Post");
                  v1crc = graph.traverse(v1c, "replyOf", Direction.OUT, false, "Comment");
                  TraversalResult results[] = new TraversalResult[4];
                  results[0] = v1p;
                  results[1] = v1c;
                  results[2] = v1crp;
                  results[3] = v1crc;
                  traversalResultCache.put(vpair.v1, results);
                }

                TraversalResult v2p;
                TraversalResult v2c;
                TraversalResult v2crp;
                TraversalResult v2crc;
                if (traversalResultCache.containsKey(vpair.v2)) {
                  TraversalResult results[] = traversalResultCache.get(vpair.v2);
                  v2p = results[0];
                  v2c = results[1];
                  v2crp = results[2];
                  v2crc = results[3];
                } else {
                  v2p = graph.traverse(vpair.v2, "hasCreator", Direction.IN, false, "Post");
                  v2c = graph.traverse(vpair.v2, "hasCreator", Direction.IN, false, "Comment");
                  v2crp = graph.traverse(v2c, "replyOf", Direction.OUT, false, "Post");
                  v2crc = graph.traverse(v2c, "replyOf", Direction.OUT, false, "Comment");
                  TraversalResult results[] = new TraversalResult[4];
                  results[0] = v2p;
                  results[1] = v2c;
                  results[2] = v2crp;
                  results[3] = v2crc;
                  traversalResultCache.put(vpair.v2, results);
                }

                // First calculate weights of v1's comments on v2's junk.
                for (Vertex c : v1crp.vMap.keySet()) {
                  Vertex rp = v1crp.vMap.get(c).get(0);
                  if (v2p.vSet.contains(rp))
                    pairWeight += 1.0;
                }

                for (Vertex c : v1crc.vMap.keySet()) {
                  Vertex rc = v1crc.vMap.get(c).get(0);
                  if (v2c.vSet.contains(rc))
                    pairWeight += 0.5;
                }

                // Now do v2's comments on v1's junk.
                for (Vertex c : v2crp.vMap.keySet()) {
                  Vertex rp = v2crp.vMap.get(c).get(0);
                  if (v1p.vSet.contains(rp))
                    pairWeight += 1.0;
                }

                for (Vertex c : v2crc.vMap.keySet()) {
                  Vertex rc = v2crc.vMap.get(c).get(0);
                  if (v1c.vSet.contains(rc))
                    pairWeight += 0.5;
                }

                pairWeights.put(vpair, pairWeight);
              }

              pathWeight += pairWeights.get(vpair);
            }

            path = path.p;
          }

          pathWeights.put(shortestPaths.get(i), pathWeight);
        }

        Comparator<VertexPath> c = new Comparator<VertexPath>() {
              public int compare(VertexPath p1, VertexPath p2) {
                Double p1Weight = pathWeights.get(p1);
                Double p2Weight = pathWeights.get(p2);
       
                return p2Weight.compareTo(p1Weight);
              }
            };

        Collections.sort(shortestPaths, c);

        for (int i = 0; i < shortestPaths.size(); i++) {
          VertexPath path = shortestPaths.get(i);
          List<Long> ids = new ArrayList<>();
          while (path != null) {
            ids.add(path.v.id().getLowerLong());
            path = path.p;
          }

          result.add(new LdbcQuery14Result(ids, pathWeights.get(shortestPaths.get(i))));
        }
      }

      resultReporter.report(result.size(), result, op);
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

    @Override
    public void executeOperation(final LdbcShortQuery1PersonProfile op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcShortQuery1 Start");

      Vertex person = new Vertex(new UInt128(TorcEntity.PERSON.idSpace, op.personId()), 
          TorcEntity.PERSON.label);

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

        graph.getProperties(vProps, person);

        TraversalResult place = graph.traverse(person, "isLocatedIn", Direction.OUT, false, 
            "Place");

        LdbcShortQuery1PersonProfileResult result =
            new LdbcShortQuery1PersonProfileResult(
                (String)vProps.get(person).get("firstName"),
                (String)vProps.get(person).get("lastName"),
                (Long)vProps.get(person).get("birthday"),
                (String)vProps.get(person).get("locationIP"),
                (String)vProps.get(person).get("browserUsed"),
                place.vMap.get(person).get(0).id().getLowerLong(),
                (String)vProps.get(person).get("gender"),
                (Long)vProps.get(person).get("creationDate"));

        if (graph.commitAndSyncTx()) {
          resultReporter.report(1, result, op);
          break;
        }

        txAttempts++;
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

    @Override
    public void executeOperation(final LdbcShortQuery2PersonPosts op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcShortQuery2 Start");

      Vertex person = new Vertex(new UInt128(TorcEntity.PERSON.idSpace, op.personId()), 
          TorcEntity.PERSON.label);

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        List<LdbcShortQuery2PersonPostsResult> result = new ArrayList<>();

        TraversalResult messages = graph.traverse(person, "hasCreator", Direction.IN, false, 
            "Post", "Comment");

        Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

        graph.getProperties(vProps, messages.vSet, "creationDate");

        // Sort the Posts and Comments descending by creationDate, and descending by message
        // identifier. Reversed for priority queue.
        Comparator<Vertex> c = new Comparator<Vertex>() {
              public int compare(Vertex v1, Vertex v2) {
                Long v1creationDate = ((Long)vProps.get(v1).get("creationDate"));
                Long v2creationDate = ((Long)vProps.get(v2).get("creationDate"));
                if (v1creationDate.compareTo(v2creationDate) != 0)
                  return v1creationDate.compareTo(v2creationDate);
                else {
                  Long v1Id = v1.id().getLowerLong();
                  Long v2Id = v2.id().getLowerLong();
                  return v1Id.compareTo(v2Id);
                }
              }
            };

        PriorityQueue<Vertex> pq = new PriorityQueue(op.limit(), c);
        for (Vertex m : messages.vSet) {
          Long creationDate = (Long)vProps.get(m).get("creationDate");
         
          if (pq.size() < op.limit()) {
            pq.add(m);
            continue;
          }

          if (creationDate > (Long)vProps.get(pq.peek()).get("creationDate")) {
            pq.add(m);
            pq.poll();
          }
        }

        // Create a list from the priority queue with the elements sorted in the desired order.
        LinkedList<Vertex> msgList = new LinkedList<>();
        while (pq.size() > 0)
          msgList.addFirst(pq.poll());

        // Traverse "replyOf" relationships until we find the ancestor Post of all the Comment
        // messages in our result set.
        LinkedList<TraversalResult> trList = new LinkedList<>();
        trList.addLast(new TraversalResult(null, null, new HashSet<>(msgList)));
        Set<Vertex> postSet = new HashSet<>();
        while (true) {
          List<Vertex> cList = new ArrayList<>();
          for (Vertex v : trList.getLast().vSet)
            if (v.label().equals("Comment"))
              cList.add(v);
            else
              if (trList.size() > 1)
                postSet.add(v);

          if (cList.size() > 0) 
            trList.addLast(graph.traverse(cList, "replyOf", Direction.OUT, false, "Post", "Comment"));
          else
            break;
        }
        trList.removeFirst();

        TraversalResult originalAuthors = 
          graph.traverse(postSet, "hasCreator", Direction.OUT, false, "Person");

        List<Vertex> propFetch = new ArrayList<>(msgList.size() + postSet.size() + 
            originalAuthors.vSet.size() + 1);
        propFetch.addAll(msgList);
        propFetch.addAll(postSet);
        propFetch.addAll(originalAuthors.vSet);
        propFetch.add(person);
        graph.getProperties(vProps, propFetch);

        for (int i = 0; i < msgList.size(); i++) {
          Vertex m = msgList.get(i);

          String content = (String)vProps.get(m).get("content");
          if (content.equals(""))
            content = (String)vProps.get(m).get("imageFile");

          long originalPostId = -1;
          long originalPostAuthorId = -1;
          String originalPostAuthorFirstName = "";
          String originalPostAuthorLastName = "";
          if (m.label().equals("Post")) {
            originalPostId = m.id().getLowerLong();
            originalPostAuthorId = person.id().getLowerLong();
            originalPostAuthorFirstName = (String)vProps.get(person).get("firstName");
            originalPostAuthorLastName = (String)vProps.get(person).get("lastName");
          } else {
            Vertex base = m;
            for (int j = 0; j < trList.size(); j++) {
              TraversalResult tr = trList.get(j);
              Vertex replyToV = tr.vMap.get(base).get(0);
              if (replyToV.label().equals("Post")) {
                Vertex author = originalAuthors.vMap.get(replyToV).get(0);
                originalPostId = replyToV.id().getLowerLong();
                originalPostAuthorId = author.id().getLowerLong();
                originalPostAuthorFirstName = (String)vProps.get(author).get("firstName");
                originalPostAuthorLastName = (String)vProps.get(author).get("lastName");
                break;
              }

              base = replyToV;
            }
          }

          result.add(new LdbcShortQuery2PersonPostsResult(
                  m.id().getLowerLong(), //messageId,
                  content, //messageContent,
                  (Long)vProps.get(m).get("creationDate"), //messageCreationDate,
                  originalPostId,
                  originalPostAuthorId,
                  originalPostAuthorFirstName,
                  originalPostAuthorLastName));
        }

        if (graph.commitAndSyncTx()) {
          resultReporter.report(result.size(), result, op);
          break;
        }

        txAttempts++;
      }
    }
  }

  /**
   * Given a start Person, retrieve all of their friends, and the date at which
   * they became friends. Order results descending by friendship creation date,
   * then ascending by friend identifier.[1]
   */
  public static class LdbcShortQuery3PersonFriendsHandler implements
      OperationHandler<LdbcShortQuery3PersonFriends, DbConnectionState> {

    @Override
    public void executeOperation(final LdbcShortQuery3PersonFriends op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcShortQuery3 Start");

      Vertex person = new Vertex(new UInt128(TorcEntity.PERSON.idSpace, op.personId()), 
          TorcEntity.PERSON.label);

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        List<LdbcShortQuery3PersonFriendsResult> result = new ArrayList<>();

        TraversalResult friends = graph.traverse(person, "knows", Direction.OUT, true, "Person");

        if (friends.vMap.containsKey(person)) {
          List<Vertex> friendList = friends.vMap.get(person);

          // Create map from friend to creationDate to use for sorting.
          final Map<Vertex, Long> friendshipDate = new HashMap<>(friendList.size());
          for (int i = 0; i < friendList.size(); i++) {
            Vertex friend = friendList.get(i);
            Map<Object, Object> edgeProps = friends.pMap.get(person).get(i);
            Long creationDate = (Long)edgeProps.get("creationDate");
            friendshipDate.put(friend, creationDate);
          }
          
          // Sort friends descending by creationDate, and ascending by friend identifier.
          Comparator<Vertex> c = new Comparator<Vertex>() {
                public int compare(Vertex v1, Vertex v2) {
                  Long v1creationDate = friendshipDate.get(v1);
                  Long v2creationDate = friendshipDate.get(v2);
                  if (v1creationDate.compareTo(v2creationDate) != 0)
                    return -1*v1creationDate.compareTo(v2creationDate);
                  else {
                    if (v1.id().getLowerLong() - v2.id().getLowerLong() > 0)
                      return 1;
                    else if (v1.id().getLowerLong() - v2.id().getLowerLong() < 0)
                      return -1;
                    else
                      return 0;
                  }
                }
              };
          
          Collections.sort(friendList, c);

          Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

          graph.getProperties(vProps, friendList);

          for (int i = 0; i < friendList.size(); i++) {
            Vertex f = friendList.get(i);
            result.add(new LdbcShortQuery3PersonFriendsResult(
                    f.id().getLowerLong(),
                    (String)vProps.get(f).get("firstName"),
                    (String)vProps.get(f).get("lastName"),
                    friendshipDate.get(f).longValue()));
          }
        }

        if (graph.commitAndSyncTx()) {
          resultReporter.report(result.size(), result, op);
          break;
        }

        txAttempts++;
      }
    }
  }

  /**
   * Given a Message (Post or Comment), retrieve its content and creation
   * date.[1]
   */
  public static class LdbcShortQuery4MessageContentHandler implements
      OperationHandler<LdbcShortQuery4MessageContent, DbConnectionState> {

    @Override
    public void executeOperation(final LdbcShortQuery4MessageContent op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcShortQuery4 Start");

      // So this is an interesting case. LDBC SNB treats ID spaces and labels seaparately, and this
      // is a perfect example. Here, Message is an ID space, and "Comment" and "Post" are labels
      // that vertices in the "Message" ID space can have. But in TorcDB2, we think of labels
      // themselves as the ID space. So since we don't actually know the label in this case we need
      // to read both, but we rely on the fact that the message ID that's provided refers to either
      // a Post or a Comment, but not both. For now we just do a trick where we read an imaginary
      // "Messaged" labeled vertex.
      Vertex message = new Vertex(new UInt128(TorcEntity.COMMENT.idSpace, op.messageId()), 
          "Message");

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

        graph.getProperties(vProps, message);
        
        Map<Object, Object> props = vProps.get(message);
         
        String content = (String)props.get("content");
        if (content.equals(""))
          content = (String)props.get("imageFile");
        
        long creationDate = ((Long)props.get("creationDate")).longValue();

        LdbcShortQuery4MessageContentResult result =
            new LdbcShortQuery4MessageContentResult(
                content,
                creationDate);

        if (graph.commitAndSyncTx()) {
          resultReporter.report(1, result, op);
          break;
        }

        txAttempts++;
      }
    }
  }

  /**
   * Given a Message (Post or Comment), retrieve its author.[1]
   */
  public static class LdbcShortQuery5MessageCreatorHandler implements
      OperationHandler<LdbcShortQuery5MessageCreator, DbConnectionState> {

    @Override
    public void executeOperation(final LdbcShortQuery5MessageCreator op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcShortQuery5 Start");

      // So this is an interesting case. LDBC SNB treats ID spaces and labels seaparately, and this
      // is a perfect example. Here, Message is an ID space, and "Comment" and "Post" are labels
      // that vertices in the "Message" ID space can have. But in TorcDB2, we think of labels
      // themselves as the ID space. So since we don't actually know the label in this case we need
      // to read both, but we rely on the fact that the message ID that's provided refers to either
      // a Post or a Comment, but not both. For now we just do a trick where we read an imaginary
      // "Messaged" labeled vertex.
      Vertex message = new Vertex(new UInt128(TorcEntity.COMMENT.idSpace, op.messageId()), 
          "Message");

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        TraversalResult creator = graph.traverse(message, "hasCreator", Direction.OUT, false, 
            "Person");

        Vertex author = creator.vMap.get(message).get(0);

        Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

        graph.getProperties(vProps, author);

        LdbcShortQuery5MessageCreatorResult result =
            new LdbcShortQuery5MessageCreatorResult(
                author.id().getLowerLong(),
                (String)vProps.get(author).get("firstName"),
                (String)vProps.get(author).get("lastName"));

        if (graph.commitAndSyncTx()) {
          resultReporter.report(1, result, op);
          break;
        }

        txAttempts++;
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

    @Override
    public void executeOperation(final LdbcShortQuery6MessageForum op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcShortQuery6 Start");

      // So this is an interesting case. LDBC SNB treats ID spaces and labels seaparately, and this
      // is a perfect example. Here, Message is an ID space, and "Comment" and "Post" are labels
      // that vertices in the "Message" ID space can have. But in TorcDB2, we think of labels
      // themselves as the ID space. So since we don't actually know the label in this case we need
      // to read both, but we rely on the fact that the message ID that's provided refers to either
      // a Post or a Comment, but not both. For now we just do a trick where we read an imaginary
      // "Messaged" labeled vertex.
      Vertex message = new Vertex(new UInt128(TorcEntity.COMMENT.idSpace, op.messageId()), 
          "Message");

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        // Traverse replyOf relationships to get post, forum, and moderator.
        Vertex v = message;
        Vertex post = null;
        Vertex forum = null;
        Vertex moderator = null;
        while (true) {
          TraversalResult replyOf = graph.traverse(v, "replyOf", Direction.OUT, false, "Post", "Comment");
          if (replyOf.vMap.size() == 0) {
            forum = graph.traverse(v, "containerOf", Direction.IN, false, "Forum").vMap.get(v).get(0);
            moderator = graph.traverse(forum, "hasModerator", Direction.OUT, false, "Person").vMap.get(forum).get(0);
            break;
          }
          v = replyOf.vMap.get(v).get(0);
        }

        Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

        graph.getProperties(vProps, forum, moderator);

        LdbcShortQuery6MessageForumResult result = 
          new LdbcShortQuery6MessageForumResult(
                forum.id().getLowerLong(),
                (String)vProps.get(forum).get("title"),
                moderator.id().getLowerLong(),
                (String)vProps.get(moderator).get("firstName"),
                (String)vProps.get(moderator).get("lastName"));

        if (graph.commitAndSyncTx()) {
          resultReporter.report(1, result, op);
          break;
        }

        txAttempts++;
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

    @Override
    public void executeOperation(final LdbcShortQuery7MessageReplies op,
        DbConnectionState dbConnState,
        ResultReporter resultReporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcShortQuery7 Start");

      // So this is an interesting case. LDBC SNB treats ID spaces and labels seaparately, and this
      // is a perfect example. Here, Message is an ID space, and "Comment" and "Post" are labels
      // that vertices in the "Message" ID space can have. But in TorcDB2, we think of labels
      // themselves as the ID space. So since we don't actually know the label in this case we need
      // to read both, but we rely on the fact that the message ID that's provided refers to either
      // a Post or a Comment, but not both. For now we just do a trick where we read an imaginary
      // "Messaged" labeled vertex.
      Vertex message = new Vertex(new UInt128(TorcEntity.COMMENT.idSpace, op.messageId()), 
          "Message");

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        TraversalResult author = graph.traverse(message, "hasCreator", Direction.OUT, false, "Person");
        TraversalResult replies = graph.traverse(message, "replyOf", Direction.IN, false, "Comment");
        TraversalResult replyAuthors = graph.traverse(replies, "hasCreator", Direction.OUT, false, "Person");
        TraversalResult friends = graph.traverse(author, "knows", Direction.OUT, false, "Person");

        Map<Vertex, Map<Object, Object>> vProps = new HashMap<>();

        graph.getProperties(vProps, replies, replyAuthors);

        List<LdbcShortQuery7MessageRepliesResult> result = new ArrayList<>();

        if (replies.vMap.containsKey(message)) {
          for (Vertex reply : replies.vMap.get(message)) {
            String content = (String)vProps.get(reply).get("content");
            if (content.equals(""))
              content = (String)vProps.get(reply).get("imageFile");

            Vertex replyAuthor = replyAuthors.vMap.get(reply).get(0);

            result.add(new LdbcShortQuery7MessageRepliesResult(
                    reply.id().getLowerLong(),
                    content,
                    (Long)vProps.get(reply).get("creationDate"),
                    replyAuthor.id().getLowerLong(),
                    (String)vProps.get(replyAuthor).get("firstName"),
                    (String)vProps.get(replyAuthor).get("lastName"),
                    friends.vSet.contains(replyAuthor)));
          }

          // Sort results descending by creationDate, and ascending by author identifier.
          Comparator<LdbcShortQuery7MessageRepliesResult> c = new Comparator<LdbcShortQuery7MessageRepliesResult>() {
                public int compare(LdbcShortQuery7MessageRepliesResult r1, LdbcShortQuery7MessageRepliesResult r2) {
                  long r1creationDate = r1.commentCreationDate();
                  long r2creationDate = r2.commentCreationDate();
                  if (r1creationDate != r2creationDate)
                    return -1*(int)(r1creationDate - r2creationDate);
                  else 
                    return (int)(r1.replyAuthorId() - r2.replyAuthorId());
                }
              };
          
          Collections.sort(result, c);
        }

        if (graph.commitAndSyncTx()) {
          resultReporter.report(result.size(), result, op);
          break;
        }

        txAttempts++;
      }
    }
  }

  /**
   * ------------------------------------------------------------------------
   * Update Queries
   * ------------------------------------------------------------------------
   */

  /**
   * Add a Person to the social network. [1]
   */
  public static class LdbcUpdate1AddPersonHandler implements
      OperationHandler<LdbcUpdate1AddPerson, DbConnectionState> {

    private final Calendar calendar;

    public LdbcUpdate1AddPersonHandler() {
      this.calendar = new GregorianCalendar();
    }

    @Override
    public void executeOperation(LdbcUpdate1AddPerson op, DbConnectionState dbConnState,
        ResultReporter reporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;
      if (cState.fakeUpdates()) {
        reporter.report(0, LdbcNoResult.INSTANCE, op);
      }

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcUpdate1 Start");

      // Build key value properties map
      Map<Object, Object> personProps = new HashMap<>();
      personProps.put("firstName", op.personFirstName());
      personProps.put("lastName", op.personLastName());
      personProps.put("gender", op.gender());
      personProps.put("birthday", new Long(op.birthday().getTime()));
      personProps.put("creationDate", new Long(op.creationDate().getTime()));
      personProps.put("locationIP", op.locationIp());
      personProps.put("browserUsed", op.browserUsed());
      personProps.put("language", op.languages());
      personProps.put("email", op.emails());

      Vertex person = new Vertex(new UInt128(TorcEntity.PERSON.idSpace, op.personId()), 
          TorcEntity.PERSON.label);

      Vertex place = new Vertex(new UInt128(TorcEntity.PLACE.idSpace, op.cityId()), 
          TorcEntity.PLACE.label);

      List<Vertex> tags = new ArrayList<>(op.tagIds().size());
      op.tagIds().forEach((tagid) -> tags.add(
            new Vertex(new UInt128(TorcEntity.TAG.idSpace, tagid), TorcEntity.TAG.label)));

      List<Vertex> universities = new ArrayList<>(op.studyAt().size());
      List<Map<Object, Object>> studyAtProps = new ArrayList<>(op.studyAt().size());
      op.studyAt().forEach((org) -> {
          universities.add(new Vertex(new UInt128(TorcEntity.ORGANISATION.idSpace, 
                  org.organizationId()), TorcEntity.ORGANISATION.label));
          Map<Object, Object> sprops = new HashMap<>();
          sprops.put("classYear", new Integer(org.year()));
          studyAtProps.add(sprops);
      });

      List<Vertex> companies = new ArrayList<>(op.workAt().size());
      List<Map<Object, Object>> workAtProps = new ArrayList<>(op.workAt().size());
      op.workAt().forEach((org) -> {
          companies.add(new Vertex(new UInt128(TorcEntity.ORGANISATION.idSpace, 
                  org.organizationId()), TorcEntity.ORGANISATION.label));
          Map<Object, Object> wprops = new HashMap<>();
          wprops.put("workFrom", new Integer(org.year()));
          workAtProps.add(wprops);
      });

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        graph.addVertex(person, personProps);

        graph.addEdge(person, "isLocatedIn", place, null);

        for (Vertex tag : tags)
          graph.addEdge(person, "hasInterest", tag, null);

        for (int i = 0; i < universities.size(); i++)
          graph.addEdge(person, "studyAt", universities.get(i), studyAtProps.get(i));

        for (int i = 0; i < companies.size(); i++)
          graph.addEdge(person, "workAt", companies.get(i), workAtProps.get(i));

        if (graph.commitAndSyncTx()) {
          reporter.report(0, LdbcNoResult.INSTANCE, op);
          break;
        }

        txAttempts++;
      }
    }
  }

  /**
   * Add a Like to a Post of the social network.[1]
   */
  public static class LdbcUpdate2AddPostLikeHandler implements
      OperationHandler<LdbcUpdate2AddPostLike, DbConnectionState> {

    @Override
    public void executeOperation(LdbcUpdate2AddPostLike op, DbConnectionState dbConnState,
        ResultReporter reporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;
      if (cState.fakeUpdates()) {
        reporter.report(0, LdbcNoResult.INSTANCE, op);
      }

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcUpdate2 Start");

      Vertex person = new Vertex(new UInt128(TorcEntity.PERSON.idSpace, op.personId()), 
          TorcEntity.PERSON.label);
      Vertex post = new Vertex(new UInt128(TorcEntity.POST.idSpace, op.postId()),
          TorcEntity.POST.label);

      Map<Object, Object> eprops = new HashMap<>();
      eprops.put("creationDate", new Long(op.creationDate().getTime()));

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        graph.addEdge(person, "likes", post, eprops);

        if (graph.commitAndSyncTx()) {
          reporter.report(0, LdbcNoResult.INSTANCE, op);
          break;
        }

        txAttempts++;
      }
    }
  }

  /**
   * Add a Like to a Comment of the social network.[1]
   */
  public static class LdbcUpdate3AddCommentLikeHandler implements
      OperationHandler<LdbcUpdate3AddCommentLike, DbConnectionState> {

    @Override
    public void executeOperation(LdbcUpdate3AddCommentLike op,
        DbConnectionState dbConnState,
        ResultReporter reporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;
      if (cState.fakeUpdates()) {
        reporter.report(0, LdbcNoResult.INSTANCE, op);
      }

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcUpdate3 Start");

      Vertex person = new Vertex(new UInt128(TorcEntity.PERSON.idSpace, op.personId()), 
          TorcEntity.PERSON.label);
      Vertex comment = new Vertex(new UInt128(TorcEntity.COMMENT.idSpace, op.commentId()),
          TorcEntity.COMMENT.label);

      Map<Object, Object> eprops = new HashMap<>();
      eprops.put("creationDate", new Long(op.creationDate().getTime()));

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        graph.addEdge(person, "likes", comment, eprops);

        if (graph.commitAndSyncTx()) {
          reporter.report(0, LdbcNoResult.INSTANCE, op);
          break;
        }

        txAttempts++;
      }
    }
  }

  /**
   * Add a Forum to the social network.[1]
   */
  public static class LdbcUpdate4AddForumHandler implements
      OperationHandler<LdbcUpdate4AddForum, DbConnectionState> {

    @Override
    public void executeOperation(LdbcUpdate4AddForum op,
        DbConnectionState dbConnState,
        ResultReporter reporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;
      if (cState.fakeUpdates()) {
        reporter.report(0, LdbcNoResult.INSTANCE, op);
      }

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcUpdate4 Start");

      // Build key value properties map
      Map<Object, Object> forumProps = new HashMap<>();
      forumProps.put("title", op.forumTitle());
      forumProps.put("creationDate", new Long(op.creationDate().getTime()));
      
      Vertex forum = new Vertex(new UInt128(TorcEntity.FORUM.idSpace, op.forumId()), 
          TorcEntity.FORUM.label);

      List<Vertex> tags = new ArrayList<>(op.tagIds().size());
      op.tagIds().forEach((tagid) -> tags.add(
            new Vertex(new UInt128(TorcEntity.TAG.idSpace, tagid), TorcEntity.TAG.label)));

      Vertex moderator = new Vertex(new UInt128(TorcEntity.PERSON.idSpace, op.moderatorPersonId()), 
          TorcEntity.PERSON.label);

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        graph.addVertex(forum, forumProps);

        for (Vertex tag : tags)
          graph.addEdge(forum, "hasTag", tag, null);

        graph.addEdge(forum, "hasModerator", moderator, null);

        if (graph.commitAndSyncTx()) {
          reporter.report(0, LdbcNoResult.INSTANCE, op);
          break;
        }

        txAttempts++;
      }
    }
  }

  /**
   * Add a Forum membership to the social network.[1]
   */
  public static class LdbcUpdate5AddForumMembershipHandler implements
      OperationHandler<LdbcUpdate5AddForumMembership, DbConnectionState> {

    @Override
    public void executeOperation(LdbcUpdate5AddForumMembership op, DbConnectionState dbConnState,
        ResultReporter reporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;
      if (cState.fakeUpdates()) {
        reporter.report(0, LdbcNoResult.INSTANCE, op);
      }

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcUpdate5 Start");

      Vertex person = new Vertex(new UInt128(TorcEntity.PERSON.idSpace, op.personId()), 
          TorcEntity.PERSON.label);
      Vertex forum = new Vertex(new UInt128(TorcEntity.FORUM.idSpace, op.forumId()), 
          TorcEntity.FORUM.label);

      Map<Object, Object> eprops = new HashMap<>();
      eprops.put("joinDate", new Long(op.joinDate().getTime()));

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        graph.addEdge(forum, "hasMember", person, eprops);

        if (graph.commitAndSyncTx()) {
          reporter.report(0, LdbcNoResult.INSTANCE, op);
          break;
        }

        txAttempts++;
      }
    }
  }

  /**
   * Add a Post to the social network.[1]
   */
  public static class LdbcUpdate6AddPostHandler implements
      OperationHandler<LdbcUpdate6AddPost, DbConnectionState> {

    @Override
    public void executeOperation(LdbcUpdate6AddPost op, DbConnectionState dbConnState,
        ResultReporter reporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;
      if (cState.fakeUpdates()) {
        reporter.report(0, LdbcNoResult.INSTANCE, op);
      }

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcUpdate6 Start");

      // Build key value properties map
      Map<Object, Object> postProps = new HashMap<>();
      postProps.put("imageFile", op.imageFile());
      postProps.put("creationDate", new Long(op.creationDate().getTime()));
      postProps.put("locationIP", op.locationIp());
      postProps.put("browserUsed", op.browserUsed());
      postProps.put("language", op.language());
      postProps.put("content", op.content());
      postProps.put("length", new Integer(op.length()));

      Vertex post = new Vertex(new UInt128(TorcEntity.POST.idSpace, op.postId()),
          TorcEntity.POST.label);
      Vertex author = new Vertex(new UInt128(TorcEntity.PERSON.idSpace, op.authorPersonId()), 
          TorcEntity.PERSON.label);
      Vertex forum = new Vertex(new UInt128(TorcEntity.FORUM.idSpace, op.forumId()), 
          TorcEntity.FORUM.label);
      Vertex place = new Vertex(new UInt128(TorcEntity.PLACE.idSpace, op.countryId()), 
          TorcEntity.PLACE.label);

      List<Vertex> tags = new ArrayList<>(op.tagIds().size());
      op.tagIds().forEach((tagid) -> tags.add(
            new Vertex(new UInt128(TorcEntity.TAG.idSpace, tagid), TorcEntity.TAG.label)));

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        graph.addVertex(post, postProps);
        graph.addEdge(post, "hasCreator", author, null);
        graph.addEdge(forum, "containerOf", post, null);
        graph.addEdge(post, "isLocatedIn", place, null);

        for (Vertex tag : tags)
          graph.addEdge(post, "hasTag", tag, null);

        if (graph.commitAndSyncTx()) {
          reporter.report(0, LdbcNoResult.INSTANCE, op);
          break;
        }

        txAttempts++;
      }
    }
  }

  /**
   * Add a Comment replying to a Post/Comment to the social network.[1]
   */
  public static class LdbcUpdate7AddCommentHandler implements
      OperationHandler<LdbcUpdate7AddComment, DbConnectionState> {

    @Override
    public void executeOperation(LdbcUpdate7AddComment op, DbConnectionState dbConnState,
        ResultReporter reporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;
      if (cState.fakeUpdates()) {
        reporter.report(0, LdbcNoResult.INSTANCE, op);
      }

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcUpdate7 Start");

      // Build key value properties map
      Map<Object, Object> commentProps = new HashMap<>();
      commentProps.put("creationDate", new Long(op.creationDate().getTime()));
      commentProps.put("locationIP", op.locationIp());
      commentProps.put("browserUsed", op.browserUsed());
      commentProps.put("content", op.content());
      commentProps.put("length", new Integer(op.length()));

      Vertex comment = new Vertex(new UInt128(TorcEntity.COMMENT.idSpace, op.commentId()),
          TorcEntity.COMMENT.label);
      Vertex author = new Vertex(new UInt128(TorcEntity.PERSON.idSpace, op.authorPersonId()),
          TorcEntity.PERSON.label);
      Vertex place = new Vertex(new UInt128(TorcEntity.PLACE.idSpace, op.countryId()), 
          TorcEntity.PLACE.label);

      Vertex message = null;
      if (op.replyToCommentId() != -1)
        message = new Vertex(new UInt128(TorcEntity.COMMENT.idSpace, op.replyToCommentId()),
            TorcEntity.COMMENT.label);
      else if (op.replyToPostId() != -1)
        message = new Vertex(new UInt128(TorcEntity.POST.idSpace, op.replyToPostId()),
            TorcEntity.POST.label);

      List<Vertex> tags = new ArrayList<>(op.tagIds().size());
      op.tagIds().forEach((tagid) -> tags.add(
            new Vertex(new UInt128(TorcEntity.TAG.idSpace, tagid), TorcEntity.TAG.label)));

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        graph.addVertex(comment, commentProps);
        graph.addEdge(comment, "hasCreator", author, null);
        graph.addEdge(comment, "isLocatedIn", place, null);
        graph.addEdge(comment, "replyOf", message, null);

        for (Vertex tag : tags)
          graph.addEdge(comment, "hasTag", tag, null);

        if (graph.commitAndSyncTx()) {
          reporter.report(0, LdbcNoResult.INSTANCE, op);
          break;
        }

        txAttempts++;
      }
    }
  }

  /**
   * Add a friendship relation to the social network.[1]
   */
  public static class LdbcUpdate8AddFriendshipHandler implements
      OperationHandler<LdbcUpdate8AddFriendship, DbConnectionState> {

    @Override
    public void executeOperation(LdbcUpdate8AddFriendship op, DbConnectionState dbConnState,
        ResultReporter reporter) throws DbException {
      TorcDb2ConnectionState cState = (TorcDb2ConnectionState) dbConnState;
      if (cState.fakeUpdates()) {
        reporter.report(0, LdbcNoResult.INSTANCE, op);
      }

      Graph graph = cState.getGraph();
//    graph.getClient().nanoLogPrint("LdbcUpdate8 Start");

      // Build key value properties map
      Map<Object, Object> props = new HashMap<>();
      props.put("creationDate", new Long(op.creationDate().getTime()));

      Vertex person1 = new Vertex(new UInt128(TorcEntity.PERSON.idSpace, op.person1Id()), 
          TorcEntity.PERSON.label);
      Vertex person2 = new Vertex(new UInt128(TorcEntity.PERSON.idSpace, op.person2Id()), 
          TorcEntity.PERSON.label);

      int txAttempts = 0;
      while (txAttempts < MAX_TX_ATTEMPTS) {
        graph.beginTx();

        graph.addEdge(person1, "knows", person2, props);
        graph.addEdge(person2, "knows", person1, props);

        if (graph.commitAndSyncTx()) {
          reporter.report(0, LdbcNoResult.INSTANCE, op);
          break;
        }

        txAttempts++;
      }
    }
  }
}
