package sample;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        final int ONE_HOUR = 60 * 60 * 1000;
        final CareTaker careTaker = new CareTaker();
        final long currentTime = Instant.now().toEpochMilli();
        final List<String> commentList = new ArrayList<>();
        commentList.add("A");
        Tweet tweet = new Tweet("My first tweet", currentTime - ONE_HOUR, commentList);
        careTaker.saveStateList.add(tweet.saveToMemento());
        commentList.add("B");
        tweet = new Tweet("My second tweet", currentTime, commentList);
        careTaker.saveStateList.add(tweet.saveToMemento());
    }
}
