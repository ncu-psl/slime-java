import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@interface SlimeOwned{}
@interface SlimeBorrow{}
@interface SlimeCopy{}

public class MementoEvalTest {
    public class Main {
        public static void main(String[] args) {
            final int ONE_HOUR = 60 * 60 * 1000;
            final CareTaker careTaker = new CareTaker();
            final long currentTime = Instant.now().toEpochMilli();
            final List<String> commentList = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                final Tweet tweet = new Tweet("Tweet Num: " + i, currentTime, commentList);
                careTaker.saveStateList.add(tweet.saveToMemento());
            }
            System.out.println(System.currentTimeMillis() - currentTime);
        }
    }
}

interface Originator {
    public Memento saveToMemento();

    public void restoreFromMemento(final Memento memento);
}


class Tweet implements Originator {
    private String mText;
    private long mTimestamp;
    @SlimeOwned private List<String> mCommentList;

    public Tweet(final String text, final long timestamp, @SlimeBorrow final List<String> commentList) {
        mText = text;
        mTimestamp = timestamp;
        @SlimeCopy final List<String> commentListCopy = commentList;
        mCommentList = commentListCopy;
    }

    public String getText() {
        return mText;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    @SlimeOwned public List<String> getCommentList() {
        return mCommentList;
    }

    @Override
    public Memento saveToMemento() {
        @SlimeCopy final List<String> commentListCopy = mCommentList;
        return new Memento(mText, mTimestamp, commentListCopy);
    }

    @Override
    public void restoreFromMemento(final Memento memento) {
        mText = memento.getTextState();
        mTimestamp = memento.getTimestampState();
        mCommentList = memento.getCommentListState();
    }
}

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

class CareTaker {
    // Array list of tweet states
    final List<Memento> saveStateList = new ArrayList<>();
}