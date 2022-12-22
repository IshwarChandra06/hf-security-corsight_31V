package com.eikona.mata.util;

import java.io.UnsupportedEncodingException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eikona.mata.constants.ApplicationConstants;
import com.eikona.mata.constants.BioSecurityConstants;
import com.eikona.mata.entity.Employee;

@Component
public class BioSecurityServerUtil {

	@Value("${biosecurity.host.url}")
	private String host;

	@Value("${biosecurity.api.accesstoken}")
	private String accesstoken;

	@Value("${biosecurity.server.port}")
	private String port;

	@Autowired
	private RequestExecutionUtil requestExecutionUtil;

	public String addAccessLevelToPerson(String accessLevelIds, String empId) {
		try {
			String myjson = BioSecurityConstants.PERSON_ADD_ACCESSELEVEL_IDS_JSON.formatted(accessLevelIds, empId);
			System.out.println(myjson);
			String myurl = ApplicationConstants.HTTP_COLON_DOUBLE_SLASH + host + ApplicationConstants.DELIMITER_COLON
					+ port + BioSecurityConstants.API_ADD_PERSON + ApplicationConstants.DELIMITER_QUESTION_MARK
					+ ApplicationConstants.ACCESS_TOKEN + ApplicationConstants.DELIMITER_EQUAL_TO + accesstoken;
			HttpPost request = getHttpPostRequest(myjson, myurl);

			String responeData = requestExecutionUtil.executeHttpPostRequest(request);
			System.out.println(responeData);

			String message = getMessageFromResponse(responeData);

			return message;
		} catch (Exception e) {
			e.printStackTrace();
			return ApplicationConstants.FAILED;
		}

	}

	public String addEmployeeToBioSecurity(Employee employee) {
		try {
			String myjson = BioSecurityConstants.PERSON_ADD_JSON.formatted(employee.getName(), employee.getEmpId(),
					employee.getJoinDate());
			System.out.println(myjson);
			String myurl = ApplicationConstants.HTTP_COLON_DOUBLE_SLASH + host + ApplicationConstants.DELIMITER_COLON
					+ port + BioSecurityConstants.API_ADD_PERSON + ApplicationConstants.DELIMITER_QUESTION_MARK
					+ ApplicationConstants.ACCESS_TOKEN + ApplicationConstants.DELIMITER_EQUAL_TO + accesstoken;
			HttpPost request = getHttpPostRequest(myjson, myurl);

			String responeData = requestExecutionUtil.executeHttpPostRequest(request);
			System.out.println(responeData);

			String message = getMessageFromResponse(responeData);

			return message;
		} catch (Exception e) {
			e.printStackTrace();
			return ApplicationConstants.FAILED;
		}

	}

	private String getMessageFromResponse(String responeData) throws ParseException {
		JSONParser jsonParser = new JSONParser();
		JSONObject jsonResponse = (JSONObject) jsonParser.parse(responeData);
		String message = (String) jsonResponse.get(BioSecurityConstants.MESSAGE);
		return message;
	}

	private HttpPost getHttpPostRequest(String myjson, String myurl) throws UnsupportedEncodingException {
		HttpPost request = new HttpPost(myurl);
		StringEntity entity = new StringEntity(myjson);
		request.setHeader(ApplicationConstants.HEADER_CONTENT_TYPE, ApplicationConstants.APPLICATION_JSON);
		request.setEntity(entity);
		return request;
	}

}
