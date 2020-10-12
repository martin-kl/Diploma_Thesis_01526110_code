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
package net.ellitron.ldbcsnbimpls.interactive.torc;

import net.ellitron.ldbcsnbimpls.interactive.torc.TorcDb.*;
import net.ellitron.ldbcsnbimpls.interactive.torc.TorcDbClient.*;
import net.ellitron.ldbcsnbimpls.interactive.torc.LdbcSerializableQueriesAndResults.*;

import com.ldbc.driver.control.LoggingService;
import com.ldbc.driver.Db;
import com.ldbc.driver.DbConnectionState;
import com.ldbc.driver.DbException;
import com.ldbc.driver.Operation;
import com.ldbc.driver.OperationHandler;
import com.ldbc.driver.ResultReporter;
import com.ldbc.driver.runtime.ConcurrentErrorReporter;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcNoResult;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcQuery1;
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
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate2AddPostLike;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate3AddCommentLike;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate4AddForum;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate5AddForumMembership;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate6AddPost;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate7AddComment;
import com.ldbc.driver.workloads.ldbc.snb.interactive.LdbcUpdate8AddFriendship;

import org.docopt.Docopt;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A multithreaded server that executes LDBC SNB Interactive Workload queries
 * against TorcDB on behalf of remote clients. 
 *
 * @author Jonathan Ellithorpe (jde@cs.stanford.edu)
 */
public class TorcDbServer {

  private static final String doc =
      "TorcDbServer: A multithreaded server that executes LDBC SNB\n"
      + "Interactive Workload queries against TorcDB on behalf of remote\n"
      + "clients.\n"
      + "\n"
      + "Usage:\n"
      + "  TorcDbServer [options] COORDLOC GRAPHNAME\n"
      + "  TorcDbServer (-h | --help)\n"
      + "  TorcDbServer --version\n"
      + "\n"
      + "Arguments:\n"
      + "  COORDLOC   RAMCloud coordinator locator string.\n"
      + "  GRAPHNAME  Name of TorcDB graph to execute queries against.\n"
      + "\n"
      + "Options:\n"
      + "  --port=<n>        Port on which to listen for new connections.\n"
      + "                    [default: 5577].\n"
      + "  --verbose         Print verbose output to stdout.\n"
      + "  -h --help         Show this screen.\n"
      + "  --version         Show version.\n"
      + "\n";

  /**
   * Thread that listens for connections and spins off new threads to serve
   * client connections.
   */
  private static class ListenerThread implements Runnable {

    // Port on which we listen for incoming connections.
    private final int port;

    // Passed off to each client thread for executing queries.
    private final TorcDbConnectionState connectionState;
    private final Map<Class<? extends Operation>, OperationHandler> 
        queryHandlerMap;
    private final ConcurrentErrorReporter concurrentErrorReporter;
    private int clientID = 1;

    public ListenerThread(int port, TorcDbConnectionState connectionState,
        Map<Class<? extends Operation>, OperationHandler> queryHandlerMap,
        ConcurrentErrorReporter concurrentErrorReporter) {
      this.port = port;
      this.connectionState = connectionState;
      this.queryHandlerMap = queryHandlerMap;
      this.concurrentErrorReporter = concurrentErrorReporter;
    }

    @Override
    public void run() {
      try {
        ServerSocket server = new ServerSocket(port);

        System.out.println("Listening on: " + server.toString());

        while (true) {
          Socket client = server.accept();

          System.out.println("Client connected: " + client.toString());

          Thread clientThread = new Thread(new ClientThread(client, 
               concurrentErrorReporter, connectionState, queryHandlerMap,
               clientID));

          clientThread.start();

          clientID++;
        }

//        server.close();
      } catch (Exception e) {

      }
    }
  }

  /**
   * Thread that receives requests from clients, executes them, and returns a
   * response. Handles requests for the lifetime of the connection to the
   * client.
   */
  private static class ClientThread implements Runnable {

    private final Socket client;
    private final ConcurrentErrorReporter concurrentErrorReporter;
    private final ResultReporter resultReporter;
    private final TorcDbConnectionState connectionState;
    private final Map<Class<? extends Operation>, OperationHandler> 
        queryHandlerMap;
    private final int clientID;

    public ClientThread(Socket client, 
        ConcurrentErrorReporter concurrentErrorReporter, 
        TorcDbConnectionState connectionState,
        Map<Class<? extends Operation>, OperationHandler> queryHandlerMap,
        int clientID) {
      this.client = client;
      this.concurrentErrorReporter = concurrentErrorReporter;
      this.resultReporter = 
          new ResultReporter.SimpleResultReporter(concurrentErrorReporter);
      this.connectionState = connectionState;
      this.queryHandlerMap = queryHandlerMap;
      this.clientID = clientID;
    }

    public void run() {
      try {
        ObjectInputStream in = new ObjectInputStream(client.getInputStream());
        ObjectOutputStream out = 
            new ObjectOutputStream(client.getOutputStream());

        while (true) {
          Object query = in.readObject();

          System.out.println("Client " + clientID + " Received Query: " + query.toString());

          if (query instanceof LdbcQuery1Serializable) {
            LdbcQuery1 op = ((LdbcQuery1Serializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op, 
                connectionState, resultReporter);
            List<LdbcQuery1Result> result = 
                (List<LdbcQuery1Result>) resultReporter.result();

            List<LdbcQuery1ResultSerializable> resp = new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcQuery1ResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush();
          } else if (query instanceof LdbcQuery2Serializable) {
            LdbcQuery2 op = ((LdbcQuery2Serializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcQuery2Result> result = 
                (List<LdbcQuery2Result>) resultReporter.result();

            List<LdbcQuery2ResultSerializable> resp = new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcQuery2ResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcQuery3Serializable) {
            LdbcQuery3 op = ((LdbcQuery3Serializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcQuery3Result> result = 
                (List<LdbcQuery3Result>) resultReporter.result();

            List<LdbcQuery3ResultSerializable> resp = new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcQuery3ResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcQuery4Serializable) {
            LdbcQuery4 op = ((LdbcQuery4Serializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcQuery4Result> result = 
                (List<LdbcQuery4Result>) resultReporter.result();

            List<LdbcQuery4ResultSerializable> resp = new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcQuery4ResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcQuery5Serializable) {
            LdbcQuery5 op = ((LdbcQuery5Serializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcQuery5Result> result = 
                (List<LdbcQuery5Result>) resultReporter.result();

            List<LdbcQuery5ResultSerializable> resp = new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcQuery5ResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcQuery6Serializable) {
            LdbcQuery6 op = ((LdbcQuery6Serializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcQuery6Result> result = 
                (List<LdbcQuery6Result>) resultReporter.result();

            List<LdbcQuery6ResultSerializable> resp = new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcQuery6ResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcQuery7Serializable) {
            LdbcQuery7 op = ((LdbcQuery7Serializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcQuery7Result> result = 
                (List<LdbcQuery7Result>) resultReporter.result();

            List<LdbcQuery7ResultSerializable> resp = new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcQuery7ResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcQuery8Serializable) {
            LdbcQuery8 op = ((LdbcQuery8Serializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcQuery8Result> result = 
                (List<LdbcQuery8Result>) resultReporter.result();

            List<LdbcQuery8ResultSerializable> resp = new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcQuery8ResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcQuery9Serializable) {
            LdbcQuery9 op = ((LdbcQuery9Serializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcQuery9Result> result = 
                (List<LdbcQuery9Result>) resultReporter.result();

            List<LdbcQuery9ResultSerializable> resp = new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcQuery9ResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcQuery10Serializable) {
            LdbcQuery10 op = ((LdbcQuery10Serializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcQuery10Result> result = 
                (List<LdbcQuery10Result>) resultReporter.result();

            List<LdbcQuery10ResultSerializable> resp = new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcQuery10ResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcQuery11Serializable) {
            LdbcQuery11 op = ((LdbcQuery11Serializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcQuery11Result> result = 
                (List<LdbcQuery11Result>) resultReporter.result();

            List<LdbcQuery11ResultSerializable> resp = new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcQuery11ResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcQuery12Serializable) {
            LdbcQuery12 op = ((LdbcQuery12Serializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcQuery12Result> result = 
                (List<LdbcQuery12Result>) resultReporter.result();

            List<LdbcQuery12ResultSerializable> resp = new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcQuery12ResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcQuery13Serializable) {
            LdbcQuery13 op = ((LdbcQuery13Serializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            LdbcQuery13Result result = 
                (LdbcQuery13Result) resultReporter.result();

            LdbcQuery13ResultSerializable resp = 
                new LdbcQuery13ResultSerializable(result);

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcQuery14Serializable) {
            LdbcQuery14 op = ((LdbcQuery14Serializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcQuery14Result> result = 
                (List<LdbcQuery14Result>) resultReporter.result();

            List<LdbcQuery14ResultSerializable> resp = new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcQuery14ResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcShortQuery1PersonProfileSerializable) {
            LdbcShortQuery1PersonProfile op = 
                ((LdbcShortQuery1PersonProfileSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            LdbcShortQuery1PersonProfileResult result = 
                (LdbcShortQuery1PersonProfileResult) resultReporter.result();

            LdbcShortQuery1PersonProfileResultSerializable resp = 
                new LdbcShortQuery1PersonProfileResultSerializable(result);

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcShortQuery2PersonPostsSerializable) {
            LdbcShortQuery2PersonPosts op = 
                ((LdbcShortQuery2PersonPostsSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcShortQuery2PersonPostsResult> result = 
                (List<LdbcShortQuery2PersonPostsResult>) resultReporter.result();

            List<LdbcShortQuery2PersonPostsResultSerializable> resp = 
                new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcShortQuery2PersonPostsResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcShortQuery3PersonFriendsSerializable) {
            LdbcShortQuery3PersonFriends op = 
                ((LdbcShortQuery3PersonFriendsSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcShortQuery3PersonFriendsResult> result = 
                (List<LdbcShortQuery3PersonFriendsResult>) resultReporter.result();

            List<LdbcShortQuery3PersonFriendsResultSerializable> resp = 
                new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcShortQuery3PersonFriendsResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcShortQuery4MessageContentSerializable) {
            LdbcShortQuery4MessageContent op = 
                ((LdbcShortQuery4MessageContentSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            LdbcShortQuery4MessageContentResult result = 
                (LdbcShortQuery4MessageContentResult) resultReporter.result();

            LdbcShortQuery4MessageContentResultSerializable resp = 
                new LdbcShortQuery4MessageContentResultSerializable(result);

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcShortQuery5MessageCreatorSerializable) {
            LdbcShortQuery5MessageCreator op = 
                ((LdbcShortQuery5MessageCreatorSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            LdbcShortQuery5MessageCreatorResult result = 
                (LdbcShortQuery5MessageCreatorResult) resultReporter.result();

            LdbcShortQuery5MessageCreatorResultSerializable resp = 
                new LdbcShortQuery5MessageCreatorResultSerializable(result);

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcShortQuery6MessageForumSerializable) {
            LdbcShortQuery6MessageForum op = 
                ((LdbcShortQuery6MessageForumSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            LdbcShortQuery6MessageForumResult result = 
                (LdbcShortQuery6MessageForumResult) resultReporter.result();

            LdbcShortQuery6MessageForumResultSerializable resp = 
                new LdbcShortQuery6MessageForumResultSerializable(result);

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcShortQuery7MessageRepliesSerializable) {
            LdbcShortQuery7MessageReplies op = 
                ((LdbcShortQuery7MessageRepliesSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);
            List<LdbcShortQuery7MessageRepliesResult> result = 
                (List<LdbcShortQuery7MessageRepliesResult>) resultReporter.result();

            List<LdbcShortQuery7MessageRepliesResultSerializable> resp = 
                new ArrayList<>();
            result.forEach((v) -> {
              resp.add(new LdbcShortQuery7MessageRepliesResultSerializable(v));
            });

            out.writeObject(resp);
            out.flush(); 
          } else if (query instanceof LdbcUpdate1AddPersonSerializable) {
            LdbcUpdate1AddPerson op = 
                ((LdbcUpdate1AddPersonSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);

            out.writeObject(LdbcNoResultSerializable.INSTANCE);
            out.flush(); 
          } else if (query instanceof LdbcUpdate2AddPostLikeSerializable) {
            LdbcUpdate2AddPostLike op = 
                ((LdbcUpdate2AddPostLikeSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);

            out.writeObject(LdbcNoResultSerializable.INSTANCE);
            out.flush(); 
          } else if (query instanceof LdbcUpdate3AddCommentLikeSerializable) {
            LdbcUpdate3AddCommentLike op = 
                ((LdbcUpdate3AddCommentLikeSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);

            out.writeObject(LdbcNoResultSerializable.INSTANCE);
            out.flush(); 
          } else if (query instanceof LdbcUpdate4AddForumSerializable) {
            LdbcUpdate4AddForum op = 
                ((LdbcUpdate4AddForumSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);

            out.writeObject(LdbcNoResultSerializable.INSTANCE);
            out.flush(); 
          } else if (query instanceof LdbcUpdate5AddForumMembershipSerializable) {
            LdbcUpdate5AddForumMembership op = 
                ((LdbcUpdate5AddForumMembershipSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);

            out.writeObject(LdbcNoResultSerializable.INSTANCE);
            out.flush(); 
          } else if (query instanceof LdbcUpdate6AddPostSerializable) {
            LdbcUpdate6AddPost op = 
                ((LdbcUpdate6AddPostSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);

            out.writeObject(LdbcNoResultSerializable.INSTANCE);
            out.flush(); 
          } else if (query instanceof LdbcUpdate7AddCommentSerializable) {
            LdbcUpdate7AddComment op = 
                ((LdbcUpdate7AddCommentSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);

            out.writeObject(LdbcNoResultSerializable.INSTANCE);
            out.flush(); 
          } else if (query instanceof LdbcUpdate8AddFriendshipSerializable) {
            LdbcUpdate8AddFriendship op = 
                ((LdbcUpdate8AddFriendshipSerializable) query).unpack();

            queryHandlerMap.get(op.getClass()).executeOperation(op,
                connectionState, resultReporter);

            out.writeObject(LdbcNoResultSerializable.INSTANCE);
            out.flush(); 
          } else {
            throw new RuntimeException("Unrecognized query type.");
          }
        }
      } catch (Exception e) {

      }
    }
  }

  public static void main(String[] args) throws Exception {
    Map<String, Object> opts =
        new Docopt(doc).withVersion("TorcDbServer 1.0").parse(args);

    // Arguments.
    final String coordinatorLocator = (String) opts.get("COORDLOC");
    final String graphName = (String) opts.get("GRAPHNAME");
    final int port = Integer.decode((String) opts.get("--port"));

    System.out.println(String.format("TorcDbServer: {coordinatorLocator: %s, "
        + "graphName: %s, port: %d}",
        coordinatorLocator,
        graphName,
        port));
   
    // Connect to database. 
    Map<String, String> props = new HashMap<>();
    props.put("coordinatorLocator", coordinatorLocator);
    props.put("graphName", graphName);
    System.out.println("Connecting to TorcDB...");
    TorcDbConnectionState connectionState = new TorcDbConnectionState(props);

    // Create mapping from op type to op handler for processing requests.
    Map<Class<? extends Operation>, OperationHandler> queryHandlerMap = 
        new HashMap<>();
    queryHandlerMap.put(LdbcQuery1.class, new TorcDb.LdbcQuery1Handler());
    queryHandlerMap.put(LdbcQuery2.class, new TorcDb.LdbcQuery2Handler());
    queryHandlerMap.put(LdbcQuery3.class, new TorcDb.LdbcQuery3Handler());
    queryHandlerMap.put(LdbcQuery4.class, new TorcDb.LdbcQuery4Handler());
    queryHandlerMap.put(LdbcQuery5.class, new TorcDb.LdbcQuery5Handler());
    queryHandlerMap.put(LdbcQuery6.class, new TorcDb.LdbcQuery6Handler());
    queryHandlerMap.put(LdbcQuery7.class, new TorcDb.LdbcQuery7Handler());
    queryHandlerMap.put(LdbcQuery8.class, new TorcDb.LdbcQuery8Handler());
    queryHandlerMap.put(LdbcQuery9.class, new TorcDb.LdbcQuery9Handler());
    queryHandlerMap.put(LdbcQuery10.class, new TorcDb.LdbcQuery10Handler());
    queryHandlerMap.put(LdbcQuery11.class, new TorcDb.LdbcQuery11Handler());
    queryHandlerMap.put(LdbcQuery12.class, new TorcDb.LdbcQuery12Handler());
    queryHandlerMap.put(LdbcQuery13.class, new TorcDb.LdbcQuery13Handler());
    queryHandlerMap.put(LdbcQuery14.class, new TorcDb.LdbcQuery14Handler());
    queryHandlerMap.put(LdbcShortQuery1PersonProfile.class, 
        new TorcDb.LdbcShortQuery1PersonProfileHandler());
    queryHandlerMap.put(LdbcShortQuery2PersonPosts.class, 
        new TorcDb.LdbcShortQuery2PersonPostsHandler());
    queryHandlerMap.put(LdbcShortQuery3PersonFriends.class, 
        new TorcDb.LdbcShortQuery3PersonFriendsHandler());
    queryHandlerMap.put(LdbcShortQuery4MessageContent.class, 
        new TorcDb.LdbcShortQuery4MessageContentHandler());
    queryHandlerMap.put(LdbcShortQuery5MessageCreator.class, 
        new TorcDb.LdbcShortQuery5MessageCreatorHandler());
    queryHandlerMap.put(LdbcShortQuery6MessageForum.class, 
        new TorcDb.LdbcShortQuery6MessageForumHandler());
    queryHandlerMap.put(LdbcShortQuery7MessageReplies.class, 
        new TorcDb.LdbcShortQuery7MessageRepliesHandler());
    queryHandlerMap.put(LdbcUpdate1AddPerson.class, 
        new TorcDb.LdbcUpdate1AddPersonHandler());
    queryHandlerMap.put(LdbcUpdate2AddPostLike.class, 
        new TorcDb.LdbcUpdate2AddPostLikeHandler());
    queryHandlerMap.put(LdbcUpdate3AddCommentLike.class, 
        new TorcDb.LdbcUpdate3AddCommentLikeHandler());
    queryHandlerMap.put(LdbcUpdate4AddForum.class, 
        new TorcDb.LdbcUpdate4AddForumHandler());
    queryHandlerMap.put(LdbcUpdate5AddForumMembership.class, 
        new TorcDb.LdbcUpdate5AddForumMembershipHandler());
    queryHandlerMap.put(LdbcUpdate6AddPost.class, 
        new TorcDb.LdbcUpdate6AddPostHandler());
    queryHandlerMap.put(LdbcUpdate7AddComment.class, 
        new TorcDb.LdbcUpdate7AddCommentHandler());
    queryHandlerMap.put(LdbcUpdate8AddFriendship.class, 
        new TorcDb.LdbcUpdate8AddFriendshipHandler());
    
    // Presumably for reporting LDBC driver errors.
    ConcurrentErrorReporter concurrentErrorReporter = 
        new ConcurrentErrorReporter();

    // Listener thread accepts connections and spawns client threads.
    Thread listener = new Thread(new ListenerThread(port, connectionState,
          queryHandlerMap, concurrentErrorReporter));
    listener.start();
    listener.join();
  }
}
