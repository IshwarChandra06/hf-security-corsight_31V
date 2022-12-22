package com.eikona.mata.service.impl.sap;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import com.eikona.mata.constants.ApplicationConstants;
import com.eikona.mata.constants.NumberConstants;
import com.eikona.mata.constants.SAPServerConstants;
import com.eikona.mata.entity.AccessLevel;
import com.eikona.mata.entity.Shift;
import com.eikona.mata.repository.ShiftRepository;
import com.eikona.mata.util.AccessLevelUtil;
import com.eikona.mata.util.BioSecurityServerUtil;
import com.eikona.mata.util.RequestExecutionUtil;

@Service
@EnableScheduling
public class SapToBioSecurityShiftAssignServiceImpl {

	@Autowired
	private BioSecurityServerUtil bioSecurityUtil;

	@Autowired
	private ShiftRepository shiftRepository;

	@Autowired
	private AccessLevelUtil accessLevelUtil;

	@Autowired
	private RequestExecutionUtil requestExecutionUtil;

	@Autowired
	private SapToDbEmployeeSyncServiceImpl sapToBioSecurityEmployeeSyncServiceImpl;

	//@Scheduled(fixedDelay = 30000)
	public void getEmployeeWiseShiftFromSap() {
		try {
			List<Shift> shiftList = shiftRepository.findAllByIsDeletedFalse();
			SimpleDateFormat sdf = new SimpleDateFormat(ApplicationConstants.DATE_FORMAT_OF_US);
			String date = sdf.format(new Date());
			List<AccessLevel> accessLevelList = accessLevelUtil.syncAccessLevel();
			for (Shift shift : shiftList) {
				int top = NumberConstants.HUNDRED;
				int skip = NumberConstants.ZERO;
				while (true) {
					List<String> userIdList = getShiftInfoThroughShiftAndDate(shift.getName(), date, top, skip);

					if (!userIdList.isEmpty() && null != userIdList) {
						addAccessLevelToEmployeeInBioSecurity(accessLevelList, shift, userIdList);
						skip += NumberConstants.HUNDRED;
					} else
						break;

				}
				shift.setLastSyncDate(new Date());
				shiftRepository.save(shift);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void addAccessLevelToEmployeeInBioSecurity(List<AccessLevel> accessLevelList, Shift shift,
			List<String> userIdList) {
		for (String empId : userIdList) {

			String accessLevelIds = ApplicationConstants.DELIMITER_EMPTY;

			accessLevelIds = getAccessLevelIdByShift(accessLevelList, shift, accessLevelIds);

			bioSecurityUtil.addAccessLevelToPerson(accessLevelIds, empId);
		}

	}

	private String getAccessLevelIdByShift(List<AccessLevel> accessLevelList, Shift shift, String accessLevelIds) {
		for (AccessLevel accessLevel : accessLevelList) {
			if (accessLevel.getName().split(ApplicationConstants.DELIMITER_SPLIT_BY_SPACE)[NumberConstants.ZERO].equalsIgnoreCase(shift.getName())
					&& !(ApplicationConstants.WO.equalsIgnoreCase(shift.getName()))) {
				if (accessLevelIds.isEmpty())
					accessLevelIds = accessLevel.getAccessId();
				else
					accessLevelIds += ApplicationConstants.DELIMITER_COMMA + accessLevel.getAccessId();
			}
		}
		return accessLevelIds;
	}

	public List<String> getShiftInfoThroughShiftAndDate(String shift, String date, int top, int skip) {
		List<String> userIdList = new ArrayList<>();
		try {
			String myurl =SAPServerConstants.USER_TIME_INFO_API_BY_SHIFT.formatted(shift, date, String.valueOf(top), String.valueOf(skip));
			String newurl=myurl.replaceAll(ApplicationConstants.DELIMITER_SPACE, ApplicationConstants.DELIMITER_FORMAT_SPACE);
			HttpGet request = sapToBioSecurityEmployeeSyncServiceImpl.getSAPGetRequest(newurl);
				String responeData = requestExecutionUtil.executeHttpGetRequest(request);
				JSONParser jsonParser = new JSONParser();

				JSONObject jsonResponse = (JSONObject) jsonParser.parse(responeData);
				JSONObject firstObj = (JSONObject) jsonResponse.get(SAPServerConstants.D);
				JSONArray resultsArray = (JSONArray) firstObj.get(SAPServerConstants.RESULTS);

				for (int i = NumberConstants.ZERO; i < resultsArray.size(); i++) {
					JSONObject currentObj = (JSONObject) resultsArray.get(i);
					String userId = (String) currentObj.get(SAPServerConstants.USER_ID);
					userIdList.add(userId);
				}
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		return userIdList;
	}
}
