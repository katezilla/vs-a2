package koordinator;

import java.util.ArrayList;

public class StarterData {
    private String name;
    private ArrayList<String> processList;
    private int anzahlProzesse;

    public StarterData(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void put(String process) {
        processList.add(process);
    }

    public int getSize() {
        return processList.size();
    }

    public ArrayList<String> getProcesses() {
        return processList;
    }

    public int getAnzahlProzesse() {
        return anzahlProzesse;
    }

    public void setAnzahlProzesse(int anzahlProzesse) {
        this.anzahlProzesse = anzahlProzesse;
    }
}
