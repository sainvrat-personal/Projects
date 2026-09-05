package com.harGharAaurved.service;
import com.harGharAaurved.Department;

import java.util.List;

public interface DepartmentService {
    // Save operation
        Department saveDepartment(Department department);

        // Read operation
        List<Department> fetchDepartmentList();

        // Update operation
        Department updateDepartment(Department department,
                                    Long departmentId);

        // Delete operation
        void deleteDepartmentById(Long departmentId);
    }
