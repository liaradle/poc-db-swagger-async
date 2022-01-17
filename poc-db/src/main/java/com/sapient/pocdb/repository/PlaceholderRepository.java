package com.sapient.pocdb.repository;

import com.sapient.pocdb.data.Placeholder;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlaceholderRepository extends CrudRepository<Placeholder, Long> {
}