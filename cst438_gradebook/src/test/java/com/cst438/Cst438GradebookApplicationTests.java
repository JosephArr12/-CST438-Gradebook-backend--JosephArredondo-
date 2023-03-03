package com.cst438;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Optional;

import com.cst438.controllers.GradeBookController;
import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentDTO;
import com.cst438.domain.AssignmentGrade;
import com.cst438.domain.AssignmentGradeRepository;
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
	public static final String TEST_ASSIGNMENT_NAME = "TestA1";
	public static final String TEST_ASSIGNMENT_CHANGE_NAME= "TestA100";
	public static final int TEST_ASSIGNMENT_ID = 100;

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
	public void createAssignment() throws Exception {

		MockHttpServletResponse response;
		
		//I add the mock data that my api checks
		Course course = new Course();
		course.setCourse_id(TEST_COURSE_ID);
		course.setSemester(TEST_SEMESTER);
		course.setYear(TEST_YEAR);
		course.setInstructor(TEST_INSTRUCTOR_EMAIL);
		
		courseRepository.save(course);

//		I create a DTO to send the data to the server.	
		Date dueDate = Date.valueOf(TEST_DUE_DATE);
		
     	AssignmentDTO a = new AssignmentDTO();
     	a.dueDate = dueDate.toString();
     	a.assignmentName = TEST_ASSIGNMENT_NAME;

		// end of mock data
		//http post request using the data
		response = mvc.perform(MockMvcRequestBuilders.post("/createAssignment")
				.accept(MediaType.APPLICATION_JSON).content(asJsonString(a))
				.contentType(MediaType.APPLICATION_JSON)).andReturn()
				.getResponse();
		
     	AssignmentDTO result = fromJsonString(response.getContentAsString(), AssignmentDTO.class);
//     	
//     	//I check the status, name, and due date to see if the DTO returned matches what was sent to the server
		assertEquals(200, response.getStatus());
		assertEquals(TEST_ASSIGNMENT_NAME, result.assignmentName);
		assertEquals(dueDate.toString(), result.dueDate);
		
		//check if the assignment was added by the controller
		verify(assignmentRepository, times(1)).save(any());
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
		assignment.setName(TEST_ASSIGNMENT_NAME);
		assignment.setNeedsGrading(0);

		assignmentRepository.save(assignment);

		// given -- stubs for database repositories that return test data
		given(assignmentRepository.findById(1)).willReturn(Optional.of(assignment));

		// end of mock data
		
     	AssignmentDTO a = new AssignmentDTO();
     	a.assignmentName=TEST_ASSIGNMENT_CHANGE_NAME;


		// send updates to server
		response = mvc
				.perform(MockMvcRequestBuilders.post("/changeAssignment/1").accept(MediaType.APPLICATION_JSON)
						.content(asJsonString(a)).contentType(MediaType.APPLICATION_JSON))
				.andReturn().getResponse();

		// verify that return status = OK (value 200)
		assertEquals(200, response.getStatus());
		
		//verifying assignment saved twice since first created then saved after changing
		
		verify(assignmentRepository, times(2)).save(any());

		//Verify that the assignment name has changed to the name sent
		AssignmentDTO changeResult = fromJsonString(response.getContentAsString(), AssignmentDTO.class);
		assertEquals(TEST_ASSIGNMENT_CHANGE_NAME,changeResult.assignmentName);
	}
	
	@Test
	public void deleteAssignmentNoGrades() throws Exception {
		MockHttpServletResponse response;

		// mock database data

		Course course = new Course();
		course.setCourse_id(TEST_COURSE_ID);
		course.setSemester(TEST_SEMESTER);
		course.setYear(TEST_YEAR);
		course.setInstructor(TEST_INSTRUCTOR_EMAIL);
		course.setEnrollments(new java.util.ArrayList<Enrollment>());
		course.setAssignments(new java.util.ArrayList<Assignment>());
		
		courseRepository.save(course);
		
		Assignment assignment = new Assignment();
		assignment.setCourse(course);
		
		assignment.setId(TEST_ASSIGNMENT_ID);
		
		assignmentRepository.save(assignment);
		
		given(assignmentRepository.findById(TEST_ASSIGNMENT_ID)).willReturn(Optional.of(assignment));

		
		response = mvc.perform(MockMvcRequestBuilders.delete("/deleteAssignment/100").accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();
		assertEquals(200,response.getStatus());
		
		//verify delete was called
		verify(assignmentRepository, times(1)).deleteById(any());
	}
	
	@Test
	public void deleteAssignmentWithGrades() throws Exception {
		MockHttpServletResponse response;

		// mock database data

		Course course = new Course();
		course.setCourse_id(TEST_COURSE_ID);
		course.setSemester(TEST_SEMESTER);
		course.setYear(TEST_YEAR);
		course.setInstructor(TEST_INSTRUCTOR_EMAIL);
		course.setEnrollments(new java.util.ArrayList<Enrollment>());
		course.setAssignments(new java.util.ArrayList<Assignment>());

		Enrollment enrollment = new Enrollment();
		enrollment.setCourse(course);
		course.getEnrollments().add(enrollment);
		enrollment.setId(TEST_COURSE_ID);
		enrollment.setStudentEmail(TEST_STUDENT_EMAIL);
		enrollment.setStudentName(TEST_STUDENT_NAME);

		Assignment assignment = new Assignment();
		assignment.setCourse(course);
		course.getAssignments().add(assignment);
		// set dueDate to 1 week before now.
		assignment.setDueDate(new java.sql.Date(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000));
		assignment.setId(TEST_ASSIGNMENT_ID);
		assignment.setName("Assignment 1");
		assignment.setNeedsGrading(1);

		AssignmentGrade ag = new AssignmentGrade();
		ag.setAssignment(assignment);
		ag.setId(1);
		ag.setScore("80");
		ag.setStudentEnrollment(enrollment);
		assignmentGradeRepository.save(ag);
		
		given(assignmentGradeRepository.findByAssignmentIdAndStudentEmail(TEST_ASSIGNMENT_ID, TEST_STUDENT_EMAIL)).willReturn(ag);
		
		assignmentRepository.save(assignment);
		
		given(assignmentRepository.findById(TEST_ASSIGNMENT_ID)).willReturn(Optional.of(assignment));

		//http delete request using the id of the assignment I created
		response = mvc.perform(MockMvcRequestBuilders.delete("/deleteAssignment/100").accept(MediaType.APPLICATION_JSON)).andReturn().getResponse();
		assertEquals(200,response.getStatus());
		
		// Delete should not be called
		verify(assignmentRepository, times(0)).deleteById(any());
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