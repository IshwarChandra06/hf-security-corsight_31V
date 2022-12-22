package com.eikona.mata.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.eikona.mata.dto.IncapMonthlyAttendanceDto;
import com.eikona.mata.dto.IncapReportDto;
import com.eikona.mata.dto.PaginationDto;
import com.eikona.mata.service.MonthlyReportService;


@Controller
public class MonthlyReportController {
	
	
	@Autowired
	private MonthlyReportService monthlyReportService;
	
	@GetMapping("/monthly/report")
	@PreAuthorize("hasAuthority('monthlyattendance_view')")
	public String getMonthlyReportPage(Model model) {
		return "monthlyreport/monthlydetailreport";
	}
	
	
	@RequestMapping(value = "/api/search/monthly-report", method = RequestMethod.GET)
	@PreAuthorize("hasAuthority('monthlyattendance_view')")
	public @ResponseBody PaginationDto<IncapReportDto<IncapMonthlyAttendanceDto>> searchVehicleLog(Long id, String date, String employeeId, String employeeName,String department, String designation,
			int pageno, String sortField, String sortDir) {
		
		PaginationDto<IncapReportDto<IncapMonthlyAttendanceDto>> dtoList = monthlyReportService.searchByField(id, date, employeeId, employeeName, department, designation, pageno, sortField, sortDir);
		
		return dtoList;
	}
//
//	@GetMapping("/get/monthlyReport")
//	public String generateMonthlyReport(Model model) {
//		model.addAttribute("listDailyReport", dailyReportRepository.findAll());
//		return "monthlyreport/monthlydetailreport_list";
//	}
}
