package com.eikona.mata.service.impl.model;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import com.eikona.mata.constants.ActionDetailsConstants;
import com.eikona.mata.constants.ApplicationConstants;
import com.eikona.mata.constants.BranchConstants;
import com.eikona.mata.entity.Action;
import com.eikona.mata.entity.ActionDetails;
import com.eikona.mata.entity.Device;
import com.eikona.mata.entity.Employee;
import com.eikona.mata.entity.User;
import com.eikona.mata.repository.ActionDetailsRepository;
import com.eikona.mata.repository.ActionRepository;
import com.eikona.mata.repository.UserRepository;
import com.eikona.mata.service.ActionDetailsService;
import com.eikona.mata.service.ActionService;
import com.eikona.mata.util.GeneralSpecificationUtil;

@Service
@EnableScheduling
public class ActionServiceImpl implements ActionService {

	@Autowired
	private ActionRepository actionRepository;
	
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ActionDetailsRepository actionDetailsRepository;

	@Autowired
	private ActionDetailsService actionDetailsService;

	@Autowired
	private ActionDetailsServiceImpl actionDetailsServiceImpl;
	
	@Autowired
	private GeneralSpecificationUtil<Action> generalSpecificationAction;

	@Autowired
	private GeneralSpecificationUtil<ActionDetails> generalSpecificationActionDetails;

	@Override
	public Object getAll() {
		return actionRepository.findAll();
	}

	@Override
	public void save(Action action) {
		action.setDeleted(false);
		action = actionRepository.save(action);
		actionDetailsService.saveAsAction(action);
	}

	@Override
	public DataTablesOutput<Action> getAllFrom(@Valid DataTablesInput input) {

		Specification<Action> isDeletedFalse = generalSpecificationAction.isDeletedSpecification();
		return (DataTablesOutput<Action>) actionRepository.findAll(input, isDeletedFalse);
	}

	@Override
	public Action getById(long id) {
		return actionRepository.findById(id).get();
	}

	@Override
	public void deleteById(long id) {
		Optional<Action> actionOption = actionRepository.findById(id);
		Action action = null;

		if (actionOption.isPresent()) {
			action = actionOption.get();
			action.setDeleted(true);
		} else {
			throw new RuntimeException(BranchConstants.NOT_FOUND_FOR_ID + id);
		}
		this.actionRepository.save(action);
	}

	@Override
	public void employeeAction(Employee employee, String type, String source, String deviceName,Principal principal) {
		Action action = new Action();
		action.setType(type);
		action.setEmployee(employee);
		action.setDeleted(false);
		action.setStatus(ApplicationConstants.NEW);
		action.setSource(source);
		action.setDevice(deviceName);
		User user=userRepository.findByUserNameAndIsDeletedFalse(principal.getName());
		action.setModifiedUser(user.getUserName());
		action = actionRepository.save(action);
		String status = actionDetailsService.saveAsAction(action);
		if (!(ApplicationConstants.NEW.equalsIgnoreCase(status))) {
			action.setStatus(status);
			actionRepository.save(action);
		}
	}
	
	@Override
	public String employeeDeviceAction(Device device,Employee employee, String type, String source, String deviceName,Principal principal) {
		Action action = new Action();
		action.setType(type);
		action.setEmployee(employee);
		action.setDeleted(false);
		action.setStatus(ApplicationConstants.NEW);
		action.setSource(source);
		action.setDevice(deviceName);
		User user=userRepository.findByUserNameAndIsDeletedFalse(principal.getName());
		if(ApplicationConstants.SYSTEM.equalsIgnoreCase(principal.getName())) 
			action.setModifiedUser(principal.getName());
		else
			action.setModifiedUser(user.getUserName());
		action = actionRepository.save(action);
		String status = actionDetailsService.saveAsDeviceAction(action,device);
		if (!(ApplicationConstants.NEW.equalsIgnoreCase(status))) {
			action.setStatus(status);
			actionRepository.save(action);
		}
		
		return status;
	}

	@Override
	// @Scheduled(cron ="0 0 * ? * *")
	//@Scheduled(fixedDelay = 3000)
	public void schedulingActionStatusNew() {

		Specification<Action> actionisDeletedFalse = generalSpecificationAction.isDeletedSpecification();
		
		Specification<Action> actionStatusPending = generalSpecificationAction.stringSpecification(ApplicationConstants.PENDING, ActionDetailsConstants.STATUS);

		List<Action> actionNewList = actionRepository.findAll(actionStatusPending.and(actionisDeletedFalse));

		for (Action action : actionNewList) {
			String status = ApplicationConstants.COMPLETED;
			Specification<ActionDetails> isDeletedFalse = generalSpecificationActionDetails.isDeletedSpecification();
			
			Specification<ActionDetails> ispending = generalSpecificationActionDetails.stringSpecification(ApplicationConstants.PENDING, ActionDetailsConstants.STATUS);
					
			Specification<ActionDetails> accordingToActionObj = generalSpecificationActionDetails.objectSpecification(action, ActionDetailsConstants.ACTION);
			
			List<ActionDetails> actionDetailsSpecList = actionDetailsRepository.findAll(isDeletedFalse.and(ispending).and(accordingToActionObj));

//			get status from action details 
			status = getStatusFromActionDetails(action, status, actionDetailsSpecList);
			
			action.setStatus(status);
			actionRepository.save(action);
		}
	}

	private String getStatusFromActionDetails(Action action, String status, List<ActionDetails> actionDetailsSpecList) {
		for (ActionDetails actionDetails : actionDetailsSpecList) {
			
			String actionDetailsStatus = actionDetailsServiceImpl.saveAsDeviceAction(action, actionDetails.getDevice()); 
			
			if(ApplicationConstants.ERROR.equalsIgnoreCase(actionDetailsStatus)) {
				status = ApplicationConstants.ERROR;
			}else if(ApplicationConstants.PENDING.equalsIgnoreCase(actionDetailsStatus)) {
				status = ApplicationConstants.PENDING;
			}
		}
		return status;
	}
}
