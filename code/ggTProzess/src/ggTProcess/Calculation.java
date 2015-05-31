package ggTProcess;

public class Calculation implements IJob {

    private String prozessIdAbsender;
    private int num;

    public Calculation(String prozessIdAbsender, int num) {
        this.prozessIdAbsender = prozessIdAbsender;
        this.num = num;
    }

    public String getProzessIdAbsender() {
        return prozessIdAbsender;
    }

    public int getNum() {
        return num;
    }

}
