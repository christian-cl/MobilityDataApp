package com.example.christian.mobilitydataapp.persistence;

import java.util.List;

/**
 * Created by Christian Cintrano on 9/06/15.
 *
 */
public interface GenericDAO<Entity> {
    public void open();

    public void close();

    public List<Entity> getAll();

    public void create(Entity entity);

    public void delete(Entity entity);

    public void deleteAll();
}
