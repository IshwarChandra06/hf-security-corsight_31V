package com.eikona.mata.controller;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.apache.http.HttpHeaders;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.function.client.WebClient;

import com.eikona.mata.dto.PaginationDto;
import com.eikona.mata.entity.Area;
import com.eikona.mata.entity.Branch;
import com.eikona.mata.entity.Device;
import com.eikona.mata.entity.Employee;
import com.eikona.mata.repository.AreaRepository;
import com.eikona.mata.service.AreaService;
import com.eikona.mata.service.BranchService;
import com.eikona.mata.service.DoorService;
import com.eikona.mata.service.OrganizationService;
import com.eikona.mata.sync.WatchListSync;
import com.eikona.mata.util.CorsightAuth;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;

@Controller
public class AreaController {

	@Autowired
	private AreaService areaService;

	@Autowired
	private AreaRepository areaDatatableRepository;

	@Autowired
	private DoorService doorService;

	@Autowired
	private BranchService branchService;

	@Autowired
	private OrganizationService organizationService;
	
	@Value("${corsight.enabled}")
	private boolean corsightEnabled;
	
	@Autowired
	private WatchListSync watchListSync;
	
	@Autowired
	private CorsightAuth logIn;
	
	@Value("${corsight.host.url}")
	private String host;
	
	@Value("${corsight.poi.port}")
	private String portPoi;
	
	@Value("${defalult.branch.id}")
	private String defalutBranchId;

	@GetMapping("/area")
	@PreAuthorize("hasAuthority('area_view')")
	public String areaListPage(Model model) {
		model.addAttribute("corsightEnabled", corsightEnabled);
		return "area/area_list";
	}

	@GetMapping("/areabybranch")
	public @ResponseBody List<Area> getAreaByBranch(@RequestParam String branch) {
		Branch branchObj = new Branch();
		branchObj.setName(branch);
		List<Area> areaList = areaDatatableRepository.findByBranchAndIsDeletedFalseCustom(branch);
		return areaList;
	}

	@GetMapping("/areabybranchid")
	@PreAuthorize("hasAuthority('device_create')")
	public @ResponseBody List<Area> getAreaByBranchId(@RequestParam String branchId) {
		Branch branchObj = branchService.getById(Long.valueOf(branchId));
		List<Area> areaList = areaDatatableRepository.findByBranchAndIsDeletedFalse(branchObj);
		return areaList;
	}

	@GetMapping("/area/new")
	@PreAuthorize("hasAuthority('area_create')")
	public String newArea(Model model) {

		model.addAttribute("listOrganization", organizationService.getAll());
		model.addAttribute("listBranch", branchService.getAll());
		model.addAttribute("listDoor", doorService.getAll());
		Area area = new Area();
		model.addAttribute("area", area);
		model.addAttribute("title", "New Area");
		return "area/area_new";
	}

	@GetMapping("/watch-list-sync")
	@PreAuthorize("hasAuthority('watchlist_sync')")
	public String areaSync(Model model) {
		String message = null;
		try {
			message = watchListSync.syncWatchlist();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
		model.addAttribute("message", message);
		model.addAttribute("corsightEnabled",corsightEnabled);
		return "area/area_list";
	}
	
//	@GetMapping("/watch-list-sync")
//	@PreAuthorize("hasAuthority('watchlist_sync')")
//	private String areaSync(Model model) {
//		String message =null;
//		try {
//		
//			 message = watchListSync.syncWatchlist();
//		}catch (Exception e) {
//			e.printStackTrace();
//		}
//		model.addAttribute("message", message);
//		model.addAttribute("corsightEnabled", corsightEnabled);
//		return "area/area_list";
//	}
//	
//	public String syncWatchlist() {
//		 try {
//
//			String authHeader = "Bearer " + logIn.getToken();
//			 
//			String poiUrl = "https://"+host+":"+portPoi;
//			String  addPoiUrl ="/poi_db/watchlist/?count_pois=false";
//	
//			 SslContext sslContext = SslContextBuilder
//			            .forClient()
//			            .trustManager(InsecureTrustManagerFactory.INSTANCE)
//			            .build();
//			    
//			HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
//			    
//			 WebClient webClient = WebClient.builder()
//			  .baseUrl(poiUrl+addPoiUrl)
//			  //.defaultCookie("cookieKey", "cookieValue")
//			  .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
//			  .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
//			  .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
//			  .clientConnector(new ReactorClientHttpConnector(httpClient))
//			  //.defaultUriVariables(Collections.singletonMap("url", "http://localhost:8080"))
//			  .build();
//	
//			 ResponseEntity<JSONObject> response = webClient.get()
//			         .retrieve()
//			         .toEntity(JSONObject.class)
//			         .block();
//	
//			 String responeData = response.getBody().toString(); //EntityUtils.toString(response.getEntity());
//			 
//			 //webClient.close();
//			
//			JSONParser jsonParser = new JSONParser();
//			JSONObject jsonResponse = (JSONObject)jsonParser.parse(responeData);
//			JSONObject jsonResponseData = (JSONObject) jsonResponse.get("data");
//			JSONArray jsonArray = (JSONArray) jsonResponseData.get("watchlists");
//			List<Area>  listWatchList = new ArrayList<>();
//			for(int i=0; i< jsonArray.size(); i++) {
//				JSONObject jsonData = (JSONObject) jsonArray.get(i);
//				
//				Area area = areaDatatableRepository.findByWatchlistIdAndIsDeletedFalse((String)jsonData.get("watchlist_id"));
//				if(null == area) {
//					Area areaObj = new Area();
//					areaObj.setName((String)jsonData.get("display_name"));
//					areaObj.setWatchlist((String)jsonData.get("watchlist_type"));
//					areaObj.setWatchlistId((String)jsonData.get("watchlist_id"));
//					Branch branch = new Branch();
//					branch.setId(Long.valueOf(defalutBranchId));
//					areaObj.setBranch(branch);
//					listWatchList.add(areaObj);
//					
//				}
//			}
//			
//			areaDatatableRepository.saveAll(listWatchList);
//			System.out.println(jsonArray);
//			return "Sync Successfully!";
//		} catch (Exception e) {
//			e.printStackTrace();
//			return "Sync Failed!";
//		 }
//	 }

	@PostMapping("/area/add")
	@PreAuthorize("hasAnyAuthority('area_create','area_update')")
	public String saveArea(@ModelAttribute("area") Area area, @Valid Area ar, Errors errors, String title,
			Model model) {
		if (errors.hasErrors()) {
			model.addAttribute("listOrganization", organizationService.getAll());
			model.addAttribute("listBranch", branchService.getAll());
			model.addAttribute("listDoor", doorService.getAll());
			model.addAttribute("title", title);
			return "area/area_new";
		} else {
			if (null == area.getId())
				areaService.save(area);
			else {
				Area areaObj = areaService.getById(area.getId());
				area.setWatchlistId(areaObj.getWatchlistId());
				area.setCreatedBy(areaObj.getCreatedBy());
				area.setCreatedDate(areaObj.getCreatedDate());
				areaService.save(area);
			}
			return "redirect:/area";

		}

	}

	@GetMapping("/area/edit/{id}")
	@PreAuthorize("hasAuthority('area_update')")
	public String updateArea(@PathVariable(value = "id") long id, Model model) {
		Area area = areaService.getById(id);
		model.addAttribute("listOrganization", organizationService.getAll());
		model.addAttribute("listBranch", branchService.getAll());
		model.addAttribute("listDoor", doorService.getAll());
		model.addAttribute("area", area);
		model.addAttribute("title", "Update Area");
		model.addAttribute("corsightEnabled", corsightEnabled);
		return "area/area_new";
	}

	@GetMapping("/area/delete/{id}")
	@PreAuthorize("hasAuthority('area_delete')")
	public String deleteArea(@PathVariable(value = "id") long id) {

		this.areaService.deletedById(id);
		return "redirect:/area";
	}

	@RequestMapping(value = "/api/search/area", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('area_view')")
	public @ResponseBody PaginationDto<Area> search(Long id, String name, String office, int pageno, String sortField,
			String sortDir) {

		PaginationDto<Area> dtoList = areaService.searchByField(id, name, office, pageno, sortField, sortDir);
		return dtoList;
	}

	@GetMapping("/area-to-employee/association/{id}")
	@PreAuthorize("hasAuthority('area_employee_association')")
	public String areaEmployeeAssociation(@PathVariable(value = "id") long id, Model model) {
		Area areaObj=areaService.getById(id);
		model.addAttribute("area", areaObj.getName());
		if(null!=areaObj.getBranch())
			model.addAttribute("Office", areaObj.getBranch().getName());
		else
			model.addAttribute("Office", "");
		model.addAttribute("id", id);
		return "area/area_employee";
	}

	@RequestMapping(value = "/api/search/area-to-employee/association", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('area_employee_association')")
	public @ResponseBody PaginationDto<Employee> search(String id, String name, String office, String area, int pageno, String sortField, String sortDir) {
		
		PaginationDto<Employee> dtoList = areaService.searchAreaToEmployee(id, name, office, area, pageno, sortField, sortDir);
		return dtoList;
	}

	@GetMapping("/area-to-employee/association/save")
	@PreAuthorize("hasAuthority('area_employee_association')")
	public @ResponseBody ResponseEntity<Object> saveAreaEmployeeAssociation(@RequestParam Long employeeId,
			@RequestParam Long areaId, Principal principal) {

		String message = areaService.saveAreaEmployeeAssociation(employeeId, areaId, principal);
		return ResponseEntity.ok(message);
	}

	@GetMapping("/area-to-device/association/{id}/{branchId}")
	@PreAuthorize("hasAuthority('area_device_association')")
	public String areaDeviceAssociation(@PathVariable(value = "id") long id,
			@PathVariable(value = "branchId") long branchId, Model model) {
		Area areaObj = areaService.getById(id);

		model.addAttribute("device", new Device());

		model.addAttribute("area", areaObj.getName());
		if (null != areaObj.getBranch())
			model.addAttribute("Office", areaObj.getBranch().getName());
		else
			model.addAttribute("Office", "");
		model.addAttribute("id", id);
		model.addAttribute("branchId", branchId);
		return "area/area_device";
	}

	@RequestMapping(value = "/api/search/area-to-device/association", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('area_device_association')")
	public @ResponseBody PaginationDto<Device> searchAreaToDevice(String name, String office,
			String area, int pageno, String sortField, String sortDir) {

		PaginationDto<Device> dtoList = areaService.searchAreaToDevice(name, office, area, pageno, sortField,
				sortDir);
		return dtoList;
	}


	@GetMapping("/area-to-device/association/save")
	@PreAuthorize("hasAuthority('area_device_association')")
	public @ResponseBody ResponseEntity<Object> saveAreaDeviceAssociation(@RequestParam Long deviceId,
			@RequestParam Long areaId, Principal principal) {

		String message = areaService.saveAreaDeviceAssociation(deviceId, areaId, principal);
		return ResponseEntity.ok(message);
	}
}
