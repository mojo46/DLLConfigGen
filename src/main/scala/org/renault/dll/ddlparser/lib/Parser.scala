package org.renault.dll.ddlparser.lib

import org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_LENGTH
import org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_NAME
import org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_PRECISION
import org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_SCALE
import org.modeshape.sequencer.ddl.StandardDdlLexicon.DEFAULT_VALUE
import org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_COLUMN_DEFINITION
import org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT
import org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_TABLE_CONSTRAINT

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.PrintStream
import java.util

import org.modeshape.sequencer.ddl.DdlParsers
import org.modeshape.sequencer.ddl.StandardDdlLexicon
import org.modeshape.sequencer.ddl.node.AstNode

import org.apache.log4j.Level
import org.apache.log4j.Logger

object Parser {
  val value = null
  val logger = Logger.getLogger("Parser")


  def main(argv: Array[String]): Unit = { // Process the arguments ...
    var filename: String = null
    for (arg <- argv) { // First non-flag parameter is the filename ...
      if (filename == null) filename = arg
    }
    // Figure out if the arguments are valid ...
    if (filename == null) {
      printUsage(System.out)
      System.exit(1)
    }
    val file = new File(filename)
    if (!file.exists) printError(-1, "The file \"" + filename + "\" does not exist.")
    if (!file.isFile) printError(-2, "File could not be found at \"" + file + "\"")
    if (!file.canRead) printError(-3, "Unable to read file \"" + filename + "\".")
    // Now parse the file ...
    try {
      val parser = this
      parser.parse(file)
    } catch {
      case t: Throwable =>
        printError(-10, t.getMessage)
    }
  }

  def printError(exitCode: Int, message: String): Unit = {
    System.err.println("Error: " + message)
    System.err.println()
    printUsage(System.out)
    System.exit(exitCode)
  }

  def printUsage(stream: PrintStream): Unit = {
    stream.println("Usage:   <JAR_FILE_NAME.jar> filename")
    stream.println()
    stream.println("   where")
    stream.println("     filename         is the name of the DDL file to be parsed")
    stream.println()
  }

  @throws(classOf[IOException])
  def readFile(file: File): String = {
    val fileData = new StringBuffer(1000)
    val buffReader = new BufferedReader(new FileReader(file))
    try {
      Stream.continually(buffReader.readLine()).takeWhile(_ != null).foreach(s => (println("\n\nfile data append s==="+fileData.append(s))))
    } finally buffReader.close()

    fileData.toString
  }

  abstract class NamedComponent protected(val name: String) {
    def getName: String = this.name
  }

  def setDebugLevel(): Unit = {
    logger.setLevel(Level.DEBUG)
  }

  class Table(override val name: String) extends Parser.NamedComponent(name) with Parser.ColumnContainer {
    final val columns = new util.LinkedHashMap[String, Parser. Column]

    override def getColumn(name: String, createIfMissing: Boolean): Parser.Column = {
      var column = columns.get(name)
      if (column == null && createIfMissing) {
        column = new Parser.Column(name)
        columns.put(name, column)
      }
      column
    }

    def setPK(name: String): Unit = {
      val column = columns.get(name)
      column.isPK = true
    }

    override def getColumns: util.Map[String, Parser.Column] = columns
  }

  class View(override val name: String) extends Parser.NamedComponent(name) {
    protected var expression: String = null
  }

  class Column(override val name: String) extends Parser.NamedComponent(name) {
    var length: Int = 0
    var datatypeName: String = null
    var precision: Int = 0
    var scale: Int = 0
    var defaultValue: String = null
    var isPK: java.lang.Boolean = false
  }

  trait SchemaContainer {
    def getSchema(name: String, createIfMissing: Boolean): Parser.Schema

    def getSchemas: util.Map[String, Parser.Schema]
  }

  trait TableContainer {
    def getTable(name: String, createIfMissing: Boolean): Parser.Table

    def getTables: util.Map[String, Parser.Table]
  }

  trait ViewContainer {
    def getView(name: String, createIfMissing: Boolean): Parser.View

    def getViews: util.Map[String, Parser.View]
  }

  trait ColumnContainer {
    def getColumn(name: String, createIfMissing: Boolean): Parser.Column

    def getColumns: util.Map[String, Parser.Column]
  }

  class Schema(override val name: String) extends Parser.NamedComponent(name) with Parser.TableContainer with Parser.ViewContainer {
    final protected val tables = new util.LinkedHashMap[String, Parser.Table]
    final protected val views = new util.LinkedHashMap[String, Parser.View]

    /**
      * {@inheritDoc }
      *
      * @see org.renault.ddl.Parser.TableContainer#getTable(java.lang.String, boolean)
      */
    override def getTable(name: String, createIfMissing: Boolean): Parser.Table = {
      var table = tables.get(name)
      if (table == null && createIfMissing) {
        table = new Parser.Table(name)
        tables.put(name, table)
      }
      table
    }

    /**
      * {@inheritDoc }
      *
      * @see org.renault.ddl.Parser.TableContainer#getTables()
      */
    override def getTables: util.Map[String, Parser.Table] = tables

    /**
      * {@inheritDoc }
      *
      * @see org.renault.ddl.Parser.ViewContainer#getView(java.lang.String, boolean)
      */
    override def getView(name: String, createIfMissing: Boolean): Parser.View = {
      var view = views.get(name)
      if (view == null && createIfMissing) {
        view = new Parser.View(name)
        views.put(name, view)
      }
      view
    }

    /**
      * {@inheritDoc }
      *
      * @see org.renault.ddl.Parser.ViewContainer#getViews()
      */
    override def getViews: util.Map[String, Parser.View] = views
  }

  class Database(override val name: String) extends Parser.NamedComponent(name) with Parser.TableContainer with Parser.ViewContainer with Parser.SchemaContainer {
    final protected val tables = new util.LinkedHashMap[String, Parser.Table]
    final protected val views = new util.LinkedHashMap[String, Parser.View]
    final protected val schemas = new util.LinkedHashMap[String, Parser.Schema]

    override def getTable(name: String, createIfMissing: Boolean): Parser.Table = {
      var table = tables.get(name)
      if (table == null && createIfMissing) {
        table = new Parser.Table(name)
        tables.put(name, table)
      }
      table
    }

    override def getTables: util.Map[String, Parser.Table] = tables

    override def getView(name: String, createIfMissing: Boolean): Parser.View = {
      var view = views.get(name)
      if (view == null && createIfMissing) {
        view = new Parser.View(name)
        views.put(name, view)
      }
      view
    }

    override def getViews: util.Map[String, Parser.View] = views

    /**
      * {@inheritDoc }
      *
      * @see org.renault.ddl.Parser.SchemaContainer#getSchema(java.lang.String, boolean)
      */
    override def getSchema(name: String, createIfMissing: Boolean): Parser.Schema = {
      var schema = schemas.get(name)
      if (schema == null && createIfMissing) {
        schema = new Parser.Schema(name)
        schemas.put(name, schema)
      }
      schema
    }

    /**
      * {@inheritDoc }
      *
      * @see org.renault.ddl.Parser.SchemaContainer#getSchemas()
      */
    override def getSchemas: util.Map[String, Parser.Schema] = schemas
  }


  @throws(classOf[IOException])
  def parse(file: File): Parser.Database = { // Read the file into a single string ...
    val ddl = Parser.readFile(file)
    // Create the object that will parse the file ...
    val parsers = new DdlParsers
    val node = parsers.parse(ddl, file.getName)
    // Now process the AST ...
    System.out.println(node.toString)
    processStatements(node)
  }

  /**
    * Process the top-level 'ddl:statements' node, which contains information about the parsed content.
    *
    * @param node the node; may not be null
    * @return the database
    */
  def processStatements(node: AstNode): Parser.Database = {

    assert(node.getName == StandardDdlLexicon.STATEMENTS_CONTAINER)
    // Get the dialect that we've inferred ...
    val dialectId = string(node.getProperty(StandardDdlLexicon.PARSER_ID))
    // Now process the children of the statements node ...
    val database = new Parser.Database(dialectId)
    processChildrenOf(node, database)
    database
  }

  def processChildrenOf(node: AstNode, parent: Parser.NamedComponent): Unit = {
    import scala.collection.JavaConversions._
    for (child <- node) {
      process(child, parent)
    }
  }

  def process(node: AstNode, parent: Parser.NamedComponent): Unit = {
    val mixin = string(node.getProperty("jcr:mixinTypes"))
    // There are lots of different types of AST nodes, but we're only going to process a few ...
    println("Mixin : "+mixin)
    if (TYPE_CREATE_TABLE_STATEMENT == mixin) processCreateTable(node, parent.asInstanceOf[Parser.TableContainer])
    else if (TYPE_COLUMN_DEFINITION == mixin) processColumnDefinition(node, parent.asInstanceOf[Parser.ColumnContainer])
    else if (TYPE_TABLE_CONSTRAINT == mixin) processPrimaryKeyConstraint(node, parent.asInstanceOf[Parser.Table])
  }

  def processPrimaryKeyConstraint(node: AstNode, parent: Parser.Table): Unit = {
    val name = string(node.getName)
    if (name.contains("PK")) {
      println("Update PK \"" + name + "\"")
      node.getChildren.forEach((a: AstNode) => parent.setPK(a.getName))
    }
  }

  def processCreateTable(node: AstNode, parent: Parser.TableContainer): Unit = {
    val name = string(node.getName)
    println("Create table \"" + name + "\"")
    val names = name.split("\\.")
    val table = parent.getTable(if (names.length == 2) names(1)
    else names(0), true)
    processChildrenOf(node, table)
  }

  def processColumnDefinition(node: AstNode, parent: Parser.ColumnContainer): Unit = {
    val name = string(node.getName)
    println("Create column \"" + name + "\"")
    val column = parent.getColumn(name, true)
    var prop = node.getProperty(DATATYPE_LENGTH)
    if (prop != null) column.length = number(prop)
    prop = node.getProperty(DATATYPE_NAME)
    if (prop != null) column.datatypeName = string(prop)
    prop = node.getProperty(DATATYPE_PRECISION)
    if (prop != null) column.precision = number(prop)
    prop = node.getProperty(DATATYPE_SCALE)
    if (prop != null) column.scale = number(prop)
    prop = node.getProperty(DEFAULT_VALUE)
    if (prop != null) column.defaultValue = string(prop)
    column.isPK = false
  }

  /**
    * Convenience method to transform a property value into a string representation. Property values are often able to be
    * tranformed into multiple types, so the ModeShape Graph API is designed so that you always convert the values into the
    * desired type.
    *
    * @param value the property value; may be null
    * @return the string representation of the value, or null if the value was null
    */
  def string(value: Any): String = if (value == null) null
  else value.toString

  def number(value: Any): Int = {
    if (value == null) 0
    else if (value.isInstanceOf[Long]) value.asInstanceOf[Long].toInt
    else value.asInstanceOf[Int]
  }
}