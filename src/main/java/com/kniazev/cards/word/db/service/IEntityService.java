package com.kniazev.cards.word.db.service;

import com.kniazev.cards.word.controller.adapter.MappingFunctions;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

public interface IEntityService<T> {
    JpaRepository<T, Long> getRepository();
    
    default T getOneById(Long id) {
        return findOneById(id).orElseThrow(EntityNotFoundException::new);
    }
    
    default T getOneByExample(T example) {
        return getRepository().findOne(Example.of(example))
                .orElseThrow(EntityNotFoundException::new);
    }
    
    default Optional<T> findOneById(Long id) {
        return getRepository().findById(id);
    }
    
    default Optional<T> findOneByExample(T example) {
        return getRepository().findOne(Example.of(example));
    }
    
    default List<T> findAll() {
        return getRepository().findAll();
    }
    
    default List<T> findAll(Sort sort) {
        return getRepository().findAll(sort);
    }
    
    default List<T> findByIds(Iterable<Long> ids) {
        return getRepository().findAllById(ids);
    }
    
    default List<T> findByExample(T example) {
        return getRepository().findAll(Example.of(example));
    }
    
    default Page<T> findByExample(T example, Pageable pageRequest) {
        return getRepository().findAll(Example.of(example), pageRequest);
    }
    
    default T save(T entity) {
        return getRepository().save(entity);
    }
    
    default List<T> save(Iterable<T> entities) {
        return getRepository().saveAll(entities);
    }
    
    default T saveAndFlush(T entity) {
        return getRepository().saveAndFlush(entity);
    }
    
    default List<T> saveAndFlush(Iterable<T> entities) {
        return getRepository().saveAllAndFlush(entities);
    }
    
    default T update(Long id, T entity) {
        return getRepository().findById(id)
                .map(persistedEntity -> getRepository()
                        .save(MappingFunctions.mergeObjects(entity, persistedEntity)))
                .orElseThrow(EntityNotFoundException::new);
    }
    
    default void deleteById(Long id) {
        getRepository().deleteById(id);
    }
    
    default void deleteByIds(Iterable<Long> ids) {
        getRepository().deleteAllByIdInBatch(ids);
    }
    
    default void delete(T entity) {
        getRepository().delete(entity);
    }
    
    default void delete(Iterable<T> entities) {
        getRepository().deleteAllInBatch(entities);
    }
}
