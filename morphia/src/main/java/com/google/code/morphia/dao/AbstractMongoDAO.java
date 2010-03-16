/**
 * Copyright (C) 2010 Olafur Gauti Gudmundsson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.code.morphia.dao;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.code.morphia.Mapper;
import com.google.code.morphia.Morphia;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
@SuppressWarnings("unchecked")
public abstract class AbstractMongoDAO<T> implements MongoDAO<T> {

    private final Class<T> entityClass;
    private final Morphia morphia;

    public AbstractMongoDAO( Class<T> entityClass, Morphia morphia ) {
        this.entityClass = entityClass;
        this.morphia = morphia;
    }

    protected abstract DBCollection collection();

    @Override
    public long getCount() {
        return collection().getCount();
    }

    @Override
    public void removeById(String id) {
        collection().remove(new BasicDBObject(Mapper.ID_KEY, Mapper.fixupId(id)));
    }

    @Override
    public T save(T entity) {
        BasicDBObject obj = (BasicDBObject) morphia.toDBObject(entity);
        collection().save(obj);
        return get(obj.get(Mapper.ID_KEY).toString());
    }

    @Override
    public boolean exists(String key, String value) {
        return collection().getCount(new BasicDBObject(key, value)) > 0;
    }

    @Override
    public boolean exists(String key, int value) {
        return collection().getCount(new BasicDBObject(key, value)) > 0;
    }

    @Override
    public boolean exists(String key, long value) {
        return collection().getCount(new BasicDBObject(key, value)) > 0;
    }

    @Override
    public boolean exists(String key, double value) {
        return collection().getCount(new BasicDBObject(key, value)) > 0;
    }

    @Override
    public boolean exists(String key, boolean value) {
        return collection().getCount(new BasicDBObject(key, value)) > 0;
    }

    @Override
    public boolean exists(String key, Enum value) {
        return collection().getCount(new BasicDBObject(key, value.name())) > 0;
    }

    @Override
    public T get(String id) {
        return morphia.fromDBObject(entityClass, (BasicDBObject) collection().findOne(Mapper.fixupId(id)));
    }

    @Override
    public T getByValue(String key, String value) {
        return morphia.fromDBObject(entityClass, (BasicDBObject) collection().findOne(new BasicDBObject(key, value)));
    }

    @Override
    public T getByValue(String key, int value) {
        return morphia.fromDBObject(entityClass, (BasicDBObject) collection().findOne(new BasicDBObject(key, value)));
    }

    @Override
    public T getByValue(String key, long value) {
        return morphia.fromDBObject(entityClass, (BasicDBObject) collection().findOne(new BasicDBObject(key, value)));
    }

    @Override
    public T getByValue(String key, double value) {
        return morphia.fromDBObject(entityClass, (BasicDBObject) collection().findOne(new BasicDBObject(key, value)));
    }

    @Override
    public T getByValue(String key, boolean value) {
        return morphia.fromDBObject(entityClass, (BasicDBObject) collection().findOne(new BasicDBObject(key, value)));
    }

    @Override
    public T getByValue(String key, Enum value) {
        return morphia.fromDBObject(entityClass, (BasicDBObject) collection().findOne(new BasicDBObject(key, value.name())));
    }

    @Override
    public void dropCollection() {
        collection().drop();
    }

	@Override
    public List<T> findAll(int startIndex, int resultSize) {
        Iterator cursor = collection().find(new BasicDBObject(), new BasicDBObject(), startIndex, resultSize);
        return toList(cursor);
    }

    protected T map( BasicDBObject dbObject ) {
        return morphia.fromDBObject(entityClass, dbObject);
    }

    protected List<T> toList( Iterator<BasicDBObject> cursor ) {
        List<T> list = new ArrayList<T>();
        while ( cursor.hasNext() ) {
            list.add(morphia.fromDBObject(entityClass, cursor.next()));
        }
        return list;
    }

}
