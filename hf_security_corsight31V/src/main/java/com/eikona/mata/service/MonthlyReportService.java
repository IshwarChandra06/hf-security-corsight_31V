package com.eikona.mata.service;

import com.eikona.mata.dto.IncapMonthlyAttendanceDto;
import com.eikona.mata.dto.IncapReportDto;
import com.eikona.mata.dto.PaginationDto;



public interface MonthlyReportService {


	PaginationDto<IncapReportDto<IncapMonthlyAttendanceDto>> searchByField(Long id, String date, String employeeId,
			String employeeName, String department, String designation, int pageno, String sortField, String sortDir);

}
