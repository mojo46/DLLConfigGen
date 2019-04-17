package org.renault.dll.jsongenerator

import org.apache.poi.ss.usermodel._
import java.io.File

import org.apache.commons.io.FileUtils
import org.json4s.JsonAST.JValue

import scala.collection.mutable.StringBuilder
import collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.io.Source

import org.json4s._
import org.json4s.jackson.Serialization.write
import org.json4s.jackson.JsonMethods._


object ConfigGen {

  println("\n\n$$$$$$$$$$$$$$$$$config gen object is called$$$$$$$$$$$$$$$$$$$")

  var globalTblName = ""
  var flag: Boolean = false
  val jsonBuffer = ListBuffer.empty[Tuple2[String, Map[String, String]]]
  val jsonString: StringBuilder = new StringBuilder()
  val context: scala.collection.mutable.Map[String, JValue] = collection.mutable.Map[String, JValue]()

  var dsColNameAndAliases: Map[String, Map[String, String]] = Map()

  def populateDSColsAndAliases(wb: Workbook) = {
    println("####### Inside populateDSColsAndAliases function ######")
    val dataStructureSheet: Sheet = wb.getSheet("DataStructure")

    dsColNameAndAliases = dataStructureSheet.rowIterator().filter(x => x.getRowNum != 0).map { eachRow =>
      (eachRow.getCell(0).getStringCellValue, eachRow.getCell(1).getStringCellValue, eachRow.getCell(4).getStringCellValue)
    }.toList.groupBy(_._1).mapValues(_.map(r => (r._2, r._3)).toMap)

    dataStructureSheet.rowIterator().filter(x=>x.getRowNum != 0 ).map(e=>println("datastructure sheet:"+e))

  }

  /*def filterByLookup(pair: (String, String)): Boolean = {
    if (pair._1.equalsIgnoreCase("name"))
      if (dsColNameAndAliases.get(globalTblName).get.containsKey(pair._2)) {
        flag = true; true
      }
      else {
        flag = false;
        false
      }
    else {
      if (flag) true else false
    }
  }*/

  def apply: ConfigGen.type = this

  def transformByLookup(k: String, v: String): String = {

    println("####### Inside transformByLookup function ######")

    if (k.equalsIgnoreCase("name")) {
      if (dsColNameAndAliases.get(globalTblName).get.get(v).get.isEmpty) {
        v
      } else {
        dsColNameAndAliases.get(globalTblName).get.get(v).get
      }
    }
    else v

  }

  def getTableMap(wb: Workbook, sheetName: String, addTechColumns: Boolean = false, appendComputeColumns: Boolean = false, filterFunc: (((String, String)) => Boolean) = (x: (String, String)) => {
    true
  }, transformFunc: (String, String) => String = (x: String, y: String) => y, lookupNeeded: Boolean = false): Map[String, List[Map[String, String]]] = {

    jsonBuffer.clear()

    val desiredSheet: Sheet = wb.getSheet(sheetName)
    val techColsSheet: Sheet = wb.getSheet("TechnicalColumns")
    val computeColSheet: Sheet = wb.getSheet("ComputedFields")

    if (!desiredSheet.getRow(0).getCell(0).getStringCellValue.contains("TableName")) {
      throw new java.lang.RuntimeException("Illegal Sheet - Header Missing")
    }

    val hdrList: Array[String] = desiredSheet.getRow(0).map(col => col.getStringCellValue).toArray
    val techHdrList: Array[String] = techColsSheet.getRow(0).map(col => col.getStringCellValue).toArray
    val comuteHdrList: Array[String] = computeColSheet.getRow(0).map(col => col.getStringCellValue).toArray


    var techColumnsList: List[Map[String, String]] = Nil
    var computeColBuffer = ListBuffer.empty[Tuple2[String, Map[String, String]]]

    if (addTechColumns) {
      techColumnsList = techColsSheet.rowIterator().filter(x => x.getRowNum != 0).map { eachRow =>

        val tempMap: Map[String, String] = eachRow.cellIterator().map { col =>
          val idx = col.getColumnIndex
          val key = techHdrList(idx)
          val elem = col.getStringCellValue

          (key, elem)
        } toMap

        tempMap.filter(x => !x._2.isEmpty)
      }.toList
    }
    else {
      techColumnsList = List.empty
    }

    if (appendComputeColumns) {

      computeColSheet.rowIterator().foreach {
        rw =>

          val tbl = rw.getCell(0).getStringCellValue

          if (rw.getRowNum != 0) {

            val pairMap: Map[String, String] = rw.cellIterator().filter(col => col.getColumnIndex != 0).map { col =>
              val idx = col.getColumnIndex
              val key = comuteHdrList(idx)
              //The below code did not work for numeric cell data
              // val elem = col.getStringCellValue
              //Feel free to extend the matches to boolean also....
              val elem: String = col.getCellType match {
                case Cell.CELL_TYPE_NUMERIC => col.getNumericCellValue.toInt.toString
                case _ => col.getStringCellValue
              }

              (key, elem)
            } toMap


            computeColBuffer += ((tbl, pairMap.filter(x => !x._2.isEmpty).filter(x => x._1.matches("name|fieldType|comment"))))
          }
      }

    }

    desiredSheet.rowIterator().foreach {
      rw =>

        globalTblName = rw.getCell(0).getStringCellValue

        if (rw.getRowNum != 0 && (!sheetName.matches("Persister") || (lookupNeeded == true && dsColNameAndAliases.get(globalTblName).get.keySet.contains(rw.getCell(1).getStringCellValue)) || sheetName.matches("Persister"))) {

          val pairMap: Map[String, String] = rw.cellIterator().filter(col => col.getColumnIndex != 0).map { col =>
            val idx = col.getColumnIndex
            val key = hdrList(idx)
            //The below code did not work for numeric cell data
            // val elem = col.getStringCellValue
            //Feel free to extend the matches to boolean also....
            val elem: String = col.getCellType match {
              case Cell.CELL_TYPE_NUMERIC => col.getNumericCellValue.toInt.toString
              case _ => col.getStringCellValue
            }
            (key, elem)
          } toMap

          jsonBuffer += ((globalTblName, pairMap.filter(x => !x._2.isEmpty).transform(transformFunc)))
        }
    }

    if (appendComputeColumns ) {
      jsonBuffer ++= computeColBuffer.filter( !_._2.isEmpty )
    }

    jsonBuffer.groupBy(y => y._1).mapValues(_.map(r => (r._2)).toList ++ techColumnsList)


  }

  def dumpJsons(outputPath: String, tblName: String, content: StringBuilder, readerFormat: String) = {

    println("####### Inside dumpJsons function ######")


    val file = new File(outputPath + "//" + tblName.toLowerCase + "//" + "reader_" + readerFormat + ".json")
    file.getParentFile.mkdirs();
    //val writer = new FileWriter(file)
    import java.io.FileOutputStream
    import java.io.OutputStreamWriter
    val fileStream = new FileOutputStream(file)
    val writer = new OutputStreamWriter(fileStream, "UTF8")
    val jParsed: JValue = parse(jsonString.toString())
    //writer.write(pretty(jParsed))
    writer.write(pretty(jParsed removeField { case JField(_,JArray(List(JObject(List())))) => true case _ => false }))

    writer.close()
  }

  def dumpJsons(outputPath: String, jsonFileName: String) = {

    context foreach { entry =>
      val file = new File(outputPath + "//" + entry._1.toLowerCase + "//" + jsonFileName + ".json")
      file.getParentFile.mkdirs();
      //val writer = new FileWriter(file)
      //writer.write(pretty(entry._2))
      import java.io.FileOutputStream
      import java.io.OutputStreamWriter
      val fileStream = new FileOutputStream(file)
      val writer = new OutputStreamWriter(fileStream, "UTF8")
      writer.write(pretty(entry._2 removeField { case JField(_,JArray(List(JObject(List())))) => true case _ => false }))

      writer.close()
    }

    context.clear();
  }

  def printJsons() = {
    context.foreach(pair => println(s"JSON for Table ${pair._1}\n" + pretty(pair._2)));
    context.clear
  }

  def updateContext(tbl: String, masterJson: JValue, updateValue: JValue, updateKey: String) = {

    println("####### Inside updateContext function ######")


    if (!context.isDefinedAt(tbl)) {
      val finalJson: JValue = masterJson mapField {
        case (`updateKey`, JString(str)) => (s"${updateKey}", updateValue)
        case other => other
      }

      context.put(tbl,finalJson)
    } else {
      val finalJson: JValue = context.get(tbl).get mapField {
        case (`updateKey`, JString(str)) => (s"${updateKey}", updateValue)
        case other => other
      }

      context.put(tbl,finalJson)
    }

  }

  def generateReaders(jsonTemplateFile: String, outputPath: String, finalList: Map[String, List[Map[String, String]]], readerFormat: String): Unit = {

    println("####### Inside generateReaders function ######")

    finalList.foreach { eachTable =>
      jsonString.clear();
      Source.fromFile(new File(jsonTemplateFile)).getLines().filter(x => !x.matches(" +#.*,")).foreach { line => val updatedline = line.replace("upper(%tablename%)", eachTable._1.toUpperCase).replace("lower(%tablename%)", eachTable._1.toLowerCase).replace("upper(%tablename%)", eachTable._1.toUpperCase).replace("%tablename%", eachTable._1.toLowerCase); jsonString.append(updatedline) }
      dumpJsons(outputPath, eachTable._1, jsonString, readerFormat)
    }

  }

  def generateJsonList(jsonTemplateFile: String, finalList: Map[String, List[Map[String, String]]], updateKey: String, dbName: String) = {

    finalList.foreach { eachTable =>

      jsonString.clear()
      Source.fromFile(new File(jsonTemplateFile)).getLines().filter(x => !x.matches(" +#.*,")).foreach { line => val updatedline = line.replace("upper(%tablename%)", eachTable._1.toUpperCase).replace("lower(%tablename%)", eachTable._1.toLowerCase).replace("%tablename%", eachTable._1.toLowerCase).replace("%dbname%", dbName); jsonString.append(updatedline) }

      val jParsed: JValue = parse(jsonString.toString())
      implicit val formats = DefaultFormats
      val jColumns: JValue = parse(write(eachTable._2)).transform {
        case obj: JObject => obj.mapField {
          case (x, JString("true")) => (x, JBool(true))
          case (x, JString("false")) => (x, JBool(false))
          case (x, JString(y)) if (scala.util.Try(y.toInt).isSuccess) => (x, JInt(y.toInt))
          case other => other
        }
      }

      updateContext(eachTable._1, jParsed, jColumns, updateKey)

    }

  }


  def main(args: Array[String]): Unit = {


    println("$$$$$$$$$$$$$$$$$ inside main function $$$$$$$$$$$$$$$$$$$")


    if (args.length != 9) {
      Console.println("Insufficient Arguments - Usage: ClassName <excel - input path> <persister json template> <datastructure json template> <database name - eg: db_raw_irn_66929_pis> <output path> <reader_format> <Need datastructure (Y/N)> <Do you have compute cols? (Y/N)")
      System.exit(1)
    }

    val excelPath: String = args(0)
    val persisterTemplate: String = args(1)
    val datastructTemplate: String = args(2)
    val readerTemplate: String = args(3)
    val dbName = args(4).toLowerCase
    val outputPath = args(5)
    val readerFormat = args(6)
    val needDataStructure = args(7).toLowerCase() match {
      case "y" => 'y'
      case "n" => 'n'
      case _ => throw new RuntimeException("datastructure input  y/n")
    }
    val doYouHaveComputeCols: Boolean = args(8).toLowerCase() match {
      case "y" => true
      case "n" => false
      case _ => throw new RuntimeException("Last input should be y/n")
    }


    val deletePath = new File(outputPath)

    FileUtils.deleteDirectory(deletePath)

    val wb: Workbook = WorkbookFactory.create(new File(excelPath))

    if (needDataStructure == 'y') { populateDSColsAndAliases(wb) }


    val persister: Map[String, List[Map[String, String]]] = if (needDataStructure == 'y') {
      println("$$$$$$$$$$$$$$$$$ inside confgen persiter $$$$$$$$$$$$$$$$$$$")
      getTableMap(wb, "Persister", addTechColumns = true, appendComputeColumns = doYouHaveComputeCols, transformFunc = transformByLookup, lookupNeeded = true)
    }
    else {
      getTableMap(wb, "Persister", addTechColumns = true, appendComputeColumns = doYouHaveComputeCols, lookupNeeded = false)
    }

    generateJsonList(persisterTemplate, persister, "fields", dbName)

    dumpJsons(outputPath, s"persister_$readerFormat")
//    printJsons()

    if (needDataStructure == 'y') {
      val dataStructure: Map[String, List[Map[String, String]]] = getTableMap(wb, "DataStructure", lookupNeeded = true)
      generateJsonList(datastructTemplate, dataStructure, "fields", dbName)

      val computedFields: Map[String, List[Map[String, String]]] = getTableMap(wb, "ComputedFields")

      generateJsonList(datastructTemplate, computedFields, "computedFields", dbName)

      dumpJsons(outputPath, "datastructure")
//      printJsons()
    }

    if (readerFormat.equalsIgnoreCase("fixed")) {

      val reader: Map[String, List[Map[String, String]]] = getTableMap(wb, "Fixed")

      generateJsonList(readerTemplate, reader, "fields", dbName)

      dumpJsons(outputPath, s"reader_fixed")
//      printJsons()

    }
    else {
      generateReaders(readerTemplate, outputPath, persister, readerFormat)
      dumpJsons(outputPath, s"reader_$readerFormat")
//      printJsons()
    }

  }

}