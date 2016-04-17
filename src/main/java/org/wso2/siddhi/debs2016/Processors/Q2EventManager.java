package org.wso2.siddhi.debs2016.Processors;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.wso2.siddhi.debs2016.comment.CommentStore;
import org.wso2.siddhi.debs2016.graph.Graph;
import org.wso2.siddhi.debs2016.util.Constants;
import org.wso2.siddhi.query.api.definition.Attribute;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * Created by bhagya on 3/30/16.
 */
public class Q2EventManager {

    static StringBuilder builder = new StringBuilder();
    private Disruptor<DEBSEvent> dataReadDisruptor;
    private RingBuffer dataReadBuffer;
    private static long startiij_timestamp;
    private static long endiij_timestamp;
    private String ts;
    public Graph friendshipGraph ;
    private CommentStore commentStore ;
    private static int count = 0;
    static volatile long timeDifference = 0; //This is the time difference for this time window.
    long startTime = 0;
    private Date startDateTime;
    private static Long latency = 0L;
    private static Long  numberOfOutputs = 0L;
    static int bufferSize = 512;
    private long sequenceNumber;
    public static volatile boolean Q2_COMPLETED = false;


    /**
     * The constructor
     *
     */
    public Q2EventManager(int k, long duration){
        List<Attribute> attributeList = new ArrayList<Attribute>();
        friendshipGraph = new Graph();
        commentStore = new CommentStore(duration, friendshipGraph, k);

        //We print the start and the end times of the experiment even if the performance logging is disabled.
        startDateTime = new Date();
        startTime = startDateTime.getTime();
        SimpleDateFormat ft = new SimpleDateFormat ("yyyy-MM-dd.hh:mm:ss-a-zzz");
    }

    /**
     * Gets the data reader distruptor
     *
     * @return the data reader distruptor
     */
    public Disruptor<DEBSEvent> getDataReadDisruptor() {
        return dataReadDisruptor;
    }

    /**
     *
     * Starts the distruptor
     *
     */
    public void run() {
        dataReadDisruptor = new Disruptor<DEBSEvent>(new com.lmax.disruptor.EventFactory<DEBSEvent>() {

            @Override
            public DEBSEvent newInstance() {
                return new DEBSEvent();
            }
        }, bufferSize, Executors.newFixedThreadPool(1), ProducerType.SINGLE, new SleepingWaitStrategy());


        DEBSEventHandler debsEventHandler = new DEBSEventHandler();
        dataReadDisruptor.handleEventsWith(debsEventHandler);
        dataReadBuffer = dataReadDisruptor.start();
    }

    /**
     * Gets the reference to next DebsEvent from the ring butter
     *
     * @return the DebsEvent
     */
    public DEBSEvent getNextDebsEvent()
    {
        sequenceNumber = dataReadBuffer.next();
        return dataReadDisruptor.get(sequenceNumber);
    }

    /**
     * Publish the new event
     *
     */
    public void publish()
    {
        dataReadBuffer.publish(sequenceNumber);
    }

    /**
     * Writes the output to the file
     */
    public static void outputwritter() {
        try {
            File performance = new File("performance.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(performance, true));
            String result = builder.toString();
            writer.write(result);
            writer.close();
            System.out.flush();
        } catch (Exception e)
        {
            e.printStackTrace();
        }

    }
    /**
     *
     * Print the throughput etc
     *
     */
    private synchronized void showFinalStatistics()
    {
        try {
            commentStore.destroy();
            builder.setLength(0);
            timeDifference = endiij_timestamp - startiij_timestamp;
            Date dNow = new Date();
            SimpleDateFormat ft = new SimpleDateFormat("yyyy-MM-dd.hh:mm:ss-a-zzz");
            System.out.println("Query 2 completed .....at : " + dNow.getTime() + "--" + ft.format(dNow));
            System.out.println("Event count : " + count);
            String timeDifferenceString = Float.toString(((float)timeDifference/1000)) + "000000";
            System.out.println("Total run time : " + timeDifferenceString.substring(0, 7));
            builder.append(timeDifferenceString.substring(0, 7));
            builder.append(", ");

            System.out.println("Throughput (events/s): " + Math.round((count * 1000.0) / timeDifference));
            System.out.println("Total Latency " + latency);
            System.out.println("Total Outputs " + numberOfOutputs);
            if (numberOfOutputs != 0) {
                float temp = ((float)latency/numberOfOutputs)/1000;
                BigDecimal averageLatency = new BigDecimal(temp);
                String latencyString = averageLatency.toPlainString() + "000000";
                System.out.println("Average Latency " + latencyString.substring(0, 7));
                builder.append(latencyString.substring(0, 7));
            } else {
                String latencyString = "000000";
                builder.append(latencyString);
            }
        }finally {
            Q2EventManager.Q2_COMPLETED = true;
            if(Q1EventManager.Q1_COMPLETED)
            {
                Q1EventManager.outputwriter();
                outputwritter();
                System.exit(0);
            }
        }
    }


    /**
     *
     * The debs event handler
     *
     */
    private class DEBSEventHandler implements EventHandler<DEBSEvent>{
        @Override
        public void onEvent(DEBSEvent debsEvent, long l, boolean b) throws Exception {
            try{

                Object [] objects = debsEvent.getObjectArray();

                long ts = (Long) objects[1];
                //Note that we cannot cast int to enum type. Java enums are classes. Hence we cannot cast them to int.
                int streamType = (Integer) objects[8];
                commentStore.cleanCommentStore(ts);
                count++;

                switch (streamType) {
                    case Constants.COMMENTS:
                        long comment_id = (Long) objects[3];
                        String comment = (String) objects[4];
                        commentStore.registerComment(comment_id, ts, comment, false);
                        break;
                    case Constants.FRIENDSHIPS:
                        if (ts == -2){
                            count--;
                            showFinalStatistics();
                            commentStore.destroy();
                            break;
                        }else if (ts == -1) {
                            count--;
                            startiij_timestamp = (Long) debsEvent.getSystemArrivalTime();
                            break;
                        }else{
                            long user_id_1 = (Long) objects[2];
                            long friendship_user_id_2 = (Long) objects[3];
                            friendshipGraph.addEdge(user_id_1, friendship_user_id_2);
                            commentStore.handleNewFriendship(user_id_1, friendship_user_id_2);
                            break;
                        }
                    case Constants.LIKES:
                        long user_id_1 = (Long) objects[2];
                        long like_comment_id = (Long) objects[3];
                        commentStore.registerLike(like_comment_id, user_id_1);
                        break;
                }

                if (ts != -2 && ts != -1){
                    Long endTime = commentStore.computeKLargestComments("," ,false, true);

                    if (endTime != -1L){
                        latency += (endTime - (Long) debsEvent.getSystemArrivalTime());
                        numberOfOutputs++;
                    }

                    endiij_timestamp = System.currentTimeMillis();
                }

            }catch (Exception e)
            {
                e.printStackTrace();
            }

        }
    }

}

