package com.example.demo.p1;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemoRepository1 extends JpaRepository<DemoEntity1, Long> {

}
