package org.apache.camel.quarkus.component.influxdb.it;

import java.util.ArrayList;
import java.util.List;

import org.influxdb.dto.BatchPoints;

public class Points {

    private String database;

    private List<Point> points = new ArrayList<>();

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void addPoint(Point point) {
        this.points.add(point);
    }

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }

    public BatchPoints toBatchPoints() {
        BatchPoints.Builder batchPoints = BatchPoints.database(this.database);

        points.forEach(p -> batchPoints.point(p.toPoint()));

        return batchPoints.build();
    }
}
