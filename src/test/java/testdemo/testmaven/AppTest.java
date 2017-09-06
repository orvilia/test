package testdemo.testmaven;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.testng.annotations.Test;

public class AppTest 
    
{
	
   @Test
	
   public static void FirstTest(){
	   
	   
	  System.setProperty("webdriver.chrome.driver", "/Users/saurabhdubey/Desktop/chromedriver");
	   
	   WebDriver driver = new ChromeDriver();
	   
	   driver.get("https://www.google.com");
	   
	   driver.quit();
	   
   }
}
