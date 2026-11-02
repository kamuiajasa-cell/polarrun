[1mdiff --git a/huawei/src/main/java/com/joyodigitama/huaweiwear/SqlConnection.kt b/huawei/src/main/java/com/joyodigitama/huaweiwear/SqlConnection.kt[m
[1mindex 50b1d34..497cbad 100644[m
[1m--- a/huawei/src/main/java/com/joyodigitama/huaweiwear/SqlConnection.kt[m
[1m+++ b/huawei/src/main/java/com/joyodigitama/huaweiwear/SqlConnection.kt[m
[36m@@ -145,7 +145,7 @@[m [mprivate class AndroidSqlStatement([m
       }[m
       SQLiteCursor(driver, editTable, query)[m
     }[m
[31m-    return db.rawQueryWithFactory(factory, sql, null, null).also { cursor = it }[m
[32m+[m[32m    return db.rawQueryWithFactory(factory, sql, null, "").also { cursor = it }[m
   }[m
 [m
   override fun columnCount(): Int = if (isQuery) ensureCursor().columnCount else 0[m
