package com.eikona.mata.service.impl.sap;


import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.methods.HttpGet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

import com.eikona.mata.constants.ApplicationConstants;
import com.eikona.mata.constants.NumberConstants;
import com.eikona.mata.constants.SAPServerConstants;
import com.eikona.mata.entity.Employee;
import com.eikona.mata.repository.EmployeeRepository;
import com.eikona.mata.util.EmployeeObjectMap;
import com.eikona.mata.util.RequestExecutionUtil;

@Service
@EnableScheduling
public class SapToDbEmployeeSyncServiceImpl {

	@Autowired
	private EmployeeObjectMap employeeObjectMap;

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private RequestExecutionUtil requestExecutionUtil;
	
	@Value("${sap.login.username}")
    private String username;
	
	@Value("${sap.login.password}")
    private String password;

	// @Scheduled(fixedDelay = 30000)
	// @Scheduled(cron = "0 0 0/1 1/1 * ?")
	public void syncEmployeeListFromSap() {
		try {
			int top = NumberConstants.HUNDRED;
			int skip = NumberConstants.ZERO;
			while (true) {

				JSONArray resultsArray = getEmployeeListFromSap(top, skip);

				if (!resultsArray.isEmpty() && null != resultsArray) {
					List<Employee> employeeList = new ArrayList<Employee>();

					for (int i = NumberConstants.ZERO; i < resultsArray.size(); i++) {
						JSONObject currentObj = (JSONObject) resultsArray.get(i);
						JSONObject userNavObj = (JSONObject) currentObj.get( SAPServerConstants.USER_NAV);

						Employee employee = new Employee();
						setEmployeeDetails(currentObj, userNavObj, employee);
						employeeList.add(employee);

					}
					skip += NumberConstants.HUNDRED;
					saveEmployeeInDbFromSap(employeeList);
				} else
					break;

			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setEmployeeDetails(JSONObject currentObj, JSONObject userNavObj, Employee employee) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(ApplicationConstants.DATE_TIME_FORMAT_OF_US_WITH_ZONE);
		String date = (String) currentObj.get(SAPServerConstants.START_DATE);
		if (null != userNavObj)
			employee.setName((String) userNavObj.get(SAPServerConstants.DEFAULT_FULL_NAME));
		employee.setEmpId((String) currentObj.get(SAPServerConstants.USER_ID));

		Date joinDate = new Date(Long.valueOf(date.substring(NumberConstants.SIX, NumberConstants.NINETEEN)));
		String createdDateStr = simpleDateFormat.format(joinDate);

		employee.setJoinDate(createdDateStr);
	}

	public void saveEmployeeInDbFromSap(List<Employee> employeeList) {
		Map<String, Employee> employeeMap = employeeObjectMap.getEmployeeByEmpId();
		List<Employee> savingList = new ArrayList<Employee>();
		for (Employee employee : employeeList) {
			Employee emp = employeeMap.get(employee.getEmpId());
			if (null == emp)
				savingList.add(employee);
		}
		employeeRepository.saveAll(savingList);

	}

	public JSONArray getEmployeeListFromSap(int top, int skip) {
		JSONArray resultsArray = new JSONArray();
		try {
			String myurl = SAPServerConstants.USER_API_GET_ALL.formatted(String.valueOf(top), String.valueOf(skip));
			String newurl=myurl.replaceAll(ApplicationConstants.DELIMITER_SPACE, ApplicationConstants.DELIMITER_FORMAT_SPACE);
			HttpGet request = getSAPGetRequest(newurl);
			String responeData = requestExecutionUtil.executeHttpGetRequest(request);
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonResponse = (JSONObject) jsonParser.parse(responeData);
			JSONObject firstObj = (JSONObject) jsonResponse.get(SAPServerConstants.D);
			resultsArray = (JSONArray) firstObj.get(SAPServerConstants.RESULTS);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return resultsArray;

	}

	public HttpGet getSAPGetRequest(String myurl) throws Exception{
			HttpGet request = new HttpGet(myurl);
			request.setHeader(ApplicationConstants.HEADER_CONTENT_TYPE, ApplicationConstants.APPLICATION_JSON);
			String auth = username + ApplicationConstants.DELIMITER_COLON + password;
			byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.ISO_8859_1));
			String authHeader = ApplicationConstants.BASIC_AUTH + new String(encodedAuth);
			request.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
			return request;
		
	}
	
}
