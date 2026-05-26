package com.dji.sample.repository;

import com.dji.sample.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Integer> {

    @Query("SELECT r.roleKey FROM Role r WHERE r.id IN :ids")
    List<String> findRoleKeysByIds(@Param("ids") List<Integer> ids);
}