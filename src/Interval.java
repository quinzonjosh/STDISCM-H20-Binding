public class Interval {
    private int start;

    private int end;

    private boolean isLast = false;

    public Interval(int start, int end, boolean isLast) {
        this.start = start;
        this.end = end;
        this.isLast = isLast;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public boolean isLast() {
        return isLast;
    }
}
