package com.eikona.mata.springbatch.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.eikona.mata.entity.EmployeeShiftDailyAssociation;
public class PersonAccessLevelMapper implements RowMapper<EmployeeShiftDailyAssociation> {

	@Override
	public EmployeeShiftDailyAssociation mapRow(ResultSet resultSet, int i) throws SQLException {
		EmployeeShiftDailyAssociation empShift= new EmployeeShiftDailyAssociation();
		empShift.setId(resultSet.getLong("id"));
		return empShift;
	}

}
