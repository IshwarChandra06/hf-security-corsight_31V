package com.eikona.mata.service;

import javax.validation.Valid;

import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;

import com.eikona.mata.entity.Action;
import com.eikona.mata.entity.ActionDetails;
import com.eikona.mata.entity.Device;

public interface ActionDetailsService {

	Object getAll();

	void save(ActionDetails actionDetails);

	ActionDetails getById(long id);

	void deleteById(long id);

	DataTablesOutput<ActionDetails> getAllFrom(@Valid DataTablesInput input);

	String saveAsAction(Action action);

	String saveAsDeviceAction(Action action,Device device);

}
