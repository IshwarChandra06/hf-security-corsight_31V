package com.eikona.mata.service.impl.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.eikona.mata.dto.IncapReportDto;
import com.eikona.mata.constants.ApplicationConstants;
import com.eikona.mata.constants.EmployeeConstants;
import com.eikona.mata.constants.HeaderConstants;
import com.eikona.mata.constants.NumberConstants;
import com.eikona.mata.dto.IncapMonthlyAttendanceDto;
import com.eikona.mata.dto.PaginationDto;
import com.eikona.mata.entity.DailyAttendance;
import com.eikona.mata.entity.Employee;
import com.eikona.mata.repository.DailyAttendanceRepository;
import com.eikona.mata.repository.EmployeeRepository;
import com.eikona.mata.service.MonthlyReportService;
import com.eikona.mata.util.CalendarUtil;
import com.eikona.mata.util.GeneralSpecificationUtil;

@Service
public class MonthlyReportServiceImpl implements MonthlyReportService {

	@Autowired
	private EmployeeRepository employeeRepository;

	@Autowired
	private DailyAttendanceRepository dailyAttendanceRepository;

	@Autowired
	private GeneralSpecificationUtil<Employee> generalSpecification;

	@Autowired
	private CalendarUtil calendarUtil;

	@SuppressWarnings(ApplicationConstants.UNUSED)
	public IncapMonthlyAttendanceDto calculateDaywiseMonthlyReport(Date startDate, Date endDate, Employee employee) {

//		Calendar startCalendar = calendarUtil.getCalendar(startDate, NumberConstants.ZERO, NumberConstants.ZERO,
//				NumberConstants.ZERO);
//
//		int lastDayOfMonth = startCalendar.getActualMaximum(Calendar.DATE);
//		
//		endDate = calendarUtil.getConvertedDate(endDate, lastDayOfMonth, NumberConstants.TWENTY_THREE, NumberConstants.FIFTY_NINE,
//				NumberConstants.FIFTY_NINE);
		

		List<DailyAttendance> dailyReportList = dailyAttendanceRepository.findDetailsByDateCustom(employee.getEmpId(),
				startDate, endDate);

		Float totalPresentCount = NumberConstants.FLOAT_ZERO;

		Float totalAbsentCount = NumberConstants.FLOAT_ZERO;

		long totalOverTime = NumberConstants.LONG_ZERO;

		long totalLateComing = NumberConstants.LONG_ZERO;

		long totalEarlyGoing = NumberConstants.LONG_ZERO;

		long totalWeekOffCount = NumberConstants.LONG_ZERO;

		IncapMonthlyAttendanceDto monthlyDailyReportDto = new IncapMonthlyAttendanceDto();

		if (null != employee) {
			setEmployeeDetails(employee, monthlyDailyReportDto);
		} else {
			setEmployeeDetailsAsBlank(monthlyDailyReportDto);

		}

		Iterator<DailyAttendance> dailyReportListItr = dailyReportList.iterator();
		DailyAttendance dailyAttendance = null;
		if (dailyReportListItr.hasNext()) {
			dailyAttendance = dailyReportListItr.next();
		}

		Calendar startDayCalendar = Calendar.getInstance();
		startDayCalendar.setTime(startDate);
		int first = startDayCalendar.getActualMinimum(Calendar.DATE);

		Calendar endDayCalendar = Calendar.getInstance();
		endDayCalendar.setTime(endDate);
		int last = endDayCalendar.getActualMaximum(Calendar.DATE);

		List<String> dataList = new ArrayList<String>();
		while (first <= last) {
//			int day = 0;
//			if(null != dailyAttendance) {
//				int day = Integer.valueOf(dailyAttendance.getDateStr().split("-")[2]);
//			}
				
//				if(first == day) {
					if (null != dailyAttendance) {
						int day = Integer.valueOf(dailyAttendance.getDateStr().split("-")[2]);
						if(first == day) {
							if (ApplicationConstants.PRESENT.equalsIgnoreCase(dailyAttendance.getAttendanceStatus()))
								totalPresentCount += NumberConstants.FLOAT_ONE;
							if (ApplicationConstants.ABSENT.equalsIgnoreCase(dailyAttendance.getAttendanceStatus()))
								totalAbsentCount += NumberConstants.FLOAT_ONE;
							dataList.add(dailyAttendance.getAttendanceStatus());
							dailyAttendance = null;
							
							if (dailyReportListItr.hasNext()) {
								dailyAttendance = dailyReportListItr.next();
							}
						}else {
							dataList.add(ApplicationConstants.DELIMITER_HYPHEN);
						}
						
					} else {
						dataList.add(ApplicationConstants.DELIMITER_HYPHEN);
					}
//				}else{
//					dataList.add(ApplicationConstants.DELIMITER_HYPHEN);
//				}
			

//			if (dailyReportListItr.hasNext()) {
//				dailyAttendance = dailyReportListItr.next();
//			}
			first++;
		}

		setAttendanceCalculation(last, totalPresentCount, totalAbsentCount, totalOverTime, totalLateComing,
				totalEarlyGoing, totalWeekOffCount, monthlyDailyReportDto, dataList);

		return monthlyDailyReportDto;

	}

	private void setAttendanceCalculation(int lastDayOfMonth, Float totalPresentCount, Float totalAbsentCount,
			long totalOverTime, long totalLateComing, long totalEarlyGoing, long totalWeekOffCount,
			IncapMonthlyAttendanceDto monthlyDailyReportDto, List<String> dataList) {
		monthlyDailyReportDto.setTotalPresentCount(String.valueOf(totalPresentCount));
		monthlyDailyReportDto.setTotalAbsentCount(String.valueOf(totalAbsentCount));
		monthlyDailyReportDto.setTotalWeekOff(String.valueOf(totalWeekOffCount));
		monthlyDailyReportDto.setTotalOverTime(String.valueOf(totalOverTime));
		monthlyDailyReportDto.setTotalLateIn(String.valueOf(totalLateComing));
		monthlyDailyReportDto.setTotalEarlyOut(String.valueOf(totalEarlyGoing));

		monthlyDailyReportDto.setTotalDays(String.valueOf(lastDayOfMonth));
		monthlyDailyReportDto.setPayableDays(String.valueOf(totalWeekOffCount + totalPresentCount));

		monthlyDailyReportDto.setDateList(dataList);
	}

	private void setEmployeeDetailsAsBlank(IncapMonthlyAttendanceDto monthlyDailyReportDto) {
		monthlyDailyReportDto.setEmpId(ApplicationConstants.DELIMITER_SPACE);
		monthlyDailyReportDto.setEmpName(ApplicationConstants.DELIMITER_SPACE);
		monthlyDailyReportDto.setOrganization(ApplicationConstants.DELIMITER_SPACE);
		monthlyDailyReportDto.setDepartment(ApplicationConstants.DELIMITER_SPACE);
		monthlyDailyReportDto.setDesignation(ApplicationConstants.DELIMITER_SPACE);
	}

	private void setEmployeeDetails(Employee employee, IncapMonthlyAttendanceDto monthlyDailyReportDto) {
		if (null != employee.getEmpId())
			monthlyDailyReportDto.setEmpId(employee.getEmpId());
		else
			monthlyDailyReportDto.setEmpId(ApplicationConstants.DELIMITER_SPACE);
		monthlyDailyReportDto.setEmpName(employee.getName());
		if (null != employee.getDepartment())
			monthlyDailyReportDto.setDepartment(employee.getDepartment().getName());
		else
			monthlyDailyReportDto.setDepartment(ApplicationConstants.DELIMITER_SPACE);
		if (null != employee.getDesignation())
			monthlyDailyReportDto.setDesignation(employee.getDesignation().getName());
		else
			monthlyDailyReportDto.setDesignation(ApplicationConstants.DELIMITER_SPACE);
		if (null != employee.getOrganization())
			monthlyDailyReportDto.setOrganization(employee.getOrganization().getName());
		else
			monthlyDailyReportDto.setOrganization(ApplicationConstants.DELIMITER_SPACE);
	}

	@Override
	public PaginationDto<IncapReportDto<IncapMonthlyAttendanceDto>> searchByField(Long id, String dateStr,
			String employeeId, String employeeName, String department, String designation, int pageno, String sortField,
			String sortDir) {

		PaginationDto<IncapReportDto<IncapMonthlyAttendanceDto>> dtoList = new PaginationDto<>();
		IncapReportDto<IncapMonthlyAttendanceDto> monthlyDetailsReport = new IncapReportDto<>();
		Date date = null;
		SimpleDateFormat format = new SimpleDateFormat(ApplicationConstants.DATE_FORMAT_OF_US);
		if (dateStr.isEmpty()) {
			dateStr = format.format(new Date());
		}
		if (!dateStr.isEmpty()) {
			try {

				date = format.parse(dateStr);
				Calendar dateCalendar = Calendar.getInstance();
				dateCalendar.setTime(date);

				String month = dateCalendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH);

				int first = dateCalendar.getActualMinimum(Calendar.DATE),
						day = dateCalendar.getActualMinimum(Calendar.DATE);
				int last = dateCalendar.getActualMaximum(Calendar.DATE);

				List<String> headList = new ArrayList<String>();
				setHeader(headList);

				while (day <= last) {
					headList.add(day + ApplicationConstants.DELIMITER_SPACE
							+ month.substring(NumberConstants.ZERO, NumberConstants.THREE));
					day++;
				}

				Date startDate = calendarUtil.getConvertedDate(date, first, NumberConstants.ZERO, NumberConstants.ZERO, NumberConstants.ZERO);

				Date endDate = calendarUtil.getConvertedDate(date, last, NumberConstants.TWENTY_THREE,
						NumberConstants.FIFTY_NINE, NumberConstants.FIFTY_NINE);

				if (null == sortDir || sortDir.isEmpty()) {
					sortDir = ApplicationConstants.ASC;
				}
				if (null == sortField || sortField.isEmpty()) {
					sortField = ApplicationConstants.ID;
				}
				Page<Employee> page = getEmployeePage(id, employeeId, employeeName, department, designation, pageno,
						sortField, sortDir);

				List<Employee> workerList = page.getContent();
				List<IncapMonthlyAttendanceDto> monthlyReportList = new ArrayList<>();
				for (Employee employee : workerList) {
					IncapMonthlyAttendanceDto monthlyDetailDto = calculateDaywiseMonthlyReport(startDate, endDate, employee);
					monthlyReportList.add(monthlyDetailDto);
				}

				monthlyDetailsReport.setHeadList(headList);
				monthlyDetailsReport.setDataList(monthlyReportList);

				sortDir = (ApplicationConstants.ASC.equalsIgnoreCase(sortDir)) ? ApplicationConstants.DESC
						: ApplicationConstants.ASC;
				List<IncapReportDto<IncapMonthlyAttendanceDto>> monthlyDetailsReportList = new ArrayList<IncapReportDto<IncapMonthlyAttendanceDto>>();
				monthlyDetailsReportList.add(monthlyDetailsReport);
				dtoList = new PaginationDto<>(monthlyDetailsReportList, page.getTotalPages(),
						page.getNumber() + NumberConstants.ONE, page.getSize(), page.getTotalElements(),
						page.getTotalElements(), sortDir, ApplicationConstants.SUCCESS,
						ApplicationConstants.MSG_TYPE_S);

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return dtoList;
	}

	private void setHeader(List<String> headList) {
		headList.add(HeaderConstants.EMPLOYEE_ID);
		headList.add(HeaderConstants.NAME);
		headList.add(HeaderConstants.ORGANIZATION);
		headList.add(HeaderConstants.DEPARTMENT);
		headList.add(HeaderConstants.DESIGNATION);
		headList.add(HeaderConstants.TOTAL_DAYS);
		headList.add(HeaderConstants.PRESENT);
		headList.add(HeaderConstants.ABSENT);
	}

	private Page<Employee> getEmployeePage(Long id, String employeeId, String employeeName, String department,
			String designation, int pageno, String sortField, String sortDir) {
		Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(sortField).ascending()
				: Sort.by(sortField).descending();

		Pageable pageable = PageRequest.of(pageno - NumberConstants.ONE, NumberConstants.TEN, sort);

		Specification<Employee> isDeletedSpec = generalSpecification.isDeletedSpecification();
		Specification<Employee> idSpec = generalSpecification.longSpecification(id, ApplicationConstants.ID);
		Specification<Employee> empIdSpec = generalSpecification.stringSpecification(employeeId,
				EmployeeConstants.EMPID);
		Specification<Employee> empNameSpec = generalSpecification.stringSpecification(employeeName,
				ApplicationConstants.NAME);
		Specification<Employee> deptSpec = generalSpecification.foreignKeyStringSpecification(department,
				EmployeeConstants.DEPARTMENT, ApplicationConstants.NAME);
		Specification<Employee> degSpec = generalSpecification.foreignKeyStringSpecification(designation,
				EmployeeConstants.DESIGNATION, ApplicationConstants.NAME);

		Page<Employee> page = employeeRepository.findAll(
				isDeletedSpec.and(idSpec).and(empIdSpec).and(empNameSpec).and(deptSpec).and(degSpec), pageable);
		return page;
	}
}
