package sample;

import java.util.List;

class Tweet implements Originator {
    private String mText;
    private long mTimestamp;
    private List<String> mCommentList;

    public Tweet(final String text, final long timestamp, final List<String> commentList) {
        mText = text;
        mTimestamp = timestamp;
        mCommentList = commentList;
    }

    public String getText() {
        return mText;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public List<String> getCommentList() {
        return mCommentList;
    }

    @Override
    public Memento saveToMemento() {
        return new Memento(mText, mTimestamp, mCommentList);
    }

    @Override
    public void restoreFromMemento(final Memento memento) {
        mText = memento.getTextState();
        mTimestamp = memento.getTimestampState();
        mCommentList = memento.getCommentListState();
    }
}