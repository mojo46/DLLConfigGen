

import java.io.File
import org.scalatest.FunSuite
import org.renault.dll.jsongenerator.ConfigGen


class ConfigGenTest extends FunSuite {

  test("Json Generator") {
    val configGen: ConfigGen.type = ConfigGen.apply

    val configPath = "..\\DLLConfigGen\\artifacts\\DEMO\\PIS_Config.xlsx"
    val persister_Json_Template_Path = "..\\DLLConfigGen\\artifacts\\persister_template.json"
    val dataStructure_Json_Template_Path = "..\\DLLConfigGen\\artifacts\\datastructure_template.json"
    val reader_Json_Template_Path = "..\\DLLConfigGen\\artifacts\\reader_template.json"
    val db_Name = "db_qa_irn_66929_pis"
    val output_Folder_Path = "..\\DLLConfigGen\\DDLParserConfig\\out"
    val reader_format = "fixed" //arg 6
    val Need_datastructure = "Y"
    val compute_cols = "Y"

    val args = Array(configPath //arg 0
      , persister_Json_Template_Path //arg 1
      , dataStructure_Json_Template_Path //arg 2
      , reader_Json_Template_Path //arg 3
      , db_Name //arg 4
      , output_Folder_Path //arg 5
      , reader_format //arg 6
      , Need_datastructure //arg 7
      , compute_cols) //arg 8

    //configGen.main(args)

    assert(new File(configPath).exists(), "config path exits")

    assert(configGen.main(args) != null)

  }

}
