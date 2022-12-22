package com.eikona.mata.repository;


import java.util.List;

import org.springframework.data.jpa.datatables.repository.DataTablesRepository;
import org.springframework.stereotype.Repository;

import com.eikona.mata.entity.Department;
@Repository
public interface DepartmentRepository extends DataTablesRepository<Department, Long> {

	List<Department> findAllByIsDeletedFalse();

	Department findByNameAndIsDeletedFalse(String department);

    
	


}
