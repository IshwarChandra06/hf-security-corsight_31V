package com.eikona.mata.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.eikona.mata.dto.PaginationDto;
import com.eikona.mata.entity.AccessLevel;
import com.eikona.mata.service.AccessLevelService;
import com.eikona.mata.util.AccessLevelUtil;

@Controller
public class AccessLevelController {
	
	@Autowired
	private AccessLevelService accessLevelService;
	
	@Autowired
	private AccessLevelUtil  accessLevelSync;
	
	@GetMapping("/access-level")
	@PreAuthorize("hasAuthority('access_level_view')")
	public String list() {
		return "accessLevel/access_level_list";
		
	}

	@RequestMapping(value = "/api/search/access-level", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('access_level_view')")
	public @ResponseBody PaginationDto<AccessLevel> searchAccessLevel(Long id, String name, int pageno, String sortField, String sortDir) {
		
		PaginationDto<AccessLevel> dtoList = accessLevelService.searchByField(id, name,  pageno, sortField, sortDir);
		return dtoList;
	}
	
	@GetMapping("/sync/access-level")
	@PreAuthorize("hasAuthority('access_level_sync')")
	public String syncAccessLevel(Model model) {
		
		String message = null;
		try {
			message = accessLevelSync.syncAndSaveAccessLevel();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		
		model.addAttribute("message", message);
		return "accessLevel/access_level_list";
		
	}
}
