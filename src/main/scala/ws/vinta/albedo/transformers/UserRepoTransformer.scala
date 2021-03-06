package ws.vinta.albedo.transformers

import org.apache.spark.ml.Transformer
import org.apache.spark.ml.param.{ParamMap, StringArrayParam}
import org.apache.spark.ml.util.{DefaultParamsReadable, DefaultParamsWritable, Identifiable}
import org.apache.spark.sql.types._
import org.apache.spark.sql.{DataFrame, Dataset}
import ws.vinta.albedo.closures.UDFs._

class UserRepoTransformer(override val uid: String)
  extends Transformer with DefaultParamsWritable {

  def this() = {
    this(Identifiable.randomUID("userRepoTransformer"))
  }

  val inputCols: StringArrayParam = new StringArrayParam(this, "inputCols", "Input column names")

  def getInputCols: Array[String] = $(inputCols)

  def setInputCols(value: Array[String]): this.type = set(inputCols, value)

  override def transformSchema(schema: StructType): StructType = {
    $(inputCols).foreach((inputColName: String) => {
      require(schema.fieldNames.contains(inputColName), s"Input column $inputColName must exist.")
    })

    val newFields: Array[StructField] = Array(
      StructField("repo_language_index_in_user_recent_repo_languages", IntegerType, nullable = false),
      StructField("repo_language_count_in_user_recent_repo_languages", IntegerType, nullable = false)
    )
    StructType(schema.fields ++ newFields)
  }

  override def transform(dataset: Dataset[_]): DataFrame = {
    transformSchema(dataset.schema)

    import dataset.sparkSession.implicits._

    dataset
      .withColumn("repo_language_index_in_user_recent_repo_languages", repoLanguageIndexInUserRecentRepoLanguagesUDF($"repo_language", $"user_recent_repo_languages"))
      .withColumn("repo_language_count_in_user_recent_repo_languages", repoLanguageCountInUserRecentRepoLanguagesUDF($"repo_language", $"user_recent_repo_languages"))
  }

  override def copy(extra: ParamMap): UserRepoTransformer = {
    defaultCopy(extra)
  }
}

object UserRepoTransformer extends DefaultParamsReadable[UserRepoTransformer]