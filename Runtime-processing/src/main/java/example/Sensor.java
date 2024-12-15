package example;


public class Sensor {
    private long time;
    private int location_id;
    private int sensor_id;
    private String sensor;
    private double value;
    private double max;

    public void setTime(long time) {
        this.time = time;
    }

    public void setLocation_id(int location_id) {
        this.location_id = location_id;
    }

    public void setSensor_id(int sensor_id) {
        this.sensor_id = sensor_id;
    }

    public void setSensor(String sensor) {
        this.sensor = sensor;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public long getTime() {
        return time;
    }

    public int getLocation_id() {
        return location_id;
    }

    public int getSensor_id() {
        return sensor_id;
    }

    public String getSensor() {
        return sensor;
    }

    public double getValue() {
        return value;
    }

    public double getMax() {
        return max;
    }

    @Override
    public String toString() {
        return "Sensor{" +
                "time=" + time +
                ", location_id=" + location_id +
                ", sensor_id=" + sensor_id +
                ", sensor='" + sensor + '\'' +
                ", value=" + value +
                ", max=" + max +
                '}';
    }


}
