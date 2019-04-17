package org.renault.dll.ddlparser.util

import org.apache.poi.ss.usermodel._
import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFWorkbook}
import org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util
object ExcelUtils {

  var workbook: XSSFWorkbook = _
  var headerStyle: XSSFCellStyle = _

  def apply():ExcelUtils.type = {

    this.workbook = new XSSFWorkbook
    this.headerStyle = this.workbook.createCellStyle
    this.headerStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex)
    this.headerStyle.setFillPattern(SOLID_FOREGROUND)

    this
  }

  def  createSheet(sheetName: String): Unit = {
    workbook.createSheet(sheetName)
  }

  def writeSheetHeader(sheetName: String, cellValues: Array[AnyRef]): Unit = {
    val row = workbook.getSheet(sheetName).createRow(0)
    var colNum = 0
    for (cellData <- cellValues) {
      val cell = row.createCell({
        colNum += 1; colNum - 1
      })
      if (cellData.isInstanceOf[String]) {
        cell.setCellValue(cellData.asInstanceOf[String])
        cell.setCellStyle(headerStyle)
      }
      else if (cellData.isInstanceOf[Integer]) {
        cell.setCellValue(cellData.asInstanceOf[Long])
        cell.setCellStyle(headerStyle)
      }
    }
  }

  def writeRows(sheetName: String, rows: util.ArrayList[Array[AnyRef]]): Unit = {
    var rowNum = 1
    val activeSheet = workbook.getSheet(sheetName)
    import scala.collection.JavaConversions._
    for (eachRowData <- rows) {
      val row = activeSheet.createRow({
        rowNum += 1; rowNum - 1
      })
      var colNum = 0
      for (field <- eachRowData) {
        val cell = row.createCell({
          colNum += 1; colNum - 1
        })
        if (field.isInstanceOf[String]) cell.setCellValue(field.asInstanceOf[String])
        else if (field.isInstanceOf[Integer]) cell.setCellValue(field.asInstanceOf[Long])
        else if (field.isInstanceOf[Boolean]) cell.setCellValue(field.toString)
          else if (field == null) cell.setCellType(CellType.BLANK)
      }
    }
  }

  def  csvToRowData(csv: String): Array[AnyRef] = {
    var arr: Array[AnyRef] = null

    arr = util.Arrays.stream(csv.split(",")).toArray
    arr
  }

  def flushCurrentWB(): Unit = {
    workbook = null
  }

  def saveAsExcel(filename: String): Unit = {
    try {
      val outputStream = new FileOutputStream(filename)
      workbook.write(outputStream)
      workbook.close()
    } catch {
      case e: FileNotFoundException =>
        e.printStackTrace()
      case e: IOException =>
        e.printStackTrace()
    }
  }
}