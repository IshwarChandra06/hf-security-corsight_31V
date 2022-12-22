package com.eikona.mata.springbatch.writer;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.eikona.mata.entity.AccessLevel;
import com.eikona.mata.entity.EmployeeShiftDailyAssociation;
import com.eikona.mata.repository.AccessLevelRepository;
import com.eikona.mata.repository.EmployeeShiftDailyAssociationRepository;
import com.eikona.mata.util.BioSecurityServerUtil;
@Component
public class PersonAccessLevelWriter implements ItemWriter<EmployeeShiftDailyAssociation> {
	
	private EmployeeShiftDailyAssociationRepository employeeShiftDailyAssociationRepository;
	
	private BioSecurityServerUtil personAccessLevelUtil;
	
	private AccessLevelRepository accessLevelRepository;
	
	@Autowired
	public PersonAccessLevelWriter(BioSecurityServerUtil personAccessLevelUtil,EmployeeShiftDailyAssociationRepository employeeShiftDailyAssociationRepository,AccessLevelRepository accessLevelRepository) {
		super();
		this.personAccessLevelUtil = personAccessLevelUtil;
		this.employeeShiftDailyAssociationRepository=employeeShiftDailyAssociationRepository;
		this.accessLevelRepository=accessLevelRepository;
	}
	
	@Override
	public void write(List<? extends EmployeeShiftDailyAssociation> employeeRosterList) throws Exception {
		List<EmployeeShiftDailyAssociation> empShiftList=new ArrayList<EmployeeShiftDailyAssociation>();
		List<AccessLevel> accessLevelList= (List<AccessLevel>) accessLevelRepository.findAll();
		for(EmployeeShiftDailyAssociation empShift : employeeRosterList) {
			EmployeeShiftDailyAssociation empShiftObj= employeeShiftDailyAssociationRepository.findById(empShift.getId()).get();
			String shiftAssignOfEmployee = empShiftObj.getShift().getName();
			String accessLevelIds="";
			for(AccessLevel accessLevel:accessLevelList) {
				System.out.println(accessLevel.getName().split("\\s+")[0]);
				if(accessLevel.getName().split("\\s+")[0].equalsIgnoreCase(shiftAssignOfEmployee)  && !("WO".equalsIgnoreCase(shiftAssignOfEmployee))) {
					if(accessLevelIds.isEmpty())
						accessLevelIds=accessLevel.getAccessId();
					else
						accessLevelIds+=","+accessLevel.getAccessId();
				}
			}
			
			String message=personAccessLevelUtil.addAccessLevelToPerson(accessLevelIds,empShiftObj.getEmployee().getEmpId());
			
			if("Success".equalsIgnoreCase(message)) {
				empShiftObj.setSync(true);
				empShiftList.add(empShiftObj);
			}
		}
		//employeeShiftDailyAssociationRepository.saveAll(empShiftList);
	}
}
