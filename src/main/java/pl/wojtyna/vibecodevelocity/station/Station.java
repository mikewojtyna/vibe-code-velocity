package pl.wojtyna.vibecodevelocity.station;

import jakarta.persistence.*;

@Entity
public class Station {

    @Id
    @GeneratedValue
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Number of docks, i.e. the maximum number of bikes the station can hold. */
    @Column(nullable = false)
    private int capacity;

    protected Station() {
    }

    public Station(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }
}
