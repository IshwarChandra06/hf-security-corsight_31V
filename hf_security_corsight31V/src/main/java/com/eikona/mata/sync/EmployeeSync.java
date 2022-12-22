package com.eikona.mata.sync;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.eikona.mata.constants.ApplicationConstants;
import com.eikona.mata.constants.CorsightDeviceConstants;
import com.eikona.mata.constants.MessageConstants;
import com.eikona.mata.constants.NumberConstants;
import com.eikona.mata.entity.Employee;
import com.eikona.mata.repository.EmployeeRepository;
import com.eikona.mata.util.RequestExecutionUtil;

@Service
public class EmployeeSync {

	@Autowired
	private EmployeeRepository workerRepository;

	@Autowired
	private RequestExecutionUtil requestExecutionUtil;
	
	@Value("${corsight.host.url}")
    private String corsightHost;
	
	@Value("${corsight.poi.port}")
    private String poiPort;

	public String syncEmployee() {
		try {
			String afterId = ApplicationConstants.DELIMITER_EMPTY;
			getEmployeeList(afterId);
			return MessageConstants.SYNC_SUCCESSFULLY;
		} catch (Exception e) {
			e.printStackTrace();
			return MessageConstants.SYNC_FAILED;
		}
	}

	public void getEmployeeList(String afterId) {
		try {

			String poiUrl = ApplicationConstants.HTTPS_COLON_DOUBLE_SLASH + corsightHost
					+ ApplicationConstants.DELIMITER_COLON + poiPort;
			String addPoiUrl = null;
			if (afterId.isEmpty())
				addPoiUrl = CorsightDeviceConstants.POI_API_SYNC;
			else
				addPoiUrl = CorsightDeviceConstants.POI_API_SYNC_AFTER_ID;

			String responeData = requestExecutionUtil.executeHttpsGetRequest(poiUrl, addPoiUrl);

			JSONParser jsonParser = new JSONParser();
			JSONObject jsonResponse = (JSONObject) jsonParser.parse(responeData);
			JSONObject jsonResponseData = (JSONObject) jsonResponse.get(CorsightDeviceConstants.DATA);
			JSONArray jsonArray = (JSONArray) jsonResponseData.get(CorsightDeviceConstants.POIS);
			List<Employee> WorkerList = new ArrayList<>();

			setEmployeeDetails(jsonArray, WorkerList);

			workerRepository.saveAll(WorkerList);

			if (!WorkerList.isEmpty()) {
				afterId = WorkerList.get(WorkerList.size() - 1).getPoi();

				getEmployeeList(afterId);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setEmployeeDetails(JSONArray jsonArray, List<Employee> WorkerList) {
		for (int i = NumberConstants.ZERO; i < jsonArray.size(); i++) {
			JSONObject jsonData = (JSONObject) jsonArray.get(i);

			Employee worker = workerRepository.findByPoiAndIsDeletedFalse((String) jsonData.get(CorsightDeviceConstants.POI_ID));
			if (null == worker) {
				Employee workerObj = new Employee();
				workerObj.setEmpId((String) jsonData.get(CorsightDeviceConstants.DISPLAY_NAME));
				workerObj.setPoi((String) jsonData.get(CorsightDeviceConstants.POI_ID));
				WorkerList.add(workerObj);
			}
		}
	}

}
