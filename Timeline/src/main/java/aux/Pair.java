package aux;

import java.io.Serializable;

public class Pair<FST, SND> implements Serializable {

    private FST first;
    private SND second;

    public Pair(FST first, SND second) {
        this.first = first;
        this.second = second;
    }

    public FST getFirst() {
        return first;
    }

    public void setFirst(FST first) {
        this.first = first;
    }

    public SND getSecond() {
        return second;
    }

    public void setSecond(SND second) {
        this.second = second;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return getFirst().equals(pair.getFirst()) &&
                getSecond().equals(pair.getSecond());
    }

    @Override
    public String toString() {
        return "Pair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}
