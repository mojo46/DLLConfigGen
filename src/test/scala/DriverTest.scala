import java.io.File
import org.scalatest.FunSuite
import org.renault.dll.ddlparser.Driver

class DriverTest extends FunSuite {

  test("parse complete ddl to excel file") {

    val driver: Driver.type =  Driver.apply
    /* the below files are available in Test resources */
    //String argv[] = {"D://Public//z025116//DDL_PARSER//postgres_create_tables.ddl"};
    val argv = Array("..\\DLLConfigGen\\artifacts\\DEMO\\sim_dll.txt", "..\\DLLConfigGen\\artifacts\\DEMO\\parser.properties", "..\\DLLConfigGen\\artifacts\\DEMO\\PIS_Config.xlsx", "Y")
    driver.main(argv)
    assert(new File("..\\DLLConfigGen\\artifacts\\DEMO\\PIS_Config.xlsx").exists === true)

  }
}