package sample;

public interface Originator {
    public Memento saveToMemento();

    public void restoreFromMemento(final Memento memento);
}
