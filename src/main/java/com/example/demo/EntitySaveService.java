package com.example.demo;

import java.util.List;

import com.example.demo.p1.DemoEntity1;
import com.example.demo.p1.DemoRepository1;
import com.example.demo.p2.DemoEntity2;
import com.example.demo.p2.DemoRepository2;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class EntitySaveService {

    private final DemoRepository1 repository1;
    private final DemoRepository2 repository2;

    /**
     * creates new entities at different datasources inside a single distributed
     * transaction.
     * parameter-values are supposed to be unique at each list, resulting in
     * constraint violations if not
     */
    @Transactional
    public void saveInNewEntitiesInTx(List<String> contents1, List<String> contents2) {

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
