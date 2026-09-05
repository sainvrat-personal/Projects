package com.harGharAaurved.repository;
import com.harGharAaurved.Department;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface DepartmentRepository extends CrudRepository<Department, Long> {

}
