package it.cnr.iit.ucs.pipwebsocket.json;


// This class represents the domo_light json schema at
// https://github.com/sifis-home/json-schemas/blob/master/domo_light.jsonschema

// When we query the DHT with RequestGetTopicName or RequestGetTopicUUID, the DHT will
// include one or more of these objects as part of the response.
public class DomoLight {
        private String area_name;
        private double energy;
        private String name;
        private double power;
        private boolean status;
        private String[] updated_properties;

    public String getArea_name() {
        return area_name;
    }

    public void setArea_name(String area_name) {
        this.area_name = area_name;
    }

    public double getEnergy() {
        return energy;
    }

    public void setEnergy(double energy) {
        this.energy = energy;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPower() {
        return power;
    }

    public void setPower(double power) {
        this.power = power;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String[] getUpdated_properties() {
        return updated_properties;
    }

    public void setUpdated_properties(String[] updated_properties) {
        this.updated_properties = updated_properties;
    }
}
