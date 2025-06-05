package com.backstage.curtaincall.specialProduct.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.backstage.curtaincall.category.repository.CategoryRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@Slf4j
@ActiveProfiles("test")
class SpecialProductRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;


    @Autowired
    private EntityManager em;

}