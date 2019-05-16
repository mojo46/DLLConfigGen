import java.io.File

import org.scalatest.FunSuite
import org.renault.dll.ddlparser.Driver
import org.renault.dll.ddlparser.Driver.{database, parser}

class DriverTest extends FunSuite {

  test("parse complete ddl to excel file") {

    val driver: Driver.type =  Driver.apply
    /* the below files are available in Test resources */
    //String argv[] = {"D://Public//z025116//DDL_PARSER//postgres_create_tables.ddl"};

    val DLL_txt_file  = "..\\DLLConfigGen\\artifacts\\DEMO\\paris_demo_dll.txt" //argv(0)
    val Properties_File  = "..\\DLLConfigGen\\artifacts\\DEMO\\parser.properties" //argv(1)
    val Excel_File       = "..\\DLLConfigGen\\artifacts\\DEMO\\PIS_Config.xlsx" //argv(2)
    val Option 		 = "Y" //argv(3)

    val argv = Array(DLL_txt_file, Properties_File, Excel_File, Option)
    driver.main(argv)

    val filename = "..\\DLLConfigGen\\artifacts\\DEMO\\sim_dll.txt"
    val file = new File(filename)

     // assert(database = parser.parse(file))

    assert(new File(Excel_File).exists === true)

  }
}