package sample;

import java.util.List;

class Memento {
    private final String mTextState;
    private final long mTimestampState;
    private final List<String> mCommentListState;

    public Memento(final String textState, final long timestampState, final List<String> commentListState) {
        mTextState = textState;
        mTimestampState = timestampState;
        mCommentListState = commentListState;
    }

    public String getTextState() {
        return mTextState;
    }

    public long getTimestampState() {
        return mTimestampState;
    }

    public List<String> getCommentListState() {
        return mCommentListState;
    }
}