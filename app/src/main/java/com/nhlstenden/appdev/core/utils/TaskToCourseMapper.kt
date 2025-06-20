package com.nhlstenden.appdev.core.utils

object TaskToCourseMapper {
    fun mapTaskIdToCourseId(taskId: String): String {
        return when (taskId) {
            // HTML tasks
            "html_basics", "html_structure", "html_elements", "html_forms" -> "html"
            
            // CSS tasks  
            "css_basics", "css_selectors", "css_layout", "css_animations" -> "css"
            
            // SQL tasks
            "sql_basics", "sql_queries", "sql_joins", "sql_database" -> "sql"
            
            // JavaScript tasks
            "javascript_basics", "dom_manipulation", "functions_and_scope", "es6_features" -> "javascript"
            
            // Python tasks
            "python_basics", "data_structures", "python_oop", "libraries_and_modules" -> "python"
            
            // Java tasks  
            "java_fundamentals", "object_oriented_programming", "collections_and_generics", "exception_handling" -> "java"
            
            // Default fallback - extract course from task ID prefix
            else -> taskId.substringBefore("_")
        }
    }
} 