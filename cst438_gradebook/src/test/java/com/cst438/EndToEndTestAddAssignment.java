package com.cst438;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.openqa.selenium.support.ui.Select;

import com.cst438.domain.Assignment;
import com.cst438.domain.AssignmentGrade;
import com.cst438.domain.AssignmentGradeRepository;
import com.cst438.domain.AssignmentRepository;
import com.cst438.domain.Course;
import com.cst438.domain.CourseRepository;
import com.cst438.domain.Enrollment;
import com.cst438.domain.EnrollmentRepository;

/*
 * This example shows how to use selenium testing using the web driver 
 * with Chrome browser.
 * 
 *  - Buttons, input, and anchor elements are located using XPATH expression.
 *  - onClick( ) method is used with buttons and anchor tags.
 *  - Input fields are located and sendKeys( ) method is used to enter test data.
 *  - Spring Boot JPA is used to initialize, verify and reset the database before
 *      and after testing.
 *      
 *  In SpringBootTest environment, the test program may use Spring repositories to 
 *  setup the database for the test and to verify the result.
 */

@SpringBootTest
public class EndToEndTestAddAssignment {

	public static final String CHROME_DRIVER_FILE_LOCATION = "/Users/josepharredondo/Downloads/chromedriver_mac64/chromedriver";
	public static final String URL = "http://localhost:3000";
	public static final String TEST_INSTRUCTOR_EMAIL = "dwisneski@csumb.edu";
	public static final int SLEEP_DURATION = 1000; // 1 second.
	public static final String TEST_ASSIGNMENT_NAME = "Test Assignment";
	public static final String TEST_COURSE_TITLE = "cst363-database";
	public static final String TEST_ASSIGNMENT_DUE_DATE = "04-30-2023";
	public static final String SUCCESS_MESSAGE = "Assignment added!";
	
	public int assignmentId;


	@Autowired
	EnrollmentRepository enrollmentRepository;

	@Autowired
	CourseRepository courseRepository;

	@Autowired
	AssignmentGradeRepository assignnmentGradeRepository;

	@Autowired
	AssignmentRepository assignmentRepository;

	@Test
	public void addCourseTest() throws Exception {


		Iterable<Assignment> assignments = assignmentRepository.findAll();
		
		//looking through all the assignments trying to match name to
		// make sure that that one being added is the only one with the name
		
	  for( Assignment element : assignments ){
	      System.out.println( element );
	      if(element.getName().equals(TEST_ASSIGNMENT_NAME)) {
	    	  Assignment assignment = element;
	    	  assignmentRepository.delete(assignment);
	      }
	  }
		
		// set the driver location and start driver
		//@formatter:off
		// browser	property name 				Java Driver Class
		// edge 	webdriver.edge.driver 		EdgeDriver
		// FireFox 	webdriver.firefox.driver 	FirefoxDriver
		// IE 		webdriver.ie.driver 		InternetExplorerDriver
		//@formatter:on
		
		/*
		 * initialize the WebDriver and get the home page. 
		 */

		System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_FILE_LOCATION);
		WebDriver driver = new ChromeDriver();
		// Puts an Implicit wait for 10 seconds before throwing exception
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

		driver.get(URL);
		Thread.sleep(SLEEP_DURATION);
		

		try {
			//click on button to go to add assignment page
	        WebElement searchButton = driver.findElement(By.id("addAssignment"));
	        Thread.sleep(SLEEP_DURATION);
	        searchButton.click();
	        Thread.sleep(SLEEP_DURATION);
			
			//finding the submit button, text input for the assignment name,
			// the date picker and the drop down for the course
			WebElement addButton = driver.findElement(By.id("submit"));
	        WebElement inputBox = driver.findElement(By.id("name"));
	        WebElement dateInput = driver.findElement(By.id("date"));
	        Select dropdownCourse = new Select(driver.findElement(By.name("course")));
     
	        //enters assignment name to input box
	        inputBox.sendKeys(TEST_ASSIGNMENT_NAME);
	        Thread.sleep(SLEEP_DURATION);
	        
	        //select the course from the drop down
	        dropdownCourse.selectByVisibleText(TEST_COURSE_TITLE);
	        Thread.sleep(SLEEP_DURATION);
	        
	        //selects the date
	        dateInput.sendKeys(TEST_ASSIGNMENT_DUE_DATE);
	        Thread.sleep(SLEEP_DURATION);
	        
	        //clicks on the submit button
	        addButton.click();
	        Thread.sleep(SLEEP_DURATION);
	        
	        //front end has alert showing success
	        String message = driver.switchTo().alert().getText();
	        Thread.sleep(SLEEP_DURATION);
	        driver.switchTo().alert().accept();
	        
	        //assert that the message is what is expected
	        
	        assertEquals(SUCCESS_MESSAGE,message);
	        
	        //create an empty assignment to be set to the one just created
			Assignment assignment = null;
	        
			//reassign assignments
	        assignments = assignmentRepository.findAll();
	        
	        //look for the assignment through the list
	  	  for( Assignment element : assignments ){
		      System.out.println( element );
		      if(element.getName().equals(TEST_ASSIGNMENT_NAME)) {
		    	  System.out.println("Assignment found!");
		    	  //set the assignment id to delete it at the end of the test;
		    	  assignmentId = element.getId();
		    	  //set assignment to look at it's values
		    	  assignment = element;
		    	  //assert statements for each of the values that are chosen
		 	  	  assertEquals(TEST_ASSIGNMENT_NAME, assignment.getName());
		 	  	  //due date format changed when inserted into mySQL 
			  	  assertEquals("2023-04-30", assignment.getDueDate().toString());
			  	  assertEquals(TEST_COURSE_TITLE, assignment.getCourse().getTitle());
		      }
		  }

		} catch (Exception ex) {
			throw ex;
		} finally {
			/*
			 *  clean up database so the test is repeatable.
			 *  
			 */
			Assignment assignment = null;
			
			assignment = assignmentRepository.findById(assignmentId).orElse(null);
			if (assignment != null) {
				assignmentRepository.delete(assignment);
			}

			driver.quit();
		}

	}
}
