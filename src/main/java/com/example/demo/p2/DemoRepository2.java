package com.example.demo.p2;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DemoRepository2 extends JpaRepository<DemoEntity2, Long> {

}
