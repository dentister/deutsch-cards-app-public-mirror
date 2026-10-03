package com.kniazev.cards.word.service;

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.lang3.StringUtils;

public interface FilteredEntityService<T> extends IEntityService<T> {
    default Collection<T> findByValue(String value) {
        return StringUtils.isEmpty(value) ? getRepository().findAll() : Collections.emptyList();
    }
}
