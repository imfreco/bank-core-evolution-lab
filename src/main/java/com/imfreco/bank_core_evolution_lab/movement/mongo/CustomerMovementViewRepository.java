package com.imfreco.bank_core_evolution_lab.movement.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CustomerMovementViewRepository extends MongoRepository<CustomerMovementViewDocument, String> {
}
