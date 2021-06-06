package aux;

import java.io.Serializable;

public class Triple<FST, SND, TRD> implements Serializable {
    private FST first;
    private SND second;
    private TRD third;

    public Triple(FST first, SND second, TRD third) {
        this.first = first;
        this.second = second;
        this.third = third;
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

    public TRD getThird() {
        return third;
    }

    public void setThird(TRD third) {
        this.third = third;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triple<?, ?, ?> triple = (Triple<?, ?, ?>) o;
        return getFirst().equals(triple.getFirst()) &&
                getSecond().equals(triple.getSecond()) &&
                getThird().equals(triple.getThird());
    }

    @Override
    public String toString() {
        return "Triple{" +
                "first=" + first +
                ", second=" + second +
                ", third=" + third +
                '}';
    }
}

