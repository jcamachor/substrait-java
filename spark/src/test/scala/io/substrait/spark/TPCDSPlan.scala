/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.substrait.spark

import org.apache.spark.sql.TPCDSBase
import org.apache.spark.sql.internal.SQLConf

class TPCDSPlan extends TPCDSBase with SubstraitPlanTestBase {

  private val runAllQueriesIncludeFailed = false
  override def beforeAll(): Unit = {
    super.beforeAll()
    sparkContext.setLogLevel("WARN")

    conf.setConf(SQLConf.DYNAMIC_PARTITION_PRUNING_ENABLED, false)
    // introduced in spark 3.4
    spark.conf.set("spark.sql.readSideCharPadding", "false")
  }

  // spotless:off
  val successfulSQL: Set[String] = Set("q1", "q3", "q4", "q5", "q7", "q8",
    "q11", "q12", "q13", "q14a", "q14b", "q15", "q16", "q18", "q19",
    "q20", "q21", "q22", "q23a", "q23b", "q24a", "q24b", "q25", "q26", "q27", "q28", "q29",
    "q30", "q31", "q32", "q33", "q36", "q37", "q38",
    "q40", "q41", "q42", "q43", "q44", "q46", "q48", "q49",
    "q50", "q52", "q54", "q55", "q56", "q58", "q59",
    "q60", "q61", "q62", "q65", "q66", "q67", "q68", "q69",
    "q70", "q71", "q73", "q76", "q77", "q79",
    "q80", "q81", "q82", "q85", "q86", "q87", "q88",
    "q90", "q91", "q92", "q93", "q94", "q95", "q96", "q97", "q98", "q99")
  // spotless:on

  tpcdsQueries.foreach {
    q =>
      if (runAllQueriesIncludeFailed || successfulSQL.contains(q)) {
        test(s"check simplified (tpcds-v1.4/$q)") {
          testQuery("tpcds", q)
        }
      } else {
        ignore(s"check simplified (tpcds-v1.4/$q)") {
          testQuery("tpcds", q)
        }
      }
  }

  test("window") {
    val qry = s"""(SELECT
                 |    item_sk,
                 |    rank()
                 |    OVER (
                 |      ORDER BY rank_col DESC) rnk
                 |  FROM (SELECT
                 |    ss_item_sk item_sk,
                 |    avg(ss_net_profit) rank_col
                 |  FROM store_sales ss1
                 |  WHERE ss_store_sk = 4
                 |  GROUP BY ss_item_sk
                 |  HAVING avg(ss_net_profit) > 0.9 * (SELECT avg(ss_net_profit) rank_col
                 |  FROM store_sales
                 |  WHERE ss_store_sk = 4
                 |    AND ss_addr_sk IS NULL
                 |  GROUP BY ss_store_sk)) V2) """.stripMargin
    assertSqlSubstraitRelRoundTrip(qry)
  }
}
