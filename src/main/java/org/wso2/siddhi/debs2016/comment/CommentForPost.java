package org.wso2.siddhi.debs2016.comment;

import org.wso2.siddhi.debs2016.post.Post;

/**
 * An Object to record the timestamp of the comment of a post
 * Created by aaw on 4/1/16.
 */
class CommentForPost {
    private long ts;
    private Post post;
    private long userID;

    public void setPost(Post post) {
        this.post = post;
    }

    public long getUserID() {
        return userID;
    }

    public void setUserID(long userID) {
        this.userID = userID;
    }

    /**
     * Gets the post
     *
     * @return the post
     */
    public Post getPost() {
        return post;
    }

    /**
     * Gets the arrival time of the comment
     *
     * @return the comment arrival time
     */
    public long getTs() {
        return ts;
    }

    /**
     * The constructor
     *
     * @param post the post
     * @param ts   the time stamp
     */
    public CommentForPost(Post post, long ts, long userId) {
        this.ts = ts;
        this.post = post;
        this.userID = userId;
    }
}
