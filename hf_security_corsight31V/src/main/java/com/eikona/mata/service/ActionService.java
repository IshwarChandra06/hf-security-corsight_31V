package com.eikona.mata.service;

import java.security.Principal;

import javax.validation.Valid;

import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;

import com.eikona.mata.entity.Action;
import com.eikona.mata.entity.Device;
import com.eikona.mata.entity.Employee;

public interface ActionService {

	Object getAll();

	void save(Action action);

	DataTablesOutput<Action> getAllFrom(@Valid DataTablesInput input);

	Action getById(long id);

	void deleteById(long id);

	void schedulingActionStatusNew();

	void employeeAction(Employee employee, String type, String source, String deviceName, Principal principal);

	String employeeDeviceAction(Device device,Employee employee, String type, String source, String deviceName, Principal principal);

}
