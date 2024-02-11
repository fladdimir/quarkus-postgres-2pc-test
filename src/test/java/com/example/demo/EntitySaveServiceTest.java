package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.p1.DemoEntity1;
import com.example.demo.p1.DemoRepository1;
import com.example.demo.p2.DemoEntity2;
import com.example.demo.p2.DemoRepository2;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.HeuristicMixedException;

@QuarkusTest
class EntitySaveServiceTest {

    @Inject
    EntitySaveService service;

    @Inject
    DemoRepository1 repository1;

    @Inject
    DemoRepository2 repository2;

    @Inject
    Logger logger;

    @PersistenceContext(unitName = "demo1")
    EntityManager em1;

    @PersistenceContext(unitName = "demo2")
    EntityManager em2;

    @BeforeEach
    void beforeEach() {

        repository1.deleteAllInBatch();
        repository2.deleteAllInBatch();

        // defer (later violated) unique-constraints,
        // so that constraint violations do fail upon prepare,
        // and not already on insert before the prepare
        // (apparently not possible via hibernate/jpa)
        QuarkusTransaction.begin();
        em1.createNativeQuery("ALTER TABLE demoentity1 DROP CONSTRAINT demoentity1_content_key;").executeUpdate();
        em1.createNativeQuery(
                "ALTER TABLE demoentity1 ADD CONSTRAINT demoentity1_content_key UNIQUE (content) DEFERRABLE INITIALLY DEFERRED;")
                .executeUpdate();
        em2.createNativeQuery("ALTER TABLE demoentity2 DROP CONSTRAINT demoentity2_content_key;").executeUpdate();
        em2.createNativeQuery(
                "ALTER TABLE demoentity2 ADD CONSTRAINT demoentity2_content_key UNIQUE (content) DEFERRABLE INITIALLY DEFERRED;")
                .executeUpdate();
        QuarkusTransaction.commit();
    }

    @Test
    void test_2pc_fail_on_2nd_prepare() {

        var exception = assertThrows(Exception.class,
                () -> {

                    QuarkusTransaction.begin();

                    // prepare will succeed
                    var contents1 = List.of("c1");

                    // duplicate content -> prepare will fail when deferred constraints are checked
                    var contents2 = List.of("c2", "c2");

                    saveContentsInNewEntities(contents1, contents2);

                    QuarkusTransaction.commit();
                });

        logger.info("exception: ", exception);

        // good: neither commit succeeded:
        assertThat(repository1.count()).isZero();
        assertThat(repository2.count()).isZero();

        // however: the following assertion succeeds:
        assertThat(exception).hasCauseInstanceOf(HeuristicMixedException.class); // why ?!
        // the ROLLBACK PREPARED of the 2nd datasource fails,
        // since it was actually not prepared and is therefore unknown to the database

        /*
         * postgres_2_1 | 2024-02-11 19:01:01.603 UTC [144] LOG: execute <unnamed>:
         * ROLLBACK PREPARED
         * '131077_AAAAAAAAAAAAAP//fwABAQAAm5NlyRltAAAAGHF1YXJrdXM=_AAAAAAAAAAAAAP//
         * fwABAQAAm5NlyRltAAAAIAAAAAAAAAAA'
         * postgres_2_1 | 2024-02-11 19:01:01.603 UTC [144] ERROR: prepared transaction
         * with identifier
         * "131077_AAAAAAAAAAAAAP//fwABAQAAm5NlyRltAAAAGHF1YXJrdXM=_AAAAAAAAAAAAAP//fwABAQAAm5NlyRltAAAAIAAAAAAAAAAA"
         * does not exist
         */
    }

    @Test
    void test_success() {

        QuarkusTransaction.begin();
        saveContentsInNewEntities(List.of("c1"), List.of("c2_1", "c2_2"));
        QuarkusTransaction.commit();

        // both commits succeeded
        assertThat(repository1.count()).isEqualTo(1);
        assertThat(repository2.count()).isEqualTo(2);
    }

    private void saveContentsInNewEntities(List<String> contents1, List<String> contents2) {

        contents1.forEach(c -> {
            var e = new DemoEntity1();
            e.setContent(c);
            repository1.save(e);
        });

        contents2.forEach(c -> {
            var e2 = new DemoEntity2();
            e2.setContent(c);
            repository2.save(e2);
        });
    }

}
