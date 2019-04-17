

import java.io.File
import org.scalatest.FunSuite
import org.renault.dll.jsongenerator.ConfigGen


class ConfigGenTest extends FunSuite {

  test("Json Generator"){
    val configGen:ConfigGen.type = ConfigGen.apply



    val args =Array("..\\DLLConfigGen\\artifacts\\DEMO\\PIS_Config.xlsx" //arg 0
      ,"..\\DLLConfigGen\\artifacts\\persister_template.json" //arg 1
      ,"..\\DLLConfigGen\\artifacts\\datastructure_template.json" //arg 2
      ,"..\\DLLConfigGen\\artifacts\\reader_template.json" //arg 3
      ,"db_qa_irn_66929_pis"  //arg 4
      ,"..\\DLLConfigGen\\DDLParserConfig\\out" //arg 5
      ,"fixed" //arg 6
      ,"Y" //arg 7
      ,"Y") //arg 8

    //configGen.main(args)

    assert(args.length == 9,"all 9 arguments are passed")
val outfolder = "D:\\DLLConfigGen\\DDLParserConfig\\out"
    assert(new java.io.File(outfolder).exists == true)


    configGen.main(args)




  }



}
