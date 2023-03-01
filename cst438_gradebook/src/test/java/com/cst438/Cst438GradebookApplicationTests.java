package com.cst438;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Optional;

import com.cst438.controllers.GradeBookController;
import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentGrade;
import com.cst438.domain.AssignmentGradeRepository;
import com.cst438.domain.AssignmentListDTO.AssignmentDTO;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Course;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.GradebookDTO;
import com.cst438.services.RegistrationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.test.context.ContextConfiguration;

/* 
 * Example of using Junit with Mockito for mock objects
 *  the database repositories are mocked with test data.
 *  
 * Mockmvc is used to test a simulated REST call to the RestController
 * 
 * the http response and repository is verified.
 * 
 *   Note: This tests uses Junit 5.
 *  ContextConfiguration identifies the controller class to be tested
 *  addFilters=false turns off security.  (I could not get security to work in test environment.)
 *  WebMvcTest is needed for test environment to create Repository classes.
 */
@ContextConfiguration(classes = { GradeBookController.class })
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest
public class Cst438GradebookApplicationTests {

	static final String URL = "http://localhost:8081";
	public static final int TEST_COURSE_ID = 999001;
	public static final String TEST_STUDENT_EMAIL = "test@csumb.edu";
	public static final String TEST_STUDENT_NAME = "test";
	public static final String TEST_INSTRUCTOR_EMAIL = "dwisneski@csumb.edu";
	public static final int TEST_YEAR = 2023;
	public static final String TEST_SEMESTER = "Spring";
	public static final String TEST_DUE_DATE = "2023-03-03";

	@MockBean
	AssignmentRepository assignmentRepository;

	@MockBean
	AssignmentGradeRepository assignmentGradeRepository;

	@MockBean
	CourseRepository courseRepository; // must have this to keep Spring test happy

	@MockBean
	RegistrationService registrationService; // must have this to keep Spring test happy

	@Autowired
	private MockMvc mvc;

	@Test
	public void updateAssignmentGrade() throws Exception {

		MockHttpServletResponse response;

		// mock database data

		Course course = new Course();
		course.setCourse_id(TEST_COURSE_ID);
		course.setSemester(TEST_SEMESTER);
		course.setYear(TEST_YEAR);
		course.setInstructor(TEST_INSTRUCTOR_EMAIL);
		
		courseRepository.save(course);
		
		Assignment assignment = new Assignment();
		assignment.setCourse(course);
		
		Date dueDate = Date.valueOf(TEST_DUE_DATE);
		
		assignment.setDueDate(dueDate);
		assignment.setId(1);
		assignment.setName("Assignment 1");
		assignment.setNeedsGrading(0);

		
		assignmentRepository.save(assignment);

		// given -- stubs for database repositories that return test data
		given(assignmentRepository.findById(1)).willReturn(Optional.of(assignment));

		// end of mock data

//		// then do an http post request to create an assignment
		response = mvc.perform(MockMvcRequestBuilders.post("/createAssignment/Assignment1/2023-03-03").accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();
		System.out.print(response.getStatus());
		
		assertEquals(200, response.getStatus());
//
//		// verify that a save was called once on the repository because the assignment is being added
		verify(assignmentRepository, times(1)).save(any());
//
		// verify that returned data has non zero primary key
		AssignmentDTO result = fromJsonString(response.getContentAsString(), AssignmentDTO.class);
		// assignment id is 1
		assertEquals(1, result.assignmentId);
		// there is one student list
		assertEquals("Assignment 1", result.assignmentName);
		assertEquals(TEST_COURSE_ID, result.courseId);
		assertEquals(dueDate, result.dueDate);
	}
	
	@Test
	public void changeAssignment() throws Exception {
		MockHttpServletResponse response;
		
		Course course = new Course();
		course.setCourse_id(TEST_COURSE_ID);
		course.setSemester(TEST_SEMESTER);
		course.setYear(TEST_YEAR);
		course.setInstructor(TEST_INSTRUCTOR_EMAIL);
		
		courseRepository.save(course);
		
		Assignment assignment = new Assignment();
		assignment.setCourse(course);
		
		Date dueDate = Date.valueOf(TEST_DUE_DATE);
		
		assignment.setDueDate(dueDate);
		assignment.setId(1);
		assignment.setName("Assignment 1");
		assignment.setNeedsGrading(0);

		
		assignmentRepository.save(assignment);

		// given -- stubs for database repositories that return test data
		given(assignmentRepository.findById(1)).willReturn(Optional.of(assignment));

		// end of mock data

//		// then do an http post request to create an assignment
		response = mvc.perform(MockMvcRequestBuilders.post("/createAssignment/Assignment1/2023-03-03").accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();
		System.out.print(response.getStatus());
		
		assertEquals(200, response.getStatus());
		
		// verify that returned data has non zero primary key
		AssignmentDTO result = fromJsonString(response.getContentAsString(), AssignmentDTO.class);
		// assignment id is 1
		assertEquals(1, result.assignmentId);
		// there is one student list
		assertEquals("Assignment 1", result.assignmentName);
		assertEquals(TEST_COURSE_ID, result.courseId);
		assertEquals(dueDate, result.dueDate);

		// change name
		result.assignmentName = "NewAssignment";

		// send updates to server
		response = mvc
				.perform(MockMvcRequestBuilders.put("/changeAssignment/").accept(MediaType.APPLICATION_JSON)
						.content(asJsonString(result)).contentType(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// verify that return status = OK (value 200)
		assertEquals(200, response.getStatus());

		AssignmentDTO changeResult = fromJsonString(response.getContentAsString(), AssignmentDTO.class);
		assertEquals("NewAssignment",changeResult.assignmentName);
	}
	
	@Test
	public void deleteAssignment() throws Exception {
		MockHttpServletResponse response;

		// mock database data

		Course course = new Course();
		course.setCourse_id(TEST_COURSE_ID);
		course.setSemester(TEST_SEMESTER);
		course.setYear(TEST_YEAR);
		course.setInstructor(TEST_INSTRUCTOR_EMAIL);
		
		courseRepository.save(course);
		
		Assignment assignment = new Assignment();
		assignment.setCourse(course);
		
		Date dueDate = Date.valueOf(TEST_DUE_DATE);
		
		assignment.setDueDate(dueDate);
		assignment.setId(1);
		assignment.setName("Assignment 1");
		assignment.setNeedsGrading(0);
		
		assignmentRepository.save(assignment);
		
		verify(assignmentRepository, times(1)).save(any());

		
		given(assignmentRepository.findById(1)).willReturn(Optional.of(assignment));

		
		response = mvc.perform(MockMvcRequestBuilders.post("/deleteAssignment/1").accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();
		assertEquals(200,response.getStatus());
		
	}
	


	private static String asJsonString(final Object obj) {
		try {

			return new ObjectMapper().writeValueAsString(obj);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static <T> T fromJsonString(String str, Class<T> valueType) {
		try {
			return new ObjectMapper().readValue(str, valueType);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}