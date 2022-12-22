package com.eikona.mata.service.impl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import com.eikona.mata.constants.ActionDetailsConstants;
import com.eikona.mata.constants.ApplicationConstants;
import com.eikona.mata.constants.BewardDeviceConstants;
import com.eikona.mata.constants.BranchConstants;
import com.eikona.mata.constants.CorsightDeviceConstants;
import com.eikona.mata.constants.NumberConstants;
import com.eikona.mata.constants.UnvDeviceConstants;
import com.eikona.mata.entity.Action;
import com.eikona.mata.entity.ActionDetails;
import com.eikona.mata.entity.Area;
import com.eikona.mata.entity.Device;
import com.eikona.mata.entity.Employee;
import com.eikona.mata.repository.ActionDetailsRepository;
import com.eikona.mata.repository.DeviceRepository;
import com.eikona.mata.repository.EmployeeRepository;
import com.eikona.mata.service.ActionDetailsService;
import com.eikona.mata.service.DeviceSyncAbstractService;
import com.eikona.mata.util.UnvErrorMessage;
import com.eikona.mata.util.GeneralSpecificationUtil;

@Service
@EnableScheduling
public class ActionDetailsServiceImpl implements ActionDetailsService {

	@Autowired
	private ActionDetailsRepository actionDetailsRepository;

	@Autowired
	private DeviceRepository deviceRepository;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private UnvErrorMessage errorMessage;

	@Autowired
	@Qualifier(UnvDeviceConstants.SERVICE)
	private DeviceSyncAbstractService<Long> unvSyncServiceImpl;

	@Autowired
	@Qualifier(BewardDeviceConstants.SERVICE)
	private DeviceSyncAbstractService<String> bewardSyncServiceImpl;

	@Autowired
	@Qualifier(CorsightDeviceConstants.SERVICE)
	private DeviceSyncAbstractService<String> corsightSyncServiceImpl;
	
	@Autowired
	private GeneralSpecificationUtil<ActionDetails> generalSpecificationActionDetails;

	@Override
	public Object getAll() {
		return actionDetailsRepository.findAll();
	}

	@Override
	public void save(ActionDetails actionDetails) {
		actionDetails.setDeleted(false);
		actionDetailsRepository.save(actionDetails);

	}

	@Override
	public ActionDetails getById(long id) {
		return actionDetailsRepository.findById(id).get();
	}

	@Override
	public void deleteById(long id) {
		Optional<ActionDetails> option = actionDetailsRepository.findById(id);
		ActionDetails actionDetails = null;

		if (option.isPresent()) {
			actionDetails = option.get();
			actionDetails.setDeleted(true);
		} else {
			throw new RuntimeException(BranchConstants.NOT_FOUND_FOR_ID + id);
		}
		this.actionDetailsRepository.save(actionDetails);
	}

	@Override
	public DataTablesOutput<ActionDetails> getAllFrom(@Valid DataTablesInput input) {

		Specification<ActionDetails> isDeletedFalse = generalSpecificationActionDetails.isDeletedSpecification();

		Specification<ActionDetails> statusPending = generalSpecificationActionDetails.stringSpecification(ApplicationConstants.PENDING, ApplicationConstants.STATUS);
		
		Specification<ActionDetails> statusError = generalSpecificationActionDetails.stringSpecification(ApplicationConstants.ERROR, ApplicationConstants.STATUS);

		return (DataTablesOutput<ActionDetails>) actionDetailsRepository.findAll(input,
				isDeletedFalse.and(statusPending).or(statusError));
	}

	@Override
	public String saveAsDeviceAction(Action action, Device device) {
		String status = ApplicationConstants.NEW;

		if (null != device.getArea()) {

			ActionDetails actionDetails = new ActionDetails();
			actionDetails.setAction(action);
			actionDetails.setDeleted(false);
			actionDetails.setDevice(device);
			if (UnvDeviceConstants.MODEL_TYPE.equalsIgnoreCase(device.getModel())) {
				Long responseCode = unvSyncServiceImpl.deviceBasicInfo(device.getIpAddress());
				if (responseCode == NumberConstants.LONG_ZERO) {
					if (ApplicationConstants.DELETE.equalsIgnoreCase(actionDetails.getAction().getType())) {
						unvSyncServiceImpl.deleteSingleEmployeeFromDevice(actionDetails);
						actionDetails.setStatus(ApplicationConstants.COMPLETED);
						actionDetails.setMessage(ApplicationConstants.COMPLETED);
						removeEmployeeFromAreaAndSave(action, device);
						
						status = ApplicationConstants.COMPLETED;
					} else {
						Long resultCode = unvSyncServiceImpl.addEmployeeToDevice(actionDetails);
						status = addEmployeeToUnvDevice(actionDetails, resultCode);
					}
				} else {
					actionDetails.setStatus(ApplicationConstants.PENDING);
					status = ApplicationConstants.PENDING;

				}
			} else if (BewardDeviceConstants.MODEL_TYPE.equalsIgnoreCase(device.getModel())) {
				String responseData = bewardSyncServiceImpl.deviceBasicInfo(device.getIpAddress());
				if (null != responseData) {
					actionDetails = addEmployeeToBeward(actionDetails, action.getEmployee(), device);

					if (ApplicationConstants.ERROR.equalsIgnoreCase(actionDetails.getStatus())) {
						status = ApplicationConstants.ERROR;
					} else {
						if (!(ApplicationConstants.PENDING.equalsIgnoreCase(status)) && !(ApplicationConstants.ERROR.equalsIgnoreCase(status)))
							status = ApplicationConstants.COMPLETED;
					}
				} else {
					actionDetails.setStatus(ApplicationConstants.PENDING);
					status = ApplicationConstants.PENDING;

				}
			} 
			actionDetailsRepository.save(actionDetails);
		}

		return status;

	}

	private String addEmployeeToUnvDevice(ActionDetails actionDetails, Long resultCode) {
		String status;
		if (resultCode == NumberConstants.LONG_ZERO) {
			actionDetails.setStatus(ApplicationConstants.COMPLETED);
			actionDetails.setMessage(ApplicationConstants.COMPLETED);
			status = ApplicationConstants.COMPLETED;

		} else if (resultCode == -NumberConstants.LONG_ONE) {
			actionDetails.setStatus(ApplicationConstants.ERROR);
			actionDetails.setMessage(ActionDetailsConstants.USER_NOT_ADDED_IMAGE_NOT_FOUND);
			status = ApplicationConstants.ERROR;
		} else {
			actionDetails.setStatus(ApplicationConstants.ERROR);
			actionDetails
					.setMessage("User added, image error: " + errorMessage.errormap.get(resultCode));
			status = ApplicationConstants.ERROR;
		}
		return status;
	}

	private void removeEmployeeFromAreaAndSave(Action action, Device device) {
		
		List<Area> areaList = action.getEmployee().getArea();

		List<Area> areaListObj = new ArrayList<Area>();
		for (Area area : areaList) {

			if (device.getArea() != area) {
				areaListObj.add(area);
			}
		}

		action.getEmployee().setArea(areaListObj);
		employeeRepository.save(action.getEmployee());
	}

	@Override
	public String saveAsAction(Action action) {

		String status = ApplicationConstants.NEW;

		if (null != action.getEmployee().getArea()) {
			List<Device> deviceList = deviceRepository.findByAreaAndIsDeletedFalseCustom(action.getEmployee().getArea());

			List<ActionDetails> actionDetailsList = new ArrayList<>();

			if (null != deviceList && !deviceList.isEmpty()) {
				for (Device device : deviceList) {
					ActionDetails actionDetails = new ActionDetails();
					actionDetails.setAction(action);
					actionDetails.setDeleted(false);
					actionDetails.setDevice(device);

					if (UnvDeviceConstants.DEVICE_TYPE.equalsIgnoreCase(device.getModel())) {

						Long responseCode = unvSyncServiceImpl.deviceBasicInfo(device.getIpAddress());
						if (responseCode == NumberConstants.LONG_ZERO) {
							if (ApplicationConstants.DELETE.equalsIgnoreCase(actionDetails.getAction().getType())) {
								unvSyncServiceImpl.deleteSingleEmployeeFromDevice(actionDetails);
								actionDetails.setStatus(ApplicationConstants.COMPLETED);
								actionDetails.setMessage(ApplicationConstants.COMPLETED);
								removeEmployeeFromAreaAndSave(action, device);
								
								status = ApplicationConstants.COMPLETED;
							} else {
								try {
									Long resultCode = unvSyncServiceImpl.addEmployeeToDevice(actionDetails);

									status = addEmployeeToUnvDevice(actionDetails, resultCode);
									
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						} else {
							actionDetails.setStatus(ApplicationConstants.PENDING);
							status = ApplicationConstants.PENDING;

						}
					} else if (BewardDeviceConstants.MODEL_TYPE.equalsIgnoreCase(device.getModel())) {
						String responseData = bewardSyncServiceImpl.deviceBasicInfo(device.getIpAddress());

						if (null != responseData) {
							actionDetails = addEmployeeToBeward(actionDetails, action.getEmployee(), device);

							if (ApplicationConstants.ERROR.equalsIgnoreCase(actionDetails.getStatus())) {
								status = ApplicationConstants.ERROR;
							} else {
								if (!(ApplicationConstants.PENDING.equalsIgnoreCase(status)) && !(ApplicationConstants.ERROR.equalsIgnoreCase(status)))
									status = ApplicationConstants.COMPLETED;
							}

						} else {
							actionDetails.setStatus(ApplicationConstants.PENDING);
							status = ApplicationConstants.PENDING;

						}
					} 
					actionDetailsList.add(actionDetails);
				}
			}
			actionDetailsRepository.saveAll(actionDetailsList);
		}
		return status;
	}
	
	@SuppressWarnings(ApplicationConstants.UNUSED)
	private ActionDetails addEmployeeToCorsight(ActionDetails actionDetails, Employee employee, Device device) {

		if (ApplicationConstants.CREATE.equalsIgnoreCase(actionDetails.getAction().getType())) {

			String poid = null;
			String message = null;
			if (ApplicationConstants.FALSE.equalsIgnoreCase(corsightSyncServiceImpl.queryFromDevice(actionDetails.getDevice(),employee))) {
				poid = corsightSyncServiceImpl.addEmployeeToDevice(actionDetails);
			} else {
				message = corsightSyncServiceImpl.editEmployeeInDevice(actionDetails);
			}

			employee.setPoi(poid);

			if (null != poid || ApplicationConstants.SUCCESS.equalsIgnoreCase(message)) {
				actionDetails.setStatus(ApplicationConstants.COMPLETED);
				actionDetails.setMessage(ApplicationConstants.COMPLETED);

			} else {
				actionDetails.setStatus(ApplicationConstants.ERROR);
				actionDetails.setMessage(ActionDetailsConstants.PROCESS_FAILED);
			}
			employeeRepository.save(employee);
		} else if (ApplicationConstants.EDIT.equalsIgnoreCase(actionDetails.getAction().getType())) {
			String poid = null;
			String message = null;
			if (ApplicationConstants.TRUE.equalsIgnoreCase(corsightSyncServiceImpl.queryFromDevice(actionDetails.getDevice(),employee))) {
				poid = corsightSyncServiceImpl.addEmployeeToDevice(actionDetails);
				employee.setPoi(poid);
				employeeRepository.save(employee);

			} else {
				message = corsightSyncServiceImpl.editEmployeeInDevice(actionDetails);
			}
			if (null != poid || ApplicationConstants.SUCCESS.equalsIgnoreCase(message)) {
				actionDetails.setStatus(ApplicationConstants.COMPLETED);
				actionDetails.setMessage(ApplicationConstants.COMPLETED);

			} else {
				actionDetails.setStatus(ApplicationConstants.ERROR);
				actionDetails.setMessage(ActionDetailsConstants.PROCESS_FAILED);
			}

		} else if (ApplicationConstants.DELETE.equalsIgnoreCase(actionDetails.getAction().getType())) {
				String msg = corsightSyncServiceImpl.deleteSingleEmployeeFromDevice(actionDetails);
				if (ApplicationConstants.TRUE.equalsIgnoreCase(msg)) {
					actionDetails.setStatus(ApplicationConstants.COMPLETED);
					actionDetails.setMessage(ApplicationConstants.COMPLETED);
				} else {
					actionDetails.setStatus(ApplicationConstants.ERROR);
					actionDetails.setMessage(ActionDetailsConstants.PROCESS_FAILED);
				}
				removeEmployeeFromAreaAndSave(actionDetails.getAction(), device);
		}
		return actionDetails;
	}

	private ActionDetails addEmployeeToBeward(ActionDetails actionDetails, Employee employee, Device device) {

		@SuppressWarnings(ApplicationConstants.UNUSED)
		Long resultCode = NumberConstants.LONG_ZERO;
		String responeData = null;
		try {

			if (ApplicationConstants.DELETE.equalsIgnoreCase(actionDetails.getAction().getType())) {
				responeData = bewardSyncServiceImpl.deleteSingleEmployeeFromDevice(actionDetails);

				removeEmployeeFromAreaAndSave(actionDetails.getAction(), device);
				
			} else if (ApplicationConstants.EDIT.equalsIgnoreCase(actionDetails.getAction().getType())) {
				try {
					String pullRespone = bewardSyncServiceImpl.queryFromDevice(actionDetails.getDevice(),
							actionDetails.getAction().getEmployee());

					JSONParser jsonParser = new JSONParser();
					JSONObject jsonResponse = (JSONObject) jsonParser.parse(pullRespone);
					Long StatusCode = (Long) jsonResponse.get(BewardDeviceConstants.CODE);

					if (null != StatusCode) {
						responeData = bewardSyncServiceImpl.addEmployeeToDevice(actionDetails);
					} else {
						responeData = bewardSyncServiceImpl.editEmployeeInDevice(actionDetails);
					}
					if (null != responeData) {
						JSONParser jsonParserEdit = new JSONParser();
						JSONObject jsonResponseEdit = (JSONObject) jsonParserEdit.parse(responeData);

						JSONObject responseObj = (JSONObject) jsonResponseEdit.get(BewardDeviceConstants.INFO);
						resultCode = (Long) jsonResponseEdit.get(BewardDeviceConstants.CODE);
						String resultStatus = (String) responseObj.get(BewardDeviceConstants.RESULT);

						if (ApplicationConstants.FAIL.equalsIgnoreCase(resultStatus)) {
							String message = (String) responseObj.get(BewardDeviceConstants.DETAIL);
							actionDetails.setStatus(ApplicationConstants.ERROR);
							actionDetails.setMessage(message);
						} else {
							actionDetails.setStatus(ApplicationConstants.COMPLETED);
							actionDetails.setMessage(ApplicationConstants.COMPLETED);
						}
					} else {
						actionDetails.setStatus(ApplicationConstants.ERROR);
						actionDetails.setMessage(ActionDetailsConstants.USER_NOT_ADDED_IMAGE_NOT_FOUND);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (ApplicationConstants.CREATE.equalsIgnoreCase(actionDetails.getAction().getType())
					|| ApplicationConstants.SYNC.equalsIgnoreCase(actionDetails.getAction().getType())) {

				String pullRespone = bewardSyncServiceImpl.queryFromDevice(actionDetails.getDevice(),
						actionDetails.getAction().getEmployee());

				JSONParser jsonParser = new JSONParser();
				JSONObject jsonResponse = (JSONObject) jsonParser.parse(pullRespone);
				Long StatusCode = (Long) jsonResponse.get(BewardDeviceConstants.CODE);
				try {
					if (null != StatusCode) {
						responeData = bewardSyncServiceImpl.addEmployeeToDevice(actionDetails);
					} else {
						responeData = bewardSyncServiceImpl.editEmployeeInDevice(actionDetails);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			if (null != responeData) {
				JSONParser jsonParser = new JSONParser();
				JSONObject jsonResponse = (JSONObject) jsonParser.parse(responeData);

				JSONObject responseObj = (JSONObject) jsonResponse.get(BewardDeviceConstants.INFO);
				resultCode = (Long) jsonResponse.get(BewardDeviceConstants.CODE);
				String resultStatus = (String) responseObj.get(BewardDeviceConstants.RESULT);

				if (ApplicationConstants.FAIL.equalsIgnoreCase(resultStatus)) {
					String message = (String) responseObj.get(BewardDeviceConstants.DETAIL);
					actionDetails.setStatus(ApplicationConstants.ERROR);
					actionDetails.setMessage(message);
				} else {
					actionDetails.setStatus(ApplicationConstants.COMPLETED);
					actionDetails.setMessage(ApplicationConstants.COMPLETED);
				}
			} else {
				actionDetails.setStatus(ApplicationConstants.ERROR);
				actionDetails.setMessage(ActionDetailsConstants.USER_NOT_ADDED_IMAGE_NOT_FOUND);
			}

		} catch (ParseException e) {
			e.printStackTrace();
		}
		return actionDetails;
	}
}
