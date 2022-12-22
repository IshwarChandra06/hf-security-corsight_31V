package com.eikona.mata.springbatch.job;

import javax.sql.DataSource;

import org.apache.http.conn.ConnectTimeoutException;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DeadlockLoserDataAccessException;

import com.eikona.mata.entity.EmployeeShiftDailyAssociation;
import com.eikona.mata.springbatch.mapper.PersonAccessLevelMapper;
import com.eikona.mata.springbatch.processor.PersonAccessLevelProcessor;
import com.eikona.mata.springbatch.writer.PersonAccessLevelWriter;

@Configuration
public class PersonAccessLevel {
	private JobBuilderFactory jobBuilderFactory;
    private StepBuilderFactory stepBuilderFactory;
    private PersonAccessLevelProcessor personAccessLevelProcessor;
    private DataSource dataSource;
    private PersonAccessLevelWriter personAccessLevelWriter;

    @Autowired
    public PersonAccessLevel(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, PersonAccessLevelProcessor personAccessLevelProcessor, DataSource dataSource, PersonAccessLevelWriter personAccessLevelWriter){
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.personAccessLevelProcessor = personAccessLevelProcessor;
        this.dataSource = dataSource;
        this.personAccessLevelWriter = personAccessLevelWriter;
    }

    @Qualifier(value = "personAccessLevel")
    @Bean
    public Job personAccessLevelJob() throws Exception {
        return this.jobBuilderFactory.get("personAccessLevel")
                .start(step1PersonAccessLevel())
                .build();
    }
    
    @Bean
    public Step step1PersonAccessLevel() throws Exception {
        return this.stepBuilderFactory.get("step1PersonAccessLevel")
    		.<EmployeeShiftDailyAssociation, EmployeeShiftDailyAssociation>chunk(100)
            .reader(employeeShiftDBReader())
            .processor(personAccessLevelProcessor)
            .writer(personAccessLevelWriter)
            .faultTolerant()
            .retryLimit(30)
            .retry(ConnectTimeoutException.class)
            .retry(DeadlockLoserDataAccessException.class)
            .build();
    }
 
    @Bean
    public ItemStreamReader<EmployeeShiftDailyAssociation> employeeShiftDBReader() {
        JdbcCursorItemReader<EmployeeShiftDailyAssociation> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql("select * from employee_shift_daily_association es where is_sync=false and shift_id is not null  and date=CURRENT_DATE");
        reader.setRowMapper(new PersonAccessLevelMapper());
        return reader;
    }
}
