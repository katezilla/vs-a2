package ggTProcess;


public class Marker implements IJob {
    private String prozessIdAbsender;
    private int seqNr;

    public Marker(String prozessIdAbsender, int seqNr) {
        this.prozessIdAbsender = prozessIdAbsender;
        this.seqNr = seqNr;
    }

    public String getProzessIdAbsender() {
        return prozessIdAbsender;
    }

    public int getSeqNr() {
        return seqNr;
    }

}
