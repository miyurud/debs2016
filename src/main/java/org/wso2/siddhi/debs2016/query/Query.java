package org.wso2.siddhi.debs2016.query;

import org.wso2.siddhi.debs2016.input.DataLoaderThread;
import org.wso2.siddhi.debs2016.input.FileType;
import org.wso2.siddhi.debs2016.sender.OrderedEventSenderThread;
import org.wso2.siddhi.debs2016.sender.OrderedEventSenderThreadQ1;
import org.wso2.siddhi.debs2016.sender.OrderedEventSenderThreadQ2;
import org.wso2.siddhi.debs2016.util.Constants;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by malithjayasinghe on 4/6/16.
 */
public class Query {
    private String friendshipFile;
    private String postsFile;
    private String commentsFile;
    private String likesFile;

    public static void main(String[] args){

        File q2 = new File("q2.txt");
        q2.delete();

        if(args.length == 0){
            System.err.println("Incorrect arguments. Required: <Path to>friendships.dat, <Path to>posts.dat, <Path to>comments.dat, <Path to>likes.dat");
            return;
        }

        Query query = new Query(args);
        query.run();
    }

    public Query(String[] args){
        friendshipFile = args[0];
        postsFile = args[1];
        commentsFile = args[2];
        likesFile = args[3];
    }

    public void run(){


        System.out.println("Incremental data loading is performed.");

        LinkedBlockingQueue<Object[]> eventBufferListQ1 [] = new LinkedBlockingQueue[2];
        LinkedBlockingQueue<Object[]> eventBufferListQ2 [] = new LinkedBlockingQueue[3];

        //Friendships
        DataLoaderThread dataLoaderThreadFriendships = new DataLoaderThread(friendshipFile, FileType.FRIENDSHIPS);
        DataLoaderThread dataLoaderThreadComments = new DataLoaderThread(commentsFile, FileType.COMMENTS);
        DataLoaderThread dataLoaderThreadLikes = new DataLoaderThread(likesFile, FileType.LIKES);
        DataLoaderThread dataLoaderThreadPosts = new DataLoaderThread(postsFile, FileType.POSTS);


        eventBufferListQ2[0] = dataLoaderThreadFriendships.getEventBuffer();
        eventBufferListQ2[1] = dataLoaderThreadComments.getEventBuffer();
        eventBufferListQ2[2] = dataLoaderThreadLikes.getEventBuffer();

        OrderedEventSenderThreadQ2 orderedEventSenderThreadQ2 = new OrderedEventSenderThreadQ2(eventBufferListQ2);


        eventBufferListQ1 [0] = dataLoaderThreadPosts.getEventBuffer();
        eventBufferListQ1 [1] = dataLoaderThreadComments.getEventBufferListSecondary();

        OrderedEventSenderThreadQ1 orderedEventSenderThreadQ1 = new OrderedEventSenderThreadQ1(eventBufferListQ1);

        dataLoaderThreadFriendships.start();
        dataLoaderThreadComments.start();
        dataLoaderThreadLikes.start();
        dataLoaderThreadPosts.start();

        orderedEventSenderThreadQ1.start();
        orderedEventSenderThreadQ2.start();


        //Just make the main thread sleep infinitely
        //Note that we cannot have an event based mechanism to exit from this infinit loop. It is
        //because even if the data sending thread has completed its task of sending the data to
        //the SiddhiManager, the SiddhiManager object may be conducting the processing of the remaining
        //data. Furthermore, since this is CEP its better have this type of mechanism, rather than
        //terminating once we are done sending the data to the CEP engine.


        while(true){
            try {
                Thread.sleep(Constants.MAIN_THREAD_SLEEP_TIME);
                if (orderedEventSenderThreadQ1.doneFlag&&orderedEventSenderThreadQ2.doneFlag){
                    System.exit(0);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
