package com.eikona.mata.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.eikona.mata.constants.ApplicationConstants;
import com.eikona.mata.constants.BioSecurityConstants;
import com.eikona.mata.constants.MessageConstants;
import com.eikona.mata.constants.NumberConstants;
import com.eikona.mata.entity.AccessLevel;
import com.eikona.mata.repository.AccessLevelRepository;

@Component
public class AccessLevelUtil {
	
	@Autowired
	private AccessLevelRepository accessLevelRepository;
	
	@Autowired
	private RequestExecutionUtil requestExecutionUtil;
	
	@Value("${biosecurity.host.url}")
    private String host;
	
	@Value("${biosecurity.server.port}")
	private String port;
	
	@Value("${biosecurity.api.accesstoken}")
    private String accesstoken;
	

	public String syncAndSaveAccessLevel() {
		try {
			List<AccessLevel> accessLevelList =syncAccessLevel();
				accessLevelRepository.deleteAll();
				accessLevelRepository.saveAll(accessLevelList);
    			
    			return MessageConstants.SYNC_SUCCESSFULLY;
           
		} catch (Exception e) {
			e.printStackTrace();
			return MessageConstants.SYNC_FAILED;
		}
	}
	public List<AccessLevel> syncAccessLevel() {
		List<AccessLevel> accessLevelList = new ArrayList<AccessLevel>();
		try {
			String myurl=ApplicationConstants.HTTP_COLON_DOUBLE_SLASH+host+ ApplicationConstants.DELIMITER_COLON
					+ port +BioSecurityConstants.ACC_LEVEL_SYNC_API+accesstoken;
		    HttpGet request = new HttpGet(myurl);
		
		    String responeData =requestExecutionUtil.executeHttpGetRequest(request);
			
			JSONParser jsonParser = new JSONParser();
			JSONObject jsonResponse = (JSONObject) jsonParser.parse(responeData);
			JSONArray responseArray = (JSONArray) jsonResponse.get(BioSecurityConstants.DATA);
			
			setAccessLevelList(accessLevelList, responseArray);
			
		}catch (Exception e) {
			e.printStackTrace();
		}
		return accessLevelList;
	}
	private void setAccessLevelList(List<AccessLevel> accessLevelList, JSONArray responseArray) {
		for(int i=NumberConstants.ZERO; i<responseArray.size(); i++) {
			JSONObject currentObj = (JSONObject) responseArray.get(i);
			
			AccessLevel accessLevel = new AccessLevel();
			
			accessLevel.setAccessId((String)currentObj.get(ApplicationConstants.ID));
			accessLevel.setName((String)currentObj.get(ApplicationConstants.NAME));
				
			accessLevelList.add(accessLevel);
			
		}
	}
}
