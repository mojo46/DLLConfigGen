import java.io.File
import org.scalatest.FunSuite
import org.renault.dll.ddlparser.lib.Parser.Database
import org.renault.dll.ddlparser.lib.Parser

class ParserTest extends FunSuite {

  var parser: Parser.type = _
  var database: Database = _

  test("ParserTest") {

    parser = Parser
    print(parser)
    //println(this.getClass.getClassLoader.getResource("postgres_create_tables.ddl").getFile)
    database = parser.parse(testFile(getClass.getClassLoader.getResource("postgres_create_tables.ddl").getFile))

    println("Database schemas####"+database.getSchema("car_brand",true).getViews)
    println("get tables ############"+database.getTables)
    println("get table car_btrand ############"+database.getTables.get("car_brand").getColumns)

    assert(database.getName === "POSTGRES")

    import collection.JavaConverters._

    assert(database.getTables.get("car_brand").getColumns.asScala.count(p => p._2.isPK) === 2)
    assert(database.getTables.get("car_country_pmsr").getColumns.asScala.count(p => p._2.isPK) === 1)

  }

  protected def testFile(path: String) = new File(path)
}