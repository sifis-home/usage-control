package it.cnr.iit.ucsdht.json;

import java.util.List;

public class Status {

    private String database;
    private List<String> pips;
    private List<String> peps;
    private List<String> policies;

    public Status(String database, List<String> pips, List<String> peps, List<String> policies) {
        this.database = database;
        this.pips = pips;
        this.peps = peps;
        this.policies = policies;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public List<String> getPips() {
        return pips;
    }

    public void setPips(List<String> pips) {
        this.pips = pips;
    }

    public List<String> getPeps() {
        return peps;
    }

    public void setPeps(List<String> peps) {
        this.peps = peps;
    }

    public List<String> getPolicies() {
        return policies;
    }

    public void setPolicies(List<String> policies) {
        this.policies = policies;
    }

}
