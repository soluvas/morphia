package com.google.code.morphia.testmodel;

import com.google.code.morphia.AbstractMongoEntity;
import com.google.code.morphia.annotations.MongoDocument;
import com.google.code.morphia.annotations.MongoEmbedded;
import com.google.code.morphia.annotations.MongoReference;
import com.google.code.morphia.annotations.MongoValue;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Olafur Gauti Gudmundsson
 */
@MongoDocument
public class Article extends AbstractMongoEntity {
	private static final long serialVersionUID = 1L;

    @MongoEmbedded
    private Map<String,Translation> translations;
    @MongoValue
    private Map<String,Object> attributes;
    @MongoReference
    private Map<String,Article> related;

    public Article() {
        super();
        translations = new HashMap<String,Translation>();
        attributes = new HashMap<String,Object>();
        related = new HashMap<String,Article>();
    }

    public Map<String, Translation> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, Translation> translations) {
        this.translations = translations;
    }

    public void setTranslation( String langCode, Translation t ) {
        translations.put(langCode, t);
    }

    public Translation getTranslation( String langCode ) {
        return translations.get(langCode);
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public void setAttribute( String name, Object value ) {
        attributes.put(name, value);
    }

    public Object getAttribute( String name ) {
        return attributes.get(name);
    }

    public Map<String, Article> getRelated() {
        return related;
    }

    public void setRelated(Map<String, Article> related) {
        this.related = related;
    }

    public void putRelated(String name, Article a) {
        related.put(name, a);
    }

    public Article getRelated( String name ) {
        return related.get(name);
    }
}