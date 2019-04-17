package org.renault.dll.ddlparser

import org.renault.dll.ddlparser.lib._
import org.renault.dll.ddlparser.util._

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.PrintStream
import java.util.{ArrayList, Properties}

import org.renault.dll.ddlparser.lib.Parser.Database
import org.renault.dll.ddlparser.lib.Parser.printError

object Driver {

  var parser: Parser.type = _
  var database: Database = _

  def apply: Driver.type = this

  protected def printUsage(stream: PrintStream): Unit = {
    stream.println("Usage: <JAR_FILE_NAME.jar> input_file_name configuration.properties output_excel_sheet.name <fixed reader sheet required - Y/N>")
    stream.println()
  }

  def main(argv: Array[String]): Unit = { // Figure out if the arguments are valid .  .  .  .
    if (argv.length != 4) {
      printUsage(System.out)
      System.exit(1)
    }
    // Process the arguments .  .  .  .
    val filename = argv(0)
    val file = new File(filename)
    if (!file.exists) printError(-1, "The file \"" + filename + "\" does not exist.")
    if (!file.isFile) printError(-2, "File could not be found at \"" + file + "\"")
    if (!file.canRead) printError(-3, "Unable to read file \"" + filename + "\".")

    val conf = new Properties
    try
      conf.load(new FileInputStream(argv(1)))
    catch {
      case e: IOException =>
        e.printStackTrace()
        System.exit(1)
    }
    // Now parse the file .  .  .  .
    parser = Parser
    //    parser.setDebugLevel();
    try
      database = parser.parse(file)

    catch {
      case e: IOException =>
        e.printStackTrace()
        System.exit(1)
    }
    val util = ExcelUtils()

    util.createSheet("Persister")
    util.writeSheetHeader("Persister", util.csvToRowData(conf.getProperty("persister_header")))
    util.writeRows("Persister", generatePersisterData(database))

    util.createSheet("DataStructure")
    util.writeSheetHeader("DataStructure", util.csvToRowData(conf.getProperty("datastructure_header")))
    util.writeRows("DataStructure", generateDSData(database))

    util.createSheet("ComputedFields")
    util.writeSheetHeader("ComputedFields", util.csvToRowData(conf.getProperty("computedFields_header")))
    util.writeRows("ComputedFields", generateComputedFieldsData(database))

    util.createSheet("TechnicalColumns")
    util.writeSheetHeader("TechnicalColumns", util.csvToRowData(conf.getProperty("techColumns_header")))
    util.writeRows("TechnicalColumns", generateTechColumnData(database))

    if (argv(3).toUpperCase.charAt(0) == 'Y') {
      util.createSheet("Fixed")
      util.writeSheetHeader("Fixed", util.csvToRowData(conf.getProperty("fixedReader_header")))
      util.writeRows("Fixed", generateFixedReaderData(database))
    }
    util.saveAsExcel(argv(2))
    util.flushCurrentWB()
  }

  def transformDataType(sourceDType: String): String = {
    val srcDataType = sourceDType.toLowerCase
    val tgtDataType = if (srcDataType.matches("^int.*")) "bigint"
    else if (srcDataType.matches("^decimal.*|^number.*")) "double"
    else if (srcDataType.matches("^float.*|^double.*")) "double"
    else if (srcDataType.matches("^time.*")) "timestamp"
    else if (srcDataType.matches("^date.*")) "date"
    else if (srcDataType.matches("^bool.*")) "boolean"
    else if (srcDataType.matches("^varchar.*")) "string"
    else "string"
    tgtDataType
  }

  def generatePersisterData(dbMasterObj: Parser.Database): ArrayList[Array[AnyRef]] = {
    val dataArray = new ArrayList[Array[AnyRef]]
    var idx = 0
    import scala.collection.JavaConversions._
    for (tbl <- database.getTables.values) {
      import scala.collection.JavaConversions._
      for (col <- tbl.getColumns.values) {
        dataArray.add({
          idx += 1;
          idx - 1
        }, Array[AnyRef](tbl.getName, col.name, transformDataType(col.datatypeName), ".  .  .  .", col.isPK, null, null))
      }
    }
    dataArray
  }

  def generateDSData(dbMasterObj: Parser.Database): ArrayList[Array[AnyRef]] = {
    val dataArray = new ArrayList[Array[AnyRef]]
    var idx = 0
    import scala.collection.JavaConversions._
    for (tbl <- database.getTables.values) {
      import scala.collection.JavaConversions._
      for (col <- tbl.getColumns.values) {
        dataArray.add({
          idx += 1;
          idx - 1
        }, Array[AnyRef](tbl.getName, col.name, transformDataType(col.datatypeName), null, null))
      }
    }
    dataArray
  }

  def generateComputedFieldsData(dbMasterObj: Parser.Database): ArrayList[Array[AnyRef]] = {
    val dataArray = new ArrayList[Array[AnyRef]]
    var idx = 0
    import scala.collection.JavaConversions._
    for (tbl <- database.getTables.values) {
      dataArray.add({
        idx += 1;
        idx - 1
      }, Array[AnyRef](tbl.getName, null, null, null, null))
    }
    dataArray
  }

  def generateTechColumnData(dbMasterObj: Parser.Database): ArrayList[Array[AnyRef]] = {
    val dataArray = new ArrayList[Array[AnyRef]]
    dataArray.add(Array[AnyRef]("DLL_VALIDITE", "int", "LifeCycle", null, null, null))
    dataArray.add(Array[AnyRef]("DLL_CHECKSUM", "int", "Checksum of all columns except technicals columns", null, null, null))
    dataArray.add(Array[AnyRef]("DLL_DATE_MAJ", "date", "Update Date", null, null, null))
    dataArray.add(Array[AnyRef]("DLL_DATE_CREATION", "date", "Date of creation", null, null, null))
    dataArray
  }

  def generateFixedReaderData(dbMasterObj: Parser.Database): ArrayList[Array[AnyRef]] = {
    val dataArray = new ArrayList[Array[AnyRef]]
    var idx = 0
    import scala.collection.JavaConversions._
    for (tbl <- database.getTables.values) {
      import scala.collection.JavaConversions._
      for (col <- tbl.getColumns.values) {
        dataArray.add({
          idx += 1;
          idx - 1
        }, Array[AnyRef](tbl.getName, col.name, null, null))
      }
    }
    dataArray
  }
}
