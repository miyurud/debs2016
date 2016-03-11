import org.junit.Test;
import org.wso2.siddhi.debs2016.graph.CommentLikeGraph;
import org.wso2.siddhi.debs2016.graph.Graph;

import static org.junit.Assert.*;

/**
 * Created by anoukh on 3/10/16.
 */
public class CommentLikeGraphTest {

    @Test
    public void testCommentLikeGraphConstruction(){
        CommentLikeGraph commentLikeGraph = new CommentLikeGraph(1, "Hi");
        commentLikeGraph.registerLike(31);
        commentLikeGraph.registerLike(12);
        commentLikeGraph.handleNewFriendship(12, 31);

        Graph testGraph = new Graph();
        testGraph.addVertex(31);
        testGraph.addVertex(12);
        testGraph.addEdge(12,31);

        assertTrue(testGraph.equals(commentLikeGraph.getGraph()));

    }

    @Test
    public void testRegisterLike(){
        CommentLikeGraph commentLikeGraph = new CommentLikeGraph(1, "Hi");
        assertEquals(0,commentLikeGraph.getGraph().getNumberOfVertices());
        commentLikeGraph.registerLike(31);
        assertEquals(1,commentLikeGraph.getGraph().getNumberOfVertices());
        commentLikeGraph.registerLike(12);
        assertEquals(2,commentLikeGraph.getGraph().getNumberOfVertices());
    }

    @Test
    public void testHandleFriendship(){
        CommentLikeGraph commentLikeGraph = new CommentLikeGraph(1, "Hi");

        commentLikeGraph.registerLike(12);
        commentLikeGraph.registerLike(15);
        commentLikeGraph.registerLike(16);

        commentLikeGraph.handleNewFriendship(15,16);
        commentLikeGraph.handleNewFriendship(15,6);
        commentLikeGraph.handleNewFriendship(2,9);

        assertEquals(1, commentLikeGraph.getGraph().getNumberOfEdges());
    }
}